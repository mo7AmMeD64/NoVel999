# NovelNun

Native Android novel-reading app for kolnovel.com (ملوك الروايات), built with Jetpack Compose
and the Material 3 Expressive design language (Mono dark scheme, configurable system font, shape-morphing
loading indicators, expressive FAB menu, connected button groups).

## Screens
- **Home** — explore row for recently-added novels, a Popular rail, a Latest list, and a FAB
  menu (History / Open files).
- **Search** — full-text search against the site.
- **Saved** — your favorited novels (persisted locally with Room).
- **Settings** — choose the app-wide font (7 bundled Arabic fonts — Cairo, Tajawal, Almarai, Amiri,
  Noto Kufi, Reem Kufi, Lalezar — plus the platform families, or import your own `.ttf`/`.otf`
  from device storage) and reverse the chapter order, alongside basic app info.
- **Details** — cover, synopsis, favorite toggle, Continue (resumes your last-read chapter),
  and the chapter list with a chapter-number jump field (accepts western and Arabic-Indic digits;
  chapters are normalized to oldest-first, so "12" always opens chapter 12).
- **Reader** — chapter text, with reading progress recorded automatically.
- **History** — your reading history, openable per-entry, clearable.
- **Open files** — chapters you've explicitly downloaded for offline reading.

## Data source
`data/network/KolNovelSource.kt` talks to kolnovel.com's own JSON API
(`wp-json/app/v2/discover|titles|reader`) — the same one its official Android app uses — instead
of scraping HTML. This was captured from a HAR of that app's traffic, so chapter numbers, order,
and titles come back as clean structured fields, and chapter content is already sanitized (no
ads/scripts to strip).

Two things weren't visible in that capture, so rather than guess at an unverified query param:
- **Popular** fetches a larger latest-sorted batch and ranks it client-side by the `score` field
  the API already returns per novel (no confirmed `sort=rating` value existed in the capture).
- **Search** filters a fetched batch of novels by title client-side (no dedicated search endpoint
  was exercised in the capture).

**Known gap:** favorites/history saved by the previous (scraping-based) build stored the site's
human-facing page URL as the novel's identifier; this version stores the API's numeric title ID
instead. Old saved entries won't resolve after updating — re-adding them from Search/Latest picks
up the new identifier going forward.

## Building
No local build/signing is required. Push to `main` (or run the workflow manually) and GitHub
Actions builds an **unsigned** release APK and uploads it as a workflow artifact
(`NovelNun-unsigned`) - download it from the Actions run summary.

```
gradle assembleRelease
```
produces `app/build/outputs/apk/release/app-release-unsigned.apk` locally too, if you ever have a
JDK + Android SDK handy.
