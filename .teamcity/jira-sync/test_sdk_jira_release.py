"""
The tests of sdk_jira_release.py against a fake GitHub and a fake Jira (python3 -m unittest in this directory).
"""

import io
import os
import re
import tempfile
import unittest
import unittest.mock
import urllib.error
import urllib.parse

import sdk_jira_release as sdk
from jira_sync_common import MAIL_PRE_STYLE, GitHub, HttpError, Jira, Log
from test_cxx_api_jira_sync import (
    CONFIRMED, IN_DEVELOPMENT, RESOLVED, TESTING, WAITING_FOR_BUILD, WAITING_FOR_TEST, FakeMailer, FakeServer,
)

GITHUB = "https://api.github.com/repos/dxFeed/dxfeed-graal-native-sdk/"
NOTES = ("<ul>\n<li><strong>[MDAPI-302][GRAAL]</strong> Fixed the <code>UTF-8</code> names, QD-1795, wrapped<br>\n"
         "line</li>\n<li>Visual C++ 2022, UTF-8 everywhere</li>\n</ul>")


def sdk_release(tag, prerelease=False, draft=False, published_at="2026-10-01T10:00:00Z"):
    return {
        "tag_name": tag,
        "prerelease": prerelease,
        "draft": draft,
        "published_at": published_at,
        "html_url": f"https://github.com/dxFeed/dxfeed-graal-native-sdk/releases/tag/{tag}",
        "body_html": NOTES,
        "body_text": "[MDAPI-302][GRAAL] Fixed the UTF-8 names",
    }


class SdkServer(FakeServer):
    """The fake of the C++ tests with the repository of the SDK and the searches of the release."""

    def __init__(self):
        super().__init__()
        self.no_transitions = set()  # the tickets without transitions for the user of the build
        self.versions_error = None
        self.searches = []

    def ticket(self, key, status_id, summary, fix_versions=(), components=()):
        self.issue(key, status_id, fix_versions)
        self.issues[key].update(summary=summary, components=list(components))

    def request(self, method, url, headers, body=None):
        if url.startswith(GITHUB):
            return self.github(url[len(GITHUB):])

        return super().request(method, url, headers, body)

    def jira(self, method, path, body):
        if path == "project/MDAPI/versions" and self.versions_error:
            raise HttpError(method, path, self.versions_error, "Internal Server Error")

        if path.startswith("search?"):
            return self.search(urllib.parse.parse_qs(path[len("search?"):]))

        if method == "GET" and path.endswith("/transitions") and path.split("/")[1] in self.no_transitions:
            return {"transitions": []}

        return super().jira(method, path, body)

    def search(self, query):
        jql = query["jql"][0]
        self.searches.append(jql)
        version = re.match(r"fixVersion = (\S+)", jql)

        if version:
            if not version.group(1).isdigit():
                raise HttpError("GET", "search", 400, f"The value '{version.group(1)}' does not exist")

            keys = [key for key, issue in self.issues.items() if version.group(1) in issue["fixVersions"]]
        else:
            names = re.findall(r'"([^"]+)"', re.search(r"status in \(([^)]*)\)", jql).group(1))
            keys = [key for key, issue in self.issues.items() if issue["status"]["name"] in names]

        versions = {version["id"]: version for version in self.versions}
        return {"issues": [
            {"key": key, "fields": {
                "status": self.issues[key]["status"],
                "summary": self.issues[key].get("summary", key),
                "components": [{"name": name} for name in self.issues[key].get("components", [])],
                "fixVersions": [{"id": version_id, "name": versions[version_id]["name"],
                                 "released": versions[version_id]["released"]}
                                for version_id in self.issues[key]["fixVersions"]],
            }}
            for key in sorted(keys)
        ]}


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.server = SdkServer()
        self.server.releases = [
            sdk_release("v3.9.0"),
            sdk_release("v3.9.0-rc1", prerelease=True, published_at="2026-09-20T10:00:00Z"),
            sdk_release("v3.8.0", published_at="2026-09-01T10:00:00Z"),
        ]
        self.server.compare[("v3.8.0", "v3.9.0")] = ["[MDAPI-302][GRAAL] Names", "Merge MDAPI-304 into main"]
        self.server.version("100", "graal-native-sdk v3.8.0", True)
        self.server.version("101", "graal-native-sdk v3.9.0", False)
        self.server.version("200", "graal-cxx-api v4.1.0", False)
        self.server.ticket("MDAPI-302", WAITING_FOR_BUILD, "[GRAAL] Names", ["101"])
        self.server.ticket("MDAPI-304", WAITING_FOR_BUILD, "[GRAAL] Events", ["101"])
        self.out = io.StringIO()
        self.mailer = FakeMailer()

    def run_release(self, tag="v3.9.0", dry_run=False, resend_mail=None, mail_file=None):
        log = Log(self.out)
        github = GitHub(self.server, None, sdk.GITHUB_REPOSITORY, sdk.USER_AGENT)
        jira = Jira(self.server, "https://jira.test", "token", sdk.JIRA_PROJECT)
        release = sdk.Release(github, jira, log, dry_run, self.mailer, "https://dxcity.test/build/1", mail_file)
        return release.run(tag, resend_mail)

    def status_of(self, key):
        return self.server.issues[key]["status"]["id"]

    def version(self, version_id):
        return next(version for version in self.server.versions if version["id"] == version_id)

    def subjects(self):
        return [subject for subject, body_html, text in self.mailer.sent]

    def ticket_changes(self):
        return [(method, path) for method, path, body in self.server.changes if path.startswith("issue/")]

    def warnings(self):
        """The texts of the warnings of the log (unescaped)."""
        return [re.sub(r"\|(.)", r"\1", text)
                for text in re.findall(r"##teamcity\[message text='(.*?)' status='WARNING'\]", self.out.getvalue())]

    # The release.

    def test_the_tickets_waiting_for_build_are_resolved_and_the_version_is_released_after_the_mail(self):
        self.assertEqual(0, self.run_release())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-302"))
        self.assertEqual(RESOLVED, self.status_of("MDAPI-304"))
        self.assertEqual(["[release] dxFeed Graal Native SDK v3.9.0"], self.subjects())
        self.assertEqual({"released": True, "releaseDate": "2026-10-01"},
                         {key: self.version("101")[key] for key in ("released", "releaseDate")})
        self.assertEqual(("PUT", "version/101", {"released": True, "releaseDate": "2026-10-01"}),
                         self.server.changes[-1])
        self.assertEqual([], self.warnings())

    def test_without_a_tag_the_latest_published_release_is_taken(self):
        # A fix release of an earlier line, published after v3.9.0, is the one that the chain has just published.
        self.server.releases.insert(0, sdk_release("v3.8.1", published_at="2026-10-05T10:00:00Z"))
        self.server.version("102", "graal-native-sdk v3.8.1", False)
        self.server.compare[("v3.8.0", "v3.8.1")] = ["[MDAPI-401] Fix"]
        self.server.ticket("MDAPI-401", WAITING_FOR_BUILD, "[JAVA][SDK] Fix")

        self.assertEqual(0, self.run_release(tag=""))

        self.assertTrue(self.version("102")["released"])
        self.assertFalse(self.version("101")["released"])
        self.assertEqual(["102"], self.server.issues["MDAPI-401"]["fixVersions"])

    def test_without_a_tag_a_pre_release_published_last_changes_nothing(self):
        # The trigger after an rc while v3.9.0 is still unreleased in Jira: its tickets would be of the next version.
        self.server.releases.insert(0, sdk_release("v3.10.0-rc1", prerelease=True, published_at="2026-10-10T10:00:00Z"))
        self.server.ticket("MDAPI-601", WAITING_FOR_BUILD, "[GRAAL] Work of 3.10.0")

        self.assertEqual(0, self.run_release(tag=""))

        self.assertEqual([], self.server.changes)
        self.assertEqual([], self.mailer.sent)
        self.assertIn("GitHub release v3.10.0-rc1 is a draft or a pre-release", self.out.getvalue())

    def test_a_released_version_is_left_alone(self):
        # AE1: a rerun after the release.
        self.version("101")["released"] = True

        self.assertEqual(0, self.run_release(tag=""))

        self.assertEqual([], self.server.changes)
        self.assertEqual([], self.mailer.sent)
        self.assertIn("Jira version graal-native-sdk v3.9.0 of GitHub release v3.9.0 is released already",
                      self.out.getvalue())

    def test_a_pre_release_or_a_draft_changes_nothing(self):
        self.server.releases.append(sdk_release("v3.10.0", draft=True))

        for tag in ("v3.9.0-rc1", "v3.10.0"):
            with self.subTest(tag=tag):
                self.assertEqual(0, self.run_release(tag=tag))

                self.assertEqual([], self.server.changes)
                self.assertIn(f"GitHub release {tag} is a draft or a pre-release", self.out.getvalue())

    def test_a_tag_without_a_release_is_a_problem(self):
        self.assertEqual(1, self.run_release(tag="v3.9.9"))

        self.assertEqual([], self.server.changes)
        self.assertIn("No GitHub release v3.9.9", self.out.getvalue())

    def test_a_missing_version_is_created(self):
        self.server.versions.remove(self.version("101"))
        self.server.issues["MDAPI-302"]["fixVersions"] = []
        self.server.issues["MDAPI-304"]["fixVersions"] = []

        self.assertEqual(0, self.run_release())

        created = next(version for version in self.server.versions if version["name"] == "graal-native-sdk v3.9.0")
        self.assertTrue(created["released"])
        self.assertEqual([created["id"]], self.server.issues["MDAPI-302"]["fixVersions"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-304"))

    # The tickets.

    def test_an_earlier_unreleased_version_is_replaced(self):
        # AE2.
        self.server.version("102", "graal-native-sdk v3.8.1", False)
        self.server.ticket("MDAPI-500", WAITING_FOR_BUILD, "[GRAAL] Something", ["102"])

        self.assertEqual(0, self.run_release())

        self.assertEqual(["101"], self.server.issues["MDAPI-500"]["fixVersions"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-500"))
        self.assertIn("MDAPI-500: fix version graal-native-sdk v3.8.1 -> graal-native-sdk v3.9.0", self.out.getvalue())

    def test_an_earlier_version_is_removed_when_the_ticket_has_the_version_too(self):
        self.server.version("102", "graal-native-sdk v3.8.1", False)
        self.server.issues["MDAPI-302"]["fixVersions"] = ["102", "101"]

        self.assertEqual(0, self.run_release())

        self.assertEqual(["101"], self.server.issues["MDAPI-302"]["fixVersions"])

    def test_a_graal_ticket_without_a_version_gets_it(self):
        self.server.ticket("MDAPI-501", WAITING_FOR_BUILD, "[GRAAL] [JNI] Not in the commits")

        self.assertEqual(0, self.run_release())

        self.assertEqual(["101"], self.server.issues["MDAPI-501"]["fixVersions"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-501"))

    def test_a_ticket_with_an_sdk_component_is_of_the_sdk(self):
        self.server.ticket("MDAPI-502", WAITING_FOR_TEST, "No tag", components=["dxfeed-graal-native-sdk"])

        self.assertEqual(0, self.run_release())

        self.assertEqual(["101"], self.server.issues["MDAPI-502"]["fixVersions"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-502"))

    def test_an_old_style_ticket_is_of_the_sdk_by_the_commits_only(self):
        self.server.compare[("v3.8.0", "v3.9.0")].append("[MDAPI-503] Old style")
        self.server.ticket("MDAPI-503", WAITING_FOR_BUILD, "[JAVA][SDK] In the commits")
        self.server.ticket("MDAPI-504", WAITING_FOR_BUILD, "[JAVA][SDK] Not in the commits")

        self.assertEqual(0, self.run_release())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-503"))
        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-504"))
        self.assertEqual([], self.server.issues["MDAPI-504"]["fixVersions"])
        self.assertEqual([], self.warnings())

    def test_a_ticket_of_another_api_is_left_alone(self):
        # AE3.
        self.server.ticket("MDAPI-305", WAITING_FOR_BUILD, "[C++] Not ours")
        self.server.ticket("MDAPI-306", WAITING_FOR_BUILD, "[C++][GRAAL] Both")

        self.assertEqual(0, self.run_release())

        for key in ("MDAPI-305", "MDAPI-306"):
            self.assertEqual(WAITING_FOR_BUILD, self.status_of(key))
            self.assertEqual([], self.server.issues[key]["fixVersions"])

        self.assertEqual(1, len(self.warnings()))
        self.assertIn("MDAPI-306 ([GRAAL]) is of another API ([C++]) and is not in the commits", self.warnings()[0])

    def test_a_ticket_with_a_component_of_another_api_is_left_alone(self):
        # The component dxfeed-graal-api is shared by the APIs: the only sign, so a line of the log, not a warning.
        self.server.ticket("MDAPI-308", WAITING_FOR_BUILD, "Untagged fix",
                           components=["dxfeed-graal-api", "dxfeed-graal-cxx-api"])
        self.server.ticket("MDAPI-310", WAITING_FOR_BUILD, "[Swift] Fix", components=["dxfeed-graal-api"])
        self.server.ticket("MDAPI-311", WAITING_FOR_BUILD, "[Swift] SDK fix", components=["dxfeed-graal-native-sdk"])

        self.assertEqual(0, self.run_release())

        for key in ("MDAPI-308", "MDAPI-310", "MDAPI-311"):
            self.assertEqual(WAITING_FOR_BUILD, self.status_of(key))
            self.assertEqual([], self.server.issues[key]["fixVersions"])

        self.assertIn("MDAPI-308 (an SDK component) is of another API (dxfeed-graal-cxx-api)", self.out.getvalue())
        self.assertIn("MDAPI-310 (an SDK component) is of another API ([SWIFT])", self.out.getvalue())
        self.assertEqual(1, len(self.warnings()))
        self.assertIn("MDAPI-311 (an SDK component) is of another API ([SWIFT])", self.warnings()[0])

    def test_a_ticket_with_the_shared_component_and_a_foreign_version_is_logged(self):
        self.server.ticket("MDAPI-312", WAITING_FOR_BUILD, "Fix", ["200"], components=["dxfeed-graal-api"])

        self.assertEqual(0, self.run_release())

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-312"))
        self.assertIn("MDAPI-312 (an SDK component) has the fix version graal-cxx-api v4.1.0", self.out.getvalue())
        self.assertEqual([], self.warnings())

    def test_a_ticket_with_the_version_only_is_of_the_sdk(self):
        self.server.ticket("MDAPI-309", WAITING_FOR_BUILD, "Version set by hand", ["101"])

        self.assertEqual(0, self.run_release())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-309"))

    def test_a_ticket_of_another_api_in_the_commits_is_of_the_sdk(self):
        self.server.compare[("v3.8.0", "v3.9.0")].append("[MDAPI-307] Both")
        self.server.ticket("MDAPI-307", WAITING_FOR_BUILD, "[Swift] Needs the SDK")

        self.assertEqual(0, self.run_release())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-307"))

    def test_a_ticket_with_a_cxx_version_is_never_changed(self):
        self.server.compare[("v3.8.0", "v3.9.0")].append("[MDAPI-308] Shared")
        self.server.ticket("MDAPI-308", WAITING_FOR_BUILD, "[GRAAL] Shared", ["200"])

        self.assertEqual(0, self.run_release())

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-308"))
        self.assertEqual(["200"], self.server.issues["MDAPI-308"]["fixVersions"])
        self.assertIn("MDAPI-308 (in the commits, [GRAAL]) has the fix version graal-cxx-api v4.1.0 of another product",
                      "\n".join(self.warnings()))

    def test_a_ticket_with_a_released_or_a_later_version_is_left_alone(self):
        self.server.version("103", "graal-native-sdk v3.10.0", False)
        self.server.ticket("MDAPI-505", WAITING_FOR_BUILD, "[GRAAL] Released", ["100"])
        self.server.ticket("MDAPI-506", WAITING_FOR_BUILD, "[GRAAL] Later", ["103"])

        self.assertEqual(0, self.run_release())

        self.assertEqual(["100"], self.server.issues["MDAPI-505"]["fixVersions"])
        self.assertEqual(["103"], self.server.issues["MDAPI-506"]["fixVersions"])
        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-506"))
        self.assertEqual(2, len(self.warnings()))

    def test_a_ticket_waiting_for_test_without_a_transition_stays_with_a_warning(self):
        self.server.ticket("MDAPI-507", WAITING_FOR_TEST, "[GRAAL] Tested by hand", ["101"])
        self.server.no_transitions.add("MDAPI-507")

        self.assertEqual(0, self.run_release())

        self.assertEqual(WAITING_FOR_TEST, self.status_of("MDAPI-507"))
        self.assertIn("the ticket stays in Waiting for test, resolve it by hand", "\n".join(self.warnings()))
        self.assertTrue(self.version("101")["released"])

    def test_a_ticket_waiting_for_build_without_a_transition_stops_the_release(self):
        self.server.no_transitions.add("MDAPI-304")

        self.assertEqual(1, self.run_release())

        self.assertFalse(self.version("101")["released"])
        self.assertEqual(["[release] FAILED: dxFeed Graal Native SDK v3.9.0"], self.subjects())
        subject, body_html, text = self.mailer.sent[0]
        self.assertIn("MDAPI-304: no transition from Waiting for build to Resolved", body_html)
        self.assertIn("https://dxcity.test/build/1", body_html)

    def test_the_tickets_of_the_version_in_the_other_statuses_are_reported(self):
        self.server.ticket("MDAPI-508", TESTING, "[GRAAL] Testing", ["101"])
        self.server.ticket("MDAPI-509", IN_DEVELOPMENT, "[GRAAL] Unfinished", ["101"])
        self.server.ticket("MDAPI-510", RESOLVED, "[GRAAL] Done", ["101"])

        self.assertEqual(0, self.run_release())

        self.assertEqual([("POST", "issue/MDAPI-302/transitions"), ("POST", "issue/MDAPI-304/transitions")],
                         self.ticket_changes())
        self.assertIn("MDAPI-508 of graal-native-sdk v3.9.0 is in Testing: it stays there", self.out.getvalue())
        self.assertEqual(1, len(self.warnings()))
        self.assertIn("MDAPI-509 of graal-native-sdk v3.9.0 is not finished (In development)", self.warnings()[0])
        self.assertNotIn("MDAPI-510", self.out.getvalue())

    def test_a_key_of_the_commits_without_the_version_is_a_warning(self):
        self.server.compare[("v3.8.0", "v3.9.0")].append("[MDAPI-511] Confirmed only")
        self.server.ticket("MDAPI-511", CONFIRMED, "[GRAAL] Confirmed")

        self.assertEqual(0, self.run_release())

        self.assertEqual(CONFIRMED, self.status_of("MDAPI-511"))
        self.assertIn("MDAPI-511 is in the commits v3.8.0..v3.9.0 but is neither of graal-native-sdk v3.9.0 nor "
                      "waiting for build or test", "\n".join(self.warnings()))

    # The dry run.

    def test_the_dry_run_changes_nothing_and_writes_the_mail(self):
        with tempfile.TemporaryDirectory() as directory:
            mail_file = os.path.join(directory, "release-mail.html")

            self.assertEqual(0, self.run_release(dry_run=True, mail_file=mail_file))

            with open(mail_file, encoding="utf-8") as file:
                self.assertIn("dxFeed Graal Native SDK v3.9.0", file.read())

        self.assertEqual([], self.server.changes)
        self.assertEqual([], self.mailer.sent)
        self.assertIn("[dry run] Resolve: Waiting for build -> Resolved", self.out.getvalue())
        self.assertIn("[dry run] mail [release] dxFeed Graal Native SDK v3.9.0", self.out.getvalue())
        self.assertIn("[dry run] release graal-native-sdk v3.9.0 on 2026-10-01", self.out.getvalue())

    def test_the_dry_run_with_a_missing_version_does_not_search_its_tickets(self):
        self.server.versions.remove(self.version("101"))
        self.server.issues["MDAPI-302"]["fixVersions"] = []
        self.server.issues["MDAPI-304"]["fixVersions"] = []

        self.assertEqual(0, self.run_release(dry_run=True))

        self.assertEqual([], self.server.changes)
        self.assertFalse(any(jql.startswith("fixVersion") for jql in self.server.searches))
        self.assertIn("[dry run] create the version graal-native-sdk v3.9.0", self.out.getvalue())
        self.assertIn("[dry run] MDAPI-302: fix version none -> graal-native-sdk v3.9.0", self.out.getvalue())

    def test_the_dry_run_sends_no_failure_mail(self):
        self.server.no_transitions.add("MDAPI-304")

        self.assertEqual(1, self.run_release(dry_run=True))

        self.assertEqual([], self.mailer.sent)
        self.assertNotIn("FAILED", self.out.getvalue())

    # The mail.

    def test_a_failed_mail_leaves_the_version_unreleased_for_the_rerun(self):
        # AE4.
        self.mailer = FakeMailer(error=OSError("Connection refused"))

        self.assertEqual(1, self.run_release())

        self.assertFalse(self.version("101")["released"])
        self.assertIn("The release mail is not sent", self.out.getvalue())
        self.assertIn("The failure mail is not sent: Connection refused", self.out.getvalue())
        self.server.changes.clear()
        self.mailer = FakeMailer()

        self.assertEqual(0, self.run_release())

        self.assertEqual(["[release] dxFeed Graal Native SDK v3.9.0"], self.subjects())
        self.assertEqual(["version/101"], [path for method, path, body in self.server.changes])

    def test_without_recipients_the_version_is_released_without_a_mail(self):
        self.mailer = FakeMailer(to=())

        self.assertEqual(0, self.run_release())

        self.assertTrue(self.version("101")["released"])
        self.assertEqual([], self.mailer.sent)

    def test_a_failed_jira_request_is_mailed(self):
        self.server.versions_error = 500

        self.assertEqual(1, self.run_release())

        self.assertEqual(["[release] FAILED: dxFeed Graal Native SDK v3.9.0"], self.subjects())
        self.assertIn("HTTP 500", self.mailer.sent[0][2])

    def test_a_network_error_is_mailed(self):
        with unittest.mock.patch.object(self.server, "jira", side_effect=urllib.error.URLError("timed out")):
            self.assertEqual(1, self.run_release())

        self.assertEqual(["[release] FAILED: dxFeed Graal Native SDK v3.9.0"], self.subjects())
        self.assertIn("URLError", self.mailer.sent[0][2])

    def test_the_mail_links_the_tickets_the_release_the_version_and_the_apis(self):
        self.assertEqual(0, self.run_release())

        subject, body_html, text = self.mailer.sent[0]
        self.assertIn('<a href="https://jira.test/browse/MDAPI-302">MDAPI-302</a>', body_html)
        self.assertIn('<a href="https://jira.test/browse/QD-1795">QD-1795</a>', body_html)
        self.assertNotIn("browse/UTF-8", body_html)
        self.assertIn("Visual C++ 2022", body_html)
        self.assertIn("wrapped\nline", body_html)
        self.assertIn('href="https://github.com/dxFeed/dxfeed-graal-native-sdk/releases/tag/v3.9.0"', body_html)
        self.assertIn('href="https://jira.test/projects/MDAPI/versions/101"', body_html)

        for name, url, mirror in sdk.APIS:
            self.assertIn(f'<a href="{url}">{name}</a>', body_html)
            self.assertIn(url, text)

        swift = "https://stash.in.devexperts.com/projects/MDAPI/repos/dxfeed-graal-swift-api"
        self.assertIn(f'<a href="https://github.com/dxFeed/dxfeed-graal-swift-api">Swift API</a> '
                      f'(<a href="{swift}">Stash</a>)', body_html)
        self.assertIn(f"Swift API: https://github.com/dxFeed/dxfeed-graal-swift-api (Stash: {swift})", text)
        # The tickets are in the notes, no list of them.
        self.assertNotIn("Tickets", body_html)
        self.assertNotIn("MDAPI-304", body_html)

    def test_the_mail_highlights_the_code_of_the_notes(self):
        self.server.releases[0]["body_html"] = (
            '<div class="highlight highlight-source-c notranslate position-relative overflow-auto" dir="auto">'
            '<pre><span class="pl-k">int</span> <span class="pl-en">main</span>() <span class="pl-smi">x</span>'
            "</pre></div>")

        self.assertEqual(0, self.run_release())

        body_html = self.mailer.sent[0][1]
        self.assertIn(f'<div><pre style="{MAIL_PRE_STYLE}"><span style="color: #cf222e;">int</span> '
                      '<span style="color: #6639ba;">main</span>() <span>x</span></pre></div>', body_html)

    def test_the_mail_is_resent_without_other_changes(self):
        self.version("101")["released"] = True

        self.assertEqual(0, self.run_release(tag="", resend_mail="v3.9.0"))

        self.assertEqual(["[release] dxFeed Graal Native SDK v3.9.0"], self.subjects())
        self.assertEqual([], self.server.changes)
        self.assertIn("Jira version graal-native-sdk v3.9.0", self.mailer.sent[0][2])

    def test_resending_the_mail_of_an_unknown_release_is_a_problem(self):
        self.assertEqual(1, self.run_release(tag="", resend_mail="v3.7.0"))

        self.assertIn("No GitHub release v3.7.0 or no Jira version graal-native-sdk v3.7.0", self.out.getvalue())

    # The environment of the build.

    def run_main(self, **environment):
        environment = {"JIRA_URL": "https://jira.test", "JIRA_TOKEN": "token", **environment}

        with (
            tempfile.TemporaryDirectory() as directory,
            unittest.mock.patch.dict(os.environ, environment, clear=True),
            unittest.mock.patch.object(sdk, "Http", lambda: self.server),
            unittest.mock.patch.object(sdk, "Log", lambda: Log(self.out)),
            unittest.mock.patch.object(sdk, "MAIL_FILE", os.path.join(directory, "release-mail.html")),
        ):
            return sdk.main()

    def test_the_build_makes_a_dry_run_by_default(self):
        self.assertEqual(0, self.run_main(TAG=" v3.9.0 "))

        self.assertEqual([], self.server.changes)
        self.assertIn("[dry run] release graal-native-sdk v3.9.0 on 2026-10-01", self.out.getvalue())

    def test_the_build_releases_with_dry_run_0(self):
        self.assertEqual(0, self.run_main(DRY_RUN="0"))

        self.assertTrue(self.version("101")["released"])
        self.assertEqual(RESOLVED, self.status_of("MDAPI-302"))


if __name__ == "__main__":
    unittest.main()
