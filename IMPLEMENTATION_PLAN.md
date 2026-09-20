# Nuvio TV Robbdeeze Implementation Plan

## Overview
This document tracks the implementation of 9 major features for the Nuvio TV Robbdeeze Android TV app.

---

## 1. QR Code Scan for Portal Key Entry

**Goal:** Allow users to scan a QR code containing their `NVIO-...` portal key instead of typing it manually.

**QR Code Generation:** Uses existing in-app `QrCodeGenerator.kt` — no external dependencies.
- **Scan Mode:** TV app scans QR from phone camera (user has key on phone)
- **Generate Mode:** App generates QR from entered key for phone sharing

**Files:**
- `app/src/main/AndroidManifest.xml` — add `CAMERA` permission
- `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt` (lines 869-883)

**Implementation:**
```kotlin
// In PortalNutz paywall UI
val cameraLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val scanned = result.data?.getStringExtra("SCAN_RESULT")
        scanned?.let { keyInput = it.trim(); keyError = null }
    }
}

// Scan button next to text field
TextButton(onClick = {
    cameraLauncher.launch(
        Intent("com.google.zxing.client.android.SCAN").apply {
            putExtra("SCAN_MODE", "QR_CODE_MODE")
        }
    )
}) {
    Text("Scan QR", color = Color(0xFF00FF00))
}
```

**Note:** QR code generation for sharing uses existing `QrCodeGenerator.generate(content)` — produces Bitmap from string.

---

## 2. PortalNutz Caching

**Goal:** Cache verified portals for 30 minutes so subsequent searches are instant.

**File:** `app/src/main/java/com/nuvio/tv/data/portalnutz/PortalNutzScraper.kt`

**Changes:**
```kotlin
private val cache = MutableStateFlow<CacheEntry?>(null)

data class CacheEntry(
    val portals: List<PortalNutzEntry>,
    val filters: FilterState,
    val fetchedAt: Long
)

suspend fun scrape(
    filters: FilterState,
    forceRefresh: Boolean = false,
    existingSourceNames: Set<String> = emptySet()
): List<PortalNutzEntry> {
    val now = System.currentTimeMillis()
    val cached = cache.value
    if (!forceRefresh && cached != null && now - cached.fetchedAt < CACHE_TTL_MS && cached.filters == filters) {
        return cached.portals
    }
    // ... existing scrape logic ...
    cache.value = CacheEntry(results, filters, now)
    return results
}

companion object {
    const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 min
}
```

**UI Addition:** Add "Refresh" button in `RobbdeezeNutzHubScreen.kt` calling `scrape(filters, forceRefresh = true)`

---

## 3. Channel Count in Portal Results

**Goal:** Show channel count and domain for each found portal.

**File:** `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt` (lines 985-1009)

**Change:**
```kotlin
Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
        Text("Portal ${entry.label.filter { it.isDigit() }}", 
             color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("${entry.channelCount} channels · ${entry.domain}", 
             color = Color(0xFF888888), fontSize = 11.sp)
    }
    Text(if (added) "Added" else "Add", ...)
}
```

---

## 4. EPG Integration

### Phase 4.1: Data Model
**File:** `app/src/main/java/com/nuvio/tv/domain/model/IptvModels.kt`

```kotlin
data class IptvSource(
    val name: String,
    val url: String,
    val type: String,
    val epgUrl: String? = null  // NEW
)
```

### Phase 4.2: Add Source Form
**File:** `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt` (Add Source form, ~line 1015)

Add EPG URL field for M3U type:
```kotlin
if (newSourceType == "m3u") {
    OutlinedTextField(
        value = newSourceEpgUrl,
        onValueChange = { newSourceEpgUrl = it },
        label = { Text("EPG URL (XMLTV)") },
        placeholder = { Text("Optional: URL to XMLTV guide") },
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Phase 4.3: M3U Parser
**File:** `app/src/main/java/com/nuvio/tv/data/iptv/M3uParser.kt`

Add `tvg-epg` extraction:
```kotlin
return IptvChannel(
    // ... existing fields ...
    epgUrl = parseAttribute(extInf, "tvg-epg")  // NEW
)
```

### Phase 4.4: Xtream EPG Client
**File:** `app/src/main/java/com/nuvio/tv/data/iptv/XtreamClient.kt`

```kotlin
suspend fun getEpgForChannel(streamId: String): Pair<String?, String?>? {
    val resp = client.get("$baseUrl/player_api.php?username=$user&password=$pass&action=get_simple_data_table&stream_id=$streamId&stream_type=live")
    // Parse nowplaying.title, nowplaying.start, nowplaying.stop
    // Returns (currentProgramTitle, nextProgramTitle) or null
}
```

### Phase 4.5: Stalker EPG Client
**File:** `app/src/main/java/com/nuvio/tv/data/iptv/StalkerClient.kt`

```kotlin
suspend fun getEpg(mac: String, token: String): Map<String, List<IptvEpgEntry>> {
    val resp = client.get("$baseUrl/stalker_portal/api/v1/epg?mac=$mac&token=$token&type=all")
    // Parse XMLTV response
}
```

### Phase 4.6: Repository Implementation
**File:** `app/src/main/java/com/nuvio/tv/data/iptv/IptvRepositoryImpl.kt` (lines 110-112)

Replace stub with actual implementation delegating to XtreamClient/StalkerClient/M3uParser.

### Phase 4.7: Player ViewModel
**File:** `app/src/main/java/com/nuvio/tv/ui/screens/player/IptvPlayerScreen.kt` (`IptvPlayerUiState`, ~line 121)

Add:
```kotlin
val currentEpg: IptvEpgEntry? = null,
val nextEpg: IptvEpgEntry? = null,
val epgMap: Map<String, List<IptvEpgEntry>> = emptyMap()
```

### Phase 4.8: UI Integration
**File:** `app/src/main/java/com/nuvio/tv/ui/screens/player/IptvPlayerScreen.kt`

- Channel Info Bar (lines 622-641): Show current/next program
- Channel Switcher (lines 999-1161): Show program title per channel

---

## 5. Quick Channel Stream Validation

**Goal:** Validate streams when clicking a Quick Channel, show green dot on working streams, hide dead ones.

### Phase 5.1: Stream Validator
**New File:** `app/src/main/java/com/nuvio/tv/data/iptv/StreamValidator.kt`

```kotlin
class StreamValidator(private val httpClient: OkHttpClient = HttpClient.client) {
    companion object {
        const val VALIDATION_TIMEOUT_MS = 8000L
        const val MAX_CONCURRENT = 5
    }
    
    suspend fun validateStream(url: String): Boolean
    suspend fun validateChannels(channels: List<IptvChannel>, onProgress: (Int, Int) -> Unit): List<IptvChannel>
}
```

### Phase 5.2: HomeViewModel
**File:** `app/src/main/java/com/nuvio/tv/ui/screens/home/HomeViewModel.kt` (lines 276-293)

Add validation state and `validateQuickChannel()` method.

### Phase 5.3: UI Updates
**Files:**
- `app/src/main/java/com/nuvio/tv/ui/screens/home/HomeIptvRowSections.kt` — green dot, progress text
- `app/src/main/java/com/nuvio/tv/ui/screens/home/HomeScreen.kt` — validated channels only

---

## 6. Quick Channel Crash Fix

**Goal:** Fix OOM crash when loading large region sub-channels like "US Channels".

**File:** `app/src/main/java/com/nuvio/tv/ui/screens/home/HomeViewModel.kt` (lines 276-293)

```kotlin
private const val MAX_QUICK_CHANNEL_MATCHES = 200
```

Add early termination in matching loop and `.take(200)` safety net.

**Also fix:** `RobbdeezeNutzHubViewModel.kt` (lines 947-983)

---

## 7. IPTV Thumbnails 25% Bigger

**Goal:** Enlarge IPTV channel thumbnails on home screen by 25%.

| Composable | Current | 25% Bigger | File | Line |
|---|---|---|---|---|
| `IptvMiniCard` | 140×90 dp | 175×113 dp | `HomeIptvRowSections.kt` | 173 |
| `QuickChannelCard` | 150×70 dp | 188×88 dp | `HomeIptvRowSections.kt` | 218 |

---

## 8. Remove "Portal" from PortalNutz UI

### 8.1 Scraper Label Format
**File:** `app/src/main/java/com/nuvio/tv/data/portalnutz/PortalNutzScraper.kt` (line 408)
```kotlin
// portal$portalNum → P$portalNum
```

### 8.2 UI Text Changes
**File:** `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt`

| Line | Current | New |
|------|---------|-----|
| 782 | "PortalNutz" | "RdNutz" |
| 972 | "Search Portals" | "Search" |
| 990 | "Portal ${entry.label.filter...}" | `entry.label` |

### 8.3 Paywall Text
| Current | New |
|---------|-----|
| "RD NUTZ LISTS LOCKED" | "RD NUTZ SOURCES LOCKED" |
| "RdNutz Lists Active" | "RdNutz Sources Active" |
| "You now have access to RdNutz Lists" | "You now have access to RdNutz Sources" |

### 8.4 Duplicate P-Number Prevention
Pass `existingSourceNames` to `scrape()` to skip already-used P-numbers.

### 8.5 Source Card Detection
**File:** `app/src/main/java/com/nuvio/tv/ui/screens/hub/RobbdeezeNutzHubScreen.kt` (lines 680-694)
```kotlin
// portal prefix → P prefix detection
```

### 8.6 License Manager Detection
**File:** `app/src/main/java/com/nuvio/tv/data/local/PortalLicenseManager.kt` (line 138)
```kotlin
// startsWith("portal") → startsWith("p") && any digit
```

---

## 9. Remove Closed Captions by Default

**Goal:** Disable subtitles/captions on all streams by default.

**File:** `app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt` (line 135)

```kotlin
// val preferredLanguage: String = "en" → "none"
```

**Why it works:** `PlayerRuntimeControllerTracks.kt:1882` handles `"none"` via `setTrackTypeDisabled(TRACK_TYPE_TEXT, true)`.

---

## Implementation Order

| # | Feature | Dependencies | Risk |
|---|---------|-------------|------|
| 1 | Remove Closed Captions | None | Low |
| 2 | QR Code Scan | None | Low |
| 3 | PortalNutz Caching | None | Low |
| 4 | Channel Count Display | None | Low |
| 5 | Remove "Portal" UI | None | Low |
| 6 | Quick Channel Crash Fix | None | Low |
| 7 | Quick Channel Validation | Crash fix | Medium |
| 8 | IPTV Thumbnails 25% | None | Low |
| 9 | EPG Integration | Phases 4.1-4.8 | High |

---

## Files Summary

| File | Changes |
|------|---------|
| `AndroidManifest.xml` | Add CAMERA permission |
| `PortalNutzScraper.kt` | Label format, caching, existingSourceNames param |
| `RobbdeezeNutzHubScreen.kt` | UI text, paywall text, source detection, pass existing names, EPG URL field |
| `PortalLicenseManager.kt` | Source name detection pattern |
| `HomeViewModel.kt` | Quick channel crash fix, validation state |
| `HomeIptvRowSections.kt` | Green dot, progress text, thumbnail sizes |
| `HomeScreen.kt` | Validation popup, take(200) safety net |
| `IptvModels.kt` | Add epgUrl to IptvSource |
| `M3uParser.kt` | Extract tvg-epg attribute |
| `XtreamClient.kt` | Add getEpgForChannel() |
| `StalkerClient.kt` | Add getEpg() |
| `IptvRepositoryImpl.kt` | Implement getEpg() |
| `IptvPlayerScreen.kt` | Add EPG fields to UiState, UI integration |
| `PlayerSettingsDataStore.kt` | Change subtitle default to "none" |
| `RobbdeezeNutzHubViewModel.kt` | Quick channel crash fix |
| `StreamValidator.kt` | NEW file |
