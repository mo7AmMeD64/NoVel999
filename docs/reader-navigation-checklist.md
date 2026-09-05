# Reader and navigation regression checklist

## Automated checks

With JDK 17, Gradle 8.9, and Android SDK 35 available:

```sh
gradle testDebugUnitTest assembleDebug
```

The build workflow also runs `testDebugUnitTest` before building the unsigned release APK.
The tests do not contact kolnovel.com: details/history use a fake repository, and cache tests use
controlled suspend functions. Settings/import tests use Robolectric and a bundled font resource.

## Device checks (not replaced by JVM tests)

### Return from chapter search

1. Open a novel from Search, scroll to its chapter list, and enter a chapter number such as `12`.
   Repeat with Arabic-Indic `١٢` and Persian `۱۲` digits.
2. Open the result with either its row, **انتقال**, or the keyboard's Go action. Return using both
   the toolbar back button and Android Back. The details screen should appear without a loading
   replacement or another details request. The query and visible list position should be retained.
3. Tap **عرض جميع الفصول**, or **×** in the field. The query and validation error clear and all
   chapters reappear. Repeat after a query with no matches and a title query.
4. Enable **Settings → Reverse chapter order**. Chapter-number searches must still identify the
   same chapter. Clearing the query restores the entire reversed list.
5. Open any chapter, then return. **Continue** should point to the chapter just read, without a
   page reload. Also check an offline chapter from **Open files**.
6. Disconnect the network after loading details, open a downloaded chapter, and return: the loaded
   novel must remain available. Use **Refresh chapters** while offline: keep the existing content,
   filter, and scroll position, and display an error. Reconnect and refresh to get new chapters.
7. Cold-start/process recreation may fetch details again; the in-memory content cache is deliberately
   bounded and not an offline library. The saved query is restored. Downloaded chapter content and
   reader settings are persistent.

### Reader preferences and fonts

1. In a chapter, open the toolbar's **Reader settings** action. Adjust font size, line spacing,
   paragraph spacing, letter spacing, direction, and alignment. Check the live preview and chapter
   body; dismissing/reopening the sheet must not reset reading position or reload content.
2. Test Arabic, English, mixed text, and punctuation with **Auto**, **Right to left**, and
   **Left to right**. **Start/End** follow text direction; **Justify** is visibly different on
   multi-line paragraphs. Keep letter spacing at zero for joined Arabic script unless desired.
3. In **Reader font**, select a bundled font, then **Import font** and choose a valid `.ttf` or
   `.otf`. Only chapter text changes; app/navigation fonts do not. Cancel the picker and check that
   the current selection is preserved. Import an invalid, empty, and oversized (>20 MB) file:
   show an English error without changing the selection or leaving partial files.
4. Remove/move the original imported document and restart the app. The reader font should still
   work, because it is copied into private storage. Delete that imported font inside the picker:
   reader selection falls back to the app font (and the app font resets if it also used that file).
5. Check all five backgrounds in **Appearance**, including toolbar/icons, loading, and retry states.
   Text must remain readable on both dark and light palettes. Side margins affect the chapter only.
6. Restart the app and open a different chapter. All reader choices must persist. **Reset** restores
   reader defaults and **Use app font**, but keeps imported font files, the selected app font,
   and chapter order. The same sheet is available at **Settings → Reader preferences**.
7. On a small phone, in landscape, and with larger system text, scroll the sheet and font picker;
   make sure every control and close/reset action remains reachable.

### English settings and motion

1. On an Arabic-system-language device, verify that Settings, font names/descriptions, both font
   pickers, reader settings, and import errors are English/LTR. Novel titles and chapter text are
   not translated or forced into English direction.
2. Switch Home → Search → Saved → Settings and reverse direction. Content should move along the
   rail's ordering with a small depth change; the selected pill moves and the icon responds.
3. Open Details → Reader and go back. Enter/exit should use complementary horizontal depth/slide
   motion, mirrored for RTL layouts. Repeat quick tab taps and back navigation; no blank screens,
   duplicate destinations, or lost search/list state.
4. Set the system animator duration scale to zero and repeat navigation. All controls should work
   immediately without depending on animation completion.
