"""
The tests of go_api_jira_sync.py: the configuration of the Go API on the engine of test_cxx_api_jira_sync.py, and the
separate issue property and Jira versions of the Go repository (python3 -m unittest in this directory).
"""

import datetime
import io
import unittest

import github_jira_sync as engine
import go_api_jira_sync as go
from github_jira_sync import IN_DEVELOPMENT, RESOLVED, WAITING_FOR_BUILD, WAITING_FOR_REVIEW
from test_cxx_api_jira_sync import NOW, FakeMailer, FakeServer, pull_request, release

GO_REPOSITORY = "dxFeed/dxfeed-graal-go-api"


def go_pull_request(number, title, **kwargs):
    return pull_request(number, title, repository=GO_REPOSITORY, **kwargs)


class GoSyncTest(unittest.TestCase):
    def setUp(self):
        self.server = FakeServer(GO_REPOSITORY, "go-api-github-sync")
        self.out = io.StringIO()

    def run_sync(self, mailer=None):
        log = engine.Log(self.out)
        github = engine.GitHub(self.server, None, go.CONFIG)
        jira = engine.Jira(self.server, "https://jira.test", "token", go.CONFIG)
        return engine.Sync(go.CONFIG, github, jira, log, False, since=NOW - datetime.timedelta(hours=48),
                           mailer=mailer).run()

    def status_of(self, key):
        return self.server.issues[key]["status"]["id"]

    def status_and_fix_versions(self, key):
        return self.status_of(key), self.server.issues[key]["fixVersions"]

    def fix_version_names(self, key):
        names = {version["id"]: version["name"] for version in self.server.versions}
        return [names[i] for i in self.server.issues[key]["fixVersions"]]

    def released_go_and_cxx_versions(self):
        self.server.version("100", "graal-cxx-api v8.1.0", True)
        self.server.version("200", "graal-go-api v0.4.0", True)

    def test_opened_pr_is_remembered_in_the_go_property(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [go_pull_request(5, "[MDAPI-1][GO] X")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual({"opened": [5]}, self.server.issues["MDAPI-1"]["property"])
        self.assertIn(("PUT", "issue/MDAPI-1/properties/go-api-github-sync"),
                      [(method, path) for method, path, body in self.server.changes])
        self.assertEqual({}, self.server.issues["MDAPI-1"]["properties"])  # the C++ property is not touched

    def test_cxx_pr_of_the_same_number_does_not_hide_the_go_pr(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.issues["MDAPI-1"]["properties"]["cxx-api-github-sync"] = {"opened": [5], "merged": [5]}
        self.server.pulls = [go_pull_request(5, "[MDAPI-1][GO] X")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))

    def test_merge_gets_the_next_go_version(self):
        # The builds are independent: the Go build reads only its repository (FakeServer fails on another one).
        self.released_go_and_cxx_versions()
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [5]})
        self.server.pulls = [go_pull_request(5, "[MDAPI-1][GO] X", state="closed", merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual(["graal-go-api v0.4.1"], self.fix_version_names("MDAPI-1"))

    def test_release_renames_the_placeholder_and_sends_the_go_mail(self):
        self.released_go_and_cxx_versions()
        self.server.version("201", "graal-go-api v0.4.1", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["201"])
        self.server.releases = [release("v0.5.0", repository=GO_REPOSITORY),
                                release("v0.4.0", published_at="2026-09-07T10:00:00Z", repository=GO_REPOSITORY)]
        self.server.compare[("v0.4.0", "v0.5.0")] = ["[MDAPI-1][GO] X (#5)"]
        mailer = FakeMailer()

        self.assertEqual(0, self.run_sync(mailer))

        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        version = next(version for version in self.server.versions if version["id"] == "201")
        self.assertEqual(("graal-go-api v0.5.0", True), (version["name"], version["released"]))
        self.assertTrue(next(version for version in self.server.versions if version["id"] == "100")["released"])
        subject, body_html, text = mailer.sent[0]
        self.assertEqual("[release][DXFGO] dxFeed Graal Go API v0.5.0", subject)
        self.assertIn("https://pkg.go.dev/github.com/dxfeed/dxfeed-graal-go-api", text)

    def test_release_takes_the_ownerless_tickets_of_the_component(self):
        self.released_go_and_cxx_versions()
        self.server.version("101", "graal-cxx-api v8.1.1", False)
        self.server.version("201", "graal-go-api v0.4.1", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["201"])
        # Moved by the merge of its change of the CI in the SDK: no PR of the Go repository, no fix version.
        self.server.issue("MDAPI-2", WAITING_FOR_BUILD, components=["dxfeed-graal-api", "dxfeed-graal-go-api"])
        # Not of the release: another component, not in Waiting for build, a fix version of another line.
        self.server.issue("MDAPI-3", WAITING_FOR_BUILD, components=["dxfeed-graal-native-sdk"])
        self.server.issue("MDAPI-4", IN_DEVELOPMENT, components=["dxfeed-graal-go-api"])
        self.server.issue("MDAPI-5", WAITING_FOR_BUILD, fix_versions=["101"], components=["dxfeed-graal-go-api"])
        # Merged here, but the fix version failed at the merge: left to be set by hand.
        self.server.issue("MDAPI-6", WAITING_FOR_BUILD, prop={"opened": [7], "merged": [7]},
                          components=["dxfeed-graal-go-api"])
        self.server.releases = [release("v0.5.0", repository=GO_REPOSITORY),
                                release("v0.4.0", published_at="2026-09-07T10:00:00Z", repository=GO_REPOSITORY)]
        self.server.compare[("v0.4.0", "v0.5.0")] = ["[MDAPI-1][GO] X (#5)"]

        self.assertEqual(0, self.run_sync())

        self.assertEqual((RESOLVED, ["201"]), self.status_and_fix_versions("MDAPI-2"))
        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual((WAITING_FOR_BUILD, []), self.status_and_fix_versions("MDAPI-3"))
        self.assertEqual((IN_DEVELOPMENT, []), self.status_and_fix_versions("MDAPI-4"))
        self.assertEqual((WAITING_FOR_BUILD, ["101"]),
                         self.status_and_fix_versions("MDAPI-5"))
        self.assertEqual((WAITING_FOR_BUILD, []), self.status_and_fix_versions("MDAPI-6"))
        log = self.out.getvalue()
        self.assertIn("MDAPI-2: fix version graal-go-api v0.5.0 (no fix version, dxfeed-graal-go-api)", log)
        self.assertIn("MDAPI-6 is in Waiting for build after a merged PR of the repository", log)
        self.assertNotIn("MDAPI-2 has the fix version", log)  # no commits in the repository: no warning

    def test_dry_run_only_logs_the_ownerless_tickets(self):
        self.released_go_and_cxx_versions()
        self.server.version("201", "graal-go-api v0.4.1", False)
        self.server.issue("MDAPI-2", WAITING_FOR_BUILD, components=["dxfeed-graal-go-api"])
        self.server.releases = [release("v0.5.0", repository=GO_REPOSITORY)]

        log = engine.Log(self.out)
        github = engine.GitHub(self.server, None, go.CONFIG)
        jira = engine.Jira(self.server, "https://jira.test", "token", go.CONFIG)
        engine.Sync(go.CONFIG, github, jira, log, True, since=NOW - datetime.timedelta(hours=48)).run()

        self.assertEqual((WAITING_FOR_BUILD, []), self.status_and_fix_versions("MDAPI-2"))
        self.assertEqual([], self.server.changes)
        self.assertIn("[dry run] MDAPI-2: fix version graal-go-api v0.5.0 (no fix version, dxfeed-graal-go-api)",
                      self.out.getvalue())


if __name__ == "__main__":
    unittest.main()
