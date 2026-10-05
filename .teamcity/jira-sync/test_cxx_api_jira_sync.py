"""
The tests of cxx_api_jira_sync.py against a fake GitHub and a fake Jira (python3 -m unittest in this directory).
"""

import datetime
import io
import re
import unittest
import urllib.parse

import cxx_api_jira_sync as sync
from cxx_api_jira_sync import IN_DEVELOPMENT, RESOLVED, WAITING_FOR_BUILD, WAITING_FOR_REVIEW, HttpError

ABORTED = "10000"
REPORTED = "1"
CONFIRMED = "10001"
WAITING_FOR_TEST = "20001"
TESTING = "20002"

# The names and the status categories of the fake MDAPI workflow.
NAMES = {
    ABORTED: ("Aborted", "done"),
    REPORTED: ("Reported", "new"),
    CONFIRMED: ("Confirmed", "new"),
    IN_DEVELOPMENT: ("In development", "indeterminate"),
    WAITING_FOR_REVIEW: ("Waiting for review", "indeterminate"),
    WAITING_FOR_BUILD: ("Waiting for build", "done"),
    WAITING_FOR_TEST: ("Waiting for test", "done"),
    TESTING: ("Testing", "indeterminate"),
    RESOLVED: ("Resolved", "done"),
}

# The transitions of the fake MDAPI workflow: the status -> (the id, the name, the status it goes to).
WORKFLOW = {
    REPORTED: [("11", "Confirm", CONFIRMED)],
    CONFIRMED: [("61", "Start development", IN_DEVELOPMENT)],
    IN_DEVELOPMENT: [("71", "Pause development", CONFIRMED), ("151", "Send to review", WAITING_FOR_REVIEW)],
    WAITING_FOR_REVIEW: [("141", "Return to development", IN_DEVELOPMENT),
                         ("161", "Wait for Build", WAITING_FOR_BUILD)],
    # No direct transition to In development, as in MDAPI.
    WAITING_FOR_BUILD: [("171", "Wait for Test", WAITING_FOR_TEST), ("181", "Resolve", RESOLVED),
                        ("176", "Return to review", WAITING_FOR_REVIEW)],
    WAITING_FOR_TEST: [("191", "Start testing", TESTING), ("201", "Done", RESOLVED)],
    TESTING: [("211", "Pass", RESOLVED)],
}

# The resolutions that the fake transitions set (the merge sets "Waiting for build", as the real one does).
RESOLUTIONS = {WAITING_FOR_BUILD: "Waiting for build", RESOLVED: "Done"}

NOW = datetime.datetime(2026, 10, 5, 12, 0, tzinfo=datetime.timezone.utc)
GITHUB = "https://api.github.com/repos/dxFeed/dxfeed-graal-cxx-api/"
JIRA = "https://jira.test/rest/api/2/"


def status(status_id):
    name, category = NAMES[status_id]
    return {"id": status_id, "name": name, "statusCategory": {"key": category}}


def pull_request(number, title, branch="feature/x", state="open", draft=False, merged_at=None, base="main",
                 updated_at="2026-10-05T11:00:00Z"):
    return {
        "number": number,
        "title": title,
        "head": {"ref": branch, "sha": f"{number:040d}"},
        "base": {"ref": base},
        "state": state,
        "draft": draft,
        "merged_at": merged_at,
        "updated_at": updated_at,
        "html_url": f"https://github.com/dxFeed/dxfeed-graal-cxx-api/pull/{number}",
    }


def release(tag, prerelease=False, draft=False, published_at="2026-10-05T10:00:00Z"):
    return {
        "tag_name": tag,
        "prerelease": prerelease,
        "draft": draft,
        "published_at": published_at,
        "html_url": f"https://github.com/dxFeed/dxfeed-graal-cxx-api/releases/tag/{tag}",
        "body_html": "<ul>\n<li><strong>[MDAPI-1][C++]</strong> A wrapped<br>\nline</li>\n</ul>",
        "body_text": "[MDAPI-1][C++] A wrapped line",
    }


class FakeMailer:
    def __init__(self, to=("anatoly.kalin@devexperts.com",), cc=(), error=None):
        self.to = list(to)
        self.cc = list(cc)
        self.error = error
        self.sent = []

    recipients = sync.Mailer.recipients

    def send(self, subject, body_html, text):
        if self.error:
            raise self.error

        self.sent.append((subject, body_html, text))


class FakeServer:
    """GitHub and Jira: the state of the tests, and the changes that the sync makes."""

    def __init__(self):
        self.pulls = []
        self.events = []
        self.check_runs = {}  # the sha -> the check runs
        self.comments = {}  # the id -> (the ticket, the body)
        self.link_statuses = {}  # the URL -> the title of the status icon
        self.link_groups = {}  # the URL -> the relationship (the heading of the group of the link)
        self.link_resolved = {}  # the URL -> struck through
        self.releases = []
        self.compare = {}
        self.issues = {}
        self.versions = []
        self.permissions = {"BROWSE_PROJECTS": True, "TRANSITION_ISSUES": True, "EDIT_ISSUES": True,
                            "ADMINISTER_PROJECTS": True}
        self.changes = []
        self.links = []

    def issue(self, key, status_id, fix_versions=(), prop=None):
        self.issues[key] = {
            "status": status(status_id),
            "resolution": RESOLUTIONS.get(status_id),
            "fixVersions": list(fix_versions),
            "property": prop,
        }

    def version(self, version_id, name, released):
        self.versions.append({"id": version_id, "name": name, "released": released, "archived": False})

    def request(self, method, url, headers, body=None):
        if url.startswith(GITHUB):
            return self.github(url[len(GITHUB):])

        assert url.startswith(JIRA), url
        path = url[len(JIRA):]

        if method != "GET":
            self.changes.append((method, path.split("?")[0], body))

        return self.jira(method, path, body)

    def github(self, path):
        if path.startswith("pulls?state=open"):
            return [pr for pr in self.pulls if pr["state"] == "open"]

        if path.startswith("pulls?"):
            return self.pulls

        if path.startswith("pulls/"):
            return next(pr for pr in self.pulls if pr["number"] == int(path[len("pulls/"):]))

        if path.startswith("commits/") and "/check-runs" in path:
            return {"check_runs": self.check_runs.get(path.split("/")[1], [])}

        if path.startswith("events?"):
            return self.events

        if path.startswith("releases?"):
            return self.releases

        base, head = urllib.parse.unquote(path[len("compare/"):]).split("...")
        return {"commits": [{"commit": {"message": message}} for message in self.compare[(base, head)]]}

    def jira(self, method, path, body):
        if path.startswith("mypermissions?"):
            return {"permissions": {key: {"havePermission": value} for key, value in self.permissions.items()}}

        if path == "project/MDAPI/versions":
            return self.versions

        if path.startswith("search?"):
            jql = urllib.parse.parse_qs(path[len("search?"):])["jql"][0]
            version_id = re.match(r"fixVersion = (\S+)", jql).group(1)
            return {"issues": [
                {"key": key, "fields": {"status": issue["status"], "resolution": issue["resolution"], "summary": key}}
                for key, issue in sorted(self.issues.items()) if version_id in issue["fixVersions"]
            ]}

        if path == "version":
            version = {"id": str(900 + len(self.versions)), "name": body["name"], "released": False,
                       "archived": False}
            self.versions.append(version)
            return version

        if path.startswith("version/"):
            next(version for version in self.versions if version["id"] == path[len("version/"):]).update(body)
            return None

        match = re.match(r"issue/(MDAPI-\d+)(.*)", path)
        key, rest = match.groups()

        if key not in self.issues:
            raise HttpError(method, path, 404, "Issue Does Not Exist")

        issue = self.issues[key]

        if rest == "?fields=status":
            return {"fields": {"status": issue["status"]}}

        if rest == "?fields=fixVersions":
            names = {version["id"]: version["name"] for version in self.versions}
            return {"fields": {"fixVersions": [{"id": version_id, "name": names[version_id]}
                                               for version_id in issue["fixVersions"]]}}

        if rest == "/transitions" and method == "GET":
            return {"transitions": [
                {"id": transition_id, "name": name, "to": status(to)}
                for transition_id, name, to in WORKFLOW.get(issue["status"]["id"], [])
            ]}

        if rest == "/transitions":
            to = next(to for transition_id, name, to in WORKFLOW[issue["status"]["id"]]
                      if transition_id == body["transition"]["id"])
            issue["status"] = status(to)
            issue["resolution"] = RESOLUTIONS.get(to)
            return None

        if rest.startswith("/properties/"):
            if method == "PUT":
                issue["property"] = body
                return None

            if issue["property"] is None:
                raise HttpError(method, path, 404, "No property")

            return {"key": rest, "value": issue["property"]}

        if rest == "/remotelink":
            self.links.append((key, body["globalId"], body["object"]["title"]))
            self.link_statuses[body["globalId"]] = body["object"].get("status", {}).get("icon", {}).get("title")
            self.link_groups[body["globalId"]] = body.get("relationship", "links to")
            self.link_resolved[body["globalId"]] = body["object"].get("status", {}).get("resolved", False)
            return {"id": 1}

        if rest == "/comment" and method == "POST":
            comment_id = str(500 + len(self.comments))
            self.comments[comment_id] = (key, body["body"])
            return {"id": comment_id}

        if rest.startswith("/comment/") and method == "PUT":
            comment_id = rest[len("/comment/"):]

            if comment_id not in self.comments:
                raise HttpError(method, path, 404, "No comment")

            self.comments[comment_id] = (key, body["body"])
            return None

        if rest == "" and method == "PUT":
            for operation in body["update"]["fixVersions"]:
                if "remove" in operation:
                    issue["fixVersions"].remove(operation["remove"]["id"])
                else:
                    issue["fixVersions"].append(operation["add"]["id"])
            return None

        raise AssertionError(f"{method} {path}")


class SyncTest(unittest.TestCase):
    def setUp(self):
        self.server = FakeServer()
        self.out = io.StringIO()

    def run_sync(self, dry_run=False, mailer=None):
        log = sync.Log(self.out)
        github = sync.GitHub(self.server, None)
        jira = sync.Jira(self.server, "https://jira.test", "token")
        return sync.Sync(github, jira, log, dry_run, since=NOW - datetime.timedelta(hours=48), mailer=mailer,
                         build_url="https://dxcity.test/build/1").run()

    def transitions(self):
        return [path for method, path, body in self.server.changes if path.endswith("/transitions")]

    def status_of(self, key):
        return self.server.issues[key]["status"]["id"]

    # The pull requests.

    def test_opened_pull_request_sends_the_ticket_to_review_and_links_it(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1][C++] Something")]

        self.assertEqual(0, self.run_sync())

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual({"opened": [10]}, self.server.issues["MDAPI-1"]["property"])
        self.assertEqual([("MDAPI-1", self.server.pulls[0]["html_url"], "PR #10: [MDAPI-1][C++] Something")],
                         self.server.links)

    def test_the_key_of_the_branch_counts_too(self):
        self.server.issue("MDAPI-2", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(11, "No key in the title", branch="feature/MDAPI-2-warnings")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-2"))

    def test_draft_keeps_a_ticket_in_development(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Draft", draft=True)]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual([], self.transitions())
        self.assertEqual({"drafted": [10]}, self.server.issues["MDAPI-1"]["property"])

    def test_draft_starts_the_development_of_a_confirmed_ticket(self):
        self.server.issue("MDAPI-1", CONFIRMED)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Draft", draft=True)]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))

    def test_draft_returns_a_ticket_waiting_for_build_to_development_through_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, prop={"opened": [10], "merged": [10]})
        self.server.pulls = [pull_request(11, "[MDAPI-1] Next part", draft=True)]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual(["176", "141"], [body["transition"]["id"] for method, path, body in self.server.changes
                                          if path.endswith("/transitions")])

    def test_draft_on_a_ticket_waiting_for_build_without_the_way_back_is_a_problem(self):
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, prop={"opened": [10], "merged": [10]})
        self.server.pulls = [pull_request(11, "[MDAPI-1] Next part", draft=True)]
        workflow = dict(WORKFLOW)
        WORKFLOW[WAITING_FOR_REVIEW] = [("161", "Wait for Build", WAITING_FOR_BUILD)]

        try:
            self.assertEqual(1, self.run_sync())
        finally:
            WORKFLOW.clear()
            WORKFLOW.update(workflow)

        self.assertIn("no transition from Waiting for review to In development", self.out.getvalue())
        self.assertNotIn(11, self.server.issues["MDAPI-1"]["property"].get("drafted", []))  # not remembered

    def test_draft_leaves_a_ticket_in_review_while_another_pull_request_is_in_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [
            pull_request(11, "[MDAPI-1] Next part", draft=True),
            pull_request(10, "[MDAPI-1] First", updated_at="2026-10-05T10:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertIn("PR #10 is in review", self.out.getvalue())

    def test_merge_with_only_drafts_open_returns_to_development(self):
        # The draft was opened while the first PR was in review; now the first PR is merged.
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10], "drafted": [11]})
        self.server.pulls = [
            pull_request(10, "[MDAPI-1] First", state="closed", merged_at="2026-10-05T11:00:00Z"),
            pull_request(11, "[MDAPI-1] Next part", draft=True, updated_at="2026-10-05T09:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))

    def test_draft_marked_ready_goes_to_review(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT, prop={"drafted": [11]})
        self.server.pulls = [pull_request(11, "[MDAPI-1] Next part")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual("PR #11: [MDAPI-1] Next part", self.server.links[-1][2])  # no longer a draft

    def test_dry_run_of_a_draft_on_a_ticket_waiting_for_build(self):
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, prop={"opened": [10], "merged": [10]})
        self.server.pulls = [pull_request(11, "[MDAPI-1] Next part", draft=True)]

        self.run_sync(dry_run=True)

        output = self.out.getvalue()
        self.assertIn("[dry run] Return to review: Waiting for build -> Waiting for review", output)
        self.assertIn("[dry run] then to In development", output)
        self.assertEqual([], self.server.changes)

    def test_applied_pull_request_is_not_applied_again(self):
        # Moved back to In development by hand after the PR was applied.
        self.server.issue("MDAPI-1", IN_DEVELOPMENT, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] Something")]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual([], self.server.changes)

    def test_ticket_in_another_status_stays_but_the_pull_request_is_remembered(self):
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD)
        self.server.pulls = [pull_request(12, "[MDAPI-1] The next PR of the ticket")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual([], self.transitions())
        self.assertEqual({"opened": [12]}, self.server.issues["MDAPI-1"]["property"])

    def test_merged_pull_request_goes_through_review_to_build(self):
        # Opened and merged between two runs.
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Fast", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual({"opened": [10], "merged": [10], "bases": {"10": "main"}},
                         self.server.issues["MDAPI-1"]["property"])
        self.assertEqual("PR #10 (merged): [MDAPI-1] Fast", self.server.links[-1][2])
        self.assertTrue(self.server.link_resolved[self.server.pulls[0]["html_url"]])

    def test_merged_pull_request_after_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual([("MDAPI-1", self.server.pulls[0]["html_url"], "PR #10 (merged): [MDAPI-1] X")],
                         self.server.links)  # the link of the opened PR updated

    def test_merge_into_release_branch_counts(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z",
                                          base="release/v9")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))

    def test_merge_into_another_feature_branch_is_no_merge(self):
        # No PR of the feature branch is open (see the stacked PR test for one): the work goes on there.
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] Stacked", state="closed",
                                          merged_at="2026-10-05T10:00:00Z", base="feature/MDAPI-1-base")]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertNotIn("merged", self.server.issues["MDAPI-1"]["property"])

    def test_closed_without_merge_does_not_move_a_ticket_in_development(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Abandoned", state="closed")]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual([], self.transitions())
        self.assertEqual([], self.server.links)

    def test_closed_without_merge_returns_a_ticket_in_review_to_development(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] Abandoned", state="closed")]

        self.assertEqual(0, self.run_sync())

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual({"opened": [10], "closed": [10]}, self.server.issues["MDAPI-1"]["property"])

    def test_closed_without_merge_leaves_the_ticket_in_review_while_another_pull_request_is_in_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10, 11]})
        self.server.pulls = [
            pull_request(10, "[MDAPI-1] Abandoned", state="closed"),
            pull_request(11, "[MDAPI-1] Other", updated_at="2026-10-05T09:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertIn("PR #11 is in review", self.out.getvalue())

    def test_closed_without_merge_with_only_drafts_open_returns_to_development(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10], "drafted": [11]})
        self.server.pulls = [
            pull_request(10, "[MDAPI-1] Abandoned", state="closed"),
            pull_request(11, "[MDAPI-1] Draft", draft=True, updated_at="2026-10-05T09:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))

    def test_merge_waits_for_the_other_open_pull_requests(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10, 11]})
        self.server.pulls = [
            pull_request(11, "[MDAPI-1] Second", updated_at="2026-10-05T09:00:00Z"),
            pull_request(10, "[MDAPI-1] First", state="closed", merged_at="2026-10-05T10:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual({"opened": [10, 11], "merged": [10], "bases": {"10": "main"}},
                         self.server.issues["MDAPI-1"]["property"])
        self.assertIn("stays in Waiting for review: PR #11 is in review", self.out.getvalue())

    def test_open_draft_keeps_the_ticket_from_the_build(self):
        # The draft of the next part (its key in the branch) is open when the first PR is merged.
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [
            pull_request(10, "[MDAPI-1] First", state="closed", merged_at="2026-10-05T10:00:00Z"),
            pull_request(12, "Next part", branch="feature/MDAPI-1-next", draft=True,
                         updated_at="2026-10-05T09:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertNotIn("161", [body["transition"]["id"] for method, path, body in self.server.changes
                                 if path.endswith("/transitions")])

    def test_merge_of_the_last_open_pull_request_moves_to_build(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10, 11], "merged": [10]})
        self.server.pulls = [pull_request(11, "[MDAPI-1] Second", state="closed", merged_at="2026-10-05T11:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))

    def merged_into_review(self, fix_versions=()):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, fix_versions=fix_versions, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z")]

    def test_ticket_moved_to_build_gets_the_fix_version_of_the_next_release(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("200", "java-api-experimental v2.3.2", False)  # another component of MDAPI
        self.merged_into_review()

        self.assertEqual(0, self.run_sync())

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual(["101"], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_unreleased_fix_version_is_kept(self):
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)
        self.merged_into_review(fix_versions=["102"])

        self.run_sync()

        self.assertEqual(["102"], self.server.issues["MDAPI-1"]["fixVersions"])
        self.assertNotIn("WARNING", self.out.getvalue())

    def test_released_fix_version_gets_the_next_release_too(self):
        # A reopened ticket of an earlier release.
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.merged_into_review(fix_versions=["100"])

        self.run_sync()

        self.assertEqual(["100", "101"], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_no_single_next_release_is_a_warning(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.version("102", "graal-cxx-api v8.1.0", False)
        self.merged_into_review()

        self.assertEqual(0, self.run_sync())

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual([], self.server.issues["MDAPI-1"]["fixVersions"])
        self.assertIn("set the fix version by hand", self.out.getvalue())

    def test_missing_next_release_is_created(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.version("99", "graal-cxx-api v7.0.0", True)
        self.merged_into_review()

        self.assertEqual(0, self.run_sync())

        created = next(version for version in self.server.versions if version["name"] == "graal-cxx-api v8.0.1")
        self.assertFalse(created["released"])
        self.assertEqual([created["id"]], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_next_release_is_created_once_for_several_tickets(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.issue("MDAPI-2", WAITING_FOR_REVIEW, prop={"opened": [11]})
        self.server.pulls = [
            pull_request(11, "[MDAPI-2] Y", state="closed", merged_at="2026-10-05T11:00:00Z"),
            pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z",
                         updated_at="2026-10-05T10:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(1, len([path for method, path, body in self.server.changes if path == "version"]))
        self.assertEqual(self.server.issues["MDAPI-1"]["fixVersions"], self.server.issues["MDAPI-2"]["fixVersions"])

    def test_dry_run_logs_the_creation_of_the_next_release(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.merged_into_review()

        self.run_sync(dry_run=True)

        output = self.out.getvalue()
        self.assertIn("[dry run] create the version graal-cxx-api v8.0.1", output)
        self.assertIn("[dry run] fix version graal-cxx-api v8.0.1", output)
        self.assertEqual([], self.server.changes)

    def test_dry_run_logs_the_fix_version(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.merged_into_review()

        self.run_sync(dry_run=True)

        self.assertIn("[dry run] fix version graal-cxx-api v8.1.0", self.out.getvalue())
        self.assertEqual([], self.server.changes)

    # The lines of the versions: main (the current major) and release/vN (the next one).

    def merged_into(self, base, fix_versions=()):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, fix_versions=fix_versions, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z",
                                          base=base)]

    def test_merge_into_main_gets_the_version_of_the_current_major(self):
        self.merged_into("main")
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)

        self.assertEqual(0, self.run_sync())

        self.assertEqual(["101"], self.server.issues["MDAPI-1"]["fixVersions"])
        self.assertNotIn("WARNING", self.out.getvalue())

    def test_merge_into_release_branch_gets_the_version_of_its_major(self):
        self.merged_into("release/v9")
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)

        self.run_sync()

        self.assertEqual(["102"], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_merge_into_release_branch_creates_the_version_of_its_major(self):
        self.merged_into("release/v9")
        self.server.version("101", "graal-cxx-api v8.1.0", False)

        self.run_sync()

        created = next(version for version in self.server.versions if version["name"] == "graal-cxx-api v9.0.0")
        self.assertEqual([created["id"]], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_part_merged_into_release_branch_moves_the_ticket_to_the_next_major(self):
        # The ticket has got v8.1.0 for its part in main; its last part is breaking.
        self.merged_into("release/v9", fix_versions=["101"])
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)
        self.server.issues["MDAPI-1"]["property"] = {"opened": [9, 10], "merged": [9], "bases": {"9": "main"}}

        self.run_sync()

        self.assertEqual(["102"], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_part_merged_into_main_keeps_the_version_of_the_next_major(self):
        self.merged_into("main", fix_versions=["102"])
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)

        self.run_sync()

        self.assertEqual(["102"], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_ticket_held_in_review_gets_no_fix_version(self):
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10, 11]})
        self.server.pulls = [
            pull_request(11, "[MDAPI-1] Second", updated_at="2026-10-05T09:00:00Z"),
            pull_request(10, "[MDAPI-1] First", state="closed", merged_at="2026-10-05T10:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual([], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_closing_the_last_open_pull_request_without_merge_moves_to_build(self):
        # The first PR is merged, the second one is abandoned: the merged work waits for a build.
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10, 11], "merged": [10]})
        self.server.pulls = [pull_request(11, "[MDAPI-1] Second", state="closed")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual([11], self.server.issues["MDAPI-1"]["property"]["closed"])

    def test_stacked_pull_request_waits_for_its_base(self):
        # The second PR is merged into the branch of the first one, which is still open.
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10, 11]})
        self.server.pulls = [
            pull_request(11, "[MDAPI-1] Stacked", state="closed", merged_at="2026-10-05T10:00:00Z",
                         base="feature/MDAPI-1-base"),
            pull_request(10, "[MDAPI-1] Base", branch="feature/MDAPI-1-base", updated_at="2026-10-05T09:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))

        # The base is merged then.
        self.server.pulls = [pull_request(10, "[MDAPI-1] Base", branch="feature/MDAPI-1-base", state="closed",
                                          merged_at="2026-10-05T11:30:00Z")]
        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))

    def test_pull_requests_older_than_the_lookback_are_ignored(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Old", updated_at="2026-10-01T00:00:00Z")]

        self.run_sync()

        self.assertEqual([], self.server.changes)

    def test_old_merged_pull_request_with_a_new_comment_is_ignored(self):
        self.server.issue("MDAPI-1", RESOLVED)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Old", state="closed", merged_at="2026-06-24T18:50:54Z",
                                          updated_at="2026-10-05T11:00:00Z")]

        self.run_sync()

        self.assertEqual([], self.server.changes)

    def test_missing_transition_is_a_problem_and_is_tried_again(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] X")]
        workflow = dict(WORKFLOW)
        WORKFLOW[IN_DEVELOPMENT] = [("71", "Pause development", CONFIRMED)]

        try:
            self.assertEqual(1, self.run_sync())
        finally:
            WORKFLOW.clear()
            WORKFLOW.update(workflow)

        self.assertIn("buildProblem", self.out.getvalue())
        self.assertIsNone(self.server.issues["MDAPI-1"]["property"])  # not remembered

    def test_unknown_ticket_is_a_warning(self):
        self.server.pulls = [pull_request(10, "[MDAPI-999] Typo")]

        self.assertEqual(0, self.run_sync())

        self.assertIn("MDAPI-999: no such ticket", self.out.getvalue())

    def test_several_keys(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.issue("MDAPI-2", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1][MDAPI-2] Both")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-2"))

    def test_dry_run_changes_nothing_but_logs_the_changes(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Fast", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync(dry_run=True)

        self.assertEqual([], self.server.changes)
        output = self.out.getvalue()
        self.assertIn("[dry run] Send to review: In development -> Waiting for review", output)
        self.assertIn("[dry run] then to Waiting for build", output)
        self.assertNotIn("buildProblem", output)

    def test_dry_run_keeps_the_status_it_has_moved_the_ticket_to(self):
        # The first dry run of TeamCity: two merged PRs of MDAPI-427, the latest updated first.
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [
            pull_request(117, "[MDAPI-1] B", state="closed", merged_at="2026-10-04T20:36:57Z",
                         updated_at="2026-10-04T20:37:04Z"),
            pull_request(116, "[MDAPI-1] A", state="closed", merged_at="2026-10-04T14:31:20Z",
                         updated_at="2026-10-04T14:31:24Z"),
        ]

        self.run_sync(dry_run=True)

        output = self.out.getvalue()
        self.assertEqual(1, output.count("then to Waiting for build"))
        self.assertIn("stays in Waiting for build", output)
        self.assertNotIn("stays in Waiting for review", output)

    def test_pull_requests_are_applied_in_the_order_of_their_updates(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [
            pull_request(117, "[MDAPI-1] B", updated_at="2026-10-05T11:00:00Z"),
            pull_request(116, "[MDAPI-1] A", updated_at="2026-10-05T10:00:00Z"),
        ]

        self.run_sync()

        self.assertEqual({"opened": [116, 117]}, self.server.issues["MDAPI-1"]["property"])

    def test_only_the_needed_permissions_are_logged(self):
        self.server.permissions["ARCHIVE_ISSUES"] = False  # Jira returns all the permissions

        self.run_sync()

        self.assertNotIn("ARCHIVE_ISSUES", self.out.getvalue())

    def test_no_new_release_is_logged(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.releases = [release("v8.0.0")]

        self.run_sync()

        self.assertIn("No GitHub releases after the last released Jira version graal-cxx-api v8.0.0",
                      self.out.getvalue())

    def test_dry_run_checks_the_transition_of_a_merge(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync(dry_run=True)

        self.assertIn("[dry run] Wait for Build: Waiting for review -> Waiting for build", self.out.getvalue())

    # The branches and the start of the development.

    def branch_created(self, branch, time="2026-10-05T11:00:00Z", ref_type="branch"):
        self.server.events.append({"type": "CreateEvent", "created_at": time,
                                   "payload": {"ref": branch, "ref_type": ref_type}})

    def test_created_branch_starts_the_development(self):
        self.server.issue("MDAPI-1", CONFIRMED)
        self.branch_created("feature/MDAPI-1-something")

        self.assertEqual(0, self.run_sync())

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual({"branches": ["feature/MDAPI-1-something"]}, self.server.issues["MDAPI-1"]["property"])
        self.assertEqual([("MDAPI-1", "https://github.com/dxFeed/dxfeed-graal-cxx-api/tree/feature/MDAPI-1-something",
                           "Branch feature/MDAPI-1-something")], self.server.links)

    def test_created_branch_confirms_a_reported_ticket(self):
        self.server.issue("MDAPI-1", REPORTED)
        self.branch_created("bugfix/MDAPI-1")

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
        self.assertEqual(["11", "61"], [body["transition"]["id"] for method, path, body in self.server.changes
                                        if path.endswith("/transitions")])

    def test_any_branch_with_the_key_counts(self):
        self.server.issue("MDAPI-12345", CONFIRMED)
        self.branch_created("experiment/try-MDAPI-12345-quickly")

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-12345"))

    def test_created_branch_returns_a_ticket_waiting_for_build_to_development(self):
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, prop={"opened": [10], "merged": [10]})
        self.branch_created("feature/MDAPI-1-next-part")

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))

    def test_created_branch_leaves_a_ticket_in_review_while_a_pull_request_is_in_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] First", updated_at="2026-10-05T09:00:00Z")]
        self.branch_created("feature/MDAPI-1-next-part")

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))

    def test_created_branch_is_applied_once(self):
        self.server.issue("MDAPI-1", CONFIRMED)
        self.branch_created("feature/MDAPI-1")
        self.run_sync()
        self.server.issues["MDAPI-1"]["status"] = status(CONFIRMED)  # paused by hand

        self.run_sync()

        self.assertEqual(CONFIRMED, self.status_of("MDAPI-1"))

    def test_old_branches_and_tags_are_ignored(self):
        self.server.issue("MDAPI-1", CONFIRMED)
        self.branch_created("feature/MDAPI-1", time="2026-10-01T00:00:00Z")
        self.branch_created("MDAPI-1-tag", ref_type="tag")

        self.run_sync()

        self.assertEqual(CONFIRMED, self.status_of("MDAPI-1"))
        self.assertEqual([], self.server.changes)

    def test_branch_and_pull_request_are_applied_in_the_order_of_their_times(self):
        # The branch is created, then its PR opened, between two runs: the ticket goes through development to review.
        self.server.issue("MDAPI-1", CONFIRMED)
        self.server.pulls = [
            pull_request(10, "[MDAPI-1] X", branch="feature/MDAPI-1", updated_at="2026-10-05T11:30:00Z"),
        ]
        self.branch_created("feature/MDAPI-1", time="2026-10-05T11:00:00Z")

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual("branch", self.out.getvalue().split("MDAPI-1: ")[1].split()[0])

    def test_pull_request_opened_on_a_reported_ticket_goes_to_review(self):
        self.server.issue("MDAPI-1", REPORTED)
        self.server.pulls = [pull_request(10, "[MDAPI-1] X")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual(["11", "61", "151"], [body["transition"]["id"] for method, path, body in self.server.changes
                                               if path.endswith("/transitions")])

    def test_draft_on_a_reported_ticket_starts_the_development(self):
        self.server.issue("MDAPI-1", REPORTED)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Draft", draft=True)]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))

    def test_dry_run_of_the_start_of_a_reported_ticket(self):
        self.server.issue("MDAPI-1", REPORTED)
        self.branch_created("feature/MDAPI-1")

        self.run_sync(dry_run=True)

        output = self.out.getvalue()
        self.assertIn("[dry run] Confirm: Reported -> Confirmed", output)
        self.assertIn("[dry run] then to In development", output)
        self.assertEqual([], self.server.changes)

    # The groups of the links: a branch and its PRs.

    def test_branch_and_its_pull_request_are_in_the_group_of_the_branch(self):
        self.server.issue("MDAPI-1", CONFIRMED)
        self.branch_created("feature/MDAPI-1-x", time="2026-10-05T10:00:00Z")
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", branch="feature/MDAPI-1-x")]

        self.run_sync()

        self.assertEqual({
            "https://github.com/dxFeed/dxfeed-graal-cxx-api/tree/feature/MDAPI-1-x": "x",
            self.server.pulls[0]["html_url"]: "x",
        }, self.server.link_groups)

    def test_relink_puts_the_remembered_links_into_the_groups_with_the_status_of_the_checks(self):
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, prop={
            "opened": [10, 11], "merged": [10], "branches": ["feature/MDAPI-1-b"],
            "checks": {"11": {"state": "failure"}},
        })
        self.server.pulls = [
            pull_request(10, "[MDAPI-1] A", branch="feature/MDAPI-1-a", updated_at="2026-09-01T00:00:00Z"),
            pull_request(11, "[MDAPI-1] B", branch="feature/MDAPI-1-b", updated_at="2026-09-01T00:00:00Z"),
        ]
        log = sync.Log(self.out)
        github = sync.GitHub(self.server, None)
        jira = sync.Jira(self.server, "https://jira.test", "token")

        result = sync.Sync(github, jira, log, False, since=NOW).run(relink=["MDAPI-1"])

        self.assertEqual(0, result)
        urls = [pr["html_url"] for pr in self.server.pulls]
        self.assertEqual("a", self.server.link_groups[urls[0]])
        self.assertEqual("b", self.server.link_groups[urls[1]])
        self.assertEqual("Checks failed", self.server.link_statuses[urls[1]])
        self.assertIsNone(self.server.link_statuses[urls[0]])
        self.assertEqual("b", self.server.link_groups[
            "https://github.com/dxFeed/dxfeed-graal-cxx-api/tree/feature/MDAPI-1-b"])
        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))  # links only

    def test_draft_pull_request_is_linked_as_a_draft(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Draft", draft=True)]

        self.run_sync()

        self.assertEqual("PR #10 (draft): [MDAPI-1] Draft", self.server.links[-1][2])
        self.assertFalse(self.server.link_resolved[self.server.pulls[0]["html_url"]])

    def test_closed_pull_request_is_struck_through(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] Abandoned", state="closed")]

        self.run_sync()

        self.assertEqual("PR #10 (closed): [MDAPI-1] Abandoned", self.server.links[-1][2])
        self.assertTrue(self.server.link_resolved[self.server.pulls[0]["html_url"]])

    def test_merged_pull_request_keeps_the_icon_of_its_checks(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10], "checks": {"10": {"state": "failure"}}})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        url = self.server.pulls[0]["html_url"]
        self.assertEqual("Checks failed", self.server.link_statuses[url])
        self.assertTrue(self.server.link_resolved[url])

    def test_group_heading_is_the_distinct_part_of_the_branch(self):
        # Jira cuts the heading at about 20 characters, and the branches of a ticket share feature/MDAPI-NNN-.
        self.assertEqual("test-runtime-copy", sync.link_group("feature/MDAPI-427-test-runtime-copy"))
        self.assertEqual("memory-pool", sync.link_group("spike/MDAPI-240-memory-pool"))
        self.assertEqual("MDAPI-411", sync.link_group("bugfix/MDAPI-411"))
        self.assertEqual("x", sync.link_group("MDAPI-5-x"))

    def test_pull_requests_in_the_log(self):
        self.assertEqual("PR #118 is", sync.prs([118]))
        self.assertEqual("PRs #1, #2 are", sync.prs([1, 2]))

    # The checks of the PRs.

    def checks(self, number, *runs):
        """The check runs of the PR: (the name, the status, the conclusion)."""
        self.server.check_runs[f"{number:040d}"] = [
            {"name": name, "status": run_status, "conclusion": conclusion, "html_url": f"https://ci/{name}"}
            for name, run_status, conclusion in runs
        ]

    def open_pull_request_in_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", updated_at="2026-10-05T09:00:00Z")]
        return self.server.pulls[0]["html_url"]

    def check_comments(self):
        return [body for key, body in self.server.comments.values()]

    def test_passed_checks_mark_the_link_and_comment_once(self):
        url = self.open_pull_request_in_review()
        self.checks(10, ("linux", "completed", "success"), ("windows", "completed", "skipped"))

        self.assertEqual(0, self.run_sync())
        self.run_sync()  # nothing changed

        self.assertEqual("Checks passed", self.server.link_statuses[url])
        self.assertEqual(1, len(self.server.comments))
        self.assertTrue(self.check_comments()[0].startswith(f"(/) [PR #10|{url}]: checks passed"))
        self.assertEqual({"state": "success", "result": "success", "comment": "500"},
                         self.server.issues["MDAPI-1"]["property"]["checks"]["10"])

    def test_failed_checks_edit_the_same_comment_with_the_failed_jobs(self):
        url = self.open_pull_request_in_review()
        self.checks(10, ("linux", "completed", "success"))
        self.run_sync()
        self.checks(10, ("linux", "completed", "failure"), ("windows", "completed", "timed_out"))

        self.run_sync()

        self.assertEqual("Checks failed", self.server.link_statuses[url])
        self.assertEqual(1, len(self.server.comments))  # edited, not added
        comment = self.check_comments()[0]
        self.assertTrue(comment.startswith("(x) "))
        self.assertIn("* [linux|https://ci/linux]: failure", comment)
        self.assertIn("* [windows|https://ci/windows]: timed_out", comment)

    def test_running_checks_change_the_icon_only(self):
        url = self.open_pull_request_in_review()
        self.checks(10, ("linux", "completed", "success"))
        self.run_sync()
        comment = self.check_comments()[0]
        self.checks(10, ("linux", "in_progress", None))

        self.run_sync()

        self.assertEqual("Checks are running", self.server.link_statuses[url])
        self.assertEqual([comment], self.check_comments())

        self.checks(10, ("linux", "completed", "success"))
        changes = len(self.server.changes)
        self.run_sync()

        self.assertEqual("Checks passed", self.server.link_statuses[url])
        self.assertEqual([comment], self.check_comments())  # the same result: no edit, no mail
        self.assertNotIn("/comment/500", [path for method, path, body in self.server.changes[changes:]])

    def test_first_running_checks_make_no_comment(self):
        url = self.open_pull_request_in_review()
        self.checks(10, ("linux", "queued", None))

        self.run_sync()

        self.assertEqual("Checks are running", self.server.link_statuses[url])
        self.assertEqual({}, self.server.comments)

    def test_deleted_comment_is_added_again(self):
        self.open_pull_request_in_review()
        self.checks(10, ("linux", "completed", "success"))
        self.run_sync()
        self.server.comments.clear()  # deleted by hand
        self.checks(10, ("linux", "completed", "failure"))

        self.assertEqual(0, self.run_sync())

        self.assertEqual(1, len(self.server.comments))
        self.assertTrue(self.check_comments()[0].startswith("(x) "))

    def test_pull_request_without_checks_or_keys_is_skipped(self):
        self.open_pull_request_in_review()
        self.server.pulls.append(pull_request(11, "Bump something", branch="dependabot/x"))
        self.checks(11, ("linux", "completed", "failure"))

        self.run_sync()

        self.assertEqual({}, self.server.comments)
        self.assertEqual({}, self.server.link_statuses)

    def test_dry_run_of_the_checks(self):
        self.open_pull_request_in_review()
        self.checks(10, ("linux", "completed", "failure"))

        self.run_sync(dry_run=True)

        output = self.out.getvalue()
        self.assertIn("[dry run] link status: Checks failed", output)
        self.assertIn("[dry run] comment the checks", output)
        self.assertEqual([], self.server.changes)

    def test_checks_state(self):
        self.assertEqual((None, []), sync.checks_state([]))
        runs = [{"name": "a", "status": "completed", "conclusion": "neutral"},
                {"name": "b", "status": "completed", "conclusion": "cancelled"}]
        self.assertEqual(("failure", [runs[1]]), sync.checks_state(runs))
        queued = {"name": "c", "status": "queued", "conclusion": None}
        self.assertEqual("pending", sync.checks_state([runs[0], queued])[0])

    # The releases.

    def released_jira_and_github(self):
        self.server.version("100", "graal-cxx-api v8.0.0", True)
        self.server.releases = [release("v8.0.0", published_at="2026-09-07T10:00:00Z")]

    def test_release_resolves_the_tickets_moves_the_unfinished_and_releases_the_version(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.issue("MDAPI-2", IN_DEVELOPMENT, fix_versions=["101"])
        self.server.issue("MDAPI-3", RESOLVED, fix_versions=["101"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] X (#20)", "[MDAPI-3] Y (#21)"]

        self.assertEqual(0, self.run_sync())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-2"))
        next_version = next(version for version in self.server.versions
                            if version["name"] == "graal-cxx-api v8.1.1")
        self.assertEqual([next_version["id"]], self.server.issues["MDAPI-2"]["fixVersions"])
        self.assertEqual(["101"], self.server.issues["MDAPI-1"]["fixVersions"])
        released = next(version for version in self.server.versions if version["id"] == "101")
        self.assertTrue(released["released"])
        self.assertEqual("2026-10-05", released["releaseDate"])
        self.assertIn("MDAPI-2 has the fix version v8.1.0 but no commits", self.out.getvalue())

    def test_release_resolves_waiting_for_test_and_leaves_testing_and_aborted(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_TEST, fix_versions=["101"])
        self.server.issue("MDAPI-2", TESTING, fix_versions=["101"])
        self.server.issue("MDAPI-3", ABORTED, fix_versions=["101"])
        self.server.issue("MDAPI-4", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] A", "[MDAPI-2] B", "[MDAPI-4] D"]

        self.assertEqual(0, self.run_sync())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual(TESTING, self.status_of("MDAPI-2"))
        self.assertEqual(ABORTED, self.status_of("MDAPI-3"))
        self.assertEqual(RESOLVED, self.status_of("MDAPI-4"))
        self.assertEqual(["181"], [body["transition"]["id"] for method, path, body in self.server.changes
                                   if path == "issue/MDAPI-4/transitions"])  # Resolve, not Send to test
        for key in ("MDAPI-1", "MDAPI-2", "MDAPI-3", "MDAPI-4"):
            self.assertEqual(["101"], self.server.issues[key]["fixVersions"], key)

    def test_new_major_takes_the_tickets_of_the_earlier_line_and_archives_its_versions(self):
        # Released v9.0.0 instead of v8.1.0: main had release/v9 merged.
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_TEST, fix_versions=["101"])
        self.server.issue("MDAPI-2", IN_DEVELOPMENT, fix_versions=["101"])
        self.server.issue("MDAPI-3", WAITING_FOR_BUILD, fix_versions=["102"])
        self.server.releases.append(release("v9.0.0"))
        self.server.compare[("v8.0.0", "v9.0.0")] = ["[MDAPI-1] A", "[MDAPI-3] C"]

        self.assertEqual(0, self.run_sync())

        versions = {version["name"]: version for version in self.server.versions}
        self.assertTrue(versions["graal-cxx-api v8.1.0"]["archived"])
        self.assertTrue(versions["graal-cxx-api v9.0.0"]["released"])
        self.assertEqual(["102"], self.server.issues["MDAPI-1"]["fixVersions"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual([versions["graal-cxx-api v9.0.1"]["id"]], self.server.issues["MDAPI-2"]["fixVersions"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-3"))

    def test_new_major_without_its_version_creates_it(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.releases.append(release("v9.0.0"))
        self.server.compare[("v8.0.0", "v9.0.0")] = ["[MDAPI-1] A"]

        self.run_sync()

        versions = {version["name"]: version for version in self.server.versions}
        self.assertTrue(versions["graal-cxx-api v9.0.0"]["released"])
        self.assertTrue(versions["graal-cxx-api v8.1.0"]["archived"])
        self.assertEqual([versions["graal-cxx-api v9.0.0"]["id"]], self.server.issues["MDAPI-1"]["fixVersions"])

    def test_release_of_the_current_major_leaves_the_next_one(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.issue("MDAPI-2", WAITING_FOR_BUILD, fix_versions=["102"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] A"]

        self.run_sync()

        versions = {version["id"]: version for version in self.server.versions}
        self.assertEqual("graal-cxx-api v8.1.0", versions["101"]["name"])  # the placeholder of the line, renamed
        self.assertTrue(versions["101"]["released"])
        self.assertFalse(versions["102"]["released"] or versions["102"]["archived"])
        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-2"))

    def test_waiting_for_test_without_a_transition_to_resolved_is_a_warning(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_TEST, fix_versions=["101"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] A"]
        workflow = dict(WORKFLOW)
        WORKFLOW[WAITING_FOR_TEST] = [("191", "Start testing", TESTING)]

        try:
            self.assertEqual(0, self.run_sync())
        finally:
            WORKFLOW.clear()
            WORKFLOW.update(workflow)

        self.assertEqual(WAITING_FOR_TEST, self.status_of("MDAPI-1"))  # not through Testing
        self.assertIn("resolve it by hand", self.out.getvalue())
        self.assertNotIn("buildProblem", self.out.getvalue())
        self.assertTrue(next(version for version in self.server.versions if version["id"] == "101")["released"])

    def test_release_renames_the_placeholder(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] Minor (#20)"]

        self.run_sync()

        names = {version["id"]: version["name"] for version in self.server.versions}
        self.assertEqual("graal-cxx-api v8.1.0", names["101"])
        self.assertIn("graal-cxx-api v8.1.1", names.values())
        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))

    def test_release_uses_the_version_of_the_name(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.version("102", "graal-cxx-api v8.0.2", False)
        self.server.releases.append(release("v8.0.2"))
        self.server.compare[("v8.0.0", "v8.0.2")] = []

        self.run_sync()

        versions = {version["id"]: version for version in self.server.versions}
        self.assertEqual("graal-cxx-api v8.0.1", versions["101"]["name"])
        self.assertTrue(versions["102"]["released"])

    def test_release_without_a_single_placeholder_is_a_problem(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.version("102", "graal-cxx-api v8.1.0", False)
        self.server.releases.append(release("v8.2.0"))

        self.assertEqual(1, self.run_sync())

        self.assertEqual([], self.server.changes)
        self.assertIn("create or rename the version by hand", self.out.getvalue())

    def test_pre_releases_drafts_and_released_versions_are_ignored(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.releases += [
            release("v7.0.0"),  # older than the released Jira version
            release("v8.1.0-rc1", prerelease=True),
            release("v8.1.0", draft=True),
        ]

        self.run_sync()

        self.assertEqual([], self.server.changes)

    def test_dry_run_of_a_release_changes_nothing(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.issue("MDAPI-2", IN_DEVELOPMENT, fix_versions=["101"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] X (#20)"]

        self.run_sync(dry_run=True)

        self.assertEqual([], self.server.changes)
        output = self.out.getvalue()
        self.assertIn("[dry run] rename graal-cxx-api v8.0.1 -> graal-cxx-api v8.1.0", output)
        self.assertIn("[dry run] Resolve: Waiting for build -> Resolved", output)
        self.assertIn("[dry run] create the version graal-cxx-api v8.1.1", output)
        self.assertIn("[dry run] MDAPI-2: fix version graal-cxx-api v8.1.0 -> graal-cxx-api v8.1.1", output)
        self.assertIn("[dry run] release graal-cxx-api v8.1.0 on 2026-10-05", output)

    def test_no_project_administration_is_a_warning(self):
        self.server.permissions["ADMINISTER_PROJECTS"] = False

        self.assertEqual(0, self.run_sync())

        self.assertIn("status='WARNING'", self.out.getvalue())

    # The release mail.

    def release_v8_1_0(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.1.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["101"])
        self.server.releases.append(release("v8.1.0"))
        self.server.compare[("v8.0.0", "v8.1.0")] = ["[MDAPI-1] A"]

    def test_release_is_mailed_once(self):
        self.release_v8_1_0()
        mailer = FakeMailer()

        self.assertEqual(0, self.run_sync(mailer=mailer))
        self.run_sync(mailer=mailer)  # the next run: the version is released

        self.assertEqual(1, len(mailer.sent))
        subject, body_html, text = mailer.sent[0]
        self.assertEqual("[release] dxFeed Graal C++ API v8.1.0", subject)
        self.assertIn('<a href="https://jira.test/projects/MDAPI/versions/101">Jira version graal-cxx-api v8.1.0</a>',
                      body_html)
        self.assertIn('<a href="https://jira.test/browse/MDAPI-1">MDAPI-1</a>', body_html)
        self.assertIn("A wrapped\nline", body_html)  # no <br> of the wrapped lines
        self.assertIn('href="https://dxcity.test/build/1"', body_html)
        self.assertIn("[MDAPI-1][C++] A wrapped line", text)

    def test_dry_run_does_not_mail(self):
        self.release_v8_1_0()
        mailer = FakeMailer()

        self.run_sync(dry_run=True, mailer=mailer)

        self.assertEqual([], mailer.sent)
        self.assertIn("[dry run] mail [release] dxFeed Graal C++ API v8.1.0 to anatoly.kalin@devexperts.com",
                      self.out.getvalue())

    def test_no_recipients_no_mail(self):
        self.release_v8_1_0()
        mailer = FakeMailer(to=())

        self.assertEqual(0, self.run_sync(mailer=mailer))

        self.assertNotIn("mail", self.out.getvalue().replace("Jira permissions", ""))

    def test_failed_mail_is_tried_again_by_the_next_run(self):
        self.release_v8_1_0()

        self.assertEqual(1, self.run_sync(mailer=FakeMailer(error=OSError("relay is down"))))

        version = next(version for version in self.server.versions if version["id"] == "101")
        self.assertFalse(version["released"])  # so the next run repeats the release
        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertIn("the next run tries again", self.out.getvalue())

        mailer = FakeMailer()
        self.assertEqual(0, self.run_sync(mailer=mailer))

        self.assertEqual(1, len(mailer.sent))
        self.assertTrue(version["released"])
        self.assertEqual(1, len([version for version in self.server.versions
                                 if version["name"] == "graal-cxx-api v8.1.1"]))

    def test_dry_run_logs_the_copies(self):
        self.release_v8_1_0()

        self.run_sync(dry_run=True, mailer=FakeMailer(cc=["team@devexperts.com", "lead@devexperts.com"]))

        self.assertIn("to anatoly.kalin@devexperts.com (cc team@devexperts.com, lead@devexperts.com)",
                      self.out.getvalue())

    def test_mailer_puts_the_copies_into_cc(self):
        sent = []

        class Smtp:
            def __init__(self, host, port, timeout):
                sent.append((host, port))

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

            def starttls(self, context):
                sent.append("starttls")

            def send_message(self, message):
                sent.append(message)

        smtp = sync.smtplib.SMTP
        sync.smtplib.SMTP = Smtp

        try:
            sync.Mailer(["a@devexperts.com"], ["b@devexperts.com", "c@devexperts.com"]).send("S", "<p>H</p>", "T")
        finally:
            sync.smtplib.SMTP = smtp

        self.assertEqual(("mxeu0.devexperts.com", 25), sent[0])
        self.assertEqual("starttls", sent[1])
        self.assertEqual("a@devexperts.com", sent[2]["To"])
        self.assertEqual("b@devexperts.com, c@devexperts.com", sent[2]["Cc"])
        self.assertEqual("dxcity <dxcity@bots.devexperts.com>", sent[2]["From"])

    def test_mail_is_resent_for_the_tag(self):
        self.released_jira_and_github()
        mailer = FakeMailer(to=["someone@devexperts.com"])
        log = sync.Log(self.out)
        github = sync.GitHub(self.server, None)
        jira = sync.Jira(self.server, "https://jira.test", "token")

        result = sync.Sync(github, jira, log, False, since=NOW, mailer=mailer).run(resend_mail="v8.0.0")

        self.assertEqual(0, result)
        self.assertEqual("[release] dxFeed Graal C++ API v8.0.0", mailer.sent[0][0])
        self.assertIn('href="https://jira.test/projects/MDAPI/versions/100"', mailer.sent[0][1])

    def test_resend_of_an_unknown_tag_is_a_problem(self):
        self.released_jira_and_github()
        log = sync.Log(self.out)
        github = sync.GitHub(self.server, None)
        jira = sync.Jira(self.server, "https://jira.test", "token")

        result = sync.Sync(github, jira, log, False, since=NOW, mailer=FakeMailer()).run(resend_mail="v7.7.7")

        self.assertEqual(1, result)
        self.assertIn("No GitHub release v7.7.7", self.out.getvalue())

    def test_ticket_keys_are_linked_in_the_text_only(self):
        notes = ('<p><strong>[MDAPI-1]</strong> see <a href="https://x/MDAPI-2">MDAPI-2</a>, '
                 '<code>MDAPI-3</code> and MDAPI-4.</p>')

        linked = sync.link_tickets(notes, "https://jira.test")

        self.assertIn('<strong>[<a href="https://jira.test/browse/MDAPI-1">MDAPI-1</a>]</strong>', linked)
        self.assertIn('<a href="https://x/MDAPI-2">MDAPI-2</a>,', linked)
        self.assertIn("<code>MDAPI-3</code>", linked)
        self.assertIn('<a href="https://jira.test/browse/MDAPI-4">MDAPI-4</a>.', linked)

    def test_service_messages_are_escaped(self):
        self.assertEqual("a|'b|||[c|]|nd", sync.Log.escape("a'b|[c]\nd"))


if __name__ == "__main__":
    unittest.main()
