#!/usr/bin/env python3
"""
Moves the MDAPI tickets of the C++ API (dxfeed-graal-cxx-api on GitHub) through the Jira workflow, as the Jira
triggers of Bitbucket do for the internal repositories, and releases the Jira versions of its GitHub releases.

The pull requests updated during the last LOOKBACK_HOURS (the tickets are the MDAPI keys of the title and the branch):
  - a PR is opened (not a draft): In development -> Waiting for review;
  - a PR is merged into main or release/*: Waiting for review -> Waiting for build.
Each PR is applied to a ticket once (the ticket keeps the numbers of the applied PRs in an issue property), so a
ticket moved back by hand stays where it is. The PR is also linked from the ticket.

A published GitHub release vX.Y.Z (not a draft, not a pre-release) newer than the last released Jira version
"graal-cxx-api vX.Y.Z":
  - the Jira version is the one of that name, or else the only unreleased graal-cxx-api version (the placeholder of the
    next release), which is renamed;
  - its tickets in Waiting for build -> Resolved (the transition "Resolve", as the Release Steps of MDAPI do);
  - its unresolved tickets -> the next version vX.Y.(Z+1), the placeholder, which is created if needed;
  - the version is released with the date of the GitHub release.

The environment: JIRA_URL, JIRA_TOKEN (a personal access token), GH_TOKEN (optional, read-only access is enough),
DRY_RUN ("1" by default: reads everything and logs the changes instead of making them), LOOKBACK_HOURS (48).

Python 3.10 (the TeamCity agents), the standard library only.
"""

import datetime
import json
import os
import re
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
IN_DEVELOPMENT = "10003"
WAITING_FOR_REVIEW = "10013"
WAITING_FOR_BUILD = "10008"
RESOLVED = "10011"

RELEASE_TRANSITION = "Resolve"


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

    def get(self, path):
        return self.http.request("GET", f"{self.API}/repos/{GITHUB_REPOSITORY}/{path}", self.headers)

    def pull_requests(self):
        """The PRs, the recently updated first."""
        return self.get("pulls?state=all&sort=updated&direction=desc&per_page=50")

    def releases(self):
        return self.get("releases?per_page=30")

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
        keys = "BROWSE_PROJECTS,TRANSITION_ISSUES,EDIT_ISSUES,ADMINISTER_PROJECTS"
        result = self.call("GET", f"mypermissions?projectKey={JIRA_PROJECT}&permissions={keys}")
        return {key: value["havePermission"] for key, value in result["permissions"].items()}

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
        query = urllib.parse.urlencode({"jql": jql, "fields": "status,resolution,summary", "maxResults": "500"})
        return self.call("GET", f"search?{query}")["issues"]

    def replace_fix_version(self, key, old_id, new_id):
        self.call("PUT", f"issue/{key}", {
            "update": {"fixVersions": [{"remove": {"id": old_id}}, {"add": {"id": new_id}}]}
        })


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


def parse_time(text):
    return datetime.datetime.strptime(text, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc)


class Sync:
    def __init__(self, github, jira, log, dry_run, now, lookback_hours):
        self.github = github
        self.jira = jira
        self.log = log
        self.dry_run = dry_run
        self.since = now - datetime.timedelta(hours=lookback_hours)
        self.statuses = {}
        self.properties = {}
        self.simulated = set()  # the tickets that the dry run has moved

    def change(self, description, action):
        """Makes a change in Jira, or only logs it in the dry run."""
        if self.dry_run:
            self.log.info(f"  [dry run] {description}")
            return None

        self.log.info(f"  {description}")
        return action()

    def run(self):
        self.log_permissions()

        for name, step in (("pull requests", self.sync_pull_requests), ("releases", self.sync_releases)):
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
        for pr in self.github.pull_requests():
            if parse_time(pr["updated_at"]) < self.since:
                break  # the rest were updated even earlier

            if pr["merged_at"] is not None and parse_time(pr["merged_at"]) < self.since:
                continue  # an old PR with a new comment

            keys = sorted(set(KEY_PATTERN.findall(f"{pr['title']} {pr['head']['ref']}")))
            merged = pr["merged_at"] is not None and MERGE_BASE_PATTERN.match(pr["base"]["ref"]) is not None

            # A PR merged before a run saw it open is also opened: the ticket goes through Waiting for review.
            events = []

            if (pr["state"] == "open" and not pr["draft"]) or merged:
                events.append(("opened", IN_DEVELOPMENT, WAITING_FOR_REVIEW))

            if merged:
                events.append(("merged", WAITING_FOR_REVIEW, WAITING_FOR_BUILD))

            for key in keys:
                for event, from_status, to_status in events:
                    try:
                        self.apply(key, pr, event, from_status, to_status)
                    except HttpError as e:
                        self.log.problem(f"{key}, PR #{pr['number']} {event}: {e}")

    def apply(self, key, pr, event, from_status, to_status):
        number = pr["number"]
        state = self.state(key)

        if state is None or number in state.get(event, []):
            return

        new_pr = not any(number in numbers for numbers in state.values())
        self.log.info(f"{key}: PR #{number} {event} ({pr['html_url']})")
        status = self.status(key)

        if status["id"] != from_status:
            self.log.info(f"  stays in {status['name']}")
        elif not self.move(key, to_status):
            return  # not remembered: the next run tries again

        state.setdefault(event, []).append(number)
        self.change(f"remember PR #{number} {event}", lambda: self.jira.set_property(key, state))

        if new_pr:
            title = f"PR #{number}: {pr['title']}"
            self.change(f"link {title}", lambda: self.jira.link(key, pr["html_url"], title))

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

    def move(self, key, to_status, name=None):
        """Moves the ticket by the transition to the status (and of the name, if given); False if there is none."""
        if key in self.simulated:
            # Jira still has the ticket in the status before the dry run moved it, so its transitions are unknown.
            self.change(f"then to the status {to_status} (the transition is not checked in the dry run)", lambda: None)
            return True

        transitions = [
            transition for transition in self.jira.transitions(key)
            if transition["to"]["id"] == to_status and (name is None or transition["name"] == name)
        ]

        if not transitions:
            self.log.problem(f"{key}: no transition to the status {to_status} for the Jira user of the build")
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

        for index, release in enumerate(published):
            if version_tuple(release["tag_name"][1:]) > last_released:
                previous = published[index - 1] if index > 0 else None
                self.release(release, previous, versions)

    def release(self, release, previous, versions):
        number = release["tag_name"][1:]
        name = VERSION_PREFIX + number
        self.log.info(f"GitHub release {release['tag_name']} -> Jira version {name}")
        version = next((version for version in versions if version["name"] == name), None)

        if version is None:
            unreleased = [version for version in versions if not version["released"] and not version["archived"]]

            if len(unreleased) != 1:
                names = ", ".join(version["name"] for version in unreleased) or "none"
                self.log.problem(
                    f"No Jira version {name}, and the unreleased graal-cxx-api versions ({names}) are not a single "
                    f"placeholder to rename: create or rename the version by hand"
                )
                return

            version = unreleased[0]
            self.change(f"rename {version['name']} -> {name}",
                        lambda: self.jira.update_version(version["id"], {"name": name}))
            version["name"] = name

        tickets = self.jira.issues(f"fixVersion = {version['id']} ORDER BY key")
        keys = [ticket["key"] for ticket in tickets]
        self.log.info(f"  tickets: {', '.join(keys) or 'none'}")

        if previous is not None:
            self.check_commits(previous["tag_name"], release["tag_name"], keys)

        unresolved = []

        for ticket in tickets:
            key = ticket["key"]
            status = ticket["fields"]["status"]
            self.statuses[key] = status

            if status["id"] == WAITING_FOR_BUILD:
                self.log.info(f"{key}:")
                self.move(key, RESOLVED, RELEASE_TRANSITION)
            elif ticket["fields"]["resolution"] is None:
                self.log.warning(f"{key} of {name} is not finished ({status['name']}): it moves to the next version")
                unresolved.append(key)
            elif status["id"] != RESOLVED:
                self.log.warning(f"{key} of {name} is in {status['name']}, not in Waiting for build: it stays there")

        next_version = self.next_version(number, versions)

        for key in unresolved:
            self.change(f"{key}: fix version {name} -> {next_version['name']}",
                        lambda key=key: self.jira.replace_fix_version(key, version["id"], next_version["id"]))

        date = release["published_at"][:10]
        self.change(f"release {name} on {date}",
                    lambda: self.jira.update_version(version["id"], {"released": True, "releaseDate": date}))
        version["released"] = True

    def next_version(self, number, versions):
        major, minor, patch = version_tuple(number)
        name = f"{VERSION_PREFIX}{major}.{minor}.{patch + 1}"
        existing = next((version for version in versions if version["name"] == name), None)

        if existing is not None:
            return existing

        created = self.change(f"create the next version {name}", lambda: self.jira.create_version(name))
        version = created if created is not None else {"id": "(new)", "name": name}
        versions.append({**version, "released": False, "archived": False})
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
    sync = Sync(
        GitHub(http, os.environ.get("GH_TOKEN")),
        Jira(http, os.environ["JIRA_URL"], os.environ["JIRA_TOKEN"]),
        log,
        dry_run,
        datetime.datetime.now(datetime.timezone.utc),
        int(os.environ.get("LOOKBACK_HOURS", "48")),
    )
    return sync.run()


if __name__ == "__main__":
    sys.exit(main())
