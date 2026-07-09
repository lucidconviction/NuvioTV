package com.robbdeeze.nuviotv.data.iptv

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object M3uParser {
    fun parse(inputStream: InputStream): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var currentExtInf: String? = null
        try {
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#EXTINF:")) {
                    currentExtInf = trimmed
                } else if (!trimmed.startsWith("#")) {
                    val url = trimmed
                    if (currentExtInf != null) {
                        channels.add(buildChannel(currentExtInf, url))
                        currentExtInf = null
                    }
                }
            }
        } catch (_: Exception) { }
        return channels
    }

    fun parseFlow(inputStream: InputStream): Flow<IptvChannel> = flow {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        var currentExtInf: String? = null
        var lineCount = 0
        try {
            while (reader.readLine().also { line = it } != null) {
                if (!currentCoroutineContext().isActive) break
                lineCount++
                if (lineCount % 50 == 0) currentCoroutineContext().ensureActive()
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith("#EXTINF:")) {
                    currentExtInf = trimmed
                } else if (!trimmed.startsWith("#")) {
                    val url = trimmed
                    if (currentExtInf != null) {
                        emit(buildChannel(currentExtInf, url))
                        currentExtInf = null
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun buildChannel(extInf: String, url: String): IptvChannel {
        val name = parseName(extInf)
        return IptvChannel(
            id = parseAttribute(extInf, "tvg-id") ?: url.hashCode().toString(),
            name = name,
            url = url,
            logoUrl = parseAttribute(extInf, "tvg-logo"),
            categoryName = parseAttribute(extInf, "group-title")
        )
    }

    private fun parseName(extInf: String): String {
        val commaIndex = extInf.lastIndexOf(',')
        return if (commaIndex != -1) extInf.substring(commaIndex + 1).trim() else "Unknown"
    }

    private fun parseAttribute(extInf: String, attributeName: String): String? {
        val key = "$attributeName=\""
        val startIndex = extInf.indexOf(key)
        if (startIndex == -1) return null
        val valueStart = startIndex + key.length
        val endIndex = extInf.indexOf('"', valueStart)
        if (endIndex == -1) return null
        return extInf.substring(valueStart, endIndex)
    }
}
