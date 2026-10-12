#!/usr/bin/env python3
"""
Releases the Jira version of a GitHub release of the Graal Native SDK (dxfeed-graal-native-sdk): the build runs after
"Publish GitHub Release" of the release chain.

The release is the one of TAG, or else the latest published GitHub release (the one the chain has just published); a
draft, a pre-release (an rc) or a version released in Jira already leaves everything as it is, and the earlier
releases are never caught up. For the
release vX.Y.Z:
  - the Jira version "graal-native-sdk vX.Y.Z" of MDAPI, created if there is none;
  - the SDK tickets in Waiting for build and Waiting for test get it: added if a ticket has no fix version, or in
    place of its earlier unreleased graal-native-sdk versions. A ticket is of the SDK when its key is in the commits
    of the release (from the previous release vX.Y.Z), its summary has [GRAAL], it has the component dxfeed-graal-api
    or dxfeed-graal-native-sdk, or it has the version already. MDAPI is shared with the other APIs, so a ticket is left
    as it is (with a warning) when it has a fix version of another product (graal-cxx-api v...), a tag of another API
    in the summary ([C++], [C#], [.NET], [Go], [Swift]) or a component of another API (dxfeed-graal-cxx-api) and no
    commit of the release, or a released earlier or a later graal-native-sdk version;
  - the tickets of the version in Waiting for build -> Resolved (the transition "Resolve", as the Release Steps of
    MDAPI do), in Waiting for test -> Resolved if there is such a transition; the ones in the other statuses are
    reported (the unfinished ones with a warning) and stay;
  - the release mail goes to MAIL_TO and MAIL_CC: the notes of the GitHub release (as GitHub renders them, the
    tickets linked), the tickets of the version, the links to the release, the Jira version and the APIs built on the
    SDK; it is sent before the version is released, so a failed mail leaves the version unreleased and a rerun repeats
    the release (its ticket changes are done already) and the mail. The mail is written to release-mail.html too;
  - the version is released with the date of the GitHub release.
A live run that ends with build problems mails them to MAIL_TO and MAIL_CC.

The environment: JIRA_URL, JIRA_TOKEN (a personal access token), GH_TOKEN (optional, read-only access is enough),
DRY_RUN ("1" by default: reads everything and logs the changes instead of making them), TAG (v3.9.0: the release, the
latest one by default), MAIL_TO and MAIL_CC (the comma-separated recipients of the mail; no MAIL_TO: no mail),
RESEND_MAIL (a tag, v3.9.0: sends the mail of its release again and changes nothing else), BUILD_URL (TeamCity sets
it).

Python 3.10 (the TeamCity agents), the standard library only.
"""

import html
import http.client
import os
import re
import sys

from jira_sync_common import (
    GitHub, Http, HttpError, Jira, Log, Mailer, Workflow, env_list, prefixed_version_tuple, release_mail_html,
    release_notes_html, version_tuple,
)

GITHUB_REPOSITORY = "dxFeed/dxfeed-graal-native-sdk"
USER_AGENT = "dxfeed-graal-native-sdk-jira-release"
JIRA_PROJECT = "MDAPI"
VERSION_PREFIX = "graal-native-sdk v"
TITLE = "dxFeed Graal Native SDK"
MAIL_FILE = "release-mail.html"

TAG_PATTERN = re.compile(r"^v(\d+)\.(\d+)\.(\d+)$")
# The tickets of the commits.
KEY_PATTERN = re.compile(r"\bMDAPI-\d+\b")
# The tickets linked in the release notes: the projects that the notes refer to (not any word-number, as UTF-8).
NOTES_KEY_PATTERN = re.compile(r"\b(?:MDAPI|QD)-\d+\b")

# The signs of a ticket of the SDK, and the tags of the summaries of the tickets of the other APIs.
SDK_TAG = "[graal]"
SDK_COMPONENTS = {"dxfeed-graal-api", "dxfeed-graal-native-sdk"}
OTHER_API_TAGS = ("[c++]", "[c#]", "[.net]", "[go]", "[swift]")
OTHER_API_COMPONENT_PREFIX = "dxfeed-graal-"  # the components of the APIs other than SDK_COMPONENTS

# The ids of the statuses of the MDAPI workflow.
WAITING_FOR_BUILD = "10008"
RESOLVED = "10011"
STATUS_NAMES = {WAITING_FOR_BUILD: "Waiting for build", RESOLVED: "Resolved"}

RELEASE_TRANSITION = "Resolve"

# The statuses of the testing step, by name (their ids are not needed elsewhere).
WAITING_FOR_TEST_NAME = "waiting for test"
TESTING_NAME = "testing"

WAITING_JQL = f'project = {JIRA_PROJECT} AND status in ("Waiting for build", "Waiting for test") ORDER BY key'
TICKET_FIELDS = "status,summary,components,fixVersions"

# The APIs built on the SDK, for the mail: (the name, the repository).
APIS = [
    ("C++ API", "https://github.com/dxFeed/dxfeed-graal-cxx-api"),
    (".NET API", "https://github.com/dxFeed/dxfeed-graal-net-api"),
    ("Go API", "https://github.com/dxFeed/dxfeed-graal-go-api"),
    ("Swift API", "https://stash.in.devexperts.com/projects/MDAPI/repos/dxfeed-graal-swift-api"),
]


def is_release(release):
    """A published release vX.Y.Z: not a draft, not a pre-release (an rc)."""
    return not release["draft"] and not release["prerelease"] and TAG_PATTERN.match(release["tag_name"]) is not None


def release_number(release):
    return version_tuple(release["tag_name"][1:])


def key_number(key):
    """The number of the ticket: MDAPI-302 -> 302."""
    return int(key.split("-")[1])


def is_waiting_for_test(status):
    return status["name"].lower() == WAITING_FOR_TEST_NAME


def sdk_version_tuple(version):
    return prefixed_version_tuple(version, VERSION_PREFIX)


def release_mail(release, version_name, version_url, tickets, jira_url, build_url):
    """
    The subject, the HTML and the text of the mail about a GitHub release (with its notes as GitHub renders them);
    tickets: (the key, the summary, the status) of the version.
    """
    tag = release["tag_name"]
    date = release["published_at"][:10]
    title = f"{TITLE} {tag}"
    links = [
        f'<a href="{html.escape(release["html_url"])}">Release on GitHub</a> (downloads)',
        f'<a href="{html.escape(version_url)}">Jira version {html.escape(version_name)}</a>',
    ]
    cell = 'style="padding: 2px 12px 2px 0; vertical-align: top;"'
    rows = "\n".join(
        f'<tr><td {cell}><a href="{jira_url}/browse/{key}">{key}</a></td><td {cell}>{html.escape(summary)}</td>'
        f"<td {cell}>{html.escape(status)}</td></tr>"
        for key, summary, status in tickets
    )
    sections = [
        ("Release notes", release_notes_html(release, jira_url, NOTES_KEY_PATTERN)),
        ("Tickets", f'<table style="border-collapse: collapse;">\n{rows}\n</table>' if tickets else "<p>None.</p>"),
        ("APIs built on the SDK", "<p>" + " &nbsp;|&nbsp; ".join(
            f'<a href="{url}">{html.escape(name)}</a>' for name, url in APIS) + "</p>"),
    ]
    body = release_mail_html(title, date, links, sections, build_url)
    text = "\n\n".join(filter(None, [
        f"{title} has been released on {date}.",
        f"Release on GitHub: {release['html_url']}\nJira version {version_name}: {version_url}",
        release.get("body_text") or release.get("body") or "",
        "Tickets:\n" + "\n".join(f"{key} {summary} ({status})" for key, summary, status in tickets) if tickets else "",
        "APIs built on the SDK:\n" + "\n".join(f"{name}: {url}" for name, url in APIS),
        f"Build: {build_url}" if build_url else "",
    ]))
    return f"[release] {title}", body, text


def failure_mail(tag, problems, build_url):
    """The subject, the HTML and the text of the mail about a run that ended with build problems."""
    title = f"{TITLE} {tag}" if tag else TITLE
    build = f'<p>The <a href="{html.escape(build_url)}">TeamCity build</a>.</p>' if build_url else ""
    items = "\n".join(f"<li>{html.escape(problem)}</li>" for problem in problems)
    body = f"<p>The Jira release of {html.escape(title)} has failed:</p>\n<ul>\n{items}\n</ul>\n{build}\n"
    text = "\n\n".join(filter(None, [
        f"The Jira release of {title} has failed:",
        "\n".join(f"- {problem}" for problem in problems),
        f"Build: {build_url}" if build_url else "",
    ]))
    return f"[release] FAILED: {title}", body, text


class Release(Workflow):
    def __init__(self, github, jira, log, dry_run, mailer=None, build_url=None, mail_file=None):
        """
        mailer: sends the mails (none: no mail); build_url: the TeamCity build, for the mail; mail_file: the file of
        the release mail (none: not written).
        """
        super().__init__(jira, log, dry_run, STATUS_NAMES, mailer)
        self.github = github
        self.build_url = build_url
        self.mail_file = mail_file
        self.tag = None  # the tag of the run, for the failure mail

    def run(self, tag=None, resend_mail=None):
        """Releases the version of the tag (the latest release by default), or only resends the mail of a tag."""
        self.tag = resend_mail or tag

        try:
            if resend_mail:
                self.resend_mail(resend_mail)
            else:
                self.release(tag)
        except HttpError as e:
            self.log.problem(f"The release in Jira failed: {e}")
        except (OSError, ValueError, KeyError, http.client.HTTPException) as e:
            # The network (URLError, timeouts), an unexpected response or the mail file: reported and mailed too.
            self.log.problem(f"The release in Jira failed: {type(e).__name__}: {e}")

        if self.log.problems and not self.dry_run:
            subject, body_html, text = failure_mail(self.tag, self.log.problems, self.build_url)
            self.send_mail(subject, body_html, text, lambda e: self.log.warning(f"The failure mail is not sent: {e}"))

        return 1 if self.log.problems else 0

    def release(self, tag):
        releases = self.github.releases()

        if tag:
            release = next((release for release in releases if release["tag_name"] == tag), None)

            if release is None:
                self.log.problem(f"No GitHub release {tag}")
                return
        else:
            # The release that the chain has just published: after an rc, the rc, so that an earlier release still
            # unreleased in Jira is not completed with the tickets of the next one.
            release = max((release for release in releases if release.get("published_at")),
                          key=lambda release: release["published_at"], default=None)

            if release is None:
                self.log.info("No published GitHub releases")
                return

        tag = self.tag = release["tag_name"]

        if not is_release(release):
            self.log.info(f"GitHub release {tag} is a draft or a pre-release, not a release vX.Y.Z: nothing to do")
            return

        number = release_number(release)
        name = VERSION_PREFIX + tag[1:]
        versions = [version for version in self.jira.versions() if sdk_version_tuple(version) is not None]
        version = next((version for version in versions if version["name"] == name), None)

        if version is not None and version["released"]:
            self.log.info(f"Jira version {name} of GitHub release {tag} is released already")
            return

        self.log.info(f"GitHub release {tag} -> Jira version {name}")
        version = self.ensure_version(name, versions)

        earlier = [other for other in releases if is_release(other) and release_number(other) < number]
        previous = max(earlier, key=release_number)["tag_name"] if earlier else None
        committed = set()

        if previous is not None:
            for message in self.github.commit_messages(previous, tag):
                committed.update(KEY_PATTERN.findall(message))
        else:
            self.log.warning(f"No GitHub release before {tag}: the tickets are not checked against the commits")

        tickets = {ticket["key"]: ticket for ticket in self.jira.issues(WAITING_JQL, TICKET_FIELDS)}

        # A version that only the dry run has created has no tickets yet (and Jira does not take its id).
        if version["id"] != "(new)":
            for ticket in self.jira.issues(f"fixVersion = {version['id']} ORDER BY key", TICKET_FIELDS):
                tickets.setdefault(ticket["key"], ticket)

        released = []  # the tickets of the version: (the key, the summary, the status) for the mail

        for key, ticket in sorted(tickets.items(), key=lambda item: key_number(item[0])):
            status = ticket["fields"]["status"]
            self.statuses[key] = status

            if self.take(ticket, version, number, committed):
                self.finish(key, status, name)
                released.append((key, ticket["fields"]["summary"], self.statuses[key]["name"]))

        for key in sorted(committed - set(tickets), key=key_number):
            self.log.warning(
                f"{key} is in the commits {previous}..{tag} but is neither of {name} nor waiting for build or test")

        # The ticket changes are not repeated by a rerun (the tickets are Resolved), the mail and the release are: a
        # problem of a ticket stops the release, so that it is fixed by hand and the rerun completes the release.
        if self.log.problems:
            self.log.info(f"Jira version {name} is not released: there are build problems")
            return

        if not self.mail(release, version, released):
            return

        date = release["published_at"][:10]
        self.change(f"release {name} on {date}",
                    lambda: self.jira.update_version(version["id"], {"released": True, "releaseDate": date}))

    def take(self, ticket, version, number, committed):
        """
        Whether the ticket is of the release: a ticket of the SDK gets the version (if it has no fix version, or in
        place of its earlier unreleased graal-native-sdk versions); the ones that are not of the SDK, or that have
        other versions, are left as they are.
        """
        key = ticket["key"]
        fields = ticket["fields"]
        summary = fields["summary"].lower()
        fix_versions = fields.get("fixVersions") or []
        components = {component["name"] for component in fields.get("components") or []}
        in_commits = key in committed
        has_version = any(other["id"] == version["id"] for other in fix_versions)
        signs = [sign for sign, present in [
            ("in the commits", in_commits),
            ("[GRAAL]", SDK_TAG in summary),
            ("an SDK component", bool(components & SDK_COMPONENTS)),
            (f"of {version['name']}", has_version),
        ] if present]

        if not signs:
            return False  # a ticket of another API

        foreign = [other["name"] for other in fix_versions if sdk_version_tuple(other) is None]

        if foreign:
            self.log.warning(f"{key} ({', '.join(signs)}) has the fix version {', '.join(foreign)} of another "
                             f"product: it is left as it is")
            return False

        # The tags of the other APIs in the summary and their components (dxfeed-graal-cxx-api): the component
        # dxfeed-graal-api is shared, so it does not make a ticket of another API a ticket of the SDK.
        others = [tag.upper() for tag in OTHER_API_TAGS if tag in summary]
        others += sorted(name for name in components - SDK_COMPONENTS if name.startswith(OTHER_API_COMPONENT_PREFIX))

        if others and not in_commits:
            self.log.warning(f"{key} ({', '.join(signs)}) is of another API ({', '.join(others)}) and is not in the "
                             f"commits: it is left as it is")
            return False

        if fields["status"]["id"] != WAITING_FOR_BUILD and not is_waiting_for_test(fields["status"]):
            return has_version  # reported by finish, not changed

        other_sdk = [other for other in fix_versions if other["id"] != version["id"]]
        kept = [other["name"] for other in other_sdk if sdk_version_tuple(other) > number or other.get("released")]

        if kept:
            self.log.warning(f"{key} has the fix version {', '.join(kept)} (released or later): it is left as it is")
            return False

        if other_sdk or not has_version:
            names = ", ".join(other["name"] for other in other_sdk) or "none"
            self.change(f"{key}: fix version {names} -> {version['name']}",
                        lambda: self.jira.update_fix_versions(
                            key, [other["id"] for other in other_sdk], None if has_version else version["id"]))

        return True

    def finish(self, key, status, name):
        """Resolves the ticket of the version waiting for build or test; reports the ones in the other statuses."""
        status_name = status["name"].lower()

        if status["id"] == WAITING_FOR_BUILD:
            self.log.info(f"{key}:")
            self.move(key, RESOLVED, RELEASE_TRANSITION)
        elif is_waiting_for_test(status):
            self.log.info(f"{key}:")
            self.move(key, RESOLVED, RELEASE_TRANSITION, report=lambda text: self.log.warning(
                f"{text}: the ticket stays in {status['name']}, resolve it by hand"))
        elif status_name == TESTING_NAME:
            self.log.info(f"{key} of {name} is in {status['name']}: it stays there")
        elif status["statusCategory"]["key"] != "done":
            self.log.warning(f"{key} of {name} is not finished ({status['name']}): it stays in {name}, move it by hand")

    def mail(self, release, version, tickets):
        """Writes the release mail to the file and sends it to the recipients (if any); False if it fails."""
        version_url = f"{self.jira.url}/projects/{JIRA_PROJECT}/versions/{version['id']}"
        subject, body_html, text = release_mail(
            release, version["name"], version_url, tickets, self.jira.url, self.build_url)

        if self.mail_file:
            with open(self.mail_file, "w", encoding="utf-8") as file:
                file.write(body_html)

            self.log.info(f"  the mail is written to {self.mail_file}")

        return self.send_mail(subject, body_html, text, lambda e: self.log.problem(
            f"The release mail is not sent (a rerun of the build tries again): {e}"))

    def resend_mail(self, tag):
        """Sends the mail of the release of the tag again (RESEND_MAIL, set in the Run dialog of the build)."""
        release = next((release for release in self.github.releases() if release["tag_name"] == tag), None)
        name = VERSION_PREFIX + tag[1:]
        version = next((version for version in self.jira.versions() if version["name"] == name), None)

        if release is None or version is None:
            self.log.problem(f"No GitHub release {tag} or no Jira version {name} to resend the mail of")
            return

        self.log.info(f"Resending the mail of {tag}")
        tickets = [
            (ticket["key"], ticket["fields"]["summary"], ticket["fields"]["status"]["name"])
            for ticket in self.jira.issues(f"fixVersion = {version['id']} ORDER BY key")
        ]
        self.mail(release, version, tickets)


def main():
    dry_run = os.environ.get("DRY_RUN", "1") != "0"
    log = Log()
    log.info(f"Dry run: {dry_run}")
    http = Http()
    release = Release(
        github=GitHub(http, os.environ.get("GH_TOKEN"), GITHUB_REPOSITORY, USER_AGENT),
        jira=Jira(http, os.environ["JIRA_URL"], os.environ["JIRA_TOKEN"], JIRA_PROJECT),
        log=log,
        dry_run=dry_run,
        mailer=Mailer(env_list("MAIL_TO"), env_list("MAIL_CC")),
        build_url=os.environ.get("BUILD_URL"),
        mail_file=MAIL_FILE,
    )
    return release.run(tag=os.environ.get("TAG", "").strip(), resend_mail=os.environ.get("RESEND_MAIL", "").strip())


if __name__ == "__main__":
    sys.exit(main())
