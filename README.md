# NovelNun

Native Android novel-reading app for kolnovel.com (ملوك الروايات), built with Jetpack Compose
and the Material 3 Expressive design language (Mono dark scheme, configurable system font, shape-morphing
loading indicators, expressive FAB menu, connected button groups).

## Screens
- **Home** — explore row for recently-added novels, a Popular rail, a Latest list, and a FAB
  menu (History / Open files).
- **Search** — full-text search against the site.
- **Saved** — your favorited novels (persisted locally with Room).
- **Settings** — English labels, dialogs and font descriptions. Choose the app-wide font
  (7 bundled Arabic fonts — Cairo, Tajawal, Almarai, Amiri, Noto Kufi, Reem Kufi, Lalezar — plus
  platform families or imported `.ttf`/`.otf` files), open Reader preferences, or reverse chapter order.
- **Details** — cover, synopsis, favorite toggle, Continue (resumes your last-read chapter),
  and the chapter list with a chapter-number jump field (accepts western and Arabic-Indic digits;
  chapters are normalized to oldest-first, so "12" always opens chapter 12). Returning from a
  chapter keeps the loaded details, search and list position. Clear the field with **×** or use
  **عرض جميع الفصول** to restore the full list; **Refresh chapters** explicitly checks for updates.
  Continue follows local reading history without reloading the page.
- **Reader** — chapter text with automatic reading history, including offline chapters. The toolbar's
  **Reader settings** button opens a live-preview sheet: font size, line/paragraph/letter spacing,
  automatic/RTL/LTR direction, alignment, side margins, and Dark/Black/Sepia/Light/Sage backgrounds.
  The reader can follow the app font or use an independent bundled/imported font. Changes persist
  across chapters and restarts; **Reset** only resets reader preferences, not the app font or files.
- **Motion** — directional, spring-driven section transitions, depth/slide navigation into and out
  of pages, and a moving active indicator on the navigation rail (not just crossfades).
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
No local build/signing is required. Open a pull request to `main`, push to `main`, or run the workflow manually. GitHub
Actions runs the unit tests, builds an **unsigned** release APK and uploads it as a workflow artifact
(`NovelNun-unsigned`) - download it from the Actions run summary.

The workflow uses JDK 17 and Gradle 8.9. With those tools and Android SDK 35 installed locally:

```sh
gradle testDebugUnitTest assembleRelease
```

produces `app/build/outputs/apk/release/app-release-unsigned.apk`. JVM/Robolectric tests cover
chapter filtering, return-to-details state, history updates, shared detail caching, reader styling,
preference persistence, and font import/reset behavior. Actions also uploads the unit test reports.

See [the device regression checklist](docs/reader-navigation-checklist.md) for back-stack/scroll,
font picker, text direction, background contrast, and animation checks that require Android UI testing.
