# Security Policy

ArticleShelf is a personal portfolio application, but security reports are handled privately and seriously.

## Supported Scope

Security reports are accepted for the current `main` branch and the public deployment linked from the README.

In scope:

- Authentication, authorization, session, refresh token, and CSRF behavior
- User data isolation for articles, tags, notes, and read history
- OGP fetching, SSRF protection, redirects, and external URL handling
- Markdown rendering and sanitization
- Production configuration guards for secrets, cookies, CORS, and database transport

Out of scope:

- Denial-of-service tests that create excessive traffic or cost
- Social engineering, phishing, or physical attacks
- Reports that only affect unsupported local development settings
- Scanner-only reports without a reproducible impact

## Reporting a Vulnerability

Please do not open a public GitHub issue for a suspected vulnerability.

If you find a security issue, contact the repository owner privately by using the contact information on the GitHub profile. Include as much of the following as possible:

- Affected URL, endpoint, screen, or source file
- Reproduction steps and expected impact
- Proof-of-concept request, response, or screenshot when safe to share
- Whether the issue affects the public deployment, local development, or both
- Any constraints that would help reproduce the issue safely

## Response Process

This is a personal project, so response times are best-effort rather than a guaranteed SLA.

The usual handling flow is:

1. Acknowledge the report after it is seen.
2. Reproduce the issue and assess severity and affected scope.
3. Prepare a fix and update relevant tests or documentation.
4. Deploy or publish the fix.
5. Credit the reporter if they want to be credited.

If a report describes active exploitation or likely user data exposure, it is treated as the highest priority.

## Security Documentation

Current implementation details are documented in [docs/specs/security/README.md](docs/specs/security/README.md). That document describes the security behavior of the application; this file describes the private vulnerability reporting flow.
