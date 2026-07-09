package com.nuvio.tv.data.iptv

import android.util.Xml
import com.nuvio.tv.domain.model.IptvEpgEntry
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object EpgParser {
    private val dateFormat by lazy { SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US) }

    fun parse(inputStream: InputStream): Map<String, List<IptvEpgEntry>> {
        val epg = mutableMapOf<String, MutableList<IptvEpgEntry>>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var currentChannel: String? = null
            var startTime: Long = 0
            var endTime: Long = 0
            var title: String? = null
            var desc: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "programme") {
                            currentChannel = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            startTime = parseTime(startStr)
                            endTime = parseTime(stopStr)
                        } else if (tag == "title" && currentChannel != null) {
                            title = parser.nextText()
                        } else if (tag == "desc" && currentChannel != null) {
                            desc = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "programme" && currentChannel != null && title != null) {
                            val entries = epg.getOrPut(currentChannel) { mutableListOf() }
                            entries.add(IptvEpgEntry(title, desc, startTime, endTime))
                            title = null
                            desc = null
                            currentChannel = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (_: Exception) {}
        }
        return epg
    }

    private fun parseTime(timeStr: String?): Long {
        if (timeStr == null) return 0L
        return try {
            dateFormat.parse(timeStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
