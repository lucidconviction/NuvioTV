# SportNutz Enhancement Plan — League Tabs with Full Data Integration

## Current State
The TV project's `SportsSubScreen` (in `RobbdeezeNutzHubScreen.kt`) already has:
- League tabs: NFL, NBA, EPL, MLB, NHL, UFC, BKFC
- ESPN scoreboard data via `SportsClient`
- DaddyLive streaming events via `DaddyLiveClient`
- Sync2Cal TV channels via `Sync2CalClient`
- YouTube highlight videos via `VideoSuggestionEngine`
- Team detail view with standings

## Data Sources Available

| Source | Data | Status |
|--------|------|--------|
| ESPN (`SportsClient`) | Scoreboard, standings, team info, news | ✅ Working |
| TheSportsDB (`TheSportsDbClient`) | Team logos, venue info, league tables | ✅ Client exists |
| DaddyLive (`DaddyLiveClient`) | Live event streams | ✅ Working |
| Sync2Cal (`Sync2CalClient`) | TV channels airing games | ✅ Working |
| YouTube (`VideoSuggestionEngine`) | Highlights & analysis | ✅ Working |
| Wikipedia (`WikipediaClient`) | Team/league descriptions | ✅ Client exists |

## Enhancement Plan

### 1. League-Specific Data Panels
For each league tab, add a rich data panel with:

**Before game:**
- Upcoming matchups with win/loss records
- League standings table (current position, W/L, streak)
- Head-to-head history (from ESPN)
- Team stats (points/game, yards/game, etc.)

**During game:**
- Live scoreboard (already working)
- Key stats (possession, 3rd downs, etc. — from ESPN's competition details)
- Win probability chart
- Video highlights (already working)

**After game:**
- Final score with scoring summary
- Game stats (passing, rushing, etc.)
- Post-game analysis videos

### 2. Implementation Steps

#### Step 1: Enhance SportsRepository to fetch league-specific details
**File:** `domain/sports/SportsRepository.kt`
- Add methods: `getTeamStats(league, teamId)`, `getLeagueNews(league)`, `getInjuryReport(league, teamId)`
- Use existing ESPN API endpoints: `/scoreboard/{league}`, `/teams/{teamId}`, `/news/{league}`
- Add TheSportsDB integration for team logos and venue info

#### Step 2: Create enhanced league data models
**File:** `domain/model/SportsModels.kt`
- Add `LeagueDetail(standings, topTeams, recentGames, news)` data class
- Add `TeamDetail(stats, roster, injuries, schedule)` data class
- Add `GameDetail(scoringPlays, keyStats, winProbability)` data class

#### Step 3: Update SportsSubScreen layout
**File:** `ui/screens/hub/RobbdeezeNutzHubScreen.kt` (SportsSubScreen)
- For each league tab, create a multi-section layout:
  - **Row 1:** Standings table (scrollable horizontal)
  - **Row 2:** Upcoming games with team records
  - **Row 3:** Live/Recent scores with key stats
  - **Row 4:** League news & highlights
- Use `LazyColumn` with sticky headers for league tabs
- Add team detail drill-down with full stats

#### Step 4: Wire data into UI
- Connect `SportsRepository` methods to ViewModel state flows
- Add loading/error states for each data section
- Cache data with TTL (30s for live scores, 5min for standings, 1hr for team info)

### 3. Files to Modify

| File | Changes |
|------|---------|
| `domain/model/SportsModels.kt` | Add enriched data models |
| `domain/sports/SportsRepository.kt` | Add league-specific detail methods |
| `ui/screens/hub/RobbdeezeNutzHubViewModel.kt` | Add state flows for enhanced data |
| `ui/screens/hub/RobbdeezeNutzHubScreen.kt` | Redesign SportsSubScreen layout |

### 4. Priority Order
1. **Standings + team records** in each league tab (highest impact)
2. **Game stats** (possession, key plays) for live games
3. **League news + analysis videos** integration
4. **Team detail drill-down** with full stats

Ready to begin? Start with Step 1 (Standings + team records) which has the highest impact.
