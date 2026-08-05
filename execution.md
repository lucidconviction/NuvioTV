# Nuvio TV — Execution Plan

> Implementation details for 11 prioritized features.
> Each section covers architecture, files to touch, data flow, and UI additions.

---

## 1. Stream Health Dashboard

**Goal:** Dedicated screen showing all IPTV sources with live/dead stream counts, last validation time, and a "Validate All" button.

### Architecture

```
IPTV sources dashboard → "Stream Health" button → StreamHealthScreen → orchestrates validator per source
```

Already have `StreamValidationStore.kt` (persists dead/good URLs per profile) and `StreamValidator.kt` (tests URLs with byte-range GET, batched 20 at a time). Need a UI layer to surface the data.

### Files to Create

| File | Purpose |
|------|---------|
| `StreamHealthScreen.kt` | TV screen composable: scrollable card per source, showing total/good/dead counts, progress bar during validation, last validated timestamp |
| `StreamHealthViewModel.kt` | Manages validation state per source, calls existing `StreamValidator`, reads/writes `StreamValidationStore` |

### Files to Modify

| File | Change |
|------|--------|
| `Screen.kt` | Add `StreamHealth` route |
| `NuvioNavHost.kt` | Add composable route for StreamHealthScreen |
| IPTV sources dashboard | Add "Stream Health" button in the source management area (near source cards) |

### Data Flow

1. User taps "Stream Health" from IPTV dashboard
2. `StreamHealthViewModel` reads all sources from `IptvRepository`, then reads each source's key from `StreamValidationStore` to get cached good/dead counts
3. Displays cards: source name, total channels, ✅ good count, ❌ dead count, last validated timestamp
4. "Validate All" button → iterates each source through `StreamValidator.validateUrls()` with batch size 20, updates progress in real-time
5. Results persist to `StreamValidationStore`; dead streams auto-hidden per existing toggle

### UI Notes

- D-pad friendly: each source card is focusable, "Validate All" is a prominent button at top
- Validation progress shows "Validating 15/1420..." per source with indeterminate progress bar
- Final state per card: "✅ All streaming working!" or "⚠️ Found 12 dead of 1420"

---

## 2. Cross-Hub Search (Search Tab)

**Goal:** Add a unified search to the existing Search/Discover tab that searches IPTV channels, VidNutz videos, MusicNutz tracks, and Stremio catalog simultaneously. Results grouped by source type.

### Architecture

```
Search tab → query → CrossHubSearchCoordinator → fan-out to each repository → merge + rank results
```

Leverage existing search tab (`SearchScreen.kt` / `DiscoverScreen.kt`). Add a coordinator that dispatches to each hub's repository and merges results.

### Files to Create

| File | Purpose |
|------|---------|
| `CrossHubSearchCoordinator.kt` | Takes query string, calls all repositories concurrently, returns grouped results with `CrossHubSearchResult` sealed class |
| `CrossHubSearchResult.kt` | Sealed class: `IptvChannelResult`, `VidNutzVideoResult`, `MusicNutzTrackResult`, `MusicNutzAlbumResult`, `StremioMetaResult`, `SportNutzTeamResult` |

### Files to Modify

| File | Change |
|------|--------|
| `DiscoverScreen.kt` | Add a "Search All Hubs" mode — when active, show grouped results with hub-colored headers |
| `SearchScreen.kt` | Add a toggle chip row: "All" / "Movies & Series" / "IPTV" / "Videos" / "Music". "All" fan-outs to all repositories |
| `Screen.kt` | Optionally add a `CrossHubSearch` route or add it as search mode |
| `RobbdeezeNutzHubViewModel.kt` | Add `crossHubSearch(query)` function, `crossHubResults` state flow |
| `IptvRepositoryImpl.kt` | Already has `searchChannels()` — no change needed |
| `VidNutzRepositoryImpl.kt` | Already has `searchVideos()` — no change needed |
| `MusicNutzRepositoryImpl.kt` | Already has `searchTracks()` / `searchAlbums()` — no change needed |
| `CatalogRepositoryImpl.kt` | Already has Stremio catalog search — no change needed |

### Data Flow

1. User types in search field, selects "All" mode
2. 300ms debounce → `CrossHubSearchCoordinator.search(query)` launches async coroutines:
   - `IptvRepository.searchChannels(query)` → channel results
   - `VidNutzRepository.searchVideos(query)` → video results
   - `MusicNutzRepository.searchTracks(query)` → track results
   - `MusicNutzRepository.searchAlbums(query)` → album results
   - `CatalogRepository.search(query, type)` → Stremio catalog results
3. All results collected, sorted into groups by source
4. Displayed with colored section headers (IPTV=blue, VidNutz=purple, MusicNutz=teal, Stremio=brand)
5. Each result item navigates to its respective detail/play screen on tap

### UI

- Section headers: small colored chip with source name and count
- Results render using existing card components (`ChannelGridCard`, `VidNutzVideoCard`, `TrackCard`, `AlbumCard`, poster card)
- Top toggle row: `[ All ] [ Movies ] [ IPTV ] [ Videos ] [ Music ]` — non-"All" modes behave like current per-hub search
- Keyboard supports D-pad navigation through results

---

## 3. Sports Calendar View

**Goal:** In-app calendar view showing upcoming games by day/week — not just live scores. Reuses Sync2CalClient data.

### Architecture

```
SportNutz → "Schedule" tab → calendar view → Sync2CalClient + ESPN data → day-grouped event cards
```

Reuse `Sync2CalClient.kt` (already fetches sports schedules) and `Sync2CalMappings.kt` (20 league mappings). Add a calendar/time-range UI.

### Files to Create

| File | Purpose |
|------|---------|
| `SportsCalendarScreen.kt` | Full-screen calendar view: day selector (horizontal row of day chips), below it a grid of upcoming events for selected day |
| `SportsCalendarViewModel.kt` | Fetches schedules from Sync2CalClient + ESPN for next 7-14 days, groups by day, caches |

### Files to Modify

| File | Change |
|------|--------|
| `RobbdeezeNutzHubViewModel.kt` | Add `sportsCalendarState` flow, `HubSubScreen` add a `Calendar` option (or add as tab in Sports) |
| `SportsSubScreen.kt` | Add "Schedule" tab/chip alongside Live/Upcoming tabs |
| `Sync2CalClient.kt` | Already fetches schedule data — may need to extend range to fetch more days ahead |
| `SportsModels.kt` | Optionally add `SportCalendarDay(date: LocalDate, events: List<SportEvent>)` model |

### Data Flow

1. User opens Sports → "Schedule" tab
2. `SportsCalendarViewModel` fetches upcoming events from:
   - `Sync2CalClient` for next 14 days (uses league mappings)
   - `EspnClient` as fallback per league
3. Events grouped by day (LocalDate)
4. Day selector: horizontal `LazyRow` of date chips. Today highlighted. Selected day = blue chip
5. Below: 2-column grid of event cards for that day (team logos, time, league badge)
6. Tapping an event → runs existing `GameToChannelMatcher` to find IPTV channel, or shows "Not yet live" message
7. "Notify me" button on future events triggers a system notification (see Feature 6)

### UI Notes

- Day chips format: "Wed Jul 29" / "Thu Jul 30" etc., 5-7 days visible, scrollable for more
- Past days grayed out, today has a "Live" dot, future days show event count badge
- Each event card shows: team logo 1, score/vs, team logo 2, start time (device local), league badge
- Empty day state: "No events scheduled"

---

## 4. Playlist Auto-Refresh Scheduler

**Goal:** Auto-refresh IPTV playlists at configurable intervals (daily/weekly) per source. Re-runs stream validation after refresh.

### Architecture

```
Source model → refreshInterval field → WorkManager periodic task → refresh + validate → notification
```

Can extend the existing source model with a `refreshIntervalHours` field and use Android WorkManager for background scheduling.

### Files to Create

| File | Purpose |
|------|---------|
| `IptvRefreshWorker.kt` | `CoroutineWorker` implementation: reads all sources from `IptvRepository`, checks each source's refresh interval, refreshes overdue ones, runs stream validation, posts notification |
| `IptvRefreshScheduler.kt` | Utility to schedule/cancel/reschedule WorkManager periodic work per source |

### Files to Modify

| File | Change |
|------|--------|
| `IptvModels.kt` (domain model) | Add `refreshIntervalHours: Int` (0 = never, 24 = daily, 168 = weekly) to source model |
| `IptvStorage.kt` | Persist `refreshIntervalHours` alongside source data |
| Source management UI (IPTV dashboard) | Add "Auto-refresh" setting per source card in source menu overlay: "Never / Daily / Weekly" picker |
| `StreamValidationStore.kt` | May need timestamp per validation run for freshness tracking |
| `AndroidManifest.xml` | Register `IptvRefreshWorker` if using WorkManager (check if already included) |

### Data Flow

1. User opens source menu overlay → "Auto-refresh" → picks interval (Daily / Weekly / Off)
2. `IptvRefreshScheduler` calculates next run time, schedules with WorkManager
3. When trigger fires, `IptvRefreshWorker`:
   - Refetches M3U/Xtream URL
   - Re-parses channels into cache
   - Runs `StreamValidator` on the new channels
   - Posts notification: "📺 [Source Name] refreshed — 8 dead channels found"
4. If source URL is dead, notification shows "⚠️ Failed to refresh — source URL unreachable"
5. User taps notification → opens IPTV dashboard

### UI for Scheduler Settings

Add to the source menu overlay (already exists: View Channels / Refresh / Validate / Delete):
- **Auto-refresh:** `[ Off ] [ Daily ] [ Weekly ]`
- **Last refresh:** shows timestamp if previously refeshed
- **Next refresh:** shows estimated next time

---

## 5. Watch History Hub

**Goal:** A single hub showing chronologically everything watched across IPTV, Stremio, VidNutz, and MusicNutz. Filterable by type.

### Architecture

```
New hub card → WatchHistoryScreen → WatchHistoryViewModel → aggregates from all history stores → sorted list
```

You already have per-hub history tracking: `ChannelHistoryStore` (IPTV, last 20 channels), `ContinueWatchingStore` (Stremio, last 10), search history stores. Add a unified aggregator.

### Files to Create

| File | Purpose |
|------|---------|
| `WatchHistoryScreen.kt` | Scrollable timeline: date headers, history items grouped by day, each item with thumbnail/icon + title + source badge + timestamp |
| `WatchHistoryViewModel.kt` | Aggregates from all history stores, merges by `lastWatchedAt` timestamp, supports type filtering |
| `WatchHistoryStore.kt` | Optional: unified DataStore-backed history store if per-hub stores are insufficient |

### Files to Modify

| File | Change |
|------|--------|
| `WatchHistoryModels.kt` (or new) | Define `WatchHistoryEntry(id, source, title, subtitle, thumbnailUrl, lastWatchedAt, sourceHub)` — sealed class per source type |
| `RobbdeezeNutzHubViewModel.kt` | Add `watchHistory` state flow, add `HubSubScreen.History` entry |
| `RobbdeezeNutzHubScreen.kt` | Add History hub card (icon: clock 🕐, color: gray/amber) |
| `NuvioNavHost.kt` | Add route for WatchHistoryScreen |
| `ChannelHistoryStore.kt` | Already stores channels; may need `lastWatchedAt` per entry (check current schema) |
| `ContinueWatchingStore.kt` | Already has timestamps |
| `VidNutzRepositoryImpl.kt` | Need to add a "viewed video" log — currently no per-video watch tracking |
| `MusicNutzStore.kt` | May need to log played tracks |

### Data Sources to Aggregate

| Source | Current Store | Data Available |
|--------|--------------|----------------|
| IPTV channels | `ChannelHistoryStore` | Channel name, logo, URL, timestamp |
| Stremio movies/episodes | `ContinueWatchingStore` | Title, poster, progress, timestamp |
| VidNutz videos | None — needs new tracking | Video title, thumbnail, duration, timestamp |
| MusicNutz tracks | `MusicNutzStore` (playlists only) | Track name, artist, album art, timestamp |

**VidNutz gap:** Currently no watch tracking for VidNutz videos. Add a simple `VidNutzHistoryStore.kt` (DataStore, serialized list of last 50 watched videos) to fill this gap.

### UI

- Hub icon: clock symbol, color gradient (amber/gray)
- List view: date divider ("Today", "Yesterday", "Jul 27"), then chronological cards
- Each card shows: thumbnail/icon (16:9 for video, channel logo for IPTV, album art for music), title, subtitle (channel name / episode name / artist), source badge (small colored chip: "IPTV", "Movie", "VidNutz", "Music"), timestamp ("2h ago")
- Filter chips at top: `[ All ] [ IPTV ] [ Movies ] [ Videos ] [ Music ]`
- Tap → navigate to appropriate player (or detail screen for movies)
- Clear All button in overflow menu

---

## 6. Sports Notification Engine

**Goal:** Push notification when a favorite team's game goes live, with "Watch Now" action button to open the IPTV player.

### Architecture

```
Favorites (DataStore) + ESPN/DaddyLive poll → match → NotificationCompat → PendingIntent → IPTV player
```

Background polling via WorkManager (15-min periodic check during known game windows). Uses existing favorite teams (needs new store) and GameToChannelMatcher.

### Files to Create

| File | Purpose |
|------|---------|
| `FavoriteTeamsStore.kt` | DataStore-backed list of favorite team names/abbreviations per league |
| `SportsNotificationWorker.kt` | `CoroutineWorker`: polls ESPN/DaddyLive for live events, cross-refs against favorite teams, sends notification on match |
| `SportsNotificationChannels.kt` | Creates Android notification channel "Sports Alerts" on app startup |

### Files to Modify

| File | Change |
|------|--------|
| `SportsModels.kt` | Add `isFavorited` field or reference favorite check |
| `TeamDetailScreen.kt` | Add "Favorite ⭐" toggle button on team detail page |
| `SportsCalendarScreen.kt` (v3) | Add "Notify me" button on future events |
| `RobbdeezeNutzHubViewModel.kt` | Add `toggleFavoriteTeam(teamName, league)` function |
| `NuvioApplication.kt` | Create notification channel on startup |
| `AndroidManifest.xml` | Register `SportsNotificationWorker` |
| `GameToChannelMatcher.kt` | Accept a "favorite teams" filter param to prioritize matched channels |

### Data Flow

1. User browses SportNutz → finds team → taps ⭐ or uses "Notify me" on future event
2. Team name + league saved to `FavoriteTeamsStore.kt`
3. `SportsNotificationWorker` scheduled: runs every 15 minutes, only during typical game hours (configurable, default 9:00–23:00 ET)
4. On each run:
   - Fetches live events from ESPN/DaddyLive
   - Checks if any live event contains a favorite team name
   - If match found AND notification hasn't been sent for this game yet (dedup by gameId):
     - Builds notification title: "⚽ [Team Name] game is live!"
     - Body: "[Opponent] — [Score] — [Quarter/Period]"
     - Action button: "Watch Now" → `PendingIntent` opens IPTV player on matched channel (via `GameToChannelMatcher`)
     - Action button: "Scores" → opens TeamDetailScreen
5. User taps "Watch Now" → app opens to matched IPTV channel
6. Dedup prevents re-notifying for the same game until it ends

### Settings UI

Add to SportNutz settings (new or existing):
- **Favorite Teams:** scrollable list of saved teams with remove button
- **Notifications:** toggle on/off for sports alerts
- **Notify for:** chip selector `[ All games ] [ Live now only ] [ Close games only (<7pts) ]`
- **Game hours:** start/end time picker to avoid late-night notifications

---

## 9. Speed Dial — Quick Channel Shortcuts on Home

**Goal:** Configurable row of favorite/shortcut channels on the hub home screen — one-click play, no sub-menus.

### Architecture

```
Hub home screen → SpeedDialRow (LazyRow of 6 channel cards) → tap → IptvPlayer or MultiWindow
```

Uses existing `QuickChannelList.kt` (~240 curated channels) plus user's own favorites from `IptvRepository`.

### Files to Create

| File | Purpose |
|------|---------|
| `SpeedDialStore.kt` | DataStore-backed list of 6 shortcut channel entries (channel name, URL, logoUrl, sourceLabel) |
| `SpeedDialRow.kt` | Composable: horizontal `LazyRow` of channel cards (logo + name) with empty-state placeholder |

### Files to Modify

| File | Change |
|------|--------|
| `HubScreenContent.kt` | Add `SpeedDialRow` at the top, above the hub cards grid, with section header "📺 Quick Play" |
| `RobbdeezeNutzHubScreen.kt` | Pass `speedDialChannels` state, edit mode toggle |
| `RobbdeezeNutzHubViewModel.kt` | Add `speedDialChannels` state flow, `addToSpeedDial(channel)`, `removeFromSpeedDial(index)`, `reorderSpeedDial(from, to)` |
| `ChannelGridCard.kt` or channel popup | Add "Add to Quick Play" option in the channel options popup (tapping a channel already shows Watch/Multi-View options) |
| `QuickChannelList.kt` | Already exists with ~240 curated channels — this surfaces them as selectable shortcuts |
| `SettingsScreen.kt` or IPTV settings | Add "Configure Quick Play" entry that opens a picker |

### Data Flow

1. User taps channel anywhere (from Quick Channels popup, channel grid, favorites) → channel options popup → "Add to Quick Play"
2. If Speed Dial has room (<6), channel added at next slot. If full, prompt "Replace which channel?" with current Speed Dial shown as selectable grid
3. On hub home screen, Speed Dial row renders as scrollable `LazyRow` with 6 slots
   - Filled slots: channel logo + name (compact card, 80x80dp), marquee on focus
   - Empty slots: "+" placeholder with "Add Channel" label
4. Tap filled slot → immediate IPTV player launch on that channel
5. Long-press filled slot → context menu (Remove / Replace / Move)
6. "Edit" mode: pencil icon → slots become reorderable (swap animation) with X buttons

### Speed Dial Settings

Add a `Settings → IPTV → Quick Play` screen:
- Current 6 slots shown as preview
- "Pick from Quick Channels" button opens curated list browser
- "Pick from My Favorites" opens user favorites browser
- "Clear All" button

---

## 10. IPTV Recording to Storage

**Goal:** Record a live IPTV stream to local device storage. Start/stop recording from the player overlay.

### Architecture

```
Player overlay → "Record" button → start/stop → write ExoPlayer output to file → save to Movies/Downloads
```

Uses `Media3`'s `FileDataSource` or a simple `OutputStream` sink writing to external storage. Recording runs in a foreground service to survive player dismissal.

### Files to Create

| File | Purpose |
|------|---------|
| `IptvRecorderService.kt` | Foreground service (`MediaSessionService`-compatible or standalone): receives recording start/stop commands, writes stream data to file `NuvioRecordings/ChannelName_YYYYMMDD_HHMMSS.ts` |
| `IptvRecordingManager.kt` | Singleton managing active recording state: `startRecording(channel, url)`, `stopRecording()`, `isRecording` flow |
| `RecordedStreamsScreen.kt` | Optional: list of recorded files with play/delete/rename/share options |

### Files to Modify

| File | Change |
|------|--------|
| `IptvPlayerScreen.kt` | Add "Record ⏺" button to player overlay (next to favorites heart). Red when recording, gray when idle. Tap to start/stop |
| `IptvPlayerViewModel.kt` | Wire recording toggle to `IptvRecordingManager` |
| `PlayerOverlayScaffold.kt` | Ensure recording button fits in existing control layout |
| `AndroidManifest.xml` | Register `IptvRecorderService` foreground service + `FOREGROUND_SERVICE_DATA_SYNC` permission + `WRITE_EXTERNAL_STORAGE` / `READ_MEDIA_VIDEO` |
| `NuvioApplication.kt` | Initialize `IptvRecordingManager` on startup |

### Data Flow

**Start recording:**
1. User is watching an IPTV channel and taps ⏺ button
2. `IptvRecordingManager.startRecording()`:
   - Creates file: `Movies/NuvioRecordings/{ChannelName}_{20260729_121500}.ts`
   - Starts `IptvRecorderService` foreground service with "Recording {channel}" persistent notification
   - Opens `FileOutputStream` to the file
   - Starts reading from the ExoPlayer's current `MediaSource` data source (or the raw stream URL directly if simpler)
   - Writes incoming data to file in chunks
3. Button turns red, pulsing animation, persistent notification shows recording duration

**Stop recording:**
1. User taps ⏺ again
2. `IptvRecordingManager.stopRecording()`:
   - Closes file output stream
   - Stops foreground service
   - Posts notification: "✅ Recording saved — {filename} ({duration})" with "Play" action button
3. File saved to device storage, accessible from file manager / gallery apps

### Storage Permissions

- Android 10+ (API 29+): use `MediaStore.Video.Media` with `WRITE_EXTERNAL_STORAGE` (granted at install). Files saved to `Movies/NuvioRecordings/`
- Android 13+ (API 33+): use `MediaStore` with no WRITE permission needed (scoped storage)
- Request `POST_NOTIFICATIONS` for foreground service notification

### Approaches for Capturing Stream

**Approach A (Simple, recommended):** Download the raw stream URL file to disk
- Use `OkHttp` to open a streaming connection to the channel's URL
- Write to a `.ts` or `.mp4` file on disk
- Simple, works for M3U8 (HLS) and direct RTMP/HTTP streams
- ~1GB/hour for HD streams

**Approach B (Complex):** Pipe from ExoPlayer's output
- Use ExoPlayer's `FileDownloader` or `CacheDataSink`
- More accurate capture of what the user sees
- Harder to implement, more edge cases

**Recommendation:** Start with Approach A (raw URL download) for simplicity, then improve.

---

## 11. Sleep Timer

**Goal:** Let users set a timer to automatically stop playback after a chosen interval — 15/30/60 minutes or at the end of current content. Standard TV media player feature.

### Architecture

```
Player overlay → timer button → SleepTimerDataStore + PlayerRuntimeController → countdown → pause/stop
```

Lightweight: a `SleepTimerDataStore` persists the selected duration and remaining time, a coroutine-based countdown runs in the `PlayerRuntimeController`, and the player overlay shows remaining time. No new services needed.

### Files to Create

| File | Purpose |
|------|---------|
| `SleepTimerDataStore.kt` | DataStore-backed persistence: `selectedMinutes: Int` (0=off, 15, 30, 60, -1=end of content), `remainingSeconds: Int`, `startedAtEpochMs: Long`, `timerMode: Enum (TIMER / END_OF_CONTENT / OFF)` |
| `SleepTimerOverlay.kt` | Composable chip/badge in the player overlay showing remaining time (e.g. "⏱ 23 min"), tap to extend or cancel |

### Files to Modify

| File | Change |
|------|--------|
| `PlayerRuntimeController.kt` | Add a coroutine-based countdown: launches when timer starts, ticks every second, stops playback when `remainingSeconds` hits 0. Listen to `onIsPlayingChanged` to pause/resume the countdown |
| `PlayerOverlayScaffold.kt` | Add Sleep Timer button to the player overlay control bar (alongside existing controls). Shows current timer state |
| `PlayerScreen.kt` | Wire `SleepTimerDataStore` reads/writes into the overlay UI |
| `IptvPlayerScreen.kt` | Same — wire sleep timer into IPTV player overlay as well (reuse same composable/manager) |
| `PlayerRuntimeControllerLifecycle.kt` | Ensure timer state is preserved/restored on player lifecycle changes |
| `PlayerUiState.kt` | Add `sleepTimerState: SleepTimerState` field for UI observation |
| `PlayerViewModel.kt` / `IptvPlayerViewModel.kt` | Add `onSetSleepTimer(minutes)`, `onCancelSleepTimer()`, `onExtendSleepTimer(minutes)` functions |
| `NuvioApplication.kt` | Initialize `SleepTimerDataStore` if needed

### Data Flow

1. User is watching any content (Stremio movie/episode or IPTV channel) and opens player overlay
2. Sleep Timer button shows: "⏱ Timer" (when off) or "⏱ 23 min" (when active)
3. User taps the button → popup/bottom sheet with options:
   - `[ 15 min ] [ 30 min ] [ 60 min ]`
   - `[ End of episode / content ]` — uses existing `PlayerNextEpisodeRules` / `ExternalAutoNextPolicy` to detect when current content ends
   - `[ Cancel ]` (only if timer is active)
4. When user selects an option:
   - `SleepTimerDataStore` persists the choice
   - `PlayerRuntimeController` starts a coroutine countdown:
     ```kotlin
     viewModelScope.launch {
         while (remainingSeconds > 0 && isPlaying) {
             delay(1000)
             remainingSeconds--
             sleepTimerDataStore.updateRemaining(remainingSeconds)
         }
         if (remainingSeconds <= 0 && !cancelled) {
             player.pause()
             showSleepTimerNotification("Sleep timer ended")
         }
     }
     ```
5. Countdown pauses when playback pauses (e.g. user hits pause), resumes when playback resumes
6. When timer expires: player pauses, overlay shows "⏱ Sleep timer ended" toast, notification fires
7. User wakes up to a paused screen instead of random video playing for hours

### END_OF_CONTENT Mode

- User picks "End of episode" → `timerMode = END_OF_CONTENT`
- Player monitors current track ending via `Player.Listener.onPlaybackStateChanged` → `STATE_ENDED`
- When content ends normally, player pauses instead of auto-playing next (temporarily overrides auto-play)
- After pause, timer resets to OFF
- Also works during post-play overlay — if user dismisses post-play, player stays paused

### UI Details

**Button states (in player overlay):**

| State | Button Display |
|-------|---------------|
| Off | ⏱ Timer (icon only) |
| 15 min active | ⏱ 12 min (rounded to nearest minute, updates every 5 seconds to avoid jitter) |
| 30 min active | ⏱ 28 min |
| 60 min active | ⏱ 47 min |
| End of content | ⏱ End of episode |

**Popup options:**
- D-pad friendly: 4 focusable chips in a 2×2 grid
- Active option is highlighted with accent color
- "Cancel" option only shows when timer is active
- Popup auto-dismisses on selection

### Persistence

- Timer survives device sleep/wake (DataStore persists `remainingSeconds` + `startedAtEpochMs`)
- Timer does NOT survive app kill (too risky — user may reopen hours later)
- On app/player restart: check if timer was running, discard if elapsed > 60 seconds beyond expected expiry, resume if still counting

### Edge Cases

| Case | Behavior |
|------|----------|
| User pauses content, timer hits 0 | Player stays paused — already stopped |
| User switches to different content | Timer keeps running (they set it for the session, not per-video) |
| User extends timer mid-countdown | Add selected minutes to `remainingSeconds` |
| App killed and relaunched | Timer discarded (DataStore cleared on app start if stale) |
| Content ends before timer | Post-play overlay shows, timer still counts down — if it expires while on post-play, screen stays on post-play |
| Multi-window mode | Timer applies to the focused / master player, stops all playback when expired (same as pausing all)

---

## 12. NuvioNutz — Cinematic Tuner

**Goal:** New hub that browses Nuvio addon catalogs as live TV channels with auto-rotation. Each catalog is a "channel" — flipping through them gives a TV-like browsing experience.

### Architecture

```
New hub card → CinematicTunerScreen → Nuvio addon catalogs → each catalog = channel → auto-rotate timer
```

Port of Debrify's Stremio TV Cinematic Tuner. Uses existing Stremio addon infrastructure (`AddonRepository`, `CatalogRepository`, `MetaRepository`) to turn catalog queries into channel-like entries that auto-rotate.

### Files to Create

| File | Purpose |
|------|---------|
| `CinematicTunerScreen.kt` | Main hub composable: channel grid (each channel = an addon catalog), tuner controls, auto-rotation timer display |
| `CinematicTunerViewModel.kt` | Manages addon catalog list, current channel state, rotation schedule, favorites |
| `CinematicTunerModels.kt` | `TunerChannel(id, addonName, catalogType, catalogId, currentItem, nextItem, config)` — wraps an addon catalog as a channel |
| `TunerChannelCard.kt` | Composable card: shows addon name, current "now playing" item poster, next item preview, live dot indicator |

### Files to Modify

| File | Change |
|------|--------|
| `Screen.kt` | Add `CinematicTuner` route |
| `NuvioNavHost.kt` | Add composable route for CinematicTunerScreen |
| `RobbdeezeNutzHubViewModel.kt` | Add `HubSubScreen.CinematicTuner` entry, `cinematicTunerState` flow |
| `RobbdeezeNutzHubScreen.kt` | Add NuvioNutz hub card (icon: TV/antenna, color: teal/purple gradient) |
| `AddonRepository.kt` / `CatalogRepository.kt` | Already provide addon catalogs — may need method to get all catalogs at once |

### Data Flow

1. User opens NuvioNutz hub → `CinematicTunerViewModel` loads all installed Stremio addons via `AddonRepository`
2. Each addon's catalogs (movies, series, etc.) become a **TunerChannel**:
   - Channel name: "[Addon Name] — Movies" or "[Addon Name] — Series"
   - Channel icon: addon logo
   - Content: fetched from `CatalogRepository` for that catalog
3. User sees a grid of tuner channels. Selecting one opens a full-screen view:
   - Current item displays with poster, title, year, brief description
   - "Flip to next" button or auto-rotate timer
   - Timer counts down: next item loads after configured interval (default 30s, configurable 15s/30s/60s)
4. Auto-rotation: after timer expires, next item from the catalog loads automatically
   - Uses the catalog's paginated results — cycles through available items
   - When end of catalog reached, wraps to beginning
5. User can:
   - **Watch Now** → play current item like normal (opens stream selection → player)
   - **Flip** → manually advance to next item
   - **Favorite** → pin channel to top of list
   - **Filter** → filter visible channels by addon or content type
   - **Speed** → change auto-rotation speed (15s / 30s / 60s / Manual)

### UI Design

**Channel browser (grid view):**
- 4-column grid of tuner channel cards
- Each card: rounded poster-sized card, addon logo top-left, channel name bottom, "Now Playing" item poster as background, small live dot indicator
- Favorites row at top (pinned channels)

**Full-screen tuner view:**
- Background: hero backdrop of current item (like ModernHomeHero)
- Center: large poster + title + year + brief synopsis
- Bottom bar: channel name, rotation timer countdown ("Next in 23s"), progress dots (current position in catalog)
- D-pad: Left/Right to flip channels, Up to see channel list overlay, Select to watch, Play to toggle rotation pause

**Controls overlay (auto-hide, 5s timeout):**
- ⏪ Previous item
- ⏸ Pause rotation / ▶ Resume rotation
- ⏩ Next item
- 🔄 Channel selector (opens grid overlay)
- ⏱ Speed: Manual / 15s / 30s / 60s
- ▶️ Watch Now

### Auto-Rotation Behavior

| State | Behavior |
|-------|----------|
| Tuner open, rotation on | Every N seconds, fade-transition to next catalog item |
| Tuner open, rotation paused | User manually flipping through items |
| Item selected to watch | Exit tuner, open normal player. Return to tuner on player close |
| Rotation hits end of catalog | Wrap to first item |
| Multiple catalogs | Rotation stays within selected catalog channel |
| Settings changed mid-rotation | Update interval immediately, no reset

---

## 13. DeezeNutz — Always-On Channels

**Goal:** New hub that creates auto-generated TV channels from keyword recipes combined with debrid engines. Channels continuously play content that matches keyword rules — like having your own personal TV channels that never run out.

### Architecture

```
New hub card → DeezeNutzScreen → channel list (keyword recipes + debrid) → auto-play with random start → cached content rotation
```

Port of Debrify TV's core features (#19–23). Combines keyword recipes with Real-Debrid/Torbox/Premiumize engines to auto-build always-on channels. Smart caching keeps content ready, RD-blocked filter prevents playback stalls.

### Files to Create

| File | Purpose |
|------|---------|
| `DeezeNutzScreen.kt` | Main hub composable: channel grid, channel view (currently playing), recipe editor |
| `DeezeNutzViewModel.kt` | Manages channels, recipes, auto-play state, caching, rotation |
| `DeezeNutzModels.kt` | `DeezeChannel(id, name, keywords, debridProvider, filters, currentItem, queue)`, `KeywordRecipe(keywords, genres, yearRange, minSize, maxSize)`, `ChannelItem(title, poster, torrentHash, fileIndex)` |
| `DeezeChannelCard.kt` | Composable: channel name, current item poster, "now playing" badge, next queue preview |
| `DeezeRecipeEditor.kt` | Screen/dialog for creating/editing keyword recipes with filters |
| `DeezeNutzCacheService.kt` | Manages torrent caching per channel: pre-caches next N items in background, rotates expired ones out |
| `DeezeNutzStore.kt` | DataStore-backed persistence for channels, recipes, cache state |

### Files to Modify

| File | Change |
|------|--------|
| `Screen.kt` | Add `DeezeNutz` route |
| `NuvioNavHost.kt` | Add composable route for DeezeNutzScreen |
| `RobbdeezeNutzHubViewModel.kt` | Add `HubSubScreen.DeezeNutz` entry, `deezeNutzState` flow |
| `RobbdeezeNutzHubScreen.kt` | Add DeezeNutz hub card (icon: satellite dish / radio tower 📡, color: neon green/red gradient) |
| `DebridProvider.kt` / `DirectDebridResolver.kt` | Already supports RD/PM/Torbox — used for torrent resolution |
| `TorrentService.kt` / `TorrServerApi.kt` | Used to cache/stream resolved torrents |

### Feature 19 — Keyword-Driven Channels

**How it works:**
1. User creates a channel with a keyword recipe:
   ```
   Name: "90s Action"
   Keywords: "1990-1999 action movie"
   Debrid Provider: Real-Debrid
   Filters: minSize: 2GB, maxSize: 15GB, minSeeds: 50
   ```
2. On creation, `DeezeNutzViewModel` searches debrid torrent indexes for matching content
3. Results are queued as `ChannelItem`s in the channel's play queue
4. Channel shows in the hub grid with current queue count

**Recipe editor fields:**
- Channel name
- Keywords (comma-separated, AND logic)
- Debrid provider selector (RD / PM / Torbox)
- Quality filters: `[ 720p ] [ 1080p ] [ 4K ] [ Any ]`
- Size range slider: min GB — max GB
- Year range: from — to
- Genre tags (optional, if available from torrent metadata)
- Max items in queue: slider 10–100

### Feature 20 — Auto-Launch Overlay & Random Start

**Auto-launch flow:**
1. User taps a DeezeNutz channel → `DeezeNutzViewModel.prepareChannel(channelId)`:
   - Picks a random item from the queue (random start offset)
   - Resolves torrent → debrid stream via existing `DirectDebridResolver`
   - Opens player with the resolved stream
2. On channel open, a brief overlay shows:
   - Channel name
   - Current item title
   - Progress: "Item 3 of 24"
   - "Watch from beginning" vs "Random start" badge
3. When current item ends → auto-advance to next item in queue
4. If queue runs low (<5 remaining), trigger background refill

**Random start behavior:**
- When starting a channel, skip to a random point in the current item (offset 0%–80%)
- User can tap "Restart" to play from beginning
- After the random-start item finishes, next items play normally
- Creates the feel of flipping to a live TV channel mid-movie

### Feature 21 — Smart Caching & Rotation

**Caching strategy:**
- Background work: `DeezeNutzCacheService` proactively caches torrents via `TorrentService` for the next N items in each channel's queue
- When an item finishes playing, it's moved to "played" list
- Next item's torrent is already cached → instant playback
- Expired cache: after 24h, played items' cache is cleared to free space

**Rotation logic:**
- Each channel maintains a rotating buffer of 20–50 items (configurable)
- When buffer drops below threshold (5 items), trigger a new keyword search
- New results are appended to the queue, oldest played items rotate out
- Dedup prevents same item from appearing twice
- Freshness: re-search every 24h to find new content matching the recipe

### Feature 22 — Community Channels

**Import/Export:**
- Export a channel as a YAML/JSON recipe file
- Import recipe files shared by the community
- Bundle format: ZIP containing multiple `.deezechannel` recipe files
- Import from clipboard, file picker, or URL

```yaml
# example.deezechannel
name: "90s Action"
keywords: "1990-1999 action"
provider: real_debrid
filters:
  quality: 1080p
  minSize: 2
  maxSize: 15
  minSeeds: 50
maxQueueItems: 30
```

**UI for sharing:**
- Channel card overflow menu → "Export Recipe" → generates YAML, shares via system share sheet
- Hub main screen → "Import" button → pick file or paste recipe content
- Preview before adding: shows channel name, keywords, filters, estimated result count

### Feature 23 — RD-Blocked Torrent Filtering

**Problem:** Real-Debrid blocks certain torrents (copyright-infringing content). When a blocked torrent is sent to RD, it returns an error, and playback stalls.

**Solution:** Before adding an item to a channel's queue, check if RD will accept the torrent hash:
- Use existing `RealDebridDirectDebridResolver` or add a quick hash availability check
- If RD returns blocked status, skip that torrent and try the next result
- Fallback to Torbox or Premiumize if available for that channel
- Track blocked hashes in `DeezeNutzStore` to avoid re-checking

### UI Design

**Channel browser (hub main view):**
- 3-column grid of DeezeNutz channel cards
- Each card: large gradient background with channel name, queue count badge ("32 items"), current item poster if cached
- Favorites row at top
- "+" card at end to add new channel → opens recipe editor

**Channel player view (full screen):**
- Opens existing player with debrid stream
- Player overlay shows channel info: channel name, queue position ("5 of 24"), ⏭ next-item button
- "Return to channel" instead of back to hub
- Auto-advance when item ends (already exists in player)

**Recipe editor (dialog/screen):**
- Split into Basic (name + keywords) and Advanced (filters) sections
- D-pad friendly: focusable text fields, chip selectors for quality/options
- Live preview: "Estimated results: ~42 items" updates as filters change
- "Create Channel" button

### Persistence

- All channels, recipes, and queue metadata stored in `DeezeNutzStore` (DataStore)
- Torrent cache state separate (can be cleared without losing channel config)
- Export/import via file share system
