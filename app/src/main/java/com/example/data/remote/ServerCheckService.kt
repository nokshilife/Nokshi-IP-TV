package com.example.data.remote

import android.util.Log
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ServerCheckService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Checks server log date and runs YouTube live check if date does not match Bangladesh current date
     */
    suspend fun performServerCheck(
        forceUpdate: Boolean = false,
        onUpdating: (message: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bdDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Dhaka")
            }
            val todayBd = bdDateFormat.format(Date())
            Log.d("ServerCheck", "Today in Bangladesh: $todayBd")

            val logRequest = Request.Builder()
                .url(REDIRECT_LOG_URL)
                .build()

            val logResponse = client.newCall(logRequest).execute()
            if (!logResponse.isSuccessful) {
                return@withContext Result.failure(Exception("সার্ভার তারিখ পড়া যায়নি (HTTP ${logResponse.code})"))
            }

            val serverLogContent = logResponse.body?.string()?.trim() ?: ""
            Log.d("ServerCheck", "Server reDirectLog content: $serverLogContent")

            val dateRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
            val serverDateMatch = dateRegex.find(serverLogContent)
            val serverDate = serverDateMatch?.groupValues?.get(1) ?: serverLogContent.take(10)

            if (!forceUpdate && serverDate == todayBd) {
                Log.d("ServerCheck", "Dates match ($serverDate). No update needed.")
                return@withContext Result.success("সার্ভার লগ: $serverDate | বাংলাদেশ: $todayBd (চ্যানেল হালনাগাদ রয়েছে)")
            }

            withContext(Dispatchers.Main) {
                onUpdating("চ্যানেল আপডেট হচ্ছে... (সার্ভার: $serverDate, বাংলাদেশ: $todayBd)")
            }

            Log.d("ServerCheck", "Calling check_youtube_live.php (force=$forceUpdate)...")

            val checkRequest = Request.Builder()
                .url(CHECK_YOUTUBE_LIVE_URL)
                .build()

            val checkResponse = client.newCall(checkRequest).execute()
            Log.d("ServerCheck", "check_youtube_live response code: ${checkResponse.code}")

            if (checkResponse.isSuccessful) {
                Result.success("চ্যানেল সফলভাবে আপডেট সম্পন্ন হয়েছে")
            } else {
                Result.failure(Exception("চ্যানেল আপডেট স্ক্রিপ্ট ত্রুটি (HTTP ${checkResponse.code})"))
            }
        } catch (e: Exception) {
            Log.e("ServerCheck", "Server check error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies One-time PIN code against https://bcs24admin.org.bd/tv/credentialsGemini.php
     */
    suspend fun verifyPinOnServer(enteredPin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedPin = enteredPin.trim()
        if (trimmedPin.isEmpty()) {
            return@withContext Result.failure(Exception("অনুগ্রহ করে একটি সঠিক পিন দিন"))
        }

        try {
            val jsonBody = JSONObject().put("pin", trimmedPin).put("action", "verify_pin").toString()
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val postRequest = Request.Builder()
                .url(CREDENTIALS_URL)
                .post(requestBody)
                .build()

            val response = try {
                client.newCall(postRequest).execute()
            } catch (e: Exception) {
                val getRequest = Request.Builder()
                    .url("$CREDENTIALS_URL?pin=$trimmedPin&action=verify")
                    .build()
                client.newCall(getRequest).execute()
            }

            val body = response.body?.string()?.trim() ?: ""
            Log.d("ServerCheck", "PIN verify response code: ${response.code}, body: $body")

            if (response.code == 404) {
                return@withContext Result.failure(
                    Exception("সার্ভারে credentialsGemini.php ফাইলটি পাওয়া যায়নি (404 Not Found)। অনুগ্রহ করে সার্ভারে ফাইলটি হোস্ট করুন।")
                )
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("সার্ভার ত্রুটি (HTTP ${response.code})"))
            }

            val isSuccess = try {
                val json = JSONObject(body)
                val status = json.optString("status")
                val valid = json.optBoolean("valid", false)
                val serverPin = json.optString("pin")
                status.equals("success", ignoreCase = true) || valid || (serverPin.isNotEmpty() && serverPin == trimmedPin)
            } catch (e: Exception) {
                body.contains("success", ignoreCase = true) || body.contains("ok", ignoreCase = true) || body.contains(trimmedPin)
            }

            if (isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception("ভুল পিন নম্বর! সঠিক পিন দিয়ে পুনরায় চেষ্টা করুন।"))
            }
        } catch (e: Exception) {
            Log.e("ServerCheck", "PIN verification error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches live channels from MySQL via NokshiIPTVapi.php or credentialsGemini.php
     */
    suspend fun fetchLiveChannelsFromServer(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        val urls = listOf(
            NOKSHI_API_URL,
            "$CREDENTIALS_URL?action=get_channels"
        )

        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.optString("status") == "success") {
                        val array = json.optJSONArray("channels")
                        if (array != null && array.length() > 0) {
                            val channels = mutableListOf<Channel>()
                            for (i in 0 until array.length()) {
                                val item = array.getJSONObject(i)
                                val category = item.optInt("category", 0)
                                val isActive = item.optBoolean("isActive", true)
                                if (category == 0 && isActive) {
                                    channels.add(
                                        Channel(
                                            id = item.getInt("id"),
                                            type = ChannelType.YOUTUBE,
                                            name = item.optString("name", "Channel"),
                                            streamUrl = item.optString("youtubeUrl", ""),
                                            channelUrl = item.optString("channelUrl", null),
                                            poster = item.optString("poster", null).takeIf { it.isNotEmpty() && it != "null" },
                                            groupTitle = "YouTube Live",
                                            category = category,
                                            isActive = isActive,
                                            displayOrder = item.optInt("displayOrder", i),
                                            isFavorite = false
                                        )
                                    )
                                }
                            }
                            Log.d("ServerCheck", "Loaded ${channels.size} channels from MySQL API: $url")
                            return@withContext Result.success(channels)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ServerCheck", "Failed to fetch channels from $url: ${e.message}")
            }
        }
        Result.failure(Exception("Could not fetch channels from server API"))
    }

    companion object {
        const val REDIRECT_LOG_URL = "https://bcs24admin.org.bd/tv/reDirectLog.txt"
        const val CHECK_YOUTUBE_LIVE_URL = "https://bcs24admin.org.bd/tv/check_youtube_live.php"
        const val CREDENTIALS_URL = "https://bcs24admin.org.bd/tv/credentialsGemini.php"
        const val NOKSHI_API_URL = "https://bcs24admin.org.bd/tv/NokshiIPTVapi.php?action=get_channels"
        const val BUILT_IN_M3U_URL = "https://bcs24admin.org.bd/NokshiTV/NokshiTV.m3u"
    }
}
