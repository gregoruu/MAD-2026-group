# Contribution Rules

* Each contributor must create a **separate branch** for their work. Do not make changes directly on the **main** branch.
* Branches should be created from the latest **main** branch.
* When the work is ready, create a **Pull Request (PR)** to merge the branch into **main**.
* Every Pull Request must be **reviewed and approved by at least one other contributor** before it can be merged.
* Contributors should not approve and merge their **own** Pull Request.
* Keep commits clear and related to the work being done.
* Pull Requests should contain a short description of what was changed.
* Before merging, make sure the branch is up to date with **main** and that the project still builds and works correctly.
* After a Pull Request is merged, the branch can be deleted if it is no longer needed.
* All contributors are expected to contribute to the repository and follow the same workflow regardless of their current project role.

## Android build check

The [Android build workflow](.github/workflows/android-build.yml) runs on every pull
request targeting `main`, on pushes to `main`, and when started manually from Actions.
There are no path filters: documentation-only pull requests also receive the required check.
It uses Ubuntu 24.04, JDK 17, Android SDK Platform 37.0 and Build-Tools 36.0.0, and runs
these commands from the repository root:

```sh
./gradlew assembleStaging --no-daemon --stacktrace --console=plain
./gradlew test --no-daemon --stacktrace --console=plain
```

Gradle dependencies and build outputs are cached by `setup-gradle`; pull requests only
read the cache populated by `main`. CI sets Gradle's dependency connection and read
timeouts to 120 seconds through `GRADLE_OPTS` to accommodate slow compiler downloads
when the cache is empty. These limits do not change the Gradle wrapper download timeout
or retry a failed build. Unit test reports are available for seven days in
the run's `android-test-reports` artifact, including when tests fail.

No repository secrets are needed. After [PR #82](https://github.com/gregoruu/MAD-2026-group/pull/82),
the Android app uses the backend proxy and the TMDB key lives only in Cloudflare.
The TMDB repository-secret requirement in issue #50 is therefore obsolete. Live TMDB
tests stay disabled by default, so the check does not depend on TMDB or the proxy being
available. Instrumented tests require a device and are not part of this workflow.

### Required GitHub setting

A workflow failure alone does **not** block merging. A repository administrator must
configure protection for `main` in **Settings > Branches**:

1. After the workflow has run on a pull request, create or edit the protection rule for `main`.
2. Enable **Require status checks to pass before merging** and select **Android build check**
   from GitHub Actions. Keep this job name stable in the workflow.
3. Enable **Require branches to be up to date before merging** and
   **Do not allow bypassing the above settings** so the requirement also applies to administrators.
4. Enable **Require a pull request before merging** with at least one required approval
   from another contributor, as required above, and save the rule.

This is a repository setting; merging the YAML file does not enable it. Before closing
issue #50, verify that a pull request with a failing build or test cannot be merged and
that the check passes again after the failure is fixed.
