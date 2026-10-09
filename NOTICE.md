# Notices and third-party attribution

This file collects attribution for third-party software distributed as part of Vazie.

Entries are checked against the release build's resolved dependency graph and each library's own
published licence.

---

## Vazie

Vazie is developed by Vazovsky and is licensed under the terms in [LICENSE](LICENSE): open for study
and security or privacy review, not for redistribution or derivative products.

## Third-party components

### Libraries in the application

| Component | Upstream | Licence |
|---|---|---|
| AndroidX (Activity, Core, Lifecycle, Navigation, Compose UI/Foundation/Material 3, Glance, DataStore, WorkManager, Room runtime, Startup, Profile Installer, Window and their transitive modules) | [developer.android.com/jetpack](https://developer.android.com/jetpack/androidx) | Apache-2.0 |
| Kotlin standard library, kotlinx.coroutines, kotlinx.serialization, kotlinx.collections.immutable | [JetBrains/kotlin](https://github.com/JetBrains/kotlin), [Kotlin/kotlinx.*](https://github.com/Kotlin) | Apache-2.0 |
| Dagger / Hilt, javax.inject, jakarta.inject | [google/dagger](https://github.com/google/dagger) | Apache-2.0 |
| Ktor client | [ktorio/ktor](https://github.com/ktorio/ktor) | Apache-2.0 |
| OkHttp, Okio | [square/okhttp](https://github.com/square/okhttp), [square/okio](https://github.com/square/okio) | Apache-2.0 |
| Guava ListenableFuture, JSpecify, JSR-305 annotations | [google/guava](https://github.com/google/guava), [jspecify](https://github.com/jspecify/jspecify) | Apache-2.0 |
| SLF4J API | [qos-ch/slf4j](https://github.com/qos-ch/slf4j) | MIT |
| AppMetrica SDK (crash reports, only with the person's consent) | [appmetrica/appmetrica-sdk-android](https://github.com/appmetrica/appmetrica-sdk-android) | MIT |
| Google Play services Basement, Base, Tasks, Ads Identifier, App Set; Install Referrer (pulled in by AppMetrica) | [developers.google.com](https://developers.google.com/android/guides/overview) | Android Software Development Kit License |
| Chucker (the no-op variant in release builds; the inspector itself only in debug) | [ChuckerTeam/chucker](https://github.com/ChuckerTeam/chucker) | Apache-2.0 |

Test-only libraries (JUnit, Robolectric, Ktor MockEngine, Compose UI test) are not shipped.

### VPN engine

| Component | Upstream | Version | Licence |
|---|---|---|---|
| **libXray** | [XTLS/libXray](https://github.com/XTLS/libXray) | `v26.7.28` | **MIT** |
| **Xray-core** | [XTLS/Xray-core](https://github.com/XTLS/Xray-core) | `v26.7.28` (linked by libXray) | **MPL-2.0** |

Vazie ships one gomobile-built native library, `libgojni.so`, per ABI (`arm64-v8a`, `armeabi-v7a`,
`x86`, `x86_64`). It is the upstream release artifact, **unmodified**, downloaded from the pinned
GitHub release and verified by SHA-256 at build time — see `vpn/engine/xray/build.gradle.kts`.

**MPL-2.0 obligations.** Xray-core's licence is a file-level copyleft. Vazie links it as an unmodified
binary and does not modify any Xray-core source file, so the obligation is attribution plus keeping
the covered work's source available — both are discharged by this file and the upstream links above.
Modifying Xray-core would place the modified files under MPL-2.0 and require publishing them. That is
a decision to take deliberately, not to drift into.

Planned additions once integrated (**not present yet**):

| Component | Purpose | Status |
|---|---|---|
| WireGuard Android tunnel library | WireGuard engine | Planned — not yet integrated |
| QR decoding library | Configuration import by QR | Planned |

**A packet bridge is not on this list and is not expected.** Xray-core carries its own `tun` inbound,
so Vazie needs no `tun2socks` or `hev-socks5-tunnel`.

## Fonts

The fonts below are bundled in the application, in `core/designsystem/src/main/res/font/`. Each is the
upstream **variable** font, licensed under the **SIL Open Font License 1.1**, and size-optimised for
this application.

### What was changed, and what was not

The bundled files are **Modified Versions** under the OFL. The changes are structural only:

- **Geologica:** the `CRSV` and `slnt` axes, which Vazie never sets, are pinned at their defaults (0).
  `wght` and `SHRP` stay variable. 348,640 → 248,992 bytes; outlines and advances at every weight and
  sharpness Vazie uses are identical to upstream.
- **Martian Mono:** `wdth` is pinned at 87.5 — the one width the design uses — and `wght` is limited
  to 400–600, the weights Vazie sets. 148,460 → 68,036 bytes; outlines at the used weights differ from
  upstream by at most 1.7 units of a 1000-unit em, well under a pixel.
- **Glyph names were dropped** from both (`post` table 2.0 → 3.0). Nothing on Android resolves a glyph
  by name.

**No glyph and no character was removed**: both files map exactly the same codepoints as upstream, so
no text a person types, pastes or imports can render as tofu that would not have done so before.

| Font | Upstream | Version | Licence | Licence text in this repo |
|---|---|---|---|---|
| Geologica | [monokromskriftforlag/geologisk](https://github.com/monokromskriftforlag/geologisk), distributed via [google/fonts](https://github.com/google/fonts/tree/main/ofl/geologica) | 1.010 | OFL 1.1 | [`third-party/fonts/Geologica-OFL.txt`](third-party/fonts/Geologica-OFL.txt) |
| Martian Mono | [evilmartians/mono](https://github.com/evilmartians/mono) | 1.000 | OFL 1.1 | [`third-party/fonts/MartianMono-OFL.txt`](third-party/fonts/MartianMono-OFL.txt) |

**Geologica and Martian Mono** (`geologica.ttf`, `martian_mono.ttf`) are the typefaces of the
"Маршрут" design, the same families Vazie Keep bundles, reduced here as described above.

### What the OFL actually requires

The licence permits bundling and redistributing the fonts inside software, including software that is
sold. Its conditions, applied to Vazie:

- **The copyright notice and the licence must accompany each copy of the font.** The OFL allows this
  as stand-alone text files, human-readable headers, *or* machine-readable metadata inside the font
  binary. Both files already carry both in their `name` table — copyright in ID 0, the full
  licence in ID 13 — so the requirement is satisfied by the fonts themselves, in the APK.
- **The fonts may not be sold on their own.** Vazie ships them as part of an application.
- **A modified version may not use the reserved font name.** These *are* Modified Versions, so the
  clause applies, and it is satisfied: neither upstream release declares a Reserved Font Name. Each
  copyright line reads "Copyright 2020 The *X* Project Authors" with no `with Reserved Font Name`
  clause after it, so the families keep the names they came with. The copyright (name ID 0), the full
  licence text (ID 13) and the licence URL (ID 14) are carried through unchanged.
- **The author and copyright-holder names may not be used to promote the product.** Vazie does not
  use them in marketing.
- **The fonts stay under the OFL.** They are not covered by Vazie's own all-rights-reserved statement
  above; that applies to Vazie's code, not to third-party material shipped with it.

### What is *not* required

The OFL does **not** require attribution in the product interface. Vazie needs no licences screen, no
credit in the app UI, no notice in the store listing and no acknowledgement in the README to satisfy
it. Nothing in the app has to name the fonts or their authors.

### What Vazie includes voluntarily

The stand-alone licence texts in [`third-party/fonts/`](third-party/fonts), and this section. Neither
is required — the embedded metadata already satisfies the licence — and both are here so anyone
reading the repository can check what ships and under what terms without opening a binary. An in-app
licences screen may be added later for the same reason; it would be a transparency choice, not a
compliance one.

Plus Jakarta Sans, named in the original design, is **not** bundled: the type scale uses Geologica
for product text instead, so there is no licence entry for it.

## Contributing to this file

When you add a dependency that ships in the application:

1. Verify its actual license from the artifact or its repository — do not assume.
2. Add it to the table above with component, purpose and license.
3. Reproduce any license text that the license itself requires to be distributed. For fonts that
   means a file in `third-party/fonts/`, added in the same change as the font itself.

Do not add speculative entries for dependencies that are not in the build.
