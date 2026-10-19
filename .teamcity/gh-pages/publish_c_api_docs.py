#!/usr/bin/env python3
"""
Publishes the C API reference of a release of the Graal Native SDK to GitHub Pages of the mirror
(dxFeed/dxfeed-graal-native-sdk): the build "Publish C API Docs to GitHub Pages" runs after "Publish GitHub Release" of
the release chain.

For the release vX.Y.Z or vX.Y.Z-rcN, html/ of graal-native-sdk-<version>-c-api-docs-html.zip (the qd repository of
Nexus, where the release chain deploys it) goes to the branch gh-pages of the mirror:
  - <version>/ is replaced with it;
  - latest/ is a copy of the highest X.Y.Z directory (never of an rc), recomputed on every run, so a later patch of an
    older line or an rc leaves it as it is;
  - index.html at the root redirects to latest/ and lists the versions; .nojekyll turns off Jekyll of the Pages build.
The branch is created as an orphan when there is none. A run commits and pushes only when the tree changed, so a rerun
for the same release changes nothing. A missing zip fails the run before gh-pages is read.

With C_API_DOCS_HTML, the reference is that directory instead of the zip (the build generates it from the tag with the
Doxygen of the docs-check image, to publish a release again); a directory without index.html fails the run the same
way.

The environment: TAG (v3.9.0; empty: `git describe --tags --abbrev=0` in the current directory, the release that
"Publish GitHub Release" has just published), DRY_RUN ("1" by default: lays out the pages and logs the changes, pushes
nothing), REMOTE (the mirror by default), NEXUS_URL (https://maven.in.devexperts.com by default), C_API_DOCS_HTML (empty
by default: the zip in Nexus). The commit takes the git identity of the agent, or GIT_AUTHOR_NAME, GIT_AUTHOR_EMAIL,
GIT_COMMITTER_NAME and GIT_COMMITTER_EMAIL.

Python 3.10 (the TeamCity agents), the standard library and git only.
"""

import os
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile

DEFAULT_REMOTE = "git@github.com:dxFeed/dxfeed-graal-native-sdk.git"
DEFAULT_NEXUS_URL = "https://maven.in.devexperts.com"
BRANCH = "gh-pages"
SITE_URL = "https://dxfeed.github.io/dxfeed-graal-native-sdk/"
TITLE = "dxFeed Graal Native SDK: C API"
LATEST = "latest"

VERSION_PATTERN = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-rc(\d+))?$")


class Failure(Exception):
    """A failure of the run; its message is the report."""


def log(message):
    print(message, flush=True)


def git(*args, cwd=None):
    result = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Failure(f"git {' '.join(args)} failed ({result.returncode}):\n{result.stderr.strip()}")
    return result.stdout


def release_version(tag):
    """v3.9.0 -> 3.9.0, v3.9.0-rc1 -> 3.9.0-rc1."""
    if not (tag.startswith("v") and VERSION_PATTERN.match(tag[1:])):
        raise Failure(f"Not a release tag: {tag!r} (expected vX.Y.Z or vX.Y.Z-rcN)")
    return tag[1:]


def is_release(version):
    """3.9.0: True, 3.9.0-rc1: False."""
    return VERSION_PATTERN.match(version).group(4) is None


def version_key(version):
    """The order of the versions: 3.9.0-rc1 < 3.9.0 < 3.9.1 < 3.10.0."""
    major, minor, patch, rc = VERSION_PATTERN.match(version).groups()
    return int(major), int(minor), int(patch), float("inf") if rc is None else int(rc)


def remove_tree(path):
    def make_writable_and_retry(function, failed_path, _):
        os.chmod(failed_path, stat.S_IWRITE)
        function(failed_path)

    if os.path.isdir(path):
        shutil.rmtree(path, onerror=make_writable_and_retry)


def write_text(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as file:
        file.write(text)


def download_docs(nexus_url, version, directory):
    name = f"graal-native-sdk-{version}-c-api-docs-html.zip"
    url = f"{nexus_url.rstrip('/')}/repository/qd/com/dxfeed/graal-native-sdk/{version}/{name}"
    path = os.path.join(directory, name)
    log(f"Downloading {url}")
    try:
        with urllib.request.urlopen(url, timeout=120) as response, open(path, "wb") as file:
            shutil.copyfileobj(response, file)
    except (urllib.error.URLError, OSError) as error:
        raise Failure(f"No C API documentation of {version} at {url}: {error}") from error
    return path


def extract_html(zip_path, directory):
    target = os.path.join(directory, "docs")
    try:
        with zipfile.ZipFile(zip_path) as archive:
            archive.extractall(target)
    except zipfile.BadZipFile as error:
        raise Failure(f"{os.path.basename(zip_path)} is not a zip: {error}") from error
    html = os.path.join(target, "html")
    if not os.path.isfile(os.path.join(html, "index.html")):
        raise Failure(f"{os.path.basename(zip_path)} has no html/index.html")
    return html


def check_out(remote, site):
    """Clones gh-pages of the remote into site, or makes site an empty orphan gh-pages; True when it was created."""
    heads = git("ls-remote", "--heads", remote, f"refs/heads/{BRANCH}").splitlines()
    if any(line.endswith(f"\trefs/heads/{BRANCH}") for line in heads):
        git("clone", "--quiet", "--depth", "1", "--single-branch", "--branch", BRANCH, remote, site)
        return False
    os.makedirs(site)
    git("init", "--quiet", cwd=site)
    git("symbolic-ref", "HEAD", f"refs/heads/{BRANCH}", cwd=site)
    git("remote", "add", "origin", remote, cwd=site)
    return True


def index_html(versions, latest):
    """The root page: the redirect to latest/ (when there is a release) and the list of the versions."""
    redirect = f'  <meta http-equiv="refresh" content="0; url={LATEST}/">\n' if latest else ""
    items = "".join(
        f'    <li><a href="{version}/">{version}</a>{" (latest)" if version == latest else ""}</li>\n'
        for version in versions)
    return (
        "<!DOCTYPE html>\n"
        '<html lang="en">\n'
        "<head>\n"
        '  <meta charset="utf-8">\n'
        f"{redirect}"
        f"  <title>{TITLE}</title>\n"
        "</head>\n"
        "<body>\n"
        f"  <h1>{TITLE}</h1>\n"
        "  <p>The reference of every published version:</p>\n"
        "  <ul>\n"
        f"{items}"
        "  </ul>\n"
        "</body>\n"
        "</html>\n"
    )


def lay_out(site, version, html):
    """Replaces site/<version>/ with html, then makes latest/, index.html and .nojekyll; returns the latest version."""
    remove_tree(os.path.join(site, version))
    shutil.copytree(html, os.path.join(site, version))

    versions = sorted(
        (name for name in os.listdir(site) if VERSION_PATTERN.match(name) and os.path.isdir(os.path.join(site, name))),
        key=version_key, reverse=True)
    releases = [name for name in versions if is_release(name)]
    latest = releases[0] if releases else None

    remove_tree(os.path.join(site, LATEST))
    if latest:
        shutil.copytree(os.path.join(site, latest), os.path.join(site, LATEST))
    write_text(os.path.join(site, "index.html"), index_html(versions, latest))
    write_text(os.path.join(site, ".nojekyll"), "")
    return latest


def generated_html(html):
    if not os.path.isfile(os.path.join(html, "index.html")):
        raise Failure(f"{html} has no index.html")
    log(f"The reference generated from the tag: {html}")
    return html


def publish(tag, dry_run, remote, nexus_url, work, html=None):
    """
    Publishes the reference of tag to gh-pages of remote, using the empty directory work; True when pushed. The
    reference is the directory html when given, else html/ of the zip in nexus_url.
    """
    version = release_version(tag)
    log(f"Release {tag}{' (dry run)' if dry_run else ''}")
    if html:
        html = generated_html(html)
    else:
        html = extract_html(download_docs(nexus_url, version, work), work)

    site = os.path.join(work, "site")
    if check_out(remote, site):
        log(f"{BRANCH} does not exist: it is created as an orphan branch")
    else:
        log(f"Cloned {BRANCH} of {remote}")
    latest = lay_out(site, version, html)
    log(f"{LATEST}/: {latest or 'none, no release yet'}")

    git("add", "--all", cwd=site)
    changes = git("status", "--porcelain", cwd=site).splitlines()
    if not changes:
        log("Nothing changed, nothing to push")
        return False
    top_level = sorted({line[3:].strip('"').split("/")[0] for line in changes})
    log(f"Changed: {len(changes)} files in {', '.join(top_level)}")
    if dry_run:
        log("[dry run] Not committed and not pushed")
        return False

    git("commit", "--quiet", "-m", f"C API reference {version}", cwd=site)
    git("push", "--quiet", "origin", f"HEAD:refs/heads/{BRANCH}", cwd=site)
    log(f"Published {SITE_URL}{version}/")
    return True


def main():
    try:
        tag = os.environ.get("TAG", "").strip() or git("describe", "--tags", "--abbrev=0").strip()
        dry_run = os.environ.get("DRY_RUN", "1").strip() != "0"
        remote = os.environ.get("REMOTE", "").strip() or DEFAULT_REMOTE
        nexus_url = os.environ.get("NEXUS_URL", "").strip() or DEFAULT_NEXUS_URL
        html = os.environ.get("C_API_DOCS_HTML", "").strip() or None
        with tempfile.TemporaryDirectory(prefix="c-api-docs-") as work:
            publish(tag, dry_run, remote, nexus_url, work, html)
    except Failure as failure:
        print(failure, file=sys.stderr, flush=True)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
