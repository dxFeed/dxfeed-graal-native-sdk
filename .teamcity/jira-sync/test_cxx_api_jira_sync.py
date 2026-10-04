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
CONFIRMED = "10001"
WAITING_FOR_TEST = "20001"
TESTING = "20002"

# The names and the status categories of the fake MDAPI workflow.
NAMES = {
    ABORTED: ("Aborted", "done"),
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
    IN_DEVELOPMENT: [("71", "Pause development", CONFIRMED), ("151", "Send to review", WAITING_FOR_REVIEW)],
    WAITING_FOR_REVIEW: [("161", "Send to build", WAITING_FOR_BUILD)],
    WAITING_FOR_BUILD: [("171", "Send to test", WAITING_FOR_TEST), ("181", "Resolve", RESOLVED)],
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
        "head": {"ref": branch},
        "base": {"ref": base},
        "state": state,
        "draft": draft,
        "merged_at": merged_at,
        "updated_at": updated_at,
        "html_url": f"https://github.com/dxFeed/dxfeed-graal-cxx-api/pull/{number}",
    }


def release(tag, prerelease=False, draft=False, published_at="2026-10-05T10:00:00Z"):
    return {"tag_name": tag, "prerelease": prerelease, "draft": draft, "published_at": published_at}


class FakeServer:
    """GitHub and Jira: the state of the tests, and the changes that the sync makes."""

    def __init__(self):
        self.pulls = []
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
        if path.startswith("pulls?"):
            return self.pulls

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
            return {"id": 1}

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

    def run_sync(self, dry_run=False):
        log = sync.Log(self.out)
        github = sync.GitHub(self.server, None)
        jira = sync.Jira(self.server, "https://jira.test", "token")
        return sync.Sync(github, jira, log, dry_run, NOW, 48).run()

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

    def test_draft_does_nothing(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Draft", draft=True)]

        self.run_sync()

        self.assertEqual(IN_DEVELOPMENT, self.status_of("MDAPI-1"))
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
        self.assertEqual({"opened": [10], "merged": [10]}, self.server.issues["MDAPI-1"]["property"])
        self.assertEqual(1, len(self.server.links))

    def test_merged_pull_request_after_review(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual([], self.server.links)  # linked when it was opened

    def test_merge_into_release_branch_counts(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] X", state="closed", merged_at="2026-10-05T10:00:00Z",
                                          base="release/v9")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))

    def test_merge_into_another_feature_branch_does_not_count(self):
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [10]})
        self.server.pulls = [pull_request(10, "[MDAPI-1] Stacked", state="closed",
                                          merged_at="2026-10-05T10:00:00Z", base="feature/MDAPI-1-base")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))

    def test_closed_without_merge_does_nothing(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [pull_request(10, "[MDAPI-1] Abandoned", state="closed")]

        self.run_sync()

        self.assertEqual([], self.server.changes)

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

        self.assertIn("[dry run] Send to build: Waiting for review -> Waiting for build", self.out.getvalue())

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
        self.server.releases.append(release("v9.0.0"))
        self.server.compare[("v8.0.0", "v9.0.0")] = ["[MDAPI-1] A", "[MDAPI-2] B", "[MDAPI-4] D"]

        self.assertEqual(0, self.run_sync())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual(TESTING, self.status_of("MDAPI-2"))
        self.assertEqual(ABORTED, self.status_of("MDAPI-3"))
        self.assertEqual(RESOLVED, self.status_of("MDAPI-4"))
        self.assertEqual(["181"], [body["transition"]["id"] for method, path, body in self.server.changes
                                   if path == "issue/MDAPI-4/transitions"])  # Resolve, not Send to test
        for key in ("MDAPI-1", "MDAPI-2", "MDAPI-3", "MDAPI-4"):
            self.assertEqual(["101"], self.server.issues[key]["fixVersions"], key)
        names = {version["id"]: version["name"] for version in self.server.versions}
        self.assertEqual("graal-cxx-api v9.0.0", names["101"])

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
        self.server.releases.append(release("v9.0.0"))
        self.server.compare[("v8.0.0", "v9.0.0")] = ["[MDAPI-1] Breaking (#20)"]

        self.run_sync()

        names = {version["id"]: version["name"] for version in self.server.versions}
        self.assertEqual("graal-cxx-api v9.0.0", names["101"])
        self.assertIn("graal-cxx-api v9.0.1", names.values())
        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))

    def test_release_uses_the_version_of_the_name_and_keeps_the_placeholder(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.version("102", "graal-cxx-api v9.0.0", False)
        self.server.releases.append(release("v9.0.0"))
        self.server.compare[("v8.0.0", "v9.0.0")] = []

        self.run_sync()

        names = {version["id"]: version["name"] for version in self.server.versions}
        self.assertEqual("graal-cxx-api v8.0.1", names["101"])
        self.assertTrue(next(version for version in self.server.versions if version["id"] == "102")["released"])

    def test_release_without_a_single_placeholder_is_a_problem(self):
        self.released_jira_and_github()
        self.server.version("101", "graal-cxx-api v8.0.1", False)
        self.server.version("102", "graal-cxx-api v8.1.0", False)
        self.server.releases.append(release("v9.0.0"))

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
        self.assertIn("[dry run] create the next version graal-cxx-api v8.1.1", output)
        self.assertIn("[dry run] MDAPI-2: fix version graal-cxx-api v8.1.0 -> graal-cxx-api v8.1.1", output)
        self.assertIn("[dry run] release graal-cxx-api v8.1.0 on 2026-10-05", output)

    def test_no_project_administration_is_a_warning(self):
        self.server.permissions["ADMINISTER_PROJECTS"] = False

        self.assertEqual(0, self.run_sync())

        self.assertIn("status='WARNING'", self.out.getvalue())

    def test_service_messages_are_escaped(self):
        self.assertEqual("a|'b|||[c|]|nd", sync.Log.escape("a'b|[c]\nd"))


if __name__ == "__main__":
    unittest.main()
