#!/usr/bin/env python3
"""
Moves the MDAPI tickets of the C++ API (dxfeed-graal-cxx-api on GitHub) through the Jira workflow and releases the
Jira versions "graal-cxx-api vX.Y.Z" of its GitHub releases: github_jira_sync.py with the C++ API repository (see
there for the workflow and the environment).

Python 3.10 (the TeamCity agents), the standard library only.
"""

import sys

import github_jira_sync as engine
from github_jira_sync import *  # noqa: F401,F403 (the names of the module before the engine was split off)

CONFIG = engine.RepoConfig(
    repository="dxFeed/dxfeed-graal-cxx-api",
    version_prefix="graal-cxx-api v",
    issue_property="cxx-api-github-sync",
    user_agent="dxfeed-graal-cxx-api-jira-sync",
    product="dxFeed Graal C++ API",
    mail_tag="DXFCXX",
    documentation_url="https://dxfeed.github.io/dxfeed-graal-cxx-api/",
)


class GitHub(engine.GitHub):
    def __init__(self, http, token):
        super().__init__(http, token, CONFIG)


class Jira(engine.Jira):
    def __init__(self, http, url, token):
        super().__init__(http, url, token, CONFIG)


class Sync(engine.Sync):
    def __init__(self, github, jira, log, dry_run, since, mailer=None, build_url=None):
        super().__init__(CONFIG, github, jira, log, dry_run, since, mailer, build_url)


def release_mail(release, version_name, version_url, jira_url, build_url):
    return engine.release_mail(CONFIG, release, version_name, version_url, jira_url, build_url)


def main():
    return engine.main(CONFIG)


if __name__ == "__main__":
    sys.exit(main())
