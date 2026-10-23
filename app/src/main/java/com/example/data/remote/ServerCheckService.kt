package com.example.data.remote

import android.util.Log
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
        onUpdating: (message: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Get Bangladesh current date (Asia/Dhaka)
            val bdDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Dhaka")
            }
            val todayBd = bdDateFormat.format(Date())
            Log.d("ServerCheck", "Today in Bangladesh: $todayBd")

            // Step 2: Read date from https://bcs24admin.org.bd/tv/reDirectLog.txt
            val logRequest = Request.Builder()
                .url(REDIRECT_LOG_URL)
                .build()

            val logResponse = client.newCall(logRequest).execute()
            if (!logResponse.isSuccessful) {
                return@withContext Result.failure(Exception("সার্ভার তারিখ পড়া যায়নি (HTTP ${logResponse.code})"))
            }

            val serverLogContent = logResponse.body?.string()?.trim() ?: ""
            Log.d("ServerCheck", "Server reDirectLog content: $serverLogContent")

            // Extract date format YYYY-MM-DD
            val dateRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
            val serverDateMatch = dateRegex.find(serverLogContent)
            val serverDate = serverDateMatch?.groupValues?.get(1) ?: serverLogContent.take(10)

            // Step 3: Compare dates
            if (serverDate == todayBd) {
                Log.d("ServerCheck", "Dates match ($serverDate). No update needed.")
                return@withContext Result.success("চ্যানেল তালিকা হালনাগাদ রয়েছে ($serverDate)")
            }

            // Date does not match -> Show updating message and call check_youtube_live.php
            withContext(Dispatchers.Main) {
                onUpdating("চ্যানেল আপডেট হচ্ছে...")
            }

            Log.d("ServerCheck", "Dates mismatch (Server: $serverDate, BD: $todayBd). Calling check script...")

            val checkRequest = Request.Builder()
                .url(CHECK_YOUTUBE_LIVE_URL)
                .build()

            val checkResponse = client.newCall(checkRequest).execute()
            val checkResponseBody = checkResponse.body?.string() ?: ""
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
     * Complies with requirement: Credentials and PIN are stored on the server file, not hardcoded in app.
     */
    suspend fun verifyPinOnServer(enteredPin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedPin = enteredPin.trim()
        if (trimmedPin.isEmpty()) {
            return@withContext Result.failure(Exception("অনুগ্রহ করে একটি সঠিক পিন দিন"))
        }

        try {
            // First try POST request with JSON
            val jsonBody = JSONObject().put("pin", trimmedPin).put("action", "verify_pin").toString()
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val postRequest = Request.Builder()
                .url(CREDENTIALS_URL)
                .post(requestBody)
                .build()

            val response = try {
                client.newCall(postRequest).execute()
            } catch (e: Exception) {
                // If POST failed, try GET with query parameter
                val getRequest = Request.Builder()
                    .url("$CREDENTIALS_URL?pin=$trimmedPin&action=verify")
                    .build()
                client.newCall(getRequest).execute()
            }

            val body = response.body?.string()?.trim() ?: ""
            Log.d("ServerCheck", "PIN verify response code: ${response.code}, body: $body")

            if (response.code == 404) {
                // Server file not found yet on user's host
                return@withContext Result.failure(
                    Exception("সার্ভারে credentialsGemini.php ফাইলটি কনফিগার করা নেই (404 Not Found)। সার্ভার অ্যাডমিনের সাথে যোগাযোগ করুন।")
                )
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("সার্ভার ত্রুটি (HTTP ${response.code})"))
            }

            // Check response JSON or content
            val isSuccess = try {
                val json = JSONObject(body)
                val status = json.optString("status")
                val valid = json.optBoolean("valid", false)
                val serverPin = json.optString("pin")
                status.equals("success", ignoreCase = true) || valid || (serverPin.isNotEmpty() && serverPin == trimmedPin)
            } catch (e: Exception) {
                // Plain text response
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

    companion object {
        const val REDIRECT_LOG_URL = "https://bcs24admin.org.bd/tv/reDirectLog.txt"
        const val CHECK_YOUTUBE_LIVE_URL = "https://bcs24admin.org.bd/tv/check_youtube_live.php"
        const val CREDENTIALS_URL = "https://bcs24admin.org.bd/tv/credentialsGemini.php"
        const val BUILT_IN_M3U_URL = "https://bcs24admin.org.bd/NokshiTV/NokshiTV.m3u"
    }
}
