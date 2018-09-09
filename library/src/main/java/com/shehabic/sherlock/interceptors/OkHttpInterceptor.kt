package com.shehabic.sherlock.interceptors

import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.db.NetworkRequests
import okhttp3.Interceptor
import okhttp3.Response

class SherlockOkHttpInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain?): Response {
        val srcRequest = chain?.request()!!
        val request = NetworkRequests(0, NetworkSherlock.getInstance().getSessionId())
        request.method = srcRequest.method()
        request.requestUrl = srcRequest.url().toString()
        request.requestHeaders = srcRequest.headers().toString()
        request.requestBody = srcRequest.body().toString()
        NetworkSherlock.getInstance().startRequest()
        val response = chain.proceed(srcRequest)

        request.responseBody = response.body()?.string()
        request.statusCode = response.code()
        request.responseHeaders = response.headers().toString()
        request.responseLength = response.body()?.contentLength() ?: 0L
        request.requestStartTime = response.sentRequestAtMillis()
        request.responseTime = response.receivedResponseAtMillis() - request.requestStartTime
        NetworkSherlock.getInstance().addRequest(request)
        NetworkSherlock.getInstance().endRequest()
        return response
    }
}