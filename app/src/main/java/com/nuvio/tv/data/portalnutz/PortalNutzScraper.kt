package com.robbdeeze.nuviotv.data.portalnutz

import com.robbdeeze.nuviotv.domain.model.IptvSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min

data class PortalNutzEntry(
    val label: String,
    val url: String,
    val username: String,
    val password: String,
    val channelCount: Int,
    val domain: String,
)

object PortalNutzScraper {

    private var currentJob: kotlinx.coroutines.Job? = null

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val UA = "Mozilla/5.0 (Linux; Android 11; NuvioTV) AppleWebKit/537.36"

    private data class Portal(val url: String, val username: String, val password: String, val source: String)
    private data class VerifiedPortal(val portal: Portal, val name: String, val domain: String)

    private val ADULT_TERMS = setOf(
        "xxx", "adult", "porn", "sex", "erotic", "18+", "onlyfans", "cam", "nude",
        "playboy", "penthouse", "hustler", "brazzers", "bangbros",
        "pornhub", "xvideos", "xhamster", "xnxx", "redtube",
        "milf", "ebony", "lesbian", "gay", "shemale", "tranny",
        "sexo", "porno", "adultos", "fetish", "bdsm", "hardcore",
    )

    private val SPORTS_KEYWORDS = setOf(
        "espn", "nfl", "nba", "mlb", "nhl", "ufc", "dazn", "bein sport",
        "premier league", "laliga", "serie a", "bundesliga", "ligue 1",
        " champions ", "europa league", "world cup", "fifa", "ncaa",
        "nascar", "formula 1", "f1", "motogp", "wwe", "aew",
        "tennis", "us open", "wimbledon", "australian open",
        "nfl network", "nba tv", "mlb network", "nhl network",
        "golf", "pga", "masters", "cricket", "ipl",
        "boxing", "mma", "one championship", "wrc", "world rally",
        "olympics", "super bowl", "stanley cup", "world series",
        "ncaa football", "ncaa basketball", "march madness",
        "sports", "sport", "deporte", "deportes",
    )

    sealed class ScrapeEvent {
        data class Progress(val message: String) : ScrapeEvent()
        data class Result(val portals: List<PortalNutzEntry>) : ScrapeEvent()
        data class Error(val message: String) : ScrapeEvent()
    }

    private fun cleanPortalUrl(raw: String): String {
        var clean = raw.replace("\\s+".toRegex(), "")
        val qIdx = clean.indexOf('?')
        if (qIdx >= 0) clean = clean.substring(0, qIdx)
        clean = clean.replace(Regex("/+(?:get|live|portal|c|index|playlist|player_api|xmltv|index\\.php|portal\\.php)\\.php\$", RegexOption.IGNORE_CASE), "")
        while (clean.endsWith('/')) clean = clean.substring(0, clean.length - 1)
        if (!clean.startsWith("http")) clean = "http://$clean"
        return clean
    }

    private fun extractDomain(url: String): String {
        return try {
            val u = java.net.URI(url)
            u.host + if (u.port > 0) ":${u.port}" else ""
        } catch (_: Exception) {
            url.replace("https?://".toRegex(), "").split("/")[0]
        }
    }

    private fun extractPortals(text: String, source: String): List<Portal> {
        if (text.length < 15) return emptyList()
        val cleaned = text
            .replace("&amp;".toRegex(), "&")
            .replace("&quot;".toRegex(), "\"")
            .replace(Regex("<(?:p|br|div|li|h\\d)[^>]*>"), "\n")
            .replace(Regex("<[^>]+>"), "")

        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        val urlParamRegex = Regex("""(https?://[^?\s"'<]+)\?(?:[^\s"'<]*?&)?(?:username|user)=([^&\s"'<]+)\s*&(?:password|pass)=([^&\s"'<]+)""", RegexOption.IGNORE_CASE)
        for (m in urlParamRegex.findAll(cleaned)) {
            val url = cleanPortalUrl(m.groupValues[1])
            val user = m.groupValues[2].trim()
            val pass = m.groupValues[3].trim()
            if (url.isNotEmpty() && user.length >= 3 && pass.length >= 3 && !user.contains("http") && !pass.contains("http")) {
                val key = "$url|$user|$pass"
                if (seen.add(key)) portals.add(Portal(url, user, pass, source))
            }
        }

        val labelRegex = Regex("""(?:Portal|Host(?:\s*URL)?|Panel|Real|URL|🔗|Url)\W*?(https?://[^<\s"']+)[\s\S]{1,500}?(?:Username|User|Usu[áa]rio|Usuario|👤|Identifiant)\W*?([^\s|<"'\n]+)[\s\S]{1,200}?(?:Password|Pass|Senha|Contrase[ñn]a|🔑|Mot\s*de\s*[Pp]asse)\W*?([^\s|<"'\n]+)""", RegexOption.IGNORE_CASE)
        for (m in labelRegex.findAll(cleaned)) {
            val url = cleanPortalUrl(m.groupValues[1])
            val user = m.groupValues[2].trim()
            val pass = m.groupValues[3].trim()
            if (url.isNotEmpty() && user.length >= 3 && pass.length >= 3 && !user.contains("http") && !pass.contains("http")) {
                val key = "$url|$user|$pass"
                if (seen.add(key)) portals.add(Portal(url, user, pass, source))
            }
        }

        return portals
    }

    private fun isAdultText(text: String): Boolean {
        val lower = text.lowercase()
        return ADULT_TERMS.any { lower.contains(it) }
    }

    private fun hasNonLatinScript(text: String): Boolean {
        return Regex("""[\u0400-\u04FF\u0500-\u052F]""").containsMatchIn(text) ||
            Regex("""[\u0600-\u06FF]""").containsMatchIn(text) ||
            Regex("""[\u4E00-\u9FFF]""").containsMatchIn(text) ||
            Regex("""[\u3040-\u30FF]""").containsMatchIn(text)
    }

    private suspend fun fetchText(url: String, timeoutMs: Long = 8000): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (_: Exception) { null }
    }

    private suspend fun fetchGitHubPortals(onEvent: (ScrapeEvent) -> Unit): List<Portal> {
        onEvent(ScrapeEvent.Progress("Shaking the tree for ripe nutz..."))
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        val repos = listOf(
            Triple("akeotaseo", "world_repo", "Updater_Matrix/XML2"),
            Triple("Armiiin", "world_repo", "Updater_Matrix/XML2"),
            Triple("rochana-sadila", "Xtream-Codes-Library", ""),
        )

        for ((owner, repo, path) in repos) {
            try {
                val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=main"
                val json = fetchText(apiUrl) ?: continue
                val arr = JSONArray(json)
                val files = mutableListOf<Pair<String, String>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("type") == "file") {
                        val name = obj.optString("name", "")
                        val downloadUrl = obj.optString("download_url", "")
                        if (name.endsWith(".txt") || name.endsWith(".json")) {
                            files.add(name to downloadUrl)
                        }
                    }
                }
                files.sortBy { it.first }
                for ((name, dlUrl) in files.take(10)) {
                    val text = fetchText(dlUrl) ?: continue
                    if (name.endsWith(".json")) {
                        try {
                            val jsonArr = if (text.trim().startsWith("[")) JSONArray(text) else JSONArray(org.json.JSONTokener(text))
                            for (j in 0 until jsonArr.length()) {
                                val entry = jsonArr.getJSONObject(j)
                                val pUrl = entry.optString("url", "")
                                val user = entry.optString("username", "").ifEmpty { entry.optString("user", "") }
                                val pass = entry.optString("password", "").ifEmpty { entry.optString("pass", "") }
                                if (pUrl.isNotEmpty() && user.length >= 3 && pass.length >= 3) {
                                    val key = "${cleanPortalUrl(pUrl)}|$user|$pass"
                                    if (seen.add(key)) portals.add(Portal(cleanPortalUrl(pUrl), user, pass, "github/$repo"))
                                }
                            }
                        } catch (_: Exception) {}
                    } else {
                        for (p in extractPortals(text, "github/$repo")) {
                            val key = "${p.url}|${p.username}|${p.password}"
                            if (seen.add(key)) portals.add(p)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        onEvent(ScrapeEvent.Progress("Bagged ${portals.size} wild nutz so far..."))
        return portals
    }

    private suspend fun fetchTelegramPortals(onEvent: (ScrapeEvent) -> Unit): List<Portal> {
        onEvent(ScrapeEvent.Progress("Tapping the social vines for nutz..."))
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()
        val channels = listOf("xtreamcodes", "xtream_iptv_code", "satglobaltv", "IPTVXTREAMPRO")

        for (channel in channels) {
            try {
                val url = "https://t.me/s/$channel"
                val html = fetchText(url) ?: continue
                val msgRegex = Regex("""<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)</div>\s*</div>""")
                for (m in msgRegex.findAll(html)) {
                    val text = m.groupValues[1]
                        .replace(Regex("<br\\s*/?>"), "\n")
                        .replace(Regex("<[^>]+>"), "")
                        .replace("&amp;".toRegex(), "&")
                        .replace("&lt;".toRegex(), "<")
                        .replace("&gt;".toRegex(), ">")
                        .replace("&quot;".toRegex(), "\"")
                        .trim()
                    if (text.isNotEmpty()) {
                        for (p in extractPortals(text, "telegram:$channel")) {
                            val key = "${p.url}|${p.username}|${p.password}"
                            if (seen.add(key)) portals.add(p)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        onEvent(ScrapeEvent.Progress("Caught ${portals.size} more rolling nutz..."))
        return portals
    }

    private suspend fun fetchAmzPortals(onEvent: (ScrapeEvent) -> Unit): List<Portal> {
        onEvent(ScrapeEvent.Progress("Digging through the deep stash..."))
        val seen = mutableSetOf<String>()
        val portals = mutableListOf<Portal>()

        try {
            for (page in 1..5) {
                val html = fetchText("https://iptv.tutoje.cz/list/?page=$page") ?: break
                val hashes = Regex("""\?data=([a-f0-9]{32})""").findAll(html).map { it.groupValues[1] }.distinct().toList()
                if (hashes.isEmpty()) break
                for (hash in hashes.take(20)) {
                    val decoded = fetchText("https://iptv.tutoje.cz/?data=$hash") ?: continue
                    val serverMatch = Regex("""name="server_url"\s+value="([^"]*)"""").find(decoded)
                    val userMatch = Regex("""<span[^>]*class="[^"]*font-black text-3xl[^"]*"[^>]*>([^<]+)</span>""").findAll(decoded).toList()
                    val server = serverMatch?.groupValues?.getOrNull(1) ?: continue
                    val username = userMatch.getOrNull(0)?.groupValues?.getOrNull(1)?.trim() ?: continue
                    val password = userMatch.getOrNull(1)?.groupValues?.getOrNull(1)?.trim() ?: ""
                    val key = "$server|$username|$password"
                    if (seen.add(key)) portals.add(Portal(server, username, password, "amziptv"))
                }
            }
        } catch (_: Exception) {}

        onEvent(ScrapeEvent.Progress("Unearthed ${portals.size} hidden nutz..."))
        return portals
    }

    private suspend fun verifyPortal(p: Portal): VerifiedPortal? = withContext(Dispatchers.IO) {
        if (isAdultText(p.url)) return@withContext null

        try {
            val url = "${p.url}/player_api.php?username=${p.username}&password=${p.password}"
            val request = Request.Builder().url(url).header("User-Agent", "VLC/3.0.20").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val data = org.json.JSONTokener(body).nextValue()
                    if (data is JSONObject) {
                        val info = data.optJSONObject("user_info") ?: data
                        val auth = info.optString("auth", "")
                        val status = info.optString("status", "").lowercase()
                        if (auth == "1" || status == "active" || data.has("user_info")) {
                            val name = info.optString("username", p.username)
                            val domain = extractDomain(p.url)
                            return@withContext VerifiedPortal(p, name, domain)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val url = "${p.url}/get.php?username=${p.username}&password=${p.password}&type=m3u_plus"
            val request = Request.Builder().url(url).header("User-Agent", "VLC/3.0.20").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val text = response.body?.string() ?: return@withContext null
                    if (Regex("#EXTM3U", RegexOption.IGNORE_CASE).containsMatchIn(text) &&
                        !Regex("<html|<head|<body", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                        val urlCount = text.lines().count { it.startsWith("http") }
                        if (urlCount >= 5) {
                            val adultRatio = text.lines().count { isAdultText(it) }.toFloat() / text.lines().size.coerceAtLeast(1)
                            if (adultRatio < 0.15f) {
                                return@withContext VerifiedPortal(p, p.username, extractDomain(p.url))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    private suspend fun getChannelCount(p: Portal): Int = withContext(Dispatchers.IO) {
        try {
            val url = "${p.url}/player_api.php?username=${p.username}&password=${p.password}&action=get_live_streams"
            val request = Request.Builder().url(url).header("User-Agent", "VLC/3.0.20").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val text = response.body?.string() ?: return@withContext 0
                    val arr = JSONArray(text)
                    return@withContext arr.length()
                }
            }
        } catch (_: Exception) {}
        try {
            val url = "${p.url}/get.php?username=${p.username}&password=${p.password}&type=m3u_plus"
            val request = Request.Builder().url(url).header("User-Agent", "VLC/3.0.20").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val text = response.body?.string() ?: return@withContext 0
                    return@withContext text.lines().count { it.startsWith("http") }
                }
            }
        } catch (_: Exception) {}
        0
    }

    fun scrape(
        englishOnly: Boolean = true,
        noAdult: Boolean = true,
        sportsOnly: Boolean = false,
        adultOnly: Boolean = false,
        onEvent: (ScrapeEvent) -> Unit,
    ) {
        cancel()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate + kotlinx.coroutines.Job())
        currentJob = scope.launch {
            try {
                val raw = mutableListOf<Portal>()

                onEvent(ScrapeEvent.Progress("Throwing nutz at portals..."))
                val fetched = coroutineScope {
                    listOf(
                        async { fetchGitHubPortals(onEvent) },
                        async { fetchTelegramPortals(onEvent) },
                        async { fetchAmzPortals(onEvent) },
                    ).awaitAll()
                }
                fetched.forEach { raw.addAll(it) }

                onEvent(ScrapeEvent.Progress("Cracking ${raw.size} shells to find the good ones..."))

                val deduped = mutableMapOf<String, Portal>()
                for (p in raw) {
                    val key = "${p.url}|${p.username}|${p.password}".lowercase()
                    if (!deduped.containsKey(key)) deduped[key] = p
                }

                onEvent(ScrapeEvent.Progress("Shelling ${deduped.size} contenders..."))

                val contenders = deduped.values.toList().shuffled().take(100)
                val verified = coroutineScope {
                    contenders.map { p ->
                        async { verifyPortal(p) }
                    }.awaitAll().filterNotNull()
                }

                onEvent(ScrapeEvent.Progress("Counting kernels in ${verified.size} good nutz..."))

                val withCounts = coroutineScope {
                    verified.map { vp ->
                        async { vp to getChannelCount(vp.portal) }
                    }.awaitAll()
                }.filter { (vp, count) ->
                    if (sportsOnly) {
                        isAdultText(vp.name) || vp.name.let { SPORTS_KEYWORDS.any { kw -> it.lowercase().contains(kw) } }
                    } else true
                }.sortedByDescending { (_, count) -> count }

                val maxPortals = if (sportsOnly) 10 else 5
                val selected = withCounts.take(min(withCounts.size, maxPortals * 3))

                val results = mutableListOf<PortalNutzEntry>()
                var portalNum = 0

                for ((vp, totalCount) in selected) {
                    if (results.size >= maxPortals) break
                    if (noAdult && isAdultText(vp.name)) continue
                    if (adultOnly && !isAdultText(vp.name)) continue
                    if (englishOnly && hasNonLatinScript(vp.name)) continue
                    if (sportsOnly && !SPORTS_KEYWORDS.any { vp.name.lowercase().contains(it) || vp.domain.lowercase().contains(it) }) continue

                    portalNum++
                    val count = min(totalCount, 500)
                    results.add(PortalNutzEntry(
                        label = "portal$portalNum",
                        url = vp.portal.url,
                        username = vp.portal.username,
                        password = vp.portal.password,
                        channelCount = count,
                        domain = vp.domain,
                    ))
                }

                if (results.isEmpty()) {
                    onEvent(ScrapeEvent.Error("No ripe nutz found — try different filters!"))
                } else {
                    onEvent(ScrapeEvent.Progress("Roasted the duds, here are the premium nutz!"))
                    onEvent(ScrapeEvent.Result(results))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                onEvent(ScrapeEvent.Error("Dropped a nutz! ${e.message ?: "Something went wrong"}"))
            }
        }
    }
}
