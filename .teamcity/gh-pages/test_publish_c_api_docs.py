"""
The tests of publish_c_api_docs.py against a local bare repository as the mirror and a file:// URL as Nexus (no
network; python3 -m unittest in this directory).
"""

import contextlib
import io
import os
import pathlib
import tempfile
import unittest
import unittest.mock
import zipfile

import publish_c_api_docs as docs

IDENTITY = {
    "GIT_AUTHOR_NAME": "test", "GIT_AUTHOR_EMAIL": "test@example.com",
    "GIT_COMMITTER_NAME": "test", "GIT_COMMITTER_EMAIL": "test@example.com",
}


class PublishTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = temporary.name
        self.remote = os.path.join(self.root, "mirror.git")
        docs.git("init", "--quiet", "--bare", self.remote)
        self.nexus = os.path.join(self.root, "nexus")
        environment = unittest.mock.patch.dict(os.environ, IDENTITY)
        environment.start()
        self.addCleanup(environment.stop)

    def deploy(self, version, files=None):
        """Puts the docs zip of version to the fake Nexus; files: {path under html/: text}."""
        directory = os.path.join(self.nexus, "repository", "qd", "com", "dxfeed", "graal-native-sdk", version)
        os.makedirs(directory, exist_ok=True)
        files = files or {"index.html": f"<p>{version}</p>", "search/search.js": "// search"}
        with zipfile.ZipFile(os.path.join(directory, f"graal-native-sdk-{version}-c-api-docs-html.zip"), "w") as zip:
            for path, text in files.items():
                zip.writestr(f"html/{path}", text)

    def publish(self, tag, dry_run=False):
        """Runs a publication of tag; returns its result and its log."""
        with tempfile.TemporaryDirectory() as work, contextlib.redirect_stdout(io.StringIO()) as output:
            pushed = docs.publish(tag, dry_run, self.remote, pathlib.Path(self.nexus).as_uri(), work)
        return pushed, output.getvalue()

    def has_branch(self):
        return bool(docs.git("--git-dir", self.remote, "branch", "--list", docs.BRANCH).strip())

    def files(self):
        return set(docs.git("--git-dir", self.remote, "ls-tree", "-r", "--name-only", docs.BRANCH).splitlines())

    def show(self, path):
        return docs.git("--git-dir", self.remote, "show", f"{docs.BRANCH}:{path}")

    def commits(self):
        return int(docs.git("--git-dir", self.remote, "rev-list", "--count", docs.BRANCH))

    def test_creates_the_orphan_branch(self):
        self.deploy("3.9.0")
        pushed, output = self.publish("v3.9.0")
        self.assertTrue(pushed)
        self.assertIn("created as an orphan branch", output)
        self.assertEqual(
            {"3.9.0/index.html", "3.9.0/search/search.js", "latest/index.html", "latest/search/search.js",
             "index.html", ".nojekyll"},
            self.files())
        self.assertEqual("<p>3.9.0</p>", self.show("latest/index.html"))
        self.assertEqual(1, self.commits())
        root = self.show("index.html")
        self.assertIn('<meta http-equiv="refresh" content="0; url=latest/">', root)
        self.assertIn('<a href="3.9.0/">3.9.0</a> (latest)', root)

    def test_dry_run_pushes_nothing(self):
        self.deploy("3.9.0")
        pushed, output = self.publish("v3.9.0", dry_run=True)
        self.assertFalse(pushed)
        self.assertIn("created as an orphan branch", output)
        self.assertIn("Changed: 6 files in .nojekyll, 3.9.0, index.html, latest", output)
        self.assertIn("[dry run]", output)
        self.assertFalse(self.has_branch())

    def test_a_higher_release_becomes_latest(self):
        self.deploy("3.9.0")
        self.deploy("3.10.0")
        self.publish("v3.9.0")
        self.publish("v3.10.0")
        self.assertEqual("<p>3.10.0</p>", self.show("latest/index.html"))
        self.assertEqual("<p>3.9.0</p>", self.show("3.9.0/index.html"))
        root = self.show("index.html")
        self.assertLess(root.index('"3.10.0/"'), root.index('"3.9.0/"'))

    def test_a_patch_of_an_older_line_keeps_latest(self):
        for version in ("3.9.0", "3.10.0", "3.9.1"):
            self.deploy(version)
            self.publish(f"v{version}")
        self.assertEqual("<p>3.10.0</p>", self.show("latest/index.html"))
        self.assertEqual("<p>3.9.1</p>", self.show("3.9.1/index.html"))

    def test_an_rc_adds_only_its_directory(self):
        self.deploy("3.10.0")
        self.deploy("3.11.0-rc1")
        self.publish("v3.10.0")
        self.publish("v3.11.0-rc1")
        self.assertEqual("<p>3.10.0</p>", self.show("latest/index.html"))
        self.assertEqual("<p>3.11.0-rc1</p>", self.show("3.11.0-rc1/index.html"))
        root = self.show("index.html")
        self.assertIn('<a href="3.11.0-rc1/">3.11.0-rc1</a></li>', root)
        self.assertIn('<a href="3.10.0/">3.10.0</a> (latest)', root)

    def test_only_an_rc_makes_no_latest(self):
        self.deploy("3.9.0-rc1")
        self.publish("v3.9.0-rc1")
        self.assertEqual({"3.9.0-rc1/index.html", "3.9.0-rc1/search/search.js", "index.html", ".nojekyll"},
                         self.files())
        self.assertNotIn("refresh", self.show("index.html"))

    def test_a_rerun_changes_nothing(self):
        self.deploy("3.9.0")
        self.publish("v3.9.0")
        pushed, output = self.publish("v3.9.0")
        self.assertFalse(pushed)
        self.assertIn("Nothing changed", output)
        self.assertEqual(1, self.commits())

    def test_a_republication_replaces_the_version_directory(self):
        self.deploy("3.9.0")
        self.publish("v3.9.0")
        self.deploy("3.9.0", {"index.html": "<p>3.9.0 again</p>"})
        self.publish("v3.9.0")
        self.assertEqual({"3.9.0/index.html", "latest/index.html", "index.html", ".nojekyll"}, self.files())
        self.assertEqual("<p>3.9.0 again</p>", self.show("latest/index.html"))

    def test_a_missing_zip_fails_before_gh_pages(self):
        self.deploy("3.9.0")
        self.publish("v3.9.0")
        with self.assertRaisesRegex(docs.Failure, "No C API documentation of 3.10.0"):
            self.publish("v3.10.0")
        self.assertEqual(1, self.commits())

    def test_a_zip_without_html_fails(self):
        self.deploy("3.9.0", {"../readme.txt": "no html"})
        with self.assertRaisesRegex(docs.Failure, "has no html/index.html"):
            self.publish("v3.9.0")
        self.assertFalse(self.has_branch())

    def test_not_a_release_tag_fails(self):
        for tag in ("3.9.0", "v3.9", "v3.9.0-SNAPSHOT"):
            with self.subTest(tag=tag), self.assertRaisesRegex(docs.Failure, "Not a release tag"):
                self.publish(tag)


class VersionKeyTest(unittest.TestCase):
    def test_order(self):
        versions = ["3.10.0", "3.9.0", "3.9.0-rc2", "3.9.1", "3.9.0-rc10", "10.0.0"]
        self.assertEqual(["3.9.0-rc2", "3.9.0-rc10", "3.9.0", "3.9.1", "3.10.0", "10.0.0"],
                         sorted(versions, key=docs.version_key))


if __name__ == "__main__":
    unittest.main()
