package com.shehabic.sherlock.interceptors

import okhttp3.Interceptor
import okhttp3.Response

class SherlockOkHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
