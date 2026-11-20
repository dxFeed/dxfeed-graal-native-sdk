"""
The tests of rust_api_jira_sync.py: the configuration of the Rust API on the engine of test_cxx_api_jira_sync.py, the
separate issue property and Jira versions of the Rust repository, and the first major release after 0.x
(python3 -m unittest in this directory).
"""

import datetime
import io
import unittest

import github_jira_sync as engine
import rust_api_jira_sync as rust
from github_jira_sync import IN_DEVELOPMENT, RESOLVED, WAITING_FOR_BUILD, WAITING_FOR_REVIEW
from test_cxx_api_jira_sync import NOW, FakeMailer, FakeServer, pull_request, release

RUST_REPOSITORY = "dxFeed/dxfeed-graal-rust-api"


def rust_pull_request(number, title, **kwargs):
    return pull_request(number, title, repository=RUST_REPOSITORY, **kwargs)


class RustSyncTest(unittest.TestCase):
    def setUp(self):
        self.server = FakeServer(RUST_REPOSITORY, "rust-api-github-sync")
        self.out = io.StringIO()

    def run_sync(self, mailer=None):
        log = engine.Log(self.out)
        github = engine.GitHub(self.server, None, rust.CONFIG)
        jira = engine.Jira(self.server, "https://jira.test", "token", rust.CONFIG)
        return engine.Sync(rust.CONFIG, github, jira, log, False, since=NOW - datetime.timedelta(hours=48),
                           mailer=mailer).run()

    def status_of(self, key):
        return self.server.issues[key]["status"]["id"]

    def fix_version_names(self, key):
        names = {version["id"]: version["name"] for version in self.server.versions}
        return [names[i] for i in self.server.issues[key]["fixVersions"]]

    def version(self, version_id):
        return next(version for version in self.server.versions if version["id"] == version_id)

    def released_rust_and_go_versions(self):
        self.server.version("200", "graal-go-api v1.0.0", True)
        self.server.version("300", "graal-rust-api v0.5.0", True)

    def test_opened_pr_is_remembered_in_the_rust_property(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.pulls = [rust_pull_request(5, "[MDAPI-1][RUST] X")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))
        self.assertEqual({"opened": [5]}, self.server.issues["MDAPI-1"]["property"])
        self.assertIn(("PUT", "issue/MDAPI-1/properties/rust-api-github-sync"),
                      [(method, path) for method, path, body in self.server.changes])
        self.assertEqual({}, self.server.issues["MDAPI-1"]["properties"])  # the C++ property is not touched

    def test_go_pr_of_the_same_number_does_not_hide_the_rust_pr(self):
        self.server.issue("MDAPI-1", IN_DEVELOPMENT)
        self.server.issues["MDAPI-1"]["properties"]["go-api-github-sync"] = {"opened": [5], "merged": [5]}
        self.server.pulls = [rust_pull_request(5, "[MDAPI-1][RUST] X")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_REVIEW, self.status_of("MDAPI-1"))

    def test_merge_gets_the_next_rust_version(self):
        self.released_rust_and_go_versions()
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [5]})
        self.server.pulls = [rust_pull_request(5, "[MDAPI-1][RUST] X", state="closed",
                                               merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual(["graal-rust-api v0.5.1"], self.fix_version_names("MDAPI-1"))

    def test_merge_keeps_the_fix_version_of_the_next_major(self):
        # A ticket filed for 1.0.0 by hand keeps it: the version of a later line is not replaced.
        self.released_rust_and_go_versions()
        self.server.version("301", "graal-rust-api v1.0.0", False)
        self.server.issue("MDAPI-1", WAITING_FOR_REVIEW, prop={"opened": [5]}, fix_versions=["301"])
        self.server.pulls = [rust_pull_request(5, "[MDAPI-1][RUST] X", state="closed",
                                               merged_at="2026-10-05T10:00:00Z")]

        self.run_sync()

        self.assertEqual(WAITING_FOR_BUILD, self.status_of("MDAPI-1"))
        self.assertEqual(["graal-rust-api v1.0.0"], self.fix_version_names("MDAPI-1"))

    def test_first_major_release_takes_the_tickets_of_the_0_x_placeholder(self):
        self.released_rust_and_go_versions()
        self.server.version("301", "graal-rust-api v1.0.0", False)
        self.server.version("302", "graal-rust-api v0.5.1", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["301"])
        self.server.issue("MDAPI-2", WAITING_FOR_BUILD, fix_versions=["302"])
        self.server.releases = [release("v1.0.0", repository=RUST_REPOSITORY),
                                release("v0.5.0", published_at="2026-09-07T10:00:00Z", repository=RUST_REPOSITORY)]
        self.server.compare[("v0.5.0", "v1.0.0")] = ["[MDAPI-1][RUST] X (#5)", "[MDAPI-2][RUST] Y (#6)"]
        mailer = FakeMailer()

        self.assertEqual(0, self.run_sync(mailer))

        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual(RESOLVED, self.status_of("MDAPI-2"))
        self.assertEqual(["graal-rust-api v1.0.0"], self.fix_version_names("MDAPI-2"))
        self.assertEqual(("graal-rust-api v1.0.0", True), (self.version("301")["name"], self.version("301")["released"]))
        self.assertTrue(self.version("302")["archived"])
        subject, body_html, text = mailer.sent[0]
        self.assertEqual("[release][DXFR] dxFeed Graal Rust API v1.0.0", subject)
        self.assertIn("https://docs.rs/dxfeed-graal", text)

    def test_release_takes_the_ownerless_tickets_of_the_component(self):
        self.released_rust_and_go_versions()
        self.server.version("201", "graal-go-api v1.0.1", False)
        self.server.version("301", "graal-rust-api v0.5.1", False)
        self.server.issue("MDAPI-1", WAITING_FOR_BUILD, fix_versions=["301"])
        # Moved by the merge of its change of the sync in the SDK: no PR of the Rust repository, no fix version.
        self.server.issue("MDAPI-2", WAITING_FOR_BUILD, components=["dxfeed-graal-api", "dxfeed-graal-rust-api"])
        # Not of the release: another component, a fix version of another API.
        self.server.issue("MDAPI-3", WAITING_FOR_BUILD, components=["dxfeed-graal-go-api"])
        self.server.issue("MDAPI-4", WAITING_FOR_BUILD, fix_versions=["201"], components=["dxfeed-graal-rust-api"])
        self.server.releases = [release("v0.5.1", repository=RUST_REPOSITORY),
                                release("v0.5.0", published_at="2026-09-07T10:00:00Z", repository=RUST_REPOSITORY)]
        self.server.compare[("v0.5.0", "v0.5.1")] = ["[MDAPI-1][RUST] X (#5)"]

        self.assertEqual(0, self.run_sync())

        self.assertEqual(RESOLVED, self.status_of("MDAPI-1"))
        self.assertEqual((RESOLVED, ["graal-rust-api v0.5.1"]),
                         (self.status_of("MDAPI-2"), self.fix_version_names("MDAPI-2")))
        self.assertEqual((WAITING_FOR_BUILD, []), (self.status_of("MDAPI-3"), self.fix_version_names("MDAPI-3")))
        self.assertEqual((WAITING_FOR_BUILD, ["graal-go-api v1.0.1"]),
                         (self.status_of("MDAPI-4"), self.fix_version_names("MDAPI-4")))


if __name__ == "__main__":
    unittest.main()
