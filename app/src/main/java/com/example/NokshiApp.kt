package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NokshiApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Log.d("NokshiApp", "Nokshi IP TV Application Initialized")
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val host = original.url.host.lowercase()
                val reqBuilder = original.newBuilder()
                    .header("Accept", "image/avif,image/webp,image/apng,image/png,image/jpeg,image/*,*/*;q=0.8")

                when {
                    host.contains("wikimedia.org") || host.contains("wikipedia.org") -> {
                        // Wikimedia Foundation policy requires a descriptive app User-Agent (blocks okhttp/* with 403)
                        reqBuilder.header(
                            "User-Agent",
                            "NokshiIPTV/1.0 (https://bcs24admin.org.bd; support@bcs24admin.org.bd)"
                        )
                    }
                    host.contains("wikia.nocookie.net") || host.contains("fandom.com") -> {
                        reqBuilder.header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                        )
                        reqBuilder.header("Referer", "https://logos.fandom.com/")
                    }
                    else -> {
                        reqBuilder.header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                        )
                    }
                }
                chain.proceed(reqBuilder.build())
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("channel_posters_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
