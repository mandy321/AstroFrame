package com.astroframe.tv.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.astroframe.tv.BuildConfig
import com.posthog.PostHog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val POSTHOG_FLAG_KEY = "app_update_info"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/mandy321/AstroFrame/releases/latest"

    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdates(context: Context, silentIfUpToDate: Boolean = false) = withContext(Dispatchers.Main) {
        val update = withContext(Dispatchers.IO) {
            fetchUpdateFromPostHog() ?: fetchUpdateFromGitHub()
        }

        val currentVersionCode = BuildConfig.VERSION_CODE
        val currentVersionName = BuildConfig.VERSION_NAME

        val isNewer = if (update != null) {
            if (update.latestVersionCode > 0 && update.latestVersionCode > currentVersionCode) {
                true
            } else {
                isVersionStringNewer(update.latestVersionName, currentVersionName)
            }
        } else false

        if (update != null && isNewer) {
            showUpdatePromptDialog(context, update)
        } else {
            if (!silentIfUpToDate) {
                Toast.makeText(
                    context,
                    "AstroFrame is up to date (v$currentVersionName)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isVersionStringNewer(remote: String, local: String): Boolean {
        val rClean = remote.removePrefix("v").trim()
        val lClean = local.removePrefix("v").trim()
        if (rClean == lClean) return false

        val rParts = rClean.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = lClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val rVal = rParts.getOrElse(i) { 0 }
            val lVal = lParts.getOrElse(i) { 0 }
            if (rVal > lVal) return true
            if (rVal < lVal) return false
        }
        return false
    }

    private fun fetchUpdateFromPostHog(): UpdateInfo? {
        try {
            // PostHog SDK caches feature flags or fetches async
            PostHog.reloadFeatureFlags()
            val payload = PostHog.getFeatureFlagPayload(POSTHOG_FLAG_KEY, null) ?: return null
            Log.d(TAG, "PostHog flag raw payload: $payload")

            val jsonStr = when (payload) {
                is String -> payload
                is Map<*, *> -> JSONObject(payload).toString()
                else -> payload.toString()
            }

            val json = JSONObject(jsonStr)
            val code = json.optInt("latest_version_code", 0)
            val name = json.optString("latest_version_name", "")
            val url = json.optString("download_url", "")
            val notes = json.optString("release_notes", "Performance improvements and bug fixes.")

            if (name.isNotBlank() && url.isNotBlank()) {
                return UpdateInfo(
                    latestVersionCode = code,
                    latestVersionName = name,
                    downloadUrl = url,
                    releaseNotes = notes
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "PostHog update check error: ${e.message}")
        }
        return null
    }

    private fun fetchUpdateFromGitHub(): UpdateInfo? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(GITHUB_LATEST_RELEASE_URL)
            .addHeader("User-Agent", "AstroFrame-Updater")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseNotes = json.optString("body", "What's new in v$tagName")
                val assets = json.optJSONArray("assets")

                var downloadUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (tagName.isNotBlank() && downloadUrl.isNotBlank()) {
                    return UpdateInfo(
                        latestVersionCode = parseVersionCodeFromTag(tagName),
                        latestVersionName = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub release check error: ${e.message}")
        }
        return null
    }

    private fun parseVersionCodeFromTag(tag: String): Int {
        val parts = tag.split(".")
        return try {
            val major = parts.getOrNull(0)?.toInt() ?: 1
            val minor = parts.getOrNull(1)?.toInt() ?: 0
            val patch = parts.getOrNull(2)?.toInt() ?: 0
            (major * 100) + (minor * 10) + patch
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE + 1
        }
    }

    private fun showUpdatePromptDialog(context: Context, update: UpdateInfo) {
        val cleanNotes = update.releaseNotes
            .replace("#", "")
            .replace("*", "•")
            .take(300)

        AlertDialog.Builder(context)
            .setTitle("🚀 New AstroFrame Update: v${update.latestVersionName}")
            .setMessage("A new version of AstroFrame is available!\n\nWhat's New:\n$cleanNotes\n\nWould you like to download and install now?")
            .setPositiveButton("Download & Install") { dialog, _ ->
                downloadAndInstallApk(context, update)
                dialog.dismiss()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallApk(context: Context, update: UpdateInfo) {
        Toast.makeText(context, "Starting download of v${update.latestVersionName}...", Toast.LENGTH_LONG).show()

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "AstroFrame-v${update.latestVersionName}.apk"
        val destinationUri = Uri.fromFile(
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        )

        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("AstroFrame Update v${update.latestVersionName}")
            .setDescription("Downloading latest TV update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(destinationUri)

        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    if (apkFile.exists()) {
                        installApk(context, apkFile)
                    } else {
                        Toast.makeText(context, "Downloaded file not found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Install intent error: ${e.message}", e)
            Toast.makeText(context, "Please click the downloaded APK to install: ${apkFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}
