# Search UI archive

These files are **not** in the Gradle source set, so they are kept in git but
never compiled into the app.

Removed from the active search page on 2026-11-19:

- `SearchScreen.pre-simplification.kt.txt` — the full search screen before the
  cut. It contains the jellyfish backdrop, card result stack, tear-off paper
  history, paper-colour extraction, and the simple history that is still live.
- `SearchStyleOverlay.pre-simplification.kt.txt` — the old long-press card with
  Jelly/Cards surface styles, Paper/Simple history styles, the jelly-look
  chooser, and typing-sound controls.
- `SongCardCarousel.kt.txt` — the swipeable card result stack.

The active search page now has only the typewriter machine and the plain result
list on the page background, with the simple history list underneath. The
jellyfish renderers were kept in `ui/screens/` because `NameCardScreen` still
uses them for its intro; search just no longer references them.

## Restoring a piece

1. Copy the file back into `app/src/main/java/com/rajatxo/lutea/` as a `.kt`
   file.
2. If it is `SearchScreen.pre-simplification.kt.txt`, restore it only as a
   reference and copy the specific section you need into the live screen. Do
   not overwrite the current search page wholesale.
3. Restore the matching wiring in `SearchSoundOverlay.kt` (formerly
   `SearchStyleOverlay.kt`) and `LuteaScaffold.kt`. The old snapshot uses
   `SearchStyleOverlayState`, `LocalSearchStyleOverlay`, `SearchSurfaceStyle`,
   `SearchHistoryStyle`, and `JellyLook`; those symbols no longer exist in the
   active UI.
4. Build before shipping. The snapshots are historical code and target the old
   APIs around them.
