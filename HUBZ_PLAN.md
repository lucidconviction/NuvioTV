# RobbdeezeNutz Hubz — Build Plan

## Overview

Replace the currently-empty **Hub** area with a full `RobbdeezeNutzHubScreen` containing 4 glass-style cards (IPTVNutz, SportNutz, VidNutz, MusicNutz). Each card launches an inline sub-screen within the same composable. The Hub is accessible from the sidebar as a new `Screen.RobbdeezeNutzHub` route.

---

## Phase 1 — Screen & Route Setup

### 1.1 Add Screen route to `Screen.kt`

**File:** `app/src/main/java/com/nuvio/tv/ui/navigation/Screen.kt`

Add a new route entry alongside the existing ones:

```kotlin
data object RobbdeezeNutzHub : Screen("robbdeeze_nutz_hub")
```

Insert after `Screen.Home` (line 7), keeping alphabetical/logical order.

### 1.2 Add sidebar navigation item

**File:** `app/src/main/java/com/nuvio/tv/MainActivity.kt`

Locate the `SidebarItem` list (near line 1030+ in the `@Composable` section where sidebar items are defined). Add a new item:

```kotlin
SidebarItem(
    label = stringResource(R.string.sidebar_hubz),  // new string
    icon = Icons.Filled.Dashboard,                   // or Apps / Widgets / GridView
    route = Screen.RobbdeezeNutzHub.route,
    selected = currentRoute == Screen.RobbdeezeNutzHub.route,
    notifications = null
)
```

Insert this between the existing items (e.g., after Library, before Settings).

### 1.3 Add route handling in `NuvioNavHost.kt`

**File:** `app/src/main/java/com/nuvio/tv/ui/navigation/NuvioNavHost.kt`

Add a `composable` block for the new route:

```kotlin
composable(Screen.RobbdeezeNutzHub.route) {
    RobbdeezeNutzHubScreen(
        onPlayChannel = { /* navigate to Player with IPTV stream */ },
        onBackPress = { navController.popBackStack() }
    )
}
```

Import `RobbdeezeNutzHubScreen` from the new file created in Phase 2.

### 1.4 Add string resource

**File:** `app/src/main/res/values/strings.xml`

```xml
<string name="sidebar_hubz">Hubz</string>
```

---

## Phase 2 — Hub Screen Composable

### 2.1 Create `RobbdeezeNutzHubScreen.kt`

**New file:** `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt`

```kotlin
package com.nuvio.tv.ui.screens.hub

// Sub-screen enum
enum class HubSubScreen { Hub, Iptv, Sports, VidNutz, MusicNutz }

@Composable
fun RobbdeezeNutzHubScreen(
    onPlayChannel: (IptvChannel) -> Unit,
    onBackPress: () -> Unit
)
```

#### State

```kotlin
var subScreen by rememberSaveable { mutableStateOf(HubSubScreen.Hub) }
var hubResetCounter by rememberSaveable { mutableStateOf(0) }
```

- `subScreen` tracks which sub-screen is active
- `hubResetCounter` is incremented by a `LaunchedEffect` to reset to `Hub` when the sidebar tab is re-tapped (see Phase 4)

#### Layout

```
BoxWithConstraints (for tablet-aware top padding)
├── Persistent Title Bar (always visible)
│   ├── BackIconButton (only when subScreen != Hub)
│   └── Text("RobbdeezeNutz Hubz")
│
└── Content Area
    ├── if (subScreen == Hub) → 4 Glass Cards (Phase 2.2)
    ├── if (subScreen == Iptv) → IptvSubScreen (Phase 3.1)
    ├── if (subScreen == Sports) → SportsSubScreen (Phase 3.2)
    ├── if (subScreen == VidNutz) → VidNutzSubScreen (Phase 3.3)
    └── if (subScreen == MusicNutz) → MusicNutzSubScreen (Phase 3.4)
```

#### Tab Re-tap Behavior

The hub needs to expose a way for the sidebar tab click to reset the state. This can be done via a `hubResetCounter` flow or by checking if the current route is the hub and adding a `key` / `LaunchedEffect`:

```kotlin
// In the hub screen, observe a shared flow or a parameter
LaunchedEffect(hubResetCounter) {
    if (hubResetCounter > 0) {
        subScreen = HubSubScreen.Hub
    }
}
```

When the sidebar tab is tapped while already selected, the `MainActivity` increments `hubResetCounter` and sends `scrollToTopRequests` to the IPTV/Sports sub-screens.

### 2.2 Hub Card Composable

```kotlin
@Composable
fun HubCard(
    title: String,
    description: String,
    badge: String,        // "TV", "SP", "VN", "MU"
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Visual spec:**

| Property | Value |
|----------|-------|
| Background | `Color(0xFF1A1A1A)` (dark grey card) |
| Border radius | `12.dp` |
| Focus ring | `2.dp` white `Color.White` border on focus |
| Focus background brighten | Slightly lighter shade or overlay |
| Title | 20sp, light grey `#E0E0E0` |
| Description | 14sp, muted grey `#B0B0B0` |
| Icon badge | "TV" / "SP" / "VN" / "MU" in a rounded box with 8-15% alpha white bg |
| Spacing | 4 cards in a vertical scrollable column, 12dp spacing between cards |

---

## Phase 3 — Sub-Screens

Each sub-screen is rendered **inline** — same composable, no navigation stack. A `when` block switches on `subScreen`.

### 3.1 IPTVNutz Sub-Screen

**Inline within hub screen or separate `@Composable`:**

```kotlin
@Composable
fun IptvSubScreen(onPlayChannel: (IptvChannel) -> Unit)
```

- Wraps the existing IPTV browsing UI (to be ported)
- Compact header: `Text("IPTVNutz Hub", style = ...)`
- Pure black background, grayscale accent colors
- No EPG UI
- State preserved via `rememberSaveable` in the parent

### 3.2 SportNutz Sub-Screen

```kotlin
@Composable
fun SportsSubScreen(onPlayChannel: (IptvChannel) -> Unit)
```

- Wraps the Sports browsing UI (to be ported)
- Compact header: `Text("SportNutz Hub", style = ...)`
- No standalone Scaffold/TopAppBar — hub provides the header
- Sequential ESPN fetch (no parallel loading)
- `onTeamClick` callback for navigation

### 3.3 VidNutz Sub-Screen

```kotlin
@Composable
fun VidNutzSubScreen(onPlayVideo: (String) -> Unit)
```

- Full YouTube video browser
- 12 categories (horizontal chips)
- Persistent search bar with 400ms debounce
- 1-column video grid, "Load More" pagination
- Monochrome design: `#000000` bg, JetBrains Mono for metadata, 12dp thumbnail radius
- Video playback via YouTubeStreamResolver → ExoPlayer

### 3.4 MusicNutz Sub-Screen

```kotlin
@Composable
fun MusicNutzSubScreen(onPlayTrack: (MusicTrack) -> Unit)
```

- 12 genre categories
- Tracks / Albums toggle
- 2-column grid with square album art, 12dp radius
- Deezer public API (free, no key) for metadata
- YouTube full-length audio for playback
- Album detail view with tracklist

---

## Phase 4 — Sidebar Tab Re-tap Reset

**File:** `app/src/main/java/com/nuvio/tv/MainActivity.kt`

In the sidebar click handler, detect when the Hubz tab is tapped while already selected:

```kotlin
// Pseudo-code:
if (route == Screen.RobbdeezeNutzHub.route && currentRoute == Screen.RobbdeezeNutzHub.route) {
    hubResetCounter++
    // Also trigger scrollToTopRequests for IPTV/Sports sub-screens
}
```

This can be implemented by:

1. Adding a `hubResetCounter` StateFlow at the Activity level
2. Passing it to `RobbdeezeNutzHubScreen` as a parameter
3. In the sidebar item click, detect if the target == current route and if so, increment the counter

---

## Phase 5 — Data Layer (Dependencies)

This phase covers the repositories, API clients, and domain models that the 4 sub-screens depend on.

### 5.1 IPTV Data

Create `app/src/main/java/com/nuvio/tv/data/iptv/`:

| File | Contents |
|------|----------|
| `IptvModels.kt` | `IptvChannel`, `IptvCategory`, `IptvEpgEntry`, `IptvSource` data classes |
| `IptvRepository.kt` | Repository with channel list, favorites, history, search |
| `IptvStorage.kt` | DataStore-backed persistence for favorites/history |
| `M3uParser.kt` | M3U playlist parser |
| `XtreamClient.kt` | Xtream Codes API client |
| `StalkerClient.kt` | Stalker Portal API client |
| `EpgParser.kt` | XMLTV EPG parser |

### 5.2 Sports Data

Create `app/src/main/java/com/nuvio/tv/data/remote/api/`:

| File | Contents |
|------|----------|
| `EspnClient.kt` | ESPN API client (26 leagues) |
| `EspnNewsClient.kt` | ESPN news/headlines client |
| `SportsClient.kt` | Aggregated sports client |
| `TvMazeClient.kt` | TVMaze API for team lookups |
| `YouTubeHighlightClient.kt` | YouTube highlight search |

Create `app/src/main/java/com/nuvio/tv/data/remote/dto/`:

| File | Contents |
|------|----------|
| `EspnModels.kt` | ESPN API DTOs |
| `EspnNewsModels.kt` | ESPN news DTOs |

Create `app/src/main/java/com/nuvio/tv/data/sports/`:

| File | Contents |
|------|----------|
| `YouTubeStreamResolver.kt` | Resolves YouTube video to playable stream |

Create `app/src/main/java/com/nuvio/tv/domain/model/`:

| File | Contents |
|------|----------|
| `SportsModels.kt` | `SportEvent`, `SportLeague`, `SportTeam`, `SportScore` |

### 5.3 VidNutz Data

Create `app/src/main/java/com/nuvio/tv/data/youtube/`:

| File | Contents |
|------|----------|
| `PlatformYouTubeSearch.kt` | YouTube search (Invidious / Piped / NewPipe fallback) |

Create `app/src/main/java/com/nuvio/tv/data/repository/`:

| File | Contents |
|------|----------|
| `VidNutzRepository.kt` | Video repository with category browsing + search |

Create `app/src/main/java/com/nuvio/tv/domain/model/`:

| File | Contents |
|------|----------|
| `VidNutzModels.kt` | `VidNutzCategory`, `VidNutzVideo`, `VidNutzSearchResult` |

### 5.4 MusicNutz Data

Create `app/src/main/java/com/nuvio/tv/data/repository/`:

| File | Contents |
|------|----------|
| `MusicNutzRepository.kt` | Music repository (Deezer API + YouTube fallback) |

Create `app/src/main/java/com/nuvio/tv/domain/model/`:

| File | Contents |
|------|----------|
| `MusicNutzModels.kt` | `MusicGenre`, `MusicAlbum`, `MusicTrack`, `MusicArtist` |

### 5.5 Shared

Create `app/src/main/java/com/nuvio/tv/core/network/`:

| File | Contents |
|------|----------|
| `HttpClient.kt` | Shared OkHttp client with config |

### 5.6 DI Registration

**File:** `app/src/main/java/com/nuvio/tv/core/di/NetworkModule.kt`

Add Retrofit instances:
- `EspnApi` (base URL: `https://site.api.espn.com`)
- `EspnNewsApi` (base URL: `https://news.espn.com`)
- `TvMazeApi` (base URL: `https://api.tvmaze.com`)

Add repository providers for:
- `VidNutzRepository`
- `MusicNutzRepository`
- `IptvRepository`

**File:** `app/src/main/java/com/nuvio/tv/core/di/AppModule.kt`

- Provide `IptvStorage` (DataStore-backed)

---

## Phase 6 — Theme & Visual Design

### 6.1 Grayscale Palette (Optional)

If grayscale is desired for the hub screens, add a grayscale accent palette. The hub itself uses hardcoded `#000000` / `#1A1A1A` colors regardless of theme, so this step can be deferred.

### 6.2 Shared Hub Colors

Define hub colors in a central place (e.g., `NuvioTheme` or a `HubDefaults` object):

```kotlin
object HubDefaults {
    val background = Color(0xFF000000)
    val cardSurface = Color(0xFF1A1A1A)
    val cardTextPrimary = Color(0xFFE0E0E0)
    val cardTextSecondary = Color(0xFFB0B0B0)
    val focusRing = Color.White
}
```

---

## Phase 7 — Build & Verify

### 7.1 Gradle Dependencies

In `gradle/libs.versions.toml`, add any missing library versions (if not already present):

- `jsoup` (for HTML parsing in YouTube extraction)
- `newpipeextractor` (for YouTube extraction fallback)

In `app/build.gradle.kts`, add dependencies for:
- VidNutz: NewPipeExtractor, jsoup, ksoup
- MusicNutz: NewPipeExtractor
- Sports: Retrofit converters (if not already present)

### 7.2 Smoke Test

```bash
./gradlew :app:assembleDebug
```

Check:
- No compilation errors
- Hubz tab appears in sidebar
- Tapping Hubz shows 4 cards
- Each card launches the correct sub-screen
- Back button returns to card grid
- Tab re-tap resets to card grid

---

## File Manifest

### New Files
```
app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt
app/src/main/java/com/nuvio/tv/data/iptv/IptvModels.kt
app/src/main/java/com/nuvio/tv/data/iptv/IptvRepository.kt
app/src/main/java/com/nuvio/tv/data/iptv/IptvStorage.kt
app/src/main/java/com/nuvio/tv/data/iptv/M3uParser.kt
app/src/main/java/com/nuvio/tv/data/iptv/XtreamClient.kt
app/src/main/java/com/nuvio/tv/data/iptv/StalkerClient.kt
app/src/main/java/com/nuvio/tv/data/iptv/EpgParser.kt
app/src/main/java/com/nuvio/tv/data/remote/api/EspnClient.kt
app/src/main/java/com/nuvio/tv/data/remote/api/EspnNewsClient.kt
app/src/main/java/com/nuvio/tv/data/remote/api/SportsClient.kt
app/src/main/java/com/nuvio/tv/data/remote/api/TvMazeClient.kt
app/src/main/java/com/nuvio/tv/data/remote/api/YouTubeHighlightClient.kt
app/src/main/java/com/nuvio/tv/data/remote/dto/EspnModels.kt
app/src/main/java/com/nuvio/tv/data/remote/dto/EspnNewsModels.kt
app/src/main/java/com/nuvio/tv/data/sports/YouTubeStreamResolver.kt
app/src/main/java/com/nuvio/tv/data/youtube/PlatformYouTubeSearch.kt
app/src/main/java/com/nuvio/tv/data/repository/VidNutzRepository.kt
app/src/main/java/com/nuvio/tv/data/repository/MusicNutzRepository.kt
app/src/main/java/com/nuvio/tv/domain/model/IptvModels.kt
app/src/main/java/com/nuvio/tv/domain/model/SportsModels.kt
app/src/main/java/com/nuvio/tv/domain/model/VidNutzModels.kt
app/src/main/java/com/nuvio/tv/domain/model/MusicNutzModels.kt
app/src/main/java/com/nuvio/tv/core/network/HttpClient.kt
```

### Modified Files
```
app/src/main/java/com/nuvio/tv/ui/navigation/Screen.kt                  — add route
app/src/main/java/com/nuvio/tv/ui/navigation/NuvioNavHost.kt            — add composable
app/src/main/java/com/nuvio/tv/MainActivity.kt                          — add sidebar item, re-tap logic
app/src/main/java/com/nuvio/tv/core/di/NetworkModule.kt                 — add API clients + repos
app/src/main/res/values/strings.xml                                     — add string resources
gradle/libs.versions.toml                                               — add dependencies (if needed)
app/build.gradle.kts                                                    — add dependencies (if needed)
```

---

## Execution Order

1. **Phase 1** (Screen + Route + Sidebar) — 15 min
2. **Phase 5.5** (Shared HttpClient) — 5 min
3. **Phase 5.1–5.4** (Data Layer) — 2 hrs
4. **Phase 5.6** (DI Registration) — 15 min
5. **Phase 2** (Hub Screen + Cards) — 45 min
6. **Phase 3** (Sub-Screens) — 2 hrs
7. **Phase 4** (Tab Re-tap) — 15 min
8. **Phase 7** (Build & Verify) — 10 min

**Total estimated time:** ~4.5–5 hours
