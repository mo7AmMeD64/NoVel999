# NovelNun

Native Android novel-reading app for kolnovel.com (ملوك الروايات), built with Jetpack Compose
and the Material 3 Expressive design language (Mono dark scheme, configurable system font, shape-morphing
loading indicators, expressive FAB menu, connected button groups).

## Screens
- **Home** — explore row for recently-added novels, a Popular rail, a Latest list, and a FAB
  menu (History / Open files).
- **Search** — full-text search against the site.
- **Saved** — your favorited novels (persisted locally with Room).
- **Settings** — choose the app-wide font and reverse the chapter order, alongside basic app info.
- **Details** — cover, synopsis, favorite toggle, Continue (resumes your last-read chapter),
  and the chapter list with a chapter-number jump field.
- **Reader** — chapter text, with reading progress recorded automatically.
- **History** — your reading history, openable per-entry, clearable.
- **Open files** — chapters you've explicitly downloaded for offline reading.

## Data source and a known caveat
The scraper (`data/network/KolNovelSource.kt`) targets `kolnovel.com`, a WordPress site running a
private/custom theme ("lightnovel") that isn't publicly documented. I confirmed the site is live
and pulled its rendered page structure, but I could not fetch raw HTML (this environment's network
access doesn't reach kolnovel.com directly - only the search/fetch tools could reach it, and those
return cleaned text, not raw markup). The CSS selectors in that file are a best-effort match based
on the rendered structure and the conventions of the upstream Keiyoushi "KolNovel" Tachiyomi/Mihon
extension (included as `kolnovel.zip` in the original request) rather than a verified DOM
inspection.

**In practice this means:** the app, its persistence, and its navigation are solid, but if a
selector in `KolNovelSource.kt` doesn't match the live markup exactly, a given list (Popular,
Latest, chapters, etc.) may come back empty instead of crashing - every screen has an empty/error
state instead of a hard failure. If that happens, the fix is localized to that one file: open it in
MT Manager, inspect the relevant kolnovel.com page's HTML (e.g. via "view source" in a browser),
and adjust the `.select(...)` calls to match.

## Building
No local build/signing is required. Push to `main` (or run the workflow manually) and GitHub
Actions builds an **unsigned** release APK and uploads it as a workflow artifact
(`NovelNun-unsigned`) - download it from the Actions run summary.

```
gradle assembleRelease
```
produces `app/build/outputs/apk/release/app-release-unsigned.apk` locally too, if you ever have a
JDK + Android SDK handy.
