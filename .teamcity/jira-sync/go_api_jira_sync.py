#!/usr/bin/env python3
"""
Moves the MDAPI tickets of the Go API (dxfeed-graal-go-api on GitHub) through the Jira workflow and releases the Jira
versions "graal-go-api vX.Y.Z" of its GitHub releases: github_jira_sync.py with the Go API repository (see there for
the workflow and the environment).

Python 3.10 (the TeamCity agents), the standard library only.
"""

import sys

import github_jira_sync as engine

CONFIG = engine.RepoConfig(
    repository="dxFeed/dxfeed-graal-go-api",
    version_prefix="graal-go-api v",
    issue_property="go-api-github-sync",
    user_agent="dxfeed-graal-go-api-jira-sync",
    product="dxFeed Graal Go API",
    mail_tag="DXFGO",
    documentation_url="https://pkg.go.dev/github.com/dxfeed/dxfeed-graal-go-api",
)

if __name__ == "__main__":
    sys.exit(engine.main(CONFIG))
