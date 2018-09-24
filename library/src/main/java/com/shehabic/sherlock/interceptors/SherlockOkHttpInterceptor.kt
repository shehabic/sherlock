package com.shehabic.sherlock.interceptors

import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.db.NetworkRequests
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset


class SherlockOkHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain?): Response {
        val srcRequest = chain?.request()!!
        val request = NetworkRequests(0, NetworkSherlock.getInstance().getSessionId())
        request.method = srcRequest.method()
        request.requestUrl = srcRequest.url().toString()
        request.requestHeaders = srcRequest.headers().toString()
        request.requestStartTime = System.currentTimeMillis()
        try {
            val buffer = Buffer()
            srcRequest.body()?.writeTo(buffer)
            request.requestBody = buffer.readUtf8()
        } catch (e: Exception) {
            request.requestBody = ""
        }
        request.requestContentType = srcRequest.body()?.contentType()?.toString()
        NetworkSherlock.getInstance().startRequest()
        val response: Response?
        try {
            response = chain.proceed(srcRequest)
        } catch (e: IOException) {
            request.responseError = e.message
            request.responseTime = System.currentTimeMillis() - request.requestStartTime
            NetworkSherlock.getInstance().addRequest(request)
            NetworkSherlock.getInstance().endRequest()
            throw e
        }
        val responseBody = response.body()
        val contentLength = responseBody!!.contentLength()
        if (this.bodyEncoded(response.headers())) {
            request.responseBody = "<encoded body omitted>"
        } else {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer()
            var charset: Charset? = Charset.defaultCharset()
            val contentType = responseBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(Charset.defaultCharset())
            }
            request.responseBody = ""
            if (!isPlaintext(buffer)) {
                request.responseBody = "<binary " + buffer.size() + " body omitted>"
            } else if (contentLength != 0L) {
                request.responseBody = buffer.clone().readString(charset!!)
            }
        }
        request.statusCode = response.code()
        request.responseHeaders = response.headers().toString()
        request.responseLength = response.body()?.contentLength() ?: 0L
        request.requestStartTime = response.sentRequestAtMillis()
        request.requestHeaders = response.networkResponse()?.request()?.headers().toString()
        request.responseTime = response.receivedResponseAtMillis() - request.requestStartTime
        request.responseMessage = response.message()?.toString()
        request.responseContentType = response.body()?.contentType()?.toString()
        request.isRedirect = response.isRedirect
        request.protocol = response.protocol().toString()

        NetworkSherlock.getInstance().addRequest(request)
        NetworkSherlock.getInstance().endRequest()

        return response
    }

    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size() < 64L) buffer.size() else 64L
            buffer.copyTo(prefix, 0L, byteCount)

            var i = 0
            while (i < 16 && !prefix.exhausted()) {
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
                ++i
            }

            return true
        } catch (var6: EOFException) {
            return false
        }

    }

    private fun bodyEncoded(headers: Headers): Boolean {
        val contentEncoding = headers.get("Content-Encoding")
        return contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)
    }
}