# Security Policy

## Supported versions

Security fixes go to the latest minor version of 3.x and come out as a new patch or minor version. Older versions get
no fixes; update to the latest version.

| Version              | Supported |
|----------------------|-----------|
| latest 3.x minor     | yes       |
| older 3.x, 2.x, 1.x  | no        |

A library of the SDK contains the JDK and the compiled code of the GraalVM that built it. The SDK follows the security
updates of GraalVM and the JDK as described in
[Security updates of GraalVM and the JDK](README.md#security-updates-of-graalvm-and-the-jdk); that section also lists
the platforms that get no fixes of the JDK after a certain version. The software bill of materials (SBOM) of each
release names the GraalVM, the JDK and the Java libraries inside each library (see
[SBOM and checksums](README.md#sbom-and-checksums)).

## Reporting a vulnerability

Do not report a vulnerability in a public issue, discussion or pull request.

Report it privately on GitHub: open the **Security** tab of this repository and click **Report a vulnerability**
([GitHub documentation](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)).
Describe the affected version and platform, the impact and, if you can, the steps to reproduce it.

We acknowledge a report within 5 business days, keep you informed of the progress and agree with you on the disclosure.
A fixed version is published with a GitHub security advisory.

A vulnerability of GraalVM or the JDK itself should also be reported to Oracle as described in the
[GraalVM security policy](https://github.com/oracle/graal/blob/master/SECURITY.md); a report to us tells us whether and when the SDK
must be rebuilt.
