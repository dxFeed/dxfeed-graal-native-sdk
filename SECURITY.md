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

## Deserialization and JMX

The SDK deserializes Java objects from a remote peer (event attachments, object fields of QD records, ORCS results)
only for the classes of an allow-list. The JMX RMI connector that the endpoint property `jmx.rmi.port` starts has no
authentication and listens on all network interfaces: do not use it outside a trusted network; a process-wide
`jdk.serialFilter` in the isolate arguments also filters what it deserializes. See
[Deserialization](README.md#deserialization).

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
