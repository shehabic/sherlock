# Sherlock
Sherlock is a shorthand for "Network Sherlock Holmes" a simple lib that you can plug into your http client to capture everything around network requests, which then provides you or your QA team member with enough info about requests.

## Why not use a proxy?
- Sherlock is not to replace your normal proxy like. Charles Proxy for example, however the right question would be .. how many times did something go wrong while QAing your app then you realized that it's too late as you were not proxying?
## How to use it?
it's a very simple and straight forward for v.0.X only OKHttp is supported.
1. Add the dependency to your gradle.
```groovy
allprojects {
    repositories {
	maven { url 'https://jitpack.io' }
    }
}
```
```groovy
dependencies {
    implementation 'com.github.shehabic:Sherlock:v0.3.0'
}
```
2. on app startup initialize sherlock by ``` NetworkSherlock.getInstance().init(appContext) ```
3. then attach Sherlock's okhttp intercepter to your OKHttpClient as follows:

**Kotlin**
```kotlin
val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(SherlockOkHttpInterceptor()).build()
```
**Java**
```java
OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new SherlockOkHttpInterceptor()).build()
```
## During the next few days I'll be adding more advanced examples on how to use it. 
