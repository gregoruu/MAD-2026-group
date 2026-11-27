# Release security and permissions audit (#123)

**Date:** 2026-11-27  
**Issue:** [Perform Security and Permissions Audit](https://github.com/gregoruu/MAD-2026-group/issues/123)  
**Baseline:** `e664674`; branch `chore/123-security-and-permissions-audit`

## Scope and conclusion

This audit reviews tracked source, locally available Git history, release build
configuration, Android permissions/components, local storage/backup, network
communication and logging. It does not certify deployed Cloudflare/Firebase
account settings, third-party retention policies or devices with root access.

No release-blocking source/configuration issue was identified within this scope.
The preventive hardening changes and automated checks below passed. This conclusion
does not certify public-store readiness: live service settings, privacy disclosures
and signed-device testing remain separate distribution checks. No signing material
or local secret values are copied into this report.

## Findings

| Area | Evidence and assessment | Action |
| --- | --- | --- |
| Repository secrets | Pattern searches found no TMDB key, private signing key, service-account credential, AWS access key or GitHub token in the reviewed tracked source. Candidates were test data, checksums, Room schema hashes, pinned Action revisions and Firebase client configuration. | Extended ignore rules for environment files, additional private-key formats and common service-account filenames. |
| Git history | Reviewed locally available refs for common private-key/token/TMDB-key patterns and secret filenames. Content matches outside Firebase client configuration were pinned Action revisions, Room schema hashes and the wrapper checksum. No secret files were found by the filename search. | No credential removal or history rewrite indicated by these searches. This is a targeted review, not an exhaustive secret scanner or proof about deleted remote refs. |
| Release signing | Keystore location/passwords come from ignored root `local.properties` or environment variables; no debug-key fallback. Partial signing configuration fails; absent configuration deliberately produces an unsigned verification APK. | Retained this behavior. Distribution requires the team's private signing configuration. |
| Permissions | Source declares only `INTERNET` and `ACCESS_NETWORK_STATE`, both needed for API/images/diagnostics and offline handling. No camera, microphone, location, contacts, shared-media/storage or notification permission requests. | Retained least-privilege source manifest; packaged release manifest checked separately below. |
| App components and Intents | Only app-owned component is launcher `MainActivity`, which must be exported for the launcher. It consumes no incoming extras, nested Intents or deep links. About opens hardcoded HTTPS attribution links via the browser. No app-owned services, receivers, providers or PendingIntents. | No component code change needed; dependency components checked in packaged release manifest. |
| Local storage | Room's `movara.db` stores saved lists and response cache; DataStore's `files/datastore/movie_taste.preferences_pb` stores taste preferences. Coil uses `cacheDir/movie_images`. No external/shared storage or account tokens. DAO queries bind parameters. | App-private storage retained. No additional app-level encryption introduced for this movie metadata/preferences threat model; the sandbox does not protect a rooted/unlocked device. |
| Backup and migration | `allowBackup=false`, `fullBackupContent=false`; Android 12+ rules exclude the whole database domain and `files/datastore/` for both cloud backup and device transfer. Cache storage is excluded by Android. | Existing user-data policy retained; manufacturer-specific transfer behavior was not exercised. |
| API communication | App calls staging/production HTTPS Workers; Worker calls a fixed HTTPS TMDB origin, permits only allowlisted GET paths/parameters and constructs its own headers. No arbitrary upstream URL, account/write endpoints, client API key or TLS verification bypass. Images use HTTPS TMDB URLs. | Added explicit app-wide cleartext prohibition, preserving platform TLS trust anchors. |
| Production debugging/logging | Release already sets `isDebuggable=false`, `ENABLE_HTTP_LOGGING=false`, shrinking and obfuscation. `AppContainer` passes the generated logging flag to the OkHttp factory. No app Log/println/stacktrace calls or custom Crashlytics logs/IDs. | Added tests of generated release constants and an actual request proving the release logging flag emits no HTTP log lines; CI now builds/lints release too. |
| Staging/debug | Both deliberately enable HTTP BODY logs; staging is debuggable and uses a separate package/Worker. | Retained development behavior. Staging logs can reveal searches and responses; use release for production. |
| Crash diagnostics | Staging/release automatically include Crashlytics; debug has no Firebase SDK. Stack traces/exception messages and installation/session/device metadata can leave the device. No explicit user IDs or list/profile uploads. | Existing collection behavior retained. See the operational limits below and [architecture](architecture.md#android-crash-reporting). |

Firebase API keys are client project identifiers, not server credentials. Their
live API restrictions cannot be inferred from the JSON file; Firebase recommends
restricting these keys to the required Firebase APIs. See
[Firebase API key guidance](https://firebase.google.com/docs/projects/api-keys).

## Best practices and security alignment update: network policy and secret hygiene

**Priority:** Low preventive hardening. The current target SDK already defaults to
rejecting cleartext; the explicit XML makes the policy reviewable and shared by API,
image and SDK clients that honor Android Network Security Configuration. It is not
a claim that the previous version allowed HTTP. Normal platform certificate and
hostname verification remains in place.

**Scope:** `frontend/app/src/main/AndroidManifest.xml`,
`frontend/app/src/main/res/xml/network_security_config.xml`, root `.gitignore`,
`frontend/app/src/test/kotlin/ee/movara/app/ReleaseSecurityConfigurationTest.kt`,
`gradle.properties`, `.github/workflows/android-build.yml`, `CONTRIBUTION_RULES.md`,
`frontend/README.md`, `docs/architecture.md` and this report. No dependencies changed; no IPC permission was added.

The manifest/network-policy change is:

```diff
--- a/frontend/app/src/main/AndroidManifest.xml
+++ b/frontend/app/src/main/AndroidManifest.xml
@@ -10,6 +10,7 @@
         android:allowBackup="false"
         android:dataExtractionRules="@xml/data_extraction_rules"
         android:fullBackupContent="false"
+        android:networkSecurityConfig="@xml/network_security_config"
         android:icon="@mipmap/ic_launcher"
         android:label="@string/app_name"
         android:roundIcon="@mipmap/ic_launcher_round"
--- /dev/null
+++ b/frontend/app/src/main/res/xml/network_security_config.xml
@@ -0,0 +1,5 @@
+<?xml version="1.0" encoding="utf-8"?>
+<network-security-config>
+    <!-- Apply to API calls, images and SDK traffic; use the platform trust anchors. -->
+    <base-config cleartextTrafficPermitted="false" />
+</network-security-config>
```

The policy also applies to debug/staging. Host JVM MockWebServer tests do not run
inside Android and remain able to use localhost HTTP. An installed app connecting
to a local Worker would need an explicit debug-only exception.

## Verification

All checks below ran locally on 2026-11-27:

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleRelease :app:lintRelease :app:test -PuploadCrashlyticsMapping=false --console=plain` | Passed: optimized unsigned release APK, release Lint and debug unit tests. |
| `./gradlew :app:testReleaseUnitTest :app:testStagingUnitTest -PuploadCrashlyticsMapping=false --console=plain` | Passed after enabling the non-debug host tests. The two new release checks execute in the release run. |
| `npm test` in `backend/` | Passed: 20 tests, including path/method/query rejection, no upstream request on invalid input, and no forwarded client credentials. No live TMDB call. |
| `npm run typecheck` in `backend/` | Passed. |
| `git diff --check` | Passed. |
| `git check-ignore` for representative secret/template filenames | Private formats and environment files ignored; `.env.example`, `.env.production.example` and `backend/.dev.vars.example` remain addable. |
| AAPT2 inspection of packaged release manifest and referenced XML resources | Passed; details below. |

Packaged artifact: `frontend/app/build/outputs/apk/release/app-release-unsigned.apk`.
Inspected with SDK Build-Tools 36.0.0 `aapt2 dump xmltree` and `dump resources`:

- Requested platform permissions are exactly `INTERNET` and `ACCESS_NETWORK_STATE`.
  AndroidX also declares/uses `ee.movara.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
  with `signature` protection (`0x2`); it grants no device-data capability.
- No `debuggable=true` attribute is packaged (the manifest default is false).
  Generated release `BuildConfig.DEBUG` and `ENABLE_HTTP_LOGGING` are both false,
  verified by the release unit test.
- `MainActivity` is the launcher. The only other exported component is AndroidX
  `ProfileInstallReceiver`, protected by the platform `android.permission.DUMP`.
  Firebase initialization/discovery/session components, AndroidX Startup, Room and
  DataTransport services/receivers are explicitly non-exported.
- `allowBackup=false` and `fullBackupContent=false` are preserved. The referenced
  extraction XML excludes databases and `datastore/` from cloud and device transfer.
- The manifest references the packaged network policy, whose base configuration
  has `cleartextTrafficPermitted=false` and no per-domain exception/custom CA.

The regression tests execute in `testReleaseUnitTest`; their two release-specific
checks are intentionally skipped for debug/staging. AGP 9 otherwise enables only
debug host tests, so `gradle.properties` explicitly sets
`android.onlyEnableUnitTestForTheTestedBuildType=false`. CI runs all variants via
`test` and also names `:app:testReleaseUnitTest` explicitly so the production check
cannot silently disappear. The release build/lint step checks production resources
and manifest merging. CI verification packages remain unsigned and skip mapping uploads;
distributed packages must retain mapping uploads and use the private release key.

## Operational limits and follow-up

- Cloudflare secret values, account access, rate-limit/WAF configuration and provider
  request-log retention were not accessed. The public proxy has no client
  authentication or code-level rate limiter. Its fixed read-only endpoint
  allowlist protects the upstream surface; third parties can still consume its
  quota. This is a residual availability risk of the existing account-free proxy;
  this audit adds no rate-limiting or authentication service.
- Firebase/Google Cloud API restrictions and project IAM were not inspected. Use
  Firebase keys only for required Firebase APIs; the public client JSON cannot
  prove these restrictions.
- Automatic diagnostics already require accurate user-facing privacy information
  and store disclosures before public distribution, as documented in architecture.
  This audit does not constitute a Google Play/privacy compliance review.
- No device backup/restore or runtime network interception test was performed.
  Signed installation and normal release smoke testing remain distribution checks.
- Searches cover available source/history and common patterns, not all possible
  encodings, dependency vulnerabilities, remote release assets or erased refs.
  No secrets were rotated or remote settings changed by this audit.

## References

- [Android Network Security Configuration](https://developer.android.com/privacy-and-security/security-config):
  explicit cleartext policy and platform trust defaults.
- [AGP 9 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes):
  host unit-test variant defaults.
- [Android Auto Backup](https://developer.android.com/identity/data/autobackup):
  backup exclusions, cache behavior and manufacturer-specific device transfers.
- [Architecture and data handling](architecture.md#permissions-and-user-accounts).
- [Build/signing and crash reporting](../frontend/README.md).
- [Backend endpoint validation and key rotation](../backend/README.md).
