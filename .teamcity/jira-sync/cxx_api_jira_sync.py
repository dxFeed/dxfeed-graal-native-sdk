#!/usr/bin/env python3
"""
Moves the MDAPI tickets of the C++ API (dxfeed-graal-cxx-api on GitHub) through the Jira workflow, as the Jira
triggers of Bitbucket do for the internal repositories, and releases the Jira versions of its GitHub releases.

The pull requests updated during the last LOOKBACK_HOURS (the tickets are the MDAPI keys of the title and the branch):
  - a draft PR is opened (the work goes on): Confirmed -> In development, Waiting for build -> Waiting for review ->
    In development (there is no direct transition), and Waiting for review -> In development if no other PR of the
    ticket is in review (open and not a draft);
  - a PR is opened (not a draft, or a draft is marked ready): In development -> Waiting for review;
  - a PR is merged into main or release/*, or another PR of the ticket is closed (without a merge, or merged into
    another branch), and a PR of the ticket has been merged: Waiting for review -> Waiting for build if no other PR of
    the ticket is open, -> In development if only drafts are open; it stays in review while another PR is in review;
  - a PR is closed without a merge, and no PR of the ticket has been merged: Waiting for review -> In development
    unless another PR of the ticket is in review;
    A ticket moved to Waiting for build gets the fix version of the next release of its line: the unreleased
    graal-cxx-api version of the major of release/vN for a PR merged there (vN.0.0, created if needed), else of the
    major of main (the patch after the last released version, created if needed).
The lines: release/vN collects the breaking changes of the next major, main has the patches of the current one until
release/vN is merged into it and vN.0.0 is released; the earlier majors are not maintained after that.
Each PR is applied to a ticket once (the ticket keeps the numbers of the applied PRs in an issue property), so a
ticket moved back by hand stays where it is. The opened and merged PRs are also linked from the ticket.

A published GitHub release vX.Y.Z (not a draft, not a pre-release) newer than the last released Jira version
"graal-cxx-api vX.Y.Z":
  - the Jira version is the one of that name, or else the only unreleased graal-cxx-api version of the major of the tag
    (the placeholder of the next release of the line), which is renamed, or else a new one;
  - the unreleased versions of the earlier majors (a new major is released) give their tickets to it and are archived;
  - its tickets in Waiting for build -> Resolved (the transition "Resolve", as the Release Steps of MDAPI do), and in
    Waiting for test (the tickets of the time with the testing step) -> Resolved if there is such a transition;
  - its tickets in Testing stay as they are;
  - its unfinished tickets (not of the status category "done": In development, Waiting for review...) -> the next
    version vX.Y.(Z+1), the placeholder, which is created if needed;
  - the version is released with the date of the GitHub release;
  - the release mail goes to MAIL_TO and MAIL_CC: the notes of the GitHub release (as GitHub renders them, the
    tickets linked), the links to the release, the documentation and the Jira version; it is sent before the version
    is released, so a failed mail leaves the version unreleased and the next run tries again.

The environment: JIRA_URL, JIRA_TOKEN (a personal access token), GH_TOKEN (optional, read-only access is enough),
DRY_RUN ("1" by default: reads everything and logs the changes instead of making them), LOOKBACK_HOURS (48),
MAIL_TO and MAIL_CC (the comma-separated recipients of the release mail; no MAIL_TO: no mail), RESEND_MAIL (a tag,
v8.1.0: sends the mail of its release again), BUILD_URL (TeamCity sets it).

Python 3.10 (the TeamCity agents), the standard library only.
"""

import datetime
import email.message
import html
import json
import os
import re
import smtplib
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request

GITHUB_REPOSITORY = "dxFeed/dxfeed-graal-cxx-api"
JIRA_PROJECT = "MDAPI"
VERSION_PREFIX = "graal-cxx-api v"
ISSUE_PROPERTY = "cxx-api-github-sync"

KEY_PATTERN = re.compile(r"\bMDAPI-\d+\b")
MERGE_BASE_PATTERN = re.compile(r"^(main|release/.+)$")
TAG_PATTERN = re.compile(r"^v(\d+)\.(\d+)\.(\d+)$")

# The ids of the statuses of the MDAPI workflow.
CONFIRMED = "10001"
IN_DEVELOPMENT = "10003"
WAITING_FOR_REVIEW = "10013"
WAITING_FOR_BUILD = "10008"
RESOLVED = "10011"
STATUS_NAMES = {
    CONFIRMED: "Confirmed",
    IN_DEVELOPMENT: "In development",
    WAITING_FOR_REVIEW: "Waiting for review",
    WAITING_FOR_BUILD: "Waiting for build",
    RESOLVED: "Resolved",
}

# The permissions that the sync needs (Jira returns all of them, whatever it is asked).
PERMISSIONS = ("BROWSE_PROJECTS", "TRANSITION_ISSUES", "EDIT_ISSUES", "ADMINISTER_PROJECTS")

RELEASE_TRANSITION = "Resolve"

# The events of a PR that the issue property of a ticket remembers (with the numbers of the PRs).
EVENTS = ("drafted", "opened", "merged", "closed")

# The statuses of the testing step, by name (their ids are not needed elsewhere).
WAITING_FOR_TEST_NAME = "waiting for test"
TESTING_NAME = "testing"


class HttpError(Exception):
    def __init__(self, method, url, code, body):
        super().__init__(f"{method} {url}: HTTP {code} {body[:500]}")
        self.code = code


class Http:
    """JSON over HTTP; the tests replace it."""

    def request(self, method, url, headers, body=None):
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(url, data=data, method=method, headers=headers)

        if data is not None:
            request.add_header("Content-Type", "application/json")

        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                content = response.read()
        except urllib.error.HTTPError as e:
            raise HttpError(method, url, e.code, e.read().decode(errors="replace")) from None

        return json.loads(content) if content else None


class GitHub:
    API = "https://api.github.com"

    def __init__(self, http, token):
        self.http = http
        self.headers = {
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2026-03-10",
            "User-Agent": "dxfeed-graal-cxx-api-jira-sync",
        }

        if token:
            self.headers["Authorization"] = f"Bearer {token}"

    def get(self, path, accept=None):
        headers = {**self.headers, "Accept": accept} if accept else self.headers
        return self.http.request("GET", f"{self.API}/repos/{GITHUB_REPOSITORY}/{path}", headers)

    def pull_requests(self):
        """The PRs, the recently updated first."""
        return self.get("pulls?state=all&sort=updated&direction=desc&per_page=50")

    def open_pull_requests(self):
        return self.get("pulls?state=open&per_page=100")

    def releases(self):
        """The releases with their notes as Markdown, text and the HTML of GitHub (body, body_text, body_html)."""
        return self.get("releases?per_page=30", accept="application/vnd.github.full+json")

    def commit_messages(self, base_tag, head_tag):
        compare = self.get(f"compare/{urllib.parse.quote(base_tag)}...{urllib.parse.quote(head_tag)}")
        return [commit["commit"]["message"] for commit in compare["commits"]]


class Jira:
    def __init__(self, http, url, token):
        self.http = http
        self.url = url.rstrip("/")
        self.headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}

    def call(self, method, path, body=None):
        return self.http.request(method, f"{self.url}/rest/api/2/{path}", self.headers, body)

    def permissions(self):
        result = self.call("GET", f"mypermissions?projectKey={JIRA_PROJECT}&permissions={','.join(PERMISSIONS)}")
        return {key: result["permissions"].get(key, {}).get("havePermission", False) for key in PERMISSIONS}

    def status(self, key):
        return self.call("GET", f"issue/{key}?fields=status")["fields"]["status"]

    def transitions(self, key):
        return self.call("GET", f"issue/{key}/transitions")["transitions"]

    def transition(self, key, transition_id):
        self.call("POST", f"issue/{key}/transitions", {"transition": {"id": transition_id}})

    def property(self, key):
        try:
            return self.call("GET", f"issue/{key}/properties/{ISSUE_PROPERTY}")["value"]
        except HttpError as e:
            if e.code == 404:
                return {}
            raise

    def set_property(self, key, value):
        self.call("PUT", f"issue/{key}/properties/{ISSUE_PROPERTY}", value)

    def link(self, key, url, title):
        # The same globalId updates the link instead of adding another one.
        self.call("POST", f"issue/{key}/remotelink", {
            "globalId": url,
            "application": {"type": "com.github", "name": "GitHub"},
            "object": {"url": url, "title": title},
        })

    def versions(self):
        return self.call("GET", f"project/{JIRA_PROJECT}/versions")

    def create_version(self, name):
        return self.call("POST", "version", {"name": name, "project": JIRA_PROJECT})

    def update_version(self, version_id, fields):
        self.call("PUT", f"version/{version_id}", fields)

    def issues(self, jql):
        query = urllib.parse.urlencode({"jql": jql, "fields": "status,summary", "maxResults": "500"})
        return self.call("GET", f"search?{query}")["issues"]

    def replace_fix_version(self, key, old_id, new_id):
        self.call("PUT", f"issue/{key}", {
            "update": {"fixVersions": [{"remove": {"id": old_id}}, {"add": {"id": new_id}}]}
        })

    def fix_versions(self, key):
        return self.call("GET", f"issue/{key}?fields=fixVersions")["fields"]["fixVersions"]

    def add_fix_version(self, key, version_id):
        self.call("PUT", f"issue/{key}", {"update": {"fixVersions": [{"add": {"id": version_id}}]}})


class Log:
    """The output of the build: TeamCity service messages for the warnings and the problems."""

    def __init__(self, out=sys.stdout):
        self.out = out
        self.problems = []

    @staticmethod
    def escape(text):
        return re.sub(r"(['|\[\]])", r"|\1", text).replace("\n", "|n").replace("\r", "|r")

    def info(self, text):
        print(text, file=self.out)

    def warning(self, text):
        print(f"##teamcity[message text='{self.escape(text)}' status='WARNING']", file=self.out)

    def problem(self, text):
        self.problems.append(text)
        print(f"##teamcity[buildProblem description='{self.escape(text[:4000])}']", file=self.out)


def version_tuple(text):
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", text)
    return tuple(int(part) for part in match.groups()) if match else None


def jira_version_tuple(version):
    name = version["name"]
    return version_tuple(name[len(VERSION_PREFIX):]) if name.startswith(VERSION_PREFIX) else None


def line_major(base, main_major):
    """The major of the versions of a branch: N of release/vN, else the major of main."""
    match = re.fullmatch(r"release/v(\d+)", base)
    return int(match.group(1)) if match else main_major


def pr_keys(pr):
    """The tickets of the PR: the MDAPI keys of its title and its branch."""
    return sorted(set(KEY_PATTERN.findall(f"{pr['title']} {pr['head']['ref']}")))


DOCUMENTATION_URL = "https://dxfeed.github.io/dxfeed-graal-cxx-api/"

# The release mail: simple HTML with inline styles, as Outlook renders it.
MAIL_STYLE = "font-family: 'Segoe UI', Arial, sans-serif; font-size: 14px; line-height: 1.45; color: #1f2328;"
MAIL_CODE_STYLE = (
    "font-family: Consolas, 'Courier New', monospace; font-size: 13px; background: #eff1f3; padding: 0 3px;"
)


def link_tickets(notes_html, jira_url):
    """Links the MDAPI keys of the text of the HTML to the tickets (not in the tags, the links and the code)."""
    parts = re.split(r"(<[^>]+>)", notes_html)
    inside = 0  # the depth of <a> and <code>

    for index, part in enumerate(parts):
        if part.startswith("<"):
            tag = re.match(r"</?(\w+)", part)

            if tag and tag.group(1).lower() in ("a", "code"):
                inside += -1 if part.startswith("</") else 1
        elif inside == 0:
            parts[index] = KEY_PATTERN.sub(
                lambda match: f'<a href="{jira_url}/browse/{match.group(0)}">{match.group(0)}</a>', part)

    return "".join(parts)


def release_mail(release, version_name, version_url, jira_url, build_url):
    """The subject, the HTML and the text of the mail about a GitHub release (with its notes as GitHub renders them)."""
    tag = release["tag_name"]
    date = release["published_at"][:10]
    title = f"dxFeed Graal C++ API {tag}"

    # GitHub renders the line breaks of the Markdown of a release as <br>: the wrapped lines of ReleaseNotes.md.
    notes = re.sub(r"<br>\s*\n", "\n", release.get("body_html") or "").strip() or "<p>No release notes.</p>"
    notes = link_tickets(notes, jira_url).replace("<code>", f'<code style="{MAIL_CODE_STYLE}">')

    links = [
        f'<a href="{html.escape(release["html_url"])}">Release on GitHub</a> (downloads)',
        f'<a href="{DOCUMENTATION_URL}">Documentation</a>',
        f'<a href="{html.escape(version_url)}">Jira version {html.escape(version_name)}</a>',
    ]
    footer = f'Sent by the <a href="{html.escape(build_url)}">TeamCity build</a>.' if build_url else ""

    body = f"""\
<div style="{MAIL_STYLE}">
<p><b>{html.escape(title)}</b> has been released on {date}.</p>
<p>{" &nbsp;|&nbsp; ".join(links)}</p>
<h3 style="font-size: 16px; margin: 18px 0 6px;">Release notes</h3>
{notes}
<hr style="border: none; border-top: 1px solid #d1d9e0; margin-top: 18px;">
<p style="font-size: 12px; color: #59636e;">{footer}</p>
</div>
"""
    text = "\n\n".join(filter(None, [
        f"{title} has been released on {date}.",
        f"Release on GitHub: {release['html_url']}\nDocumentation: {DOCUMENTATION_URL}\n"
        f"Jira version {version_name}: {version_url}",
        release.get("body_text") or release.get("body") or "",
        f"Build: {build_url}" if build_url else "",
    ]))
    return f"[release] {title}", body, text


class Mailer:
    """Sends mail through the relay that Jira uses: it takes the mail of the agents by their addresses, no login."""

    RELAY = "mxeu0.devexperts.com"
    SENDER = "dxcity <dxcity@bots.devexperts.com>"

    def __init__(self, to, cc=()):
        self.to = list(to)
        self.cc = list(cc)

    def recipients(self):
        """For the log: "a@x, b@x (cc c@x)"."""
        return ", ".join(self.to) + (f" (cc {', '.join(self.cc)})" if self.cc else "")

    def send(self, subject, body_html, text):
        message = email.message.EmailMessage()
        message["From"] = self.SENDER
        message["To"] = ", ".join(self.to)

        if self.cc:
            message["Cc"] = ", ".join(self.cc)

        message["Subject"] = subject
        message["Auto-Submitted"] = "auto-generated"
        message.set_content(text)
        message.add_alternative(body_html, subtype="html")

        with smtplib.SMTP(self.RELAY, 25, timeout=60) as smtp:
            smtp.starttls(context=ssl.create_default_context())
            smtp.send_message(message)


def parse_time(text):
    return datetime.datetime.strptime(text, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc)


class Sync:
    def __init__(self, github, jira, log, dry_run, since, mailer=None, build_url=None):
        """
        since: the PRs updated (and merged) earlier are ignored; mailer: sends the release mail (none: no mail);
        build_url: the TeamCity build, for the mail.
        """
        self.github = github
        self.jira = jira
        self.log = log
        self.dry_run = dry_run
        self.since = since
        self.mailer = mailer
        self.build_url = build_url
        self.statuses = {}
        self.properties = {}
        self.simulated = set()  # the tickets that the dry run has moved
        self.open_prs = None
        self.versions = None

    def change(self, description, action):
        """Makes a change in Jira, or only logs it in the dry run."""
        if self.dry_run:
            self.log.info(f"  [dry run] {description}")
            return None

        self.log.info(f"  {description}")
        return action()

    def run(self, resend_mail=None):
        """Syncs the PRs and the releases; resend_mail: a tag (v8.1.0) to send the mail of its release again."""
        self.log_permissions()
        steps = [("pull requests", self.sync_pull_requests), ("releases", self.sync_releases)]

        if resend_mail:
            steps.append(("resent mail", lambda: self.resend_mail(resend_mail)))

        for name, step in steps:
            try:
                step()
            except HttpError as e:
                self.log.problem(f"Sync of the {name} failed: {e}")

        return 1 if self.log.problems else 0

    def log_permissions(self):
        permissions = self.jira.permissions()
        self.log.info(f"Jira permissions in {JIRA_PROJECT}: {permissions}")

        if not permissions.get("ADMINISTER_PROJECTS"):
            self.log.warning(f"No ADMINISTER_PROJECTS in {JIRA_PROJECT}: the release of a Jira version will fail")

    # The pull requests.

    def sync_pull_requests(self):
        recent = [pr for pr in self.github.pull_requests() if parse_time(pr["updated_at"]) >= self.since]

        for pr in reversed(recent):  # the earliest updated first, as they happened
            if pr["merged_at"] is not None and parse_time(pr["merged_at"]) < self.since:
                continue  # an old PR with a new comment

            merged = pr["merged_at"] is not None and MERGE_BASE_PATTERN.match(pr["base"]["ref"]) is not None

            # A PR merged before a run saw it open is also opened: the ticket goes through Waiting for review.
            events = []

            if pr["state"] == "open" and pr["draft"]:
                events.append("drafted")

            if (pr["state"] == "open" and not pr["draft"]) or merged:
                events.append("opened")

            if merged:
                events.append("merged")
            elif pr["state"] == "closed":
                events.append("closed")  # without a merge, or merged into another branch (a stacked PR)

            for key in pr_keys(pr):
                for event in events:
                    try:
                        self.apply(key, pr, event)
                    except HttpError as e:
                        self.log.problem(f"{key}, PR #{pr['number']} {event}: {e}")

    def apply(self, key, pr, event):
        number = pr["number"]
        state = self.state(key)

        if state is None or number in state.get(event, []):
            return

        new_pr = not any(number in state.get(name, []) for name in EVENTS)
        self.log.info(f"{key}: PR #{number} {event} ({pr['html_url']})")
        state.setdefault(event, []).append(number)

        if event == "merged":
            # The branch that the PR is merged into decides the line of the fix version (main or release/vN).
            state.setdefault("bases", {})[str(number)] = pr["base"]["ref"]

        if event == "drafted":
            moved = self.to_development(key, number)
        elif event == "opened":
            moved = self.advance(key, IN_DEVELOPMENT, WAITING_FOR_REVIEW)
        else:
            moved = self.to_build(key, state, number)

        if not moved:
            state[event].remove(number)  # not remembered: the next run tries again
            state.get("bases", {}).pop(str(number), None)
            return

        self.change(f"remember PR #{number} {event}", lambda: self.jira.set_property(key, state))

        if new_pr and event != "closed":
            title = f"PR #{number}: {pr['title']}"
            self.change(f"link {title}", lambda: self.jira.link(key, pr["html_url"], title))

    def advance(self, key, from_status, to_status):
        """Moves the ticket to the status if it is in from_status; False if the transition is missing."""
        status = self.status(key)

        if status["id"] != from_status:
            self.log.info(f"  stays in {status['name']}")
            return True

        return self.move(key, to_status)

    def to_development(self, key, number):
        """A draft PR: the work on the ticket goes on, so it goes back to (or starts) In development."""
        status = self.status(key)

        if status["id"] == CONFIRMED:
            return self.move(key, IN_DEVELOPMENT)

        if status["id"] == WAITING_FOR_BUILD:
            # No direct transition: back to review, then to development.
            return self.move(key, WAITING_FOR_REVIEW) and self.move(key, IN_DEVELOPMENT)

        if status["id"] == WAITING_FOR_REVIEW:
            in_review = [
                other["number"] for other in self.open_pull_requests()
                if key in pr_keys(other) and other["number"] != number and not other["draft"]
            ]

            if not in_review:
                return self.move(key, IN_DEVELOPMENT)

            numbers = ", ".join(f"#{other}" for other in in_review)
            self.log.info(f"  stays in {status['name']}: the PRs {numbers} are in review")
            return True

        self.log.info(f"  stays in {status['name']}")
        return True

    def to_build(self, key, state, number):
        """
        A PR is merged or closed: Waiting for review -> Waiting for build once a PR is merged and the other PRs are
        closed; -> In development if only drafts are open, or if nothing has been merged and nothing is in review (the
        PR has been closed without a merge).
        """
        status = self.status(key)

        if status["id"] != WAITING_FOR_REVIEW:
            self.log.info(f"  stays in {status['name']}")
            return True

        still_open = [
            other for other in self.open_pull_requests()
            if key in pr_keys(other) and other["number"] != number
        ]
        in_review = [other["number"] for other in still_open if not other["draft"]]

        if in_review:
            numbers = ", ".join(f"#{other}" for other in in_review)
            self.log.info(f"  stays in {status['name']}: the PRs {numbers} are in review")
            return True

        if still_open:
            # Only drafts are open: the work goes on.
            numbers = ", ".join(f"#{other['number']}" for other in still_open)
            self.log.info(f"  the draft PRs {numbers} are open")
            return self.move(key, IN_DEVELOPMENT)

        if not state.get("merged"):
            # The PR is closed without a merge, and nothing has been merged: the work goes back to development.
            self.log.info("  no merged PR and no PR in review")
            return self.move(key, IN_DEVELOPMENT)

        if not self.move(key, WAITING_FOR_BUILD):
            return False

        self.set_fix_version(key, state)
        return True

    def set_fix_version(self, key, state):
        """
        Gives the ticket the unreleased graal-cxx-api version of its line: the major of release/vN for a PR merged
        there, else the major of main (of the last released version); the highest line of its merged PRs, as the
        ticket is done when its last part is released. Creates the version if there is none: vN.0.0 for release/vN,
        the patch after the last released version for main (the release renames it after its tag). Keeps an
        unreleased version of the ticket of that line or a later one, and replaces one of an earlier line.
        """
        try:
            versions = self.cxx_versions()
            released = [jira_version_tuple(version) for version in versions if version["released"]]
            last_released = max(released, default=(0, 0, 0))
            bases = state.get("bases") or {"": "main"}  # the tickets remembered before the bases: main
            major = max(line_major(base, last_released[0]) for base in bases.values())
            unreleased = {
                version["id"]: version for version in versions if not version["released"] and not version["archived"]
            }
            current = [
                unreleased[version["id"]] for version in self.jira.fix_versions(key) if version["id"] in unreleased
            ]

            if any(jira_version_tuple(version)[0] >= major for version in current):
                return

            line = [version for version in unreleased.values() if jira_version_tuple(version)[0] == major]

            if len(line) > 1:
                names = ", ".join(version["name"] for version in line)
                self.log.warning(f"{key}: the unreleased graal-cxx-api versions of v{major} ({names}) are not a "
                                 f"single next release: set the fix version by hand")
                return

            if line:
                version = line[0]
            elif major == last_released[0]:
                version = self.next_version(".".join(map(str, last_released)), versions)
            else:
                version = self.ensure_version(f"{VERSION_PREFIX}{major}.0.0", versions)

            for earlier in current:
                self.change(f"fix version {earlier['name']} -> {version['name']}",
                            lambda earlier=earlier: self.jira.replace_fix_version(key, earlier["id"], version["id"]))

            if not current:
                self.change(f"fix version {version['name']}", lambda: self.jira.add_fix_version(key, version["id"]))
        except HttpError as e:
            # The ticket has been moved: the fix version alone is left to be set by hand.
            self.log.problem(f"{key}: the fix version is not set: {e}")

    def cxx_versions(self):
        """The graal-cxx-api versions of Jira for the PRs (read once, with the versions created by the run)."""
        if self.versions is None:
            self.versions = [version for version in self.jira.versions() if jira_version_tuple(version) is not None]

        return self.versions

    def open_pull_requests(self):
        """The open PRs (the drafts too), as GitHub has them now."""
        if self.open_prs is None:
            self.open_prs = self.github.open_pull_requests()

        return self.open_prs

    def state(self, key):
        """The PRs applied to the ticket, or None if there is no such ticket."""
        if key not in self.properties:
            try:
                self.status(key)  # Jira answers 404 for a missing property too, so the ticket is checked first
            except HttpError as e:
                if e.code != 404:
                    raise

                self.log.warning(f"{key}: no such ticket")
                self.properties[key] = None
                return None

            self.properties[key] = self.jira.property(key)

        return self.properties[key]

    def status(self, key):
        if key not in self.statuses:
            self.statuses[key] = self.jira.status(key)

        return self.statuses[key]

    def move(self, key, to_status, prefer=None, report=None):
        """
        Moves the ticket by a transition to the status (the one of the name prefer, if there is one). If there is no
        such transition, reports it (a build problem by default) and returns False.
        """
        if key in self.simulated:
            # Jira still has the ticket in the status before the dry run moved it, so its transitions are unknown.
            to_name = STATUS_NAMES.get(to_status, to_status)
            self.change(f"then to {to_name} (the transition is not checked in the dry run)", lambda: None)
            self.statuses[key] = {"id": to_status, "name": to_name}
            return True

        transitions = sorted(
            (transition for transition in self.jira.transitions(key) if transition["to"]["id"] == to_status),
            key=lambda transition: transition["name"] != prefer,
        )

        if not transitions:
            to_name = STATUS_NAMES.get(to_status, to_status)
            (report or self.log.problem)(
                f"{key}: no transition from {self.status(key)['name']} to {to_name} for the Jira user of the build"
            )
            return False

        transition = transitions[0]
        description = f"{transition['name']}: {self.status(key)['name']} -> {transition['to']['name']}"
        self.change(description, lambda: self.jira.transition(key, transition["id"]))
        self.statuses[key] = transition["to"]

        if self.dry_run:
            self.simulated.add(key)

        return True

    # The releases.

    def sync_releases(self):
        versions = [version for version in self.jira.versions() if jira_version_tuple(version) is not None]
        released = [jira_version_tuple(version) for version in versions if version["released"]]
        last_released = max(released, default=(0, 0, 0))

        published = sorted(
            (
                release for release in self.github.releases()
                if not release["draft"] and not release["prerelease"] and TAG_PATTERN.match(release["tag_name"])
            ),
            key=lambda release: version_tuple(release["tag_name"][1:]),
        )

        new = [index for index, release in enumerate(published)
               if version_tuple(release["tag_name"][1:]) > last_released]

        if not new:
            last = VERSION_PREFIX + ".".join(map(str, last_released))
            self.log.info(f"No GitHub releases after the last released Jira version {last}")

        for index in new:
            self.release(published[index], published[index - 1] if index > 0 else None, versions)

    def release(self, release, previous, versions):
        number = release["tag_name"][1:]
        name = VERSION_PREFIX + number
        self.log.info(f"GitHub release {release['tag_name']} -> Jira version {name}")
        major = version_tuple(number)[0]
        version = next((version for version in versions if version["name"] == name), None)
        unreleased = [
            other for other in versions if not other["released"] and not other["archived"] and other is not version
        ]

        if version is None:
            # The placeholder of the line (of the major of the tag), renamed after the tag, or a new version.
            line = [other for other in unreleased if jira_version_tuple(other)[0] == major]

            if len(line) > 1:
                names = ", ".join(other["name"] for other in line)
                self.log.problem(
                    f"No Jira version {name}, and the unreleased graal-cxx-api versions of v{major} ({names}) are not "
                    f"a single placeholder to rename: create or rename the version by hand"
                )
                return

            if line:
                version = line[0]
                self.change(f"rename {version['name']} -> {name}",
                            lambda: self.jira.update_version(version["id"], {"name": name}))
                version["name"] = name
                unreleased.remove(version)
            else:
                version = self.ensure_version(name, versions)

        tickets = self.jira.issues(f"fixVersion = {version['id']} ORDER BY key")

        # The earlier lines end with this release (main has them all, and they are not maintained after it): their
        # unreleased versions give their tickets to it and are archived.
        for earlier in [other for other in unreleased if jira_version_tuple(other)[0] < major]:
            for ticket in self.jira.issues(f"fixVersion = {earlier['id']} ORDER BY key"):
                key = ticket["key"]
                self.change(f"{key}: fix version {earlier['name']} -> {name}",
                            lambda key=key, earlier=earlier: self.jira.replace_fix_version(
                                key, earlier["id"], version["id"]))

                if all(other["key"] != key for other in tickets):
                    tickets.append(ticket)

            self.change(f"archive {earlier['name']}",
                        lambda earlier=earlier: self.jira.update_version(earlier["id"], {"archived": True}))
            earlier["archived"] = True

        keys = [ticket["key"] for ticket in tickets]
        self.log.info(f"  tickets: {', '.join(keys) or 'none'}")

        if previous is not None:
            self.check_commits(previous["tag_name"], release["tag_name"], keys)

        unresolved = []

        for ticket in tickets:
            key = ticket["key"]
            status = ticket["fields"]["status"]
            status_name = status["name"].lower()
            self.statuses[key] = status

            if status["id"] == WAITING_FOR_BUILD:
                self.log.info(f"{key}:")
                self.move(key, RESOLVED, RELEASE_TRANSITION)
            elif status_name == WAITING_FOR_TEST_NAME:
                self.log.info(f"{key}:")
                self.move(key, RESOLVED, RELEASE_TRANSITION, report=lambda text: self.log.warning(
                    f"{text}: the ticket stays in {status['name']}, resolve it by hand"))
            elif status_name == TESTING_NAME:
                self.log.info(f"{key} of {name} is in {status['name']}: it stays there")
            elif status["statusCategory"]["key"] != "done":
                self.log.warning(f"{key} of {name} is not finished ({status['name']}): it moves to the next version")
                unresolved.append(key)
            elif status["id"] != RESOLVED:
                self.log.info(f"{key} of {name} is in {status['name']}: it stays there")

        next_version = self.next_version(number, versions)

        for key in unresolved:
            self.change(f"{key}: fix version {name} -> {next_version['name']}",
                        lambda key=key: self.jira.replace_fix_version(key, version["id"], next_version["id"]))

        # The mail before the release of the version: a failed mail leaves the version unreleased, so the next run
        # repeats the release (its steps are done already) and the mail.
        if not self.mail(release, version):
            return

        date = release["published_at"][:10]
        self.change(f"release {name} on {date}",
                    lambda: self.jira.update_version(version["id"], {"released": True, "releaseDate": date}))
        version["released"] = True

    def mail(self, release, version):
        """Sends the release mail to the recipients (if any); False if it fails."""
        if self.mailer is None or not self.mailer.to:
            return True

        version_url = f"{self.jira.url}/projects/{JIRA_PROJECT}/versions/{version['id']}"
        subject, body_html, text = release_mail(release, version["name"], version_url, self.jira.url, self.build_url)

        try:
            self.change(f"mail {subject} to {self.mailer.recipients()}",
                        lambda: self.mailer.send(subject, body_html, text))
            return True
        except (OSError, smtplib.SMTPException) as e:
            self.log.problem(f"The release mail is not sent (the next run tries again): {e}")
            return False

    def resend_mail(self, tag):
        """Sends the mail of the release of the tag again (RESEND_MAIL, set in the Run dialog of the build)."""
        release = next((release for release in self.github.releases() if release["tag_name"] == tag), None)
        name = VERSION_PREFIX + tag[1:]
        version = next((version for version in self.jira.versions() if version["name"] == name), None)

        if release is None or version is None:
            self.log.problem(f"No GitHub release {tag} or no Jira version {name} to resend the mail of")
            return

        self.log.info(f"Resending the mail of {tag}")
        self.mail(release, version)

    def next_version(self, number, versions):
        """The version of the patch after the version number ("8.1.0"), created if needed."""
        major, minor, patch = version_tuple(number)
        return self.ensure_version(f"{VERSION_PREFIX}{major}.{minor}.{patch + 1}", versions)

    def ensure_version(self, name, versions):
        """The Jira version of the name, created (and added to versions) if there is none."""
        existing = next((version for version in versions if version["name"] == name), None)

        if existing is not None:
            return existing

        created = self.change(f"create the version {name}", lambda: self.jira.create_version(name))
        version = {**(created if created is not None else {"id": "(new)", "name": name}),
                   "released": False, "archived": False}
        versions.append(version)
        return version

    def check_commits(self, previous_tag, tag, keys):
        """Warns about the tickets of the commits of the release without the version, and the other way round."""
        committed = set()

        for message in self.github.commit_messages(previous_tag, tag):
            committed.update(KEY_PATTERN.findall(message))

        for key in sorted(committed - set(keys)):
            self.log.warning(f"{key} is in the commits {previous_tag}..{tag} but has no fix version {tag}")

        for key in sorted(set(keys) - committed):
            self.log.warning(f"{key} has the fix version {tag} but no commits {previous_tag}..{tag}")


def main():
    dry_run = os.environ.get("DRY_RUN", "1") != "0"
    log = Log()
    log.info(f"Dry run: {dry_run}")
    http = Http()
    lookback = datetime.timedelta(hours=int(os.environ.get("LOOKBACK_HOURS", "48")))
    def addresses(name):
        return [address.strip() for address in os.environ.get(name, "").split(",") if address.strip()]

    sync = Sync(
        github=GitHub(http, os.environ.get("GH_TOKEN")),
        jira=Jira(http, os.environ["JIRA_URL"], os.environ["JIRA_TOKEN"]),
        log=log,
        dry_run=dry_run,
        since=datetime.datetime.now(datetime.timezone.utc) - lookback,
        mailer=Mailer(addresses("MAIL_TO"), addresses("MAIL_CC")),
        build_url=os.environ.get("BUILD_URL"),
    )
    return sync.run(resend_mail=os.environ.get("RESEND_MAIL", "").strip())


if __name__ == "__main__":
    sys.exit(main())
