"""
The parts that the Jira automation of the C++ API (cxx_api_jira_sync.py) and of the SDK releases (sdk_jira_release.py)
share: JSON over HTTP, the REST clients of GitHub and Jira, the build log, the release mail, and the changes of the
tickets that the dry run only logs. The project, the repository and the version names are the arguments of the
scripts.

Python 3.10 (the TeamCity agents), the standard library only.
"""

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
    """The REST API of a repository (owner/name); user_agent: the name of the script, as GitHub asks."""

    API = "https://api.github.com"

    def __init__(self, http, token, repository, user_agent):
        self.http = http
        self.repository = repository
        self.headers = {
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2026-03-10",
            "User-Agent": user_agent,
        }

        if token:
            self.headers["Authorization"] = f"Bearer {token}"

    def get(self, path, accept=None):
        headers = {**self.headers, "Accept": accept} if accept else self.headers
        return self.http.request("GET", f"{self.API}/repos/{self.repository}/{path}", headers)

    def releases(self):
        """The releases with their notes as Markdown, text and the HTML of GitHub (body, body_text, body_html)."""
        return self.get("releases?per_page=30", accept="application/vnd.github.full+json")

    def commit_messages(self, base_tag, head_tag):
        compare = self.get(f"compare/{urllib.parse.quote(base_tag)}...{urllib.parse.quote(head_tag)}")
        return [commit["commit"]["message"] for commit in compare["commits"]]


class Jira:
    """The REST API of Jira for the tickets and the versions of a project."""

    def __init__(self, http, url, token, project):
        self.http = http
        self.url = url.rstrip("/")
        self.project = project
        self.headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}

    def call(self, method, path, body=None):
        return self.http.request(method, f"{self.url}/rest/api/2/{path}", self.headers, body)

    def status(self, key):
        return self.call("GET", f"issue/{key}?fields=status")["fields"]["status"]

    def transitions(self, key):
        return self.call("GET", f"issue/{key}/transitions")["transitions"]

    def transition(self, key, transition_id):
        self.call("POST", f"issue/{key}/transitions", {"transition": {"id": transition_id}})

    def versions(self):
        return self.call("GET", f"project/{self.project}/versions")

    def create_version(self, name):
        return self.call("POST", "version", {"name": name, "project": self.project})

    def update_version(self, version_id, fields):
        self.call("PUT", f"version/{version_id}", fields)

    def issues(self, jql, fields="status,summary"):
        query = urllib.parse.urlencode({"jql": jql, "fields": fields, "maxResults": "500"})
        return self.call("GET", f"search?{query}")["issues"]

    def update_fix_versions(self, key, remove_ids, add_id=None):
        """Removes the fix versions of the ids and adds the one of add_id (if any), in one change of the ticket."""
        operations = [{"remove": {"id": version_id}} for version_id in remove_ids]
        operations += [{"add": {"id": add_id}}] if add_id is not None else []
        self.call("PUT", f"issue/{key}", {"update": {"fixVersions": operations}})


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


def prefixed_version_tuple(version, prefix):
    """The number of the Jira version named prefix + X.Y.Z ("graal-cxx-api v8.1.0" -> (8, 1, 0)), else None."""
    name = version["name"]
    return version_tuple(name[len(prefix):]) if name.startswith(prefix) else None


# The release mail: simple HTML with inline styles, as Outlook renders it.
MAIL_STYLE = "font-family: 'Segoe UI', Arial, sans-serif; font-size: 14px; line-height: 1.45; color: #1f2328;"
MAIL_CODE_STYLE = (
    "font-family: Consolas, 'Courier New', monospace; font-size: 13px; background: #eff1f3; padding: 0 3px;"
)


MAIL_CODE_BLOCK_STYLE = (
    "font-family: Consolas, 'Courier New', monospace; font-size: 13px; line-height: 1.4; background: #f6f8fa; "
    "padding: 8px 12px; border: 1px solid #d1d9e0;"
)
# The colors of the classes that GitHub marks the highlighted code with (its light theme); the others keep the color of
# the text.
CODE_COLORS = {
    "pl-c": "#59636e",  # a comment
    "pl-c1": "#0550ae",  # a constant, a number
    "pl-k": "#cf222e",  # a keyword
    "pl-s": "#0a3069",  # a string
    "pl-pds": "#0a3069",  # the quotes of a string
    "pl-sr": "#0a3069",  # a regular expression
    "pl-en": "#6639ba",  # the name of a function, a type
    "pl-e": "#6639ba",
    "pl-v": "#953800",  # a variable
    "pl-smw": "#953800",
    "pl-ent": "#116329",  # a tag
}


def highlight_code_blocks(notes_html):
    """
    The code blocks of the HTML that GitHub renders, for a mail: the classes of the highlighted code (the mail clients
    drop the styles of GitHub) as the inline colors, a block as a table cell (Outlook makes the lines of a <pre> in a
    list the items of the list).
    """
    notes = re.sub(r'<div class="highlight[^"]*"[^>]*>', "<div>", notes_html)
    notes = re.sub(r"<pre\b[^>]*>(.*?)</pre>", lambda match: code_block(match.group(1)), notes, flags=re.S)
    return re.sub(r'<span class="(pl-[\w-]+)">',
                  lambda match: f'<span style="color: {CODE_COLORS[match.group(1)]};">'
                  if match.group(1) in CODE_COLORS else "<span>", notes)


def code_block(code_html):
    """A code block as a table cell: the lines broken with <br>, the indents and the runs of spaces as &nbsp;."""
    code = re.sub(r"^\s*<code[^>]*>|</code>\s*$", "", code_html).strip("\n")  # no inline style of the code
    code = re.sub(r"<[^>]+>| {2,}|^ |(?<=\n) ",
                  lambda match: match.group(0) if match.group(0).startswith("<") else "&nbsp;" * len(match.group(0)),
                  code)
    lines = code.replace("\n", "<br>\n")
    return ('<table role="presentation" width="100%" cellpadding="0" cellspacing="0" '
            f'style="border-collapse: collapse; margin: 8px 0;"><tr><td style="{MAIL_CODE_BLOCK_STYLE}">{lines}</td>'
            "</tr></table>")


def link_tickets(notes_html, jira_url, key_pattern):
    """Links the keys of the text of the HTML to the tickets (not in the tags, the links and the code)."""
    parts = re.split(r"(<[^>]+>)", notes_html)
    inside = 0  # the depth of <a> and <code>

    for index, part in enumerate(parts):
        if part.startswith("<"):
            tag = re.match(r"</?(\w+)", part)

            if tag and tag.group(1).lower() in ("a", "code"):
                inside += -1 if part.startswith("</") else 1
        elif inside == 0:
            parts[index] = key_pattern.sub(
                lambda match: f'<a href="{jira_url}/browse/{match.group(0)}">{match.group(0)}</a>', part)

    return "".join(parts)


def release_notes_html(release, jira_url, key_pattern):
    """
    The notes of the GitHub release as GitHub renders them, for the mail: the tickets linked, the code styled and
    highlighted.
    """
    # GitHub renders the line breaks of the Markdown of a release as <br>: the wrapped lines of ReleaseNotes.md.
    notes = re.sub(r"<br>\s*\n", "\n", release.get("body_html") or "").strip() or "<p>No release notes.</p>"
    return highlight_code_blocks(
        link_tickets(notes, jira_url, key_pattern).replace("<code>", f'<code style="{MAIL_CODE_STYLE}">'))


def release_mail_html(title, date, links, sections, build_url):
    """
    The HTML of a release mail: the title released on the date, the row of the links (HTML), the sections (the
    heading, the HTML) and the link to the build in the footer.
    """
    footer = f'Sent by the <a href="{html.escape(build_url)}">TeamCity build</a>.' if build_url else ""
    lines = [
        f'<div style="{MAIL_STYLE}">',
        f"<p><b>{html.escape(title)}</b> has been released on {date}.</p>",
        f'<p>{" &nbsp;|&nbsp; ".join(links)}</p>',
    ]

    for heading, content in sections:
        lines += [f'<h3 style="font-size: 16px; margin: 18px 0 6px;">{html.escape(heading)}</h3>', content]

    lines += [
        '<hr style="border: none; border-top: 1px solid #d1d9e0; margin-top: 18px;">',
        f'<p style="font-size: 12px; color: #59636e;">{footer}</p>',
        "</div>",
        "",
    ]
    return "\n".join(lines)


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


def env_list(name):
    """The comma-separated items of the environment variable (MAIL_TO, MAIL_CC) as a list."""
    return [item.strip() for item in os.environ.get(name, "").split(",") if item.strip()]


class Workflow:
    """
    The changes of the tickets and the versions of a run: made in Jira, or only logged in the dry run, which
    remembers the statuses it has moved the tickets to.
    """

    def __init__(self, jira, log, dry_run, status_names, mailer=None):
        """
        status_names: the status id -> the name, for the log of the moves of the dry run; mailer: sends the mail (none:
        no mail).
        """
        self.jira = jira
        self.log = log
        self.dry_run = dry_run
        self.status_names = status_names
        self.mailer = mailer
        self.statuses = {}
        self.simulated = set()  # the tickets that the dry run has moved

    def change(self, description, action):
        """Makes a change in Jira, or only logs it in the dry run."""
        if self.dry_run:
            self.log.info(f"  [dry run] {description}")
            return None

        self.log.info(f"  {description}")
        return action()

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
            to_name = self.status_names.get(to_status, to_status)
            self.change(f"then to {to_name} (the transition is not checked in the dry run)", lambda: None)
            self.statuses[key] = {"id": to_status, "name": to_name}
            return True

        transitions = sorted(
            (transition for transition in self.jira.transitions(key) if transition["to"]["id"] == to_status),
            key=lambda transition: transition["name"] != prefer,
        )

        if not transitions:
            to_name = self.status_names.get(to_status, to_status)
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

    def send_mail(self, subject, body_html, text, failed):
        """
        Sends the mail to the recipients of the mailer (if any); failed: reports the error of the relay. False if the
        mail is not sent.
        """
        if self.mailer is None or not self.mailer.to:
            return True

        try:
            self.change(f"mail {subject} to {self.mailer.recipients()}",
                        lambda: self.mailer.send(subject, body_html, text))
            return True
        except (OSError, smtplib.SMTPException) as e:
            failed(e)
            return False
