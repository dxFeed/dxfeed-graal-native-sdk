"""
The tests of jira_sync_common.py (python3 -m unittest in this directory); the scripts that use it have their own.
"""

import io
import re
import unittest
import urllib.parse

from jira_sync_common import Jira, Log, Workflow, link_tickets, prefixed_version_tuple
from test_cxx_api_jira_sync import IN_DEVELOPMENT, RESOLVED, WAITING_FOR_BUILD, FakeServer

KEYS = re.compile(r"\b(?:MDAPI|QD)-\d+\b")


class Recorder:
    """The HTTP of the clients: remembers the requests and answers with nothing found."""

    def __init__(self):
        self.urls = []

    def request(self, method, url, headers, body=None):
        self.urls.append(url)
        return {"issues": []}


class CommonTest(unittest.TestCase):
    def test_link_tickets_links_the_keys_of_the_pattern_in_the_text(self):
        notes = "<p>MDAPI-302 and QD-1795, <code>MDAPI-1</code>, <a href=\"x\">MDAPI-2</a>, UTF-8, Visual C++</p>"

        self.assertEqual(
            '<p><a href="https://jira.test/browse/MDAPI-302">MDAPI-302</a> and '
            '<a href="https://jira.test/browse/QD-1795">QD-1795</a>, <code>MDAPI-1</code>, '
            '<a href="x">MDAPI-2</a>, UTF-8, Visual C++</p>',
            link_tickets(notes, "https://jira.test", KEYS))

    def test_prefixed_version_tuple_takes_only_the_versions_of_the_prefix(self):
        prefix = "graal-native-sdk v"

        self.assertEqual((3, 9, 0), prefixed_version_tuple({"name": "graal-native-sdk v3.9.0"}, prefix))
        self.assertIsNone(prefixed_version_tuple({"name": "graal-cxx-api v3.9.0"}, prefix))
        self.assertIsNone(prefixed_version_tuple({"name": "graal-native-sdk v3.9.0-rc1"}, prefix))

    def test_issues_asks_for_the_status_and_the_summary_by_default(self):
        http = Recorder()
        jira = Jira(http, "https://jira.test/", "token", "MDAPI")

        jira.issues("fixVersion = 1")
        jira.issues("fixVersion = 1", "status,summary,fixVersions")

        fields = [urllib.parse.parse_qs(urllib.parse.urlsplit(url).query)["fields"][0] for url in http.urls]
        self.assertEqual(["status,summary", "status,summary,fixVersions"], fields)
        self.assertTrue(http.urls[0].startswith("https://jira.test/rest/api/2/search?"))

    def test_update_fix_versions_removes_and_adds_in_one_change(self):
        server = FakeServer()
        server.version("1", "graal-native-sdk v3.8.1", False)
        server.version("2", "graal-native-sdk v3.9.0", False)
        server.issue("MDAPI-1", WAITING_FOR_BUILD, ["1"])
        jira = Jira(server, "https://jira.test", "token", "MDAPI")

        jira.update_fix_versions("MDAPI-1", ["1"], "2")
        jira.update_fix_versions("MDAPI-1", ["2"])

        self.assertEqual([], server.issues["MDAPI-1"]["fixVersions"])
        self.assertEqual(2, len(server.changes))

    def test_the_dry_run_logs_the_move_and_does_not_check_the_next_one(self):
        server = FakeServer()
        server.issue("MDAPI-1", WAITING_FOR_BUILD)
        out = io.StringIO()
        workflow = Workflow(Jira(server, "https://jira.test", "token", "MDAPI"), Log(out), True,
                            {RESOLVED: "Resolved", IN_DEVELOPMENT: "In development"})

        self.assertTrue(workflow.move("MDAPI-1", RESOLVED, "Resolve"))
        # Resolved has no transition to In development: the dry run does not ask Jira again.
        self.assertTrue(workflow.move("MDAPI-1", IN_DEVELOPMENT))

        self.assertEqual([], server.changes)
        self.assertEqual(WAITING_FOR_BUILD, server.issues["MDAPI-1"]["status"]["id"])
        self.assertIn("[dry run] Resolve: Waiting for build -> Resolved", out.getvalue())
        self.assertIn("[dry run] then to In development (the transition is not checked in the dry run)",
                      out.getvalue())

    def test_a_missing_transition_is_a_problem(self):
        server = FakeServer()
        server.issue("MDAPI-1", RESOLVED)
        log = Log(io.StringIO())
        workflow = Workflow(Jira(server, "https://jira.test", "token", "MDAPI"), log, False, {})

        self.assertFalse(workflow.move("MDAPI-1", WAITING_FOR_BUILD))

        self.assertEqual(["MDAPI-1: no transition from Resolved to 10008 for the Jira user of the build"],
                         log.problems)


if __name__ == "__main__":
    unittest.main()
