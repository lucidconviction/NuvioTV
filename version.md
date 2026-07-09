# RNutz NuvioTV — Version History & Update Log

> All changes, modifications, and additions made to this fork are documented here.
> Use this to track what was done and to roll back if needed.

---

## v0.9.1 — Sports Grid Layout, MusicNutz API Fix

**Date:** 2026-07-09

**Goal:** Better sports viewing with grid layout, fix MusicNutz not loading.

### Changed
- **Sports scoreboard** — replaced vertical `LazyColumn` list with 2-column `LazyVerticalGrid`, bigger fonts throughout
- **MusicNutz API** — added `User-Agent` and `Accept` headers to Deezer HTTP requests, HTTP status validation (was silently returning empty)
- **VidNutz API** — added `User-Agent` header to Invidious/Piped requests for consistency

---

## v0.9.0 — Sports Enhancement: Live Dashboard, Quick Switch, TheSportsDB

**Date:** 2026-07-09

**Goal:** Unified live events view, quick game switching in player, TheSportsDB fallback, better combat sports support.

### Added
- **Sports Now Dashboard** — "⚡ Sports Now" league chip scans all leagues for live events, shows unified 2-column grid with team logos, scores, league badges
- **Live Games overlay in IPTV Player** — "Live Games" button in player controls, slide-up overlay showing all live events with scores and "Switch" button
- **Find Channel button** on every live event card in the scoreboard (blue accent card, only on in-progress events)
- **TheSportsDB data source** — `TheSportsDbClient.kt` fetches upcoming/past events; falls back when ESPN returns empty (league IDs: NFL, NBA, MLB, NHL, MLS, UFC, Boxing)
- **PPV / Special Events** league — searches YouTube for "PPV events", "UFC PPV", "WrestleMania" instead of ESPN
- **Last Channel Toggle** — BACK button in IPTV player switches to previous channel instead of exiting
- **Channel categories as tab bar** — selected tab shows dark background + blue underline indicator
- **Full-bleed channel card logos** — `ChannelGridCard` redesigned: logo fills entire card with gradient overlay, name at bottom, favorite heart top-right
- **ContinueWatchingStore** — DataStore-backed persistence for last 10 watched items
- **Collapsed search on MusicNutz** — search icon button toggle (same pattern as IPTV/VidNutz/Sports)

### Changed
- **SportsModels.kt** — fixed `isLive` to use exact word matching instead of substring `contains("in")`
- **Add Source form** — always open, D-pad focus chain (Name→URL→M3U→Xtream→Add), RadioButtons replaced with clickable Cards
- **IPTV sources** — redesigned as horizontal `LazyRow` of cards (170x140dp with name, URL, type badge)
- **D-pad highlighting** — scale increased 1.05x→1.08x, shadow elevation added, across all media cards
- **Back navigation** — player returns to correct hub sub-screen (Iptv/Sports/VidNutz/MusicNutz) via `IptvPlayerStore.returnToSubScreen`
- **ChannelGridCard** — full-bleed logo background, channel name overlaid at bottom

### Removed
- **Channel health scan** — removed after user feedback
- **"YouTube" text from video cards** — removed channelName display from `VidNutzVideoCard` and `VideoCardSmall`; YouTube highlight fallback team name changed to "Highlights"

---

## v0.7.0 — System-Wide D-Pad Polish (Phase 7)

**Date:** 2026-07-09

**Goal:** Consistent premium focus effects across all hub sub-screens.

### Changed
- `VidNutzVideoCard` — scale-to-1.05x on focus, `FocusMarqueeText` on titles
- `TrackCard` — scale-to-1.05x on focus
- `AlbumCard` — scale-to-1.05x on focus
- `ChannelGridCard` — scale-to-1.05x on focus

---

## v0.6.0 — IPTV Sub-Screen Polish (Phase 6)

**Date:** 2026-07-09

**Goal:** Premium channel browsing with grid view and category filtering.

### Added
- `ChannelGridCard` — 3-column grid card with channel logo, name, and favorite indicator

### Changed
- `IptvSubScreen` channel browser — replaced `LazyColumn` list with `LazyVerticalGrid` (3 columns), added category chips row (extracted from M3U group-title), "All" chip to reset filter, favorite indicator on grid cards

---

## v0.5.0 — Hub Card Grid Polish (Phase 5)

**Date:** 2026-07-09

**Goal:** Premium TV hub card grid with glass-morphism, scale animations, and color-coded badges.

### Changed
- `HubScreenContent` — replaced vertical `LazyColumn` with 2-column `LazyVerticalGrid`
- `HubCard` — complete redesign: scale-to-1.08x on focus (`animateFloatAsState`), white border ring, increased shadow elevation, color-coded badge per hub (IPTV=blue, Sports=orange, VidNutz=purple, MusicNutz=teal), glass-morphism gradient background, 180dp height

---

## v0.4.0 — Separate IPTV Player (Phase 4)

**Date:** 2026-07-09

**Goal:** Lightweight dedicated IPTV player with channel switching, favorites, and history — zero risk to the existing movie/show player.

### Added
- `IptvPlayerScreen.kt` — standalone composable with ExoPlayer, D-pad controls (play/pause, channel up/down), channel list button, favorites toggle, history button. Auto-hides controls after 5s. Channel switcher overlay reused from `ChannelSwitcherPanel`.
- `IptvPlayerViewModel.kt` (inlined) — manages ExoPlayer lifecycle, channel switching, favorites via `IptvRepository`, history via `ChannelHistoryStore`
- `IptvPlayerStore.kt` — static singleton to bridge channel list from hub to player without navigation args
- `IptvPlayer` route in `Screen.kt` — `iptv_player/{streamUrl}/{channelName}` with optional `channelId`/`logoUrl`
- `ChannelHistoryStore.kt` — DataStore-backed persistence for last 20 played channels (serialized via kotlinx.serialization)

### Changed
- `NuvioNavHost.kt` — added `IptvPlayer` composable route; hub's `onPlayChannel` now stores channels in `IptvPlayerStore` and navigates to `IptvPlayer` instead of `Player`

---

## v0.3.0 — Sports Live Channel Selector (Phase 3)

**Date:** 2026-07-09

**Goal:** Replace highlight-only flow with live IPTV channel selection for sports events, TV-optimized.

### Added
- `SportEvent.isLive` computed flag (status contains "in"/"live"/"playing")
- `SportEvent.leagueAbbreviation` field for channel matching
- `MatchedChannel` + `MatchType` enum (LEAGUE/TEAM/GENERAL_SPORTS) data classes
- `GameToChannelMatcher` — matching engine cross-referencing live events against user's IPTV M3U channels by league abbreviation, team name, general sports keywords
- `ChannelSelectionPanel` — sub-screen when game selected: match info card (team logos, scores), channel list with match-type badges, "Watch Highlights" fallback

### Changed
- `SportsModels.kt` — added `isLive`, `leagueAbbreviation` to `SportEvent`
- `RobbdeezeNutzHubViewModel` — added `selectedSportEvent`/`matchedChannels`/`sportsChannelLoading` state flows, `selectSportEvent()`, `clearSportSelection()`, `loadMatchedChannels()`, auto-refresh 45s polling
- `SportsSubScreen` — complete rewrite: league chips, scoreboard with LIVE badges + team logos, click → channel panel, matched channel list + highlight fallback

---

## v0.2.0 — MusicNutz TV Overhaul (Phase 2)

**Date:** 2026-07-09

**Goal:** Full album browsing + track grid matching the mobile version, optimized for TV (D-pad, 10-foot UI).

### Added
- `MusicNutzCategory` enum (12 genres), `MusicNutzMode` (TRACKS/ALBUMS), `MusicNutzUiState`
- `AlbumDetailView` — full-screen album art header + track list
- `AlbumCard` / `TrackCard` — cover art with play overlay, duration badge, metadata
- `MusicModeChip` — Tracks/Albums toggle

### Changed
- `MusicNutzModels.kt` — replaced flat models with enum + UiState + rich fields
- `MusicNutzRepository` interface — album methods, enum categories
- `MusicNutzRepositoryImpl` — complete rewrite: Deezer API albums, pagination, kotlinx.serialization
- `RobbdeezeNutzHubViewModel` — `musicNutzUiState` flow, album load/search/select/dismiss
- `MusicNutzSubScreen` — complete rewrite: debounced search, category chips, mode toggle, 2-col track grid, 3-col album grid, album detail view

---

## v0.1.0 — VidNutz TV Overhaul (Phase 1)

**Date:** 2026-07-09

**Goal:** Rich video browsing experience matching the mobile version, optimized for TV.

### Added
- `VidNutzCategory` enum (12 categories), `VidNutzUiState`
- Rich `VidNutzVideo` fields: `durationSeconds`, `viewCount`, `uploadDate`, `channelName`

### Changed
- `VidNutzRepository` interface + impl — Invidious/Piped fallback chain, pagination, category-to-query mapping
- `VidNutzSubScreen` — 2-column grid, `VideoCard` with play overlay + duration badge + view count, debounced search, "Load More"
- `RobbdeezeNutzHubViewModel` — `VidNutzUiState` flow, pagination

---

## v0.0.0 — Initial Fresh Clone

**Date:** 2026-07-08

**Source:** Cloned from [tapframe/NuvioTV](https://github.com/tapframe/NuvioTV) — commit `0a36607f` ("Fix duplicate catalog Compose keys")

**State:** Pristine upstream codebase. No modifications yet.

---

## Planned Features

### Phase 2 — MusicNutz TV Overhaul ✅
- [x] Album browsing mode (Tracks/Albums toggle)
- [x] Album grid with cover art
- [x] Album detail view (header + track list)
- [x] Debounced search for tracks and albums
- [x] Pagination / Load More

### Phase 3 — Sports Live Channel Selector ✅
- [x] Game→Channel matching engine (cross-ref live events with IPTV M3U)
- [x] Channel Selection Panel overlay when game selected
- [x] Auto-refresh live scores (45s poll)

### Phase 4 — Separate IPTV Player ✅
- [x] Lightweight IPTV player screen independent of main player
- [x] Channel switching (up/down + list overlay)
- [x] Channel favorites toggle
- [x] Channel history tracking
- [x] D-pad navigation with auto-hide controls

### Phase 5 — Hub Card Grid TV Polish ✅
- [x] 2x2 grid layout
- [x] Scale-up (1.08x) + white border + shadow elevation on focus
- [x] Color-coded badge icons per hub (blue TV, orange SP, purple VN, teal MU)
- [x] Glass-morphism gradient background per card

### Phase 6 — IPTV Sub-Screen Polish ✅
- [x] Channel grid view (3-column grid with channel logos)
- [x] Category filtering chips (extracted from M3U group-title)
- [x] Favorite indicator on grid cards
- [x] "All" chip to show all channels

### Phase 7 — System-Wide D-Pad Polish ✅
- [x] Scale-to-1.05x focus animation (tween 150ms) on all media cards: VidNutzVideoCard, TrackCard, AlbumCard, ChannelGridCard
- [x] Scale-to-1.08x focus animation on HubCards
- [x] FocusMarqueeText on VidNutz video titles

---

## For Future Versions

When you make changes, log them here with this format:

```markdown
### Added
- Feature X — description

### Changed
- File Y — what changed and why

### Fixed
- Bug Z — root cause and fix

### Removed
- Feature W — reason for removal
```

---

## Upstream Reference

| Detail | Value |
|--------|-------|
| Original repo | https://github.com/tapframe/NuvioTV |
| Fork repo | https://github.com/Robbdeeze/NuvioTV |
| Base commit | `0a36607f` |
| Branch | `dev` |
