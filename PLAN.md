# Nuvio TV Robbdeeze — Implementation Plan

## Phase 1 — Remove TeleNutz (7 deletes + 5 edits)

**Delete these files:**
- `app/src/main/java/com/robbdeeze/nuviotv/ui/screens/telenutz/TeleNutzScreen.kt`
- `app/src/main/java/com/robbdeeze/nuviotv/domain/model/TeleNutzModels.kt`
- `app/src/main/java/com/robbdeeze/nuviotv/data/local/TeleNutzStorage.kt`
- `app/src/main/java/com/robbdeeze/nuviotv/data/local/TeleNutzStore.kt`
- `app/src/main/java/com/robbdeeze/nuviotv/data/repository/TeleNutzRepository.kt`
- `app/src/main/java/com/robbdeeze/nuviotv/data/repository/TelegramTdEngine.kt`
- `app/src/main/java/com/robbdeeze/nuviotv/data/local/TelegramConfig.kt`
- Delete directory `tdlib-java/` entirely

**Edit `app/src/main/java/com/nuvio/tv/ui/navigation/Screen.kt`** — delete line 26:
```kotlin
data object TeleNutz : Screen("telenutz")
```

**Edit `app/src/main/java/com/nuvio/tv/ui/navigation/NuvioNavHost.kt`** — remove import at line 60 (`import com.robbdeeze.nuviotv.ui.screens.telenutz.TeleNutzScreen`) and remove the `composable(Screen.TeleNutz.route) { ... }` block at lines 1099–1111.

**Edit `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt`** — four removals:
1. Line 111: remove `onTeleNutzClick: (() -> Unit)? = null` parameter from `RobbdeezeNutzHubScreen(...)`
2. Line 202: remove `onTeleNutzClick = onTeleNutzClick` passed to `HubScreenContent`
3. Line 271: remove `onTeleNutzClick: (() -> Unit)? = null` parameter from `HubScreenContent(...)`
4. Lines 280–281: remove `if (onTeleNutzClick != null) { item { HubCard("TeleNutz", Color(0xFF0088CC), onClick = onTeleNutzClick) } }`

**Edit `app/src/main/java/com/nuvio/tv/MainActivity.kt`** — remove `onTeleNutzClick = { navController.navigate(Screen.TeleNutz.route) }` at lines 1145–1147.

**Edit `settings.gradle.kts`** — remove `include(":tdlib-java")` line.

---

## Phase 2 — Redesign Hub Chips (Liquid Glass Style)

**Edit `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt`** — replace the `HubCard` composable at lines 292–386 with a redesigned version:
- Height: 120dp (was 180dp), width: fixed 140dp (was fillMaxWidth)
- Container color: `Color.White.copy(alpha = 0.06f)` instead of `0xFF1A1A1A`
- Border: 1dp at `Color.White.copy(alpha = 0.12f)` unfocused, white at 0.5f focused
- Focus animation: `spring(dampingRatio = 0.8f)` (was `tween(180)`), scale to 1.06x
- Top gradient overlay: `Brush.verticalGradient(listOf(badgeColor.copy(alpha = 0.12f), Color.Transparent))`
- Label color: use `NuvioTheme.colors.TextPrimary` instead of badgeColor; badgeColor only as a small 6dp dot indicator below the label

Replace `HubScreenContent` at lines 269–289:
- Gap: `16.dp` (was `24.dp`)
- Each chip: `.width(140.dp)` instead of `fillMaxWidth()`
- Update bottom label row colors to use theme colors instead of hardcoded `0xFF888888` and `0xFF555555`

---

## Phase 3 — New "Liquid Glass" Theme (Make Default)

**Edit `app/src/main/java/com/nuvio/tv/domain/model/AppTheme.kt`** — add before `WHITE`:
```kotlin
LIQUID_GLASS("Liquid Glass"),
```

**Edit `app/src/main/java/com/nuvio/tv/ui/theme/ThemeColors.kt`** — add `val LiquidGlass` palette and branch in `getColorPalette()`:
```kotlin
val LiquidGlass = ThemeColorPalette(
    secondary = NuvioPrimitives.violet400,
    secondaryVariant = NuvioPrimitives.violet600,
    focusRing = NuvioPrimitives.white,
    focusBackground = Color(0xFF1A1025),
    background = Color(0xFF0A0A0F),
    backgroundElevated = Color(0xFF12121C),
    backgroundCard = Color(0xFF1A1A2A),
    surface = Color(0xFF14141E),
    surfaceVariant = Color(0xFF1C1C2C),
    panel = Color(0xFF12121C),
    field = Color(0xFF1E1E30),
    menu = Color(0xFF161622),
    modal = Color(0xFF12121C),
    overlay = Color(0xCC0A0A14),
    playerOverlay = Color(0xAA0A0A14)
)
// In getColorPalette(): AppTheme.LIQUID_GLASS -> LiquidGlass
```

**Edit `app/src/main/java/com/nuvio/tv/ui/theme/Color.kt`** — update glass panel colors inside `NuvioMediaColors`:
```kotlin
glassPanelTop = Color(0x1AFFFFFF),
glassPanelMiddle = Color(0x15FFFFFF),
glassPanelBottom = Color(0x10FFFFFF),
```

**Edit all 6 default-theme references** (change `AppTheme.WHITE` → `AppTheme.LIQUID_GLASS`):
- `app/src/main/java/com/nuvio/tv/ui/theme/Theme.kt` line 43 (CompositionLocal fallback)
- `app/src/main/java/com/nuvio/tv/ui/theme/Theme.kt` line 50 (NuvioTheme parameter default)
- `app/src/main/java/com/nuvio/tv/data/local/ThemeDataStore.kt` lines 36 and 40 (DataStore defaults)
- `app/src/main/java/com/nuvio/tv/ui/screens/settings/ThemeSettingsViewModel.kt` line 20 (UiState defaults)
- `app/src/main/java/com/nuvio/tv/MainActivity.kt` line 184 (MainUiPrefs data class default)

**Edit `app/src/main/java/com/nuvio/tv/ui/screens/settings/ThemeSettingsScreen.kt`** — add to `localizedName()` when-block:
```kotlin
AppTheme.LIQUID_GLASS -> stringResource(R.string.theme_color_liquid_glass)
```

**Edit `app/src/main/res/values/strings.xml`** — add:
```xml
<string name="theme_color_liquid_glass">Liquid Glass</string>
```

---

## Phase 4 — External Streams Section (Below Upcoming Schedule)

**Create `app/src/main/java/com/nuvio/tv/data/remote/api/PpvStClient.kt`** — `GET https://ppv.st/api`, returns list of streams with `title`, `sport`, `streamUrl`, `quality`, `logo`.

**Create `app/src/main/java/com/nuvio/tv/data/remote/api/StreamedPkClient.kt`** — `GET https://streamed.pk/api/matches/all`, returns matches list; each match has `sources[]` with `source` and `id`; secondary call `GET https://streamed.pk/api/stream/{source}/{id}` resolves stream URLs.

**Create `app/src/main/java/com/nuvio/tv/data/remote/api/StreamsSports99Client.kt`** — `GET https://streamsports99.ru/api`, returns sports events with stream availability.

**Create `app/src/main/java/com/nuvio/tv/data/remote/api/WatchFootyClient.kt`** — `GET https://watchfooty.st/api/v1/matches/all?date={yyyy-MM-dd}` and `/api/v1/matches/all/live`; returns `Match(id, homeTeam, awayTeam, sport, startTime)`.

**Create `app/src/main/java/com/nuvio/tv/data/stream/ExternalStreamSource.kt`** — data class:
```kotlin
data class ExternalStreamSource(val name: String, val url: String, val sport: String, val logo: String? = null, val quality: String = "HD")
```

**Create `app/src/main/java/com/nuvio/tv/data/repository/ExternalStreamsRepository.kt`** — singleton `object` that calls all 4 clients in parallel via `coroutineScope { async { ... } }.awaitAll()`, normalizes results into `List<ExternalStreamSource>`, deduplicates by URL.

**Create `app/src/main/java/com/nuvio/tv/ui/screens/hub/ExternalStreamsSection.kt`** — `@Composable fun ExternalStreamsSection(sources: List<ExternalStreamSource>, isLoading: Boolean, onRefresh: () -> Unit, onStreamClick: ((String) -> Unit)? = null)`:
- Header row: "External Streams" + count badge + refresh card (matches Sync2CalSection style)
- 2-column `LazyVerticalGrid` of glass-style cards showing source name, sport tag, quality badge
- Card tap calls `onStreamClick(url)` to launch playback

**Edit `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubViewModel.kt`** — add three new state entries and one loading method:
```kotlin
private val _externalStreams = MutableStateFlow<List<ExternalStreamSource>>(emptyList())
val externalStreams: StateFlow<List<ExternalStreamSource>> = _externalStreams.asStateFlow()
private val _externalStreamsLoading = MutableStateFlow(false)
val externalStreamsLoading: StateFlow<Boolean> = _externalStreamsLoading.asStateFlow()

fun loadExternalStreams() { /* calls ExternalStreamsRepository in viewModelScope */ }
```
Call `loadExternalStreams()` during initial sports data load (near `loadSync2CalEvents()` call site).

**Edit `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt`** — in `SportsSubScreen`, inside the "now" league `Column` at line 1952, after the `Sync2CalUpcomingSection` call (lines 2122–2128), insert:
```kotlin
ExternalStreamsSection(
    sources = externalStreams,
    isLoading = externalStreamsLoading,
    onRefresh = { viewModel.loadExternalStreams() },
    onStreamClick = { url -> onPlayChannel(url) }
)
```

---

## Phase 5 — Paywall for Portals Feature (Port from Mobile Version)

**Create `app/src/main/java/com/nuvio/tv/data/iptv/PaywallModels.kt`** — copy verbatim from mobile:
- `PortalLicenseKey(id, exp, maxDev, isAdmin)`
- `LicenseStatus` enum (VALID, EXPIRED, GRACE, WRONG_DEVICE, INVALID, NOT_ACTIVATED)
- `LicenseResult` sealed class (Success/Failure)

**Create `app/src/main/java/com/nuvio/tv/data/iptv/PaywallStorage.kt`** — Android `object` persisting license, fingerprint, and installed portals via `SharedPreferences(namespace="nuvio_iptv")` with keys `portal_license`, `portal_fingerprint`, `portal_installed`.

**Create `app/src/main/java/com/nuvio/tv/data/iptv/DeviceFingerprint.kt`** — Android `object` that generates and stores a random UUID in `SharedPreferences(namespace="nuvio_device")` key `"fingerprint"`.

**Create `app/src/main/java/com/nuvio/tv/data/iptv/HmacCrypto.kt`** — `hmacSha256(data, secret)` using `javax.crypto.Mac` ("HmacSHA256"), `base64UrlDecode(input)` using `android.util.Base64.decode`.

**Create `app/src/main/java/com/nuvio/tv/data/iptv/PaywallManager.kt`** — port `PortalLicenseManager` verbatim from mobile. Key behavior: `verifyKey()` strips `NVIO-` prefix, base64url-decodes, splits on `.`, verifies HMAC-SHA256 against secret `"Rdnutz"`, parses JSON payload. `canAddPortal()` enforces `MAX_PORTALS = 3` for non-admin users.

**Edit `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt`** — inside `IptvSubScreen`, within the `PortalNutz` collapsible card (starts line 722), after `var collapsed by remember { mutableStateOf(true) }`, add:

```kotlin
val license = PaywallManager.getSavedLicense()
val licenseStatus = PaywallManager.checkStatus(license)
val hasAccess = licenseStatus == LicenseStatus.VALID || licenseStatus == LicenseStatus.GRACE
```

If `!hasAccess`, render the locked paywall UI **before** the filter chips:
- Lock icon + "RD NUTZ LISTS LOCKED" header
- Status message per `licenseStatus` value
- Key input field (placeholder `NVIO-XXXX-XXXX-XXXX`)
- "ACTIVATE KEY" button calling `PaywallManager.verifyKey(input)` → `saveActivation()`
- On success: brief "Key activated!" confirmation

If `hasAccess`, render a small "RdNutz Active" status banner above the filter chips (showing remaining time).

At the "Add" button tap (around line 846), add portal count guard:
```kotlin
if (!PaywallManager.canAddPortal(license, currentPortalCount)) {
    toastMessage = "Portal limit reached (3 max for free users)"
} else {
    viewModel.addIptvSource(...)
}
```

---

## Summary

| Phase | New Files | Deleted Files | Edited Files |
|-------|-----------|---------------|--------------|
| 1 — Remove TeleNutz | — | 7 + `tdlib-java/` dir | `Screen.kt`, `NuvioNavHost.kt`, `RobbdeezeNutzHubScreen.kt`, `MainActivity.kt`, `settings.gradle.kts` |
| 2 — Redesign Chips | — | — | `RobbdeezeNutzHubScreen.kt` (HubCard + HubScreenContent) |
| 3 — Liquid Glass Theme | — | — | `AppTheme.kt`, `ThemeColors.kt`, `Color.kt`, `Theme.kt`, `ThemeDataStore.kt`, `ThemeSettingsViewModel.kt`, `ThemeSettingsScreen.kt`, `strings.xml` |
| 4 — External Streams | 7 files | — | `RobbdeezeNutzHubViewModel.kt`, `RobbdeezeNutzHubScreen.kt` |
| 5 — Paywall | 5 files | — | `RobbdeezeNutzHubScreen.kt` (PortalNutz card) |
