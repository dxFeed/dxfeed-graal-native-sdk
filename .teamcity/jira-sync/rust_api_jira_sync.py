#!/usr/bin/env python3
"""
Moves the MDAPI tickets of the Rust API (dxfeed-graal-rust-api on GitHub) through the Jira workflow and releases the
Jira versions "graal-rust-api vX.Y.Z" of its GitHub releases: github_jira_sync.py with the Rust API repository (see
there for the workflow and the environment).

Python 3.10 (the TeamCity agents), the standard library only.
"""

import sys

import github_jira_sync as engine

CONFIG = engine.RepoConfig(
    repository="dxFeed/dxfeed-graal-rust-api",
    version_prefix="graal-rust-api v",
    issue_property="rust-api-github-sync",
    user_agent="dxfeed-graal-rust-api-jira-sync",
    product="dxFeed Graal Rust API",
    mail_tag="DXFR",
    documentation_url="https://docs.rs/dxfeed-graal",
    component="dxfeed-graal-rust-api",
)

if __name__ == "__main__":
    sys.exit(engine.main(CONFIG))
