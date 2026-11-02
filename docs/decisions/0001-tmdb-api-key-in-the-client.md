# Decision 1: The TMDB API key lives in the client, and builds are not published publicly

**Status:** accepted
**Date:** 2026-11-01
**Related issues:** #22 (key out of source control), #55 (staging build), #52 (empty `backend/` folder)

## Context

Movara reads all movie data from the TMDB API. Every request needs an API key.

The course is explicit about where that key may not go. Its general project requirements
say *"Never commit API secrets, passwords, signing keys, or private user data to
GitHub"*, Stage 3 asks us to *"apply secure configuration practices"* and to *"keep
secrets, passwords, and signing keys out of source control"*, and the submission
checklist is checked against *"secrets, passwords, signing keys, personal data, and
private tokens are not present in the repository"*.

Stage 1 ruled user accounts and authentication out of scope, because all user data is
stored locally with Room. The team confirmed on 2026-10-28 that no server component
would be built for Stage 3; `backend/` is empty.

This leaves the key somewhere inside the Android application. Two different risks get
confused here, and they have different answers:

1. **The key is readable by anyone who holds the build.** Unavoidable. A key the app
   sends to TMDB must be present in the app. `BuildConfig`, `strings.xml`, an encrypted
   blob or a native library all end up in the same place — the installed package. R8 and
   ProGuard rename classes and methods; they do not encrypt string constants, so the
   shrunk staging and release builds carry the key in cleartext just like a debug build
   does. Every client-side obfuscation technique raises the cost of extraction by
   minutes, not more.
2. **The key becomes public.** Entirely avoidable. This only happens if a build leaves
   the team, and that is a decision we control.

## Decision

**The key is compiled into the application, and we control who receives a build.**

1. The key is read from `local.properties` (ignored by Git) or from a `TMDB_API_KEY`
   environment variable on a build server, and exposed as `BuildConfig.TMDB_API_KEY`.
   It is never committed. See the "Add your TMDB API key" section of
   `frontend/README.md`.
2. Each developer uses their own TMDB key for development. Development builds never
   leave the developer's machine.
3. **The staging build is not attached to a public GitHub release.** This repository is
   public, so a release asset would be downloadable by anyone. The build is shared
   directly with the team and the course staff instead — through the course submission
   or a link only they can open. This satisfies the Stage 3 requirement of a working,
   installable staging version without publishing the key.
4. The staging build uses a **dedicated TMDB key belonging to a project account**, not
   any team member's personal key. It is revoked after the course ends.

### What this changes in issue #55

Issue #55 currently asks for the staging package to be *"attached to a GitHub release
tagged `v0.1.0-staging`"*. **That checklist item is superseded by point 3 above** and has
to be replaced with sharing the package directly with the team and the course staff.

**The course does not ask for a GitHub release.** It never mentions one. Stage 3 lists
*"staging version for testing"* among the outputs to *"submit these outputs together with
the Stage 3 Report"*, and the report is submitted through Moodle. The closing checklist
asks only that *"the submitted app installs and its documented core flows work on a clean
device or emulator"*. A package handed to the instructor with the report meets that; a
public release asset is something this team invented when writing #55, and it is the one
thing that would put the key in the open.

The rest of #55 is unaffected: the package is still built from the `staging` build type,
the install instructions still go in `README.md`, and two team members still install it
and confirm the main flow.

Until #55 is edited, this document is the decision of record and #55 is out of date.

## Consequences

**What this protects.** The key does not appear in Git history, in the public repository,
or in any publicly downloadable artefact. Nothing we publish contains it.

**What this does not protect.** Anyone we hand a build to can extract the key from it.
We accept this. The people who receive a build are the five team members and the course
staff.

**Blast radius if the key does leak.** The key reads public movie metadata. There are no
user accounts, no personal data and no payment data behind it — Stage 1 put all of those
out of scope. The realistic damage is that someone else consumes our TMDB rate limit, or
uses the key in a way that breaks TMDB's non-commercial terms. Because the staging key
belongs to a project account rather than a person, revoking it costs one new key and
breaks nobody's development environment.

**Rejected alternative: a backend proxy.** Putting a server between the app and TMDB is
the only way to keep the key out of the client entirely. It was rejected on schedule
grounds, not on principle — Stage 3 is due 2026-11-19 and `backend/` is empty. Note that
Stage 1 ruled out *user accounts and authentication*, not a server; its technical
feasibility table lists "Cloud: Supabase" as an accepted option. A key proxy needs no
accounts. If Movara is ever continued past the course, this is the change to make first.

**Rejected alternative: obfuscating the key in the client.** Rejected because it does not
work. It would cost real effort and produce a false sense of security, which is worse
than a documented, understood exposure.
