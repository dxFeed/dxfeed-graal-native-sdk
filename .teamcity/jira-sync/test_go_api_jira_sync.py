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


if __name__ == "__main__":
    unittest.main()
