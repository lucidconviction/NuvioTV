package com.nuvio.tv.core.network

import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {
    @Throws(IOException::class, org.schabi.newpipe.extractor.exceptions.ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val headers = request.headers() as? Map<String, List<String>>
        val requestBody = request.dataToSend()

        val body = requestBody?.toRequestBody()

        val requestBuilder = OkHttpRequest.Builder()
            .url(url)
            .method(method, body)

        headers?.forEach { (key, value) ->
            if (value != null) {
                for (v in value) {
                    requestBuilder.addHeader(key, v)
                }
            }
        }

        if (headers?.containsKey("User-Agent") != true) {
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseCode = response.code
            val message = response.message
            val responseHeaders = response.headers.toMultimap()
            val responseBody = response.body?.string() ?: ""
            val latestUrl = response.request.url.toString()

            return Response(responseCode, message, responseHeaders, responseBody, latestUrl)
        }
    }
}
