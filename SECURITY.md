# Security policy

## Reporting a vulnerability

Report privately — through GitHub's **Report a vulnerability** (Security tab) or to
[@vazovsky17](https://t.me/vazovsky17) on Telegram. Please do not open a public issue.

Never include your configuration or a `vless://` link: it is a credential.

You will get an answer within a few days. Confirmed issues are fixed in the next release and credited
in its notes unless you prefer otherwise.

## Scope

- the Android app in this repository and its release builds from [vazie.app](https://vazie.app);
- how the app stores configurations, talks to `api.vazie.app` / `vpn.api.vazie.app`, and what it sends
  where (see the README's "Where it connects").

Vazie's servers and backend are out of scope for testing: please report what you notice, but do not
probe or load them.

## What the project does to stay safe

- Configurations are encrypted with a key in the Android Keystore; system backup is disabled.
- Credentials are wrapped in `Secret`, which cannot be logged or serialized by accident.
- No secrets in the repository: signing keys and API keys come from GitHub secrets or local files that
  are ignored by git.
- CI validates the Gradle wrapper, pins actions by commit SHA, verifies the native engine by SHA-256
  and scans every release APK for credential shapes, debug tools and unexpected hosts.
