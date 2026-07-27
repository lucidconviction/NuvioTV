# RNutz NuvioTV — Version History & Update Log

> All changes, modifications, and additions made to this fork are documented here.
> Use this to track what was done and to roll back if needed.

---

## v0.13.0 — HD YouTube, Source Menu Overlay, Stream Validation, Upstream Bug Fixes

**Date:** 2026-07-25

**Goal:** HD YouTube playback (1080p+ with audio muxing), IPTV source management menu, stream health validation system, port critical upstream crash fixes.

### Added
- **HD YouTube playback** — `YouTubeStreamResolver.kt` rewritten to use YouTube InnerTube API directly (like `InAppYouTubeExtractor`) with visitor_data fetching, multi-client fallback (ANDROID_VR/ANDROID/IOS), and CDN probing. Returns video-only adaptive streams up to 4K paired with separate AAC audio for muxed playback. Falls back to Piped API and NewPipe extractor
- **In-player quality selector** — `IptvPlayerScreen.kt` gear button opens quality overlay listing available resolutions; selecting a quality hot-swaps the video stream while keeping audio. Resolution badge shown in channel info bar
- **Audio-video muxing** — `IptvPlayerScreen.kt` uses `MergingMediaSource` + `YoutubeChunkedDataSourceFactory` when `audioUrl` is present on a channel, enabling 1080p+ playback with separate audio
- **Source menu overlay** — tapping a playlist source in IPTVNutz dashboard now shows popup with: View Channels, Refresh (remove + re-add for fresh pull), Validate Streams, Delete
- **Stream validation system** — `StreamValidationStore.kt` persists dead/good URLs per profile via DataStore. `StreamValidator.kt` tests URLs with HTTP HEAD requests (batched 20 at a time, 5s timeout). Validating from source menu shows real-time progress and results
- **Dead stream filtering** — channels with dead URLs are hidden by default. "Show dead streams (N)" toggle appears when dead streams exist. Filters applied to both search results and category-grouped views
- **Validation progress display** — shows during validation ("Validating 15/1420...") and results ("All streams working!" or "Found 12 dead of 1420")

### Changed
- **Source cards** — removed individual delete icon buttons, moved to source menu overlay
- **BackHandler** — updated to handle source menu overlay dismissal
- **Phone/pad info bar** — resolution badge shows current playback quality (e.g. "• 1080p")

### Fixed
- **DataStore ENOENT crash** — pre-create DataStore directory and file before initialization to prevent `FileNotFoundException` race (upstream fix)
- **CookieJar concurrent modification** — `synchronized()` on per-host cookie list prevents `IndexOutOfBoundsException` during concurrent `saveFromResponse`/`loadForRequest` (upstream fix)
- **ForegroundService crash** — `startForeground()` wrapped in try-catch to handle `ForegroundServiceStartNotAllowedException` on Android 12+ (upstream fix)
- **Duplicate LazyColumn collection keys** — deduplicated collections at parse, cache, and build levels to prevent `IllegalArgumentException: Key "collection-latest-new" was already used` (upstream fix)
- **Stock LoadControl back buffer** — increased from 0 to 1.5s so 1s rewind doesn't clear buffer (upstream fix)
- **DV Profile 5 strip decision** — `DolbyVisionMatroskaTransformer` now scans for HDR10 static metadata SEI before deciding to strip DV5 RPU, preventing unnecessary stripping on sources whose base layer carries its own HDR10 metadata (upstream fix)
- **n-parameter stream filtering** — InnerTube streams with YouTube anti-leech `n` parameter are filtered out to prevent 403 playback errors
- **Player setup error handling** — `IptvPlayerScreen.kt` `playOnPlayer()` wrapped in try-catch with fallback to plain URL

---

## v0.14.0 — VidNutz Live Streams, 15/Page, Load More, Sport Focus Animations, Stream Validator Fix, Auto-Build Sync

**Date:** 2026-07-25

**Goal:** VidNutz page size 15 with working Load More, Live Streams category, visible D-pad focus on all sport video cards, reliable stream validator, automatic APK sync to Cloudflare on build.

### Added
- **VidNutz Live Streams tab** — `LIVE_STREAMS("Live Streams")` added as first category tab before Trending, searches YouTube for "live streams now"
- **VidNutz page size** — reduced from 32 to 15 per page for faster loading
- **VidNutz Load More button** — visible button at bottom of video grid with loading spinner; loads next 15 on demand
- **SportNutz scale animations** — all sport video cards (hero, clips grid, VideoCardSmall) now scale 1.05x on focus via `animateFloatAsState` + `graphicsLayer`, matching VidNutz pattern
- **YouTube stream result cache** — `YouTubeStreamResolver` caches resolved results per videoId with 5-minute TTL for instant repeat playback
- **Auto-build sync** — build script copies APK to `~/apps-www/` and purges Cloudflare cache for the APK URL

### Changed
- **VidNutz pagination** — removed special page-1 backend (VideoSuggestionEngine/NewPipe), now uses Invidious/Piped consistently for all pages with proper page offset = `(page-1)*15`
- **VidNutz search** — removed `VideoSuggestionEngine` + `PlatformYouTubeSearch` from search path, always uses Invidious/Piped with shuffled sublist pagination
- **Stream validator** — changed from HTTP HEAD to byte-range GET (`Range: bytes=0-0`) for better compatibility with IPTV streaming servers that reject HEAD
- **IptvPlayerScreen gear button** — removed (was causing crash on tap)
- **MultiWindow auto-next** — when stream ends (STATE_ENDED), advances to next channel in same slot after 2s delay
- **MusicNutz** — removed Downloads mode chip and download icon from TrackCard
- **MagNutz** — reduced poll interval from 2s to 1s for faster torrent status updates
- **SportNutz loading** — `playingVideoId` state set before stream resolution to indicate loading

### Fixed
- **VidNutz pagination** — `getVideosByCategory` preCache now correctly checks existence before removing, preventing cache miss on page 2
- **Stream validator reliability** — changed from HEAD to byte-range GET; M3U/Xtream streaming URLs often reject HEAD but respond to partial GET

---

## v0.16.0 — SportNutz Team Detail, Sync2Cal Schedule, Addon Backup, Xtream UX Fix  

**Date:** 2026-07-26  

**Goal:** Port mobile SportNutz features (TeamDetailScreen, Sync2Cal, Wikipedia images), addon per-profile backup, fix Xtream Extreme Code source entry, version footer.  

### Added  
- **TeamDetailScreen** — new TV screen showing team recent results, live games, and upcoming games when clicking a team name in event cards or event detail panel  
- **TeamClickableChip** — team logos + names in SportEventDetailPanel header, clickable to open TeamDetailScreen  
- **ESPN event cards** — enhanced with team logos, display names, and live scores (was: event name only)  
- **Wikipedia image fallback** — `WikipediaClient.kt` fetches Wikipedia REST API thumbnails for fighting events (UFC/Boxing/PFL) when ESPN images are unavailable  
- **Sync2Cal integration** — `Sync2CalClient.kt` (HTTP client), `Sync2CalMappings.kt` (20 league mappings), `Sync2CalSection.kt` (TV UI showing upcoming events with TV channels). Loaded in Sports Now tab  
- **Backup per-profile addons** — `BackupData` extended with `addonUrls` + `addonEnabledStates`; export/import of addon URLs and enabled states via `AddonPreferences`  
- **Version footer** — `v{BuildConfig.VERSION_NAME}` at bottom-right of hub screen  

### Changed  
- **Xtream/Extreme Code form** — replaced single URL field with Server URL + Username + Password fields; URL auto-constructed as `{server}?username={user}&password={pass}`  
- **parseXtreamParams** — preserves URL path (previously stripped paths like `/c/`, breaking providers on subdirectories)  
- **SportEventDetailPanel** — added `onTeamClick` callback; team names rendered as clickable chips with logos  
- **HubScreenContent** — reduced bottom padding; added version label  

### Fixed  
- **Xtream Extreme Code not working** — missing username/password fields and URL path stripping in parser  

---

## v0.15.0 — MultiWindow Refresh Fix, Memory Optimization, Update URL Fix

**Date:** 2026-07-25

**Goal:** Fix MultiWindow Refresh All button, reduce RAM usage, fix in-app update APK URL.

### Fixed
- **MultiWindow Refresh All** — player handle is now read reactively from `store.playerHandleIds` via `remember(stream.id, currentHandleId, stream.channel.url)`. When `refreshStream` creates a new player, the composable detects the handle ID change and uses the new handle instead of the stale cached one, making Refresh All actually work
- **MultiWindow auto-next removed** — each slot has only one channel (no per-slot queue), so auto-advance doesn't apply. Removed broken LaunchedEffect that was causing issues
- **Coil image cache memory** — reduced from 33% to 10% of device RAM. On a 6GB device this drops from ~2GB to ~600MB, significantly reducing memory pressure and OOM risk
- **UpdateRepository APK regex** — changed from `Nuvio-TV*.apk` to `RNutz-NuvioTV*.apk` to match the actual APK filename on `apps.rdnutz.us`
- **Full flavor universal build** — `build-all.sh` now builds the full flavor (with updater) instead of playstore, synced to Cloudflare for OTA updates

---

## v0.12.0 — Quick Channels, Channel Cache, Collapsible Groups, Multi-Window Overhaul, Direct APK Updates

**Date:** 2026-07-23

**Goal:** Add curated Quick Channels (US/UK/CA/Premium/Sports/News), cache channels for fast loading, collapsible category groups in IPTV browser, full IPTV-style overlay in multi-window, auto audio switching, SportNutz search, direct APK update URL.

### Added
- **Quick Channels** — `QuickChannelList.kt` with ~240 curated channels across US, UK, CA, Premium, Sports, News categories; filter tabs in IPTV dashboard and player overlay; tapping shows popup with all matching channels across all sources to pick from
- **Channel cache** — `ChannelCache.kt` (DataStore + JSON, 1hr TTL) caches parsed M3U/Xtream channels per source URL; `IptvRepositoryImpl` checks cache before network fetch, saves after load — subsequent loads are instant
- **Collapsible channel groups** — IPTV channel browser replaced flat category tabs + grid with expandable/collapsible category sections (arrow indicators ▸/▾), search still shows flat filtered grid
- **SportNutz search bar** — search field added to now/later tab filtering DaddyLive and ESPN events by name/sport; ESPN live/upcoming events fully merged alongside DaddyLive
- **Quick Channels popup in player overlay** — "Quick" tab in channel switcher shows match numbers per channel; tapping shows transparent popup with all matching channels, dpad auto-focuses the list
- **Multi-window audio auto-switch** — tapping a grid cell now calls `store.setAudioFocus(stream.id)` so audio follows focus
- **Direct APK update** — `UpdateRepository` now downloads directly from `https://apps.rdnutz.us/`, removed GitHub API dependency
- **`QuickChannel.kt`** — domain model with `displayName`, `aliases`, `regions`, `tags`

### Changed
- **MultiWindowCellOptions channel picker** — replaced basic channel list with full IPTV-style overlay: tabs (Channels/Favorites/History/Quick), search bar with keyboard support, dpad focus, match counts on quick channels
- **IPTV dashboard** — Recently Watched and Favorite Channels cards now show channel options popup (Watch/Multi-View) instead of playing directly
- **Quick Channels match popup** — both hub dashboard and player overlay now use transparent overlay with dpad auto-focus on first list item
- **Source limit** — 5 → 10 across all add points (Quick Add, PortalNutz, Add Source form)
- **About screen** — Check for Updates link changed from GitHub releases to `apps.rdnutz.us`
- **IptvSubScreen sections reordered** — Recently Watched, Favorite Channels, Playlist Sources, Quick Add, PortalNutz, Add New Source

### Fixed
- **20k+ channel crash** — duplicate ID collision in `LazyVerticalGrid` fixed: key changed from `it.id` to `it.url`, M3uParser fallback ID changed from `url.hashCode().toString()` to `url`
- **Focus borders on channel rows** — `ChannelListContent` and `ChannelSelectionPanel` rows now have white border on focus (was only background color change, invisible on dark screens)
- **SportNutz now/later NPE** — `selectedEvent!!` replaced with safe `?.let{}` in `onLoadStandings`
- **SportNutz blank screen** — live tab now shows ESPN events as fallback when DaddyLive returns empty
- **Multi-window channel overlay not loading** — player `IptvPlayerViewModel` now pre-populates `allChannels` from `IptvPlayerStore.channels` on init instead of waiting for `loadAllChannels()` to finish
- **Player overlay duplicate key** — changed from `ch.id` to `ch.url` in LazyColumn key

### Removed
- GitHub Release API integration from `UpdateRepository` — now points directly to `https://apps.rdnutz.us/`

---

### Added
- **Email/Password Sign-In on TV** — `AuthSignInScreen.kt` rewritten with full email/password form (was "TV sign-in is disabled"). Sign-in/sign-up toggle, error display, QR code option below divider. Uses proper Supabase Kotlin SDK `auth.signInWith(Email)` / `auth.signUpWith(Email)` instead of raw HTTP calls
- **Discord invite popup** — `DiscordPromptDialog.kt` + `DiscordPromptStorage.kt` ported from mobile; shows on first launch with "Don't show again" checkbox
- **Video player loading overlay** — `RobbdeezeNutzHubViewModel.playVideo()` and `playMusicTrack()` show full-screen loading spinner before stream resolution, preventing black screen during 5-15s YouTube stream resolve

### Changed
- **YouTubeStreamResolver** — now sorts streams by resolution descending and tries Piped API first (gives higher quality streams like 1080p+) before falling back to NewPipe local extraction
- **AuthSignInScreen** — settings account button now routes to email/password screen first, QR option available below divider
- **BackupRestoreScreen** — `restoreBackup()` now handles all 8 exported sections (iptv_sources, iptv_favorites, iptv_history, multi_window_bookmarks, magnutz_torrents, music_playlists, music_saved_albums, music_downloads). Added `channelId` to `BackupBookmarkedSlot` and `albumCover` to `BackupMusicDownload` for proper round-trip fidelity

### Fixed
- **PortalNutz search** — `scrape()` wrapped in `rememberCoroutineScope().launch` so state updates don't cause LazyColumn recomposition that scrolls the screen up
- **Sign-in error mapping** — switched from raw HTTP request + fragile `userFriendlyError` parsing to Supabase Kotlin SDK, which returns proper typed error messages
- **VidNutz/MusicNutz video resolution** — `playVideo()`/`playMusicTrack()` now show loading overlay immediately on tap, resolve stream URL, then navigate to player (was: no loading indicator during 5-15s stream resolution)

### Removed
- Direct HTTP calls to Supabase `/auth/v1/token?grant_type=password` — replaced with SDK `auth.signInWith(Email)`

---

## v0.10.0 — PortalNutz, DaddyLive Sports, Video Engine, Navigation Overhaul

**Date:** 2026-07-16

**Goal:** Add portal scraping as IPTV sources, live sports scheduling via DaddyLive API, dynamic video suggestion engine, layered back navigation.

### Added
- **PortalNutz** — on-device scraper (`PortalNutzScraper.kt`) that fetches Xtream portal credentials from GitHub repos, Telegram channels, and AMZ IPTV listings; verifies them via `player_api.php`/`get.php`; filters by adult/English/sports criteria; returns top 5 working portals as `portal1`–`portal5` labels
- **PortalNutz UI** — expandable card in IPTV sources dashboard with filter chips (English, No XXX, Sports, XXX), Search button, progress indicator, result list with "Add" button (hides server info — only shows "Portal 1" etc.)
- **Portal source cards** — portal sources display as large **P1/P2/P3** labels in source grid (no URL, no type badge); regular sources unchanged with delete button
- **Source limit** — max 5 IPTV sources enforced across all add points (quick add, form, PortalNutz), shows toast at cap
- **DaddyLiveClient** — Kotlin client (`DaddyLiveClient.kt`) that discovers active DaddyLive mirrors, fetches `/api/events` returning structured events with `eventName`, `category`, `startTime`, `day`, and `channels[]` (name, channelId, embedUrl); parses day+time assuming ET, converts to device local timezone using `startEpochMs`/`localTime`/`localDate`
- **SportNutz "Sports Now/Later"** — Live/Upcoming tabs showing DaddyLive events; Live tab (2-col grid) shows event name, category, device-local time, channel names broadcasting it; Upcoming tab (3-col grid) adds date; clicking opens popup that matches channels against IPTV sources via `matchSportEventChannels()` — shows matched channels (tap to play) or raw channel list if no match
- **Player Live Games overlay** — fetches DaddyLive events when player opens, shows live events with channel names; clicking matches channel names against `allChannels` via fuzzy containment and switches to first match
- **VideoSuggestionEngine** — global dynamic video rotation: per-category query rotation (4 variations), seen-ID tracking, random affixes, fallback reset; integrated into `VidNutzRepositoryImpl` for category browsing and `SportsSubScreen` for highlight clips
- **Dynamic video algorithm** — `VidNutzRepositoryImpl` now fetches 32 videos via engine (up from 20), rotates queries to avoid repeated results
- **Layered back navigation** — SportsNutz: event → league → home → Hub; MusicNutz: album → home → Hub; IPTV: popup → browser → dashboard → Hub
- **Hub cards** — changed to horizontal `LazyRow` centered on page (was 2-col grid); cards show nutz names in big `displaySmall` font with color gradient, no subtitle
- **Bottom bar** — removed "System Online", green dot, heart icon, "Gigabit Fiber"
- **About screen** — replaced "Made with love" with "Forked by RobbdeezeNutz" in `headlineSmall`; version number below; update button opens `https://github.com/Robbdeeze/NuvioTV/releases`
- **Focus borders** — 2dp white ring wrapping entire card (thumbnail + text) on `VidNutzVideoCard`, `AlbumCard`, `TrackCard`; placed after `graphicsLayer` to avoid scale clipping
- **VidNutz search** — fixed: added `showVidNutzSearch` toggle (was broken — search field never appeared); search icon now shows/hides field
- **App name** — RNutz NuvioTV across all 32 locale files
- **Supabase credentials** — added to `local.properties` for QR login
- **Portal scraper speed** — contenders increased 30→100, verify limited to 100 shuffled, cherry-pick 5

### Changed
- **SportNutz home** — removed filter chips (All/NFL/NBA/etc.), clips grid uses "Sports" engine key (was falling back to Trending = non-sports content)
- **IPTV sources dashboard** — added 5dp start padding, LazyColumn
- **HubCard** — removed `title` and `description` params, displays only `badge` string in large centered font
- **Back navigation** — all sub-screens now step back one level at a time instead of jumping to Hub

### Fixed
- **VidNutz search bar** — was hidden behind `AnimatedVisibility(visible = query.isNotEmpty())` with no way to start typing; now toggles via `showVidNutzSearch` state
- **SportNutz video clips** — all filter searches used "Trending" engine key (non-sports); changed to "Sports" key
- **Focus borders** — moved after `graphicsLayer` in modifier chain so they don't get clipped by scale transform; removed redundant `.focusable()`

---

**Date:** 2026-07-16

**Goal:** Polish hub sub-screens with consistent 4-column grids, D-pad navigation improvements, and MultiWindow channel integration.

### Changed
- **MusicNutz** — tracks grid changed from 2-column to 4-column, albums grid from 3-column to 4-column
- **RobbdeezeNutz Hub** — removed top and bottom margins for edge-to-edge layout
- **VidNutz** — search bar moved to top-left as a retractable button (inline expansion)
- **All sub-screens** — removed on-screen ArrowBack navigation buttons, D-pad Back is exclusive
- **MultiWindow GlobalActionBar** — redesigned with toggle-aware buttons (Mute/Unmute All, Pause/Play All, Refresh All, Close All) with active-state color feedback
- **MultiWindow bookmark system** — `MultiWindowBookmarkStore` now stores per-slot channel info (id, name, URL, logo) alongside layout; save/restore preserves exact channel grid
- **MultiWindow Browse Channels** — now navigates to IPTV channel browser screen instead of local overlay
- **MultiWindow** — removed standalone Window Picker button (Browse Channels + Cell Options CH picker provide access)

### Fixed
- **ExoPlayer controls** — D-pad navigation now works on all on-screen player controls (play/pause, seek, volume, etc.) via auto-focus on control appearance
- **Duplicate graphicsLayer import** — cleaned up

### Added
- **MultiWindow** — "Browse Channels" navigates to IPTV sub-screen for channel selection
- **MultiWindow layout bookmarks** — save/restore entire grid layout with all channel assignments per slot

---

## v0.9.2 — Package Rename, Release Build Fix

**Date:** 2026-07-09

**Goal:** Rebrand as RNutz NuvioTV with clean package name, fix build errors after rename.

### Changed
- **Package name** — `com.nuvio.tv` → `com.robbdeeze.nuviotv` across all source files, manifests, and build config
- **App name** — "Nuvio" → "RNutz NuvioTV"
- **About screen** — Version text replaced with "Forked by RobbdeezeNutz"
- **dvmkv Java files** — Fixed inline fully-qualified class references that broke after rename
- **GitHub release** — All 5 architecture APKs uploaded

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
