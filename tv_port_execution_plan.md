# TV Port Execution Plan — TeleNutz, Quick Channels, Stream Validation, Buffering

> **Target:** `/Users/robbdeeze/Documents/projects/Nuvio TV Robbdeeze`  
> **Source:** `/Users/robbdeeze/Documents/projects/Nuvio_Robbdeeze` (mobile)

## Overview
Port selected updates from mobile to TV. Skip DVR entirely. Focus on TeleNutz, quick channels, stream validation, buffering, and home layout.

---

## Task 1 — Add TeleNutz (Telegram Videos) to TV

TeleNutz lets users browse and play videos from Telegram channels/groups via TDLib.

### 1a. Check existing Telegram/TDLib infrastructure
- Does the TV project already have `TelegramTdEngine` or any TDLib integration?
- Search for `tdlib`, `Telegram`, `TeleNutz` in the TV codebase

### 1b. Create TeleNutz screen
- `ui/screens/telenutz/TeleNutzScreen.kt` — port from mobile, adapt to TV layout
- `ui/screens/telenutz/TeleNutzViewModel.kt` — port ViewModel logic

### 1c. Create TeleNutz route
- `ui/navigation/Screen.kt` — add TeleNutz route
- `ui/navigation/NuvioNavHost.kt` — add composable entry

### 1d. Add TeleNutz hub card
- `ui/screens/hub/RobbdeezeNutzHubScreen.kt` — replace MagNutz with TeleNutz

---

## Task 2 — Remove MagNutz Hub
- `ui/screens/hub/RobbdeezeNutzHubScreen.kt` — remove MagNutz from hub items and sub-screen handling

---

## Task 3 — Add Bay Area & PPV Quick Channel Buttons

### 3a. Add new tabs to Quick Channels
- Find quick channels data file (search `QuickChannel`, `QC_TABS` in TV codebase)
- Add "Bay Area" tab with local channels
- Add "PPV" tab (Sky Sports BO, BT Sport BO, UFC PPV, WWE PPV, Boxing PPV, DAZN PPV, PPV 1-5)

### 3b. Wire into Home Screen
- `ui/screens/home/HomeIptvRowSections.kt` — ensure new tabs render

### 3c. Wire into IPTV Control Center
- `ui/screens/player/IptvPlayerScreen.kt` — ensure new tabs render

---

## Task 4 — Wire Stream Validation into IPTV

### 4a. StreamValidator & StreamValidationStore — ALREADY EXIST
- `data/local/StreamValidator.kt`
- `data/local/StreamValidationStore.kt`

### 4b. Add Validate + Hide dead to IPTV channel list
- `ui/screens/player/IptvPlayerScreen.kt` — add validate action, hide dead toggle

---

## Task 5 — Increase Player Buffering for IPTV

### 5a. Adjust ExoPlayer load control
- `core/player/BitrateAwareLoadControl.kt` — IPTV: 100MB/30s/120s/10s, Live: 32MB/5s/30s/3s

### 5b. Add HLS disk cache
- Player init file — wrap data source with `CacheDataSource.Factory` + `SimpleCache` (200MB)

---

## Task 6 — IPTV History & Quick Channels as Home Screen Rows 1 & 2
- `ui/screens/home/HomeIptvRowSections.kt` — pin as rows 1 & 2 after hero
- Apply across all 3 layouts: Classic, Grid, Modern

---

## Summary of Files

### Files to Create
| # | File | Purpose |
|---|------|---------|
| 1 | `ui/screens/telenutz/TeleNutzScreen.kt` | Telegram video browser |
| 2 | `ui/screens/telenutz/TeleNutzViewModel.kt` | TeleNutz state management |

### Files to Modify
| # | File | Change |
|---|------|--------|
| 1 | `ui/navigation/Screen.kt` | Add TeleNutz route |
| 2 | `ui/navigation/NuvioNavHost.kt` | Add TeleNutz composable |
| 3 | `ui/screens/hub/RobbdeezeNutzHubScreen.kt` | Add TeleNutz, remove MagNutz |
| 4 | Quick channels data file (TBD) | Add Bay Area + PPV tabs |
| 5 | `ui/screens/home/HomeIptvRowSections.kt` | Wire new tabs, pin as rows 1 & 2 |
| 6 | `ui/screens/player/IptvPlayerScreen.kt` | Wire new tabs + validate + hide dead |
| 7 | `core/player/BitrateAwareLoadControl.kt` | Increase IPTV buffer |
| 8 | Player init file (TBD) | Add SimpleCache for HLS |
| 9 | `MainActivity.kt` / `NuvioApplication.kt` | Any TeleNutz init if needed |
