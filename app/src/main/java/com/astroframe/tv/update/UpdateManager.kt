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

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/mandy321/AstroFrame/releases/latest"

    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdates(context: Context, silentIfUpToDate: Boolean = true) {
        val update = queryUpdateInfo()

        withContext(Dispatchers.Main) {
            if (update != null && update.latestVersionCode > BuildConfig.VERSION_CODE) {
                showUpdatePromptDialog(context, update)
            } else if (!silentIfUpToDate) {
                Toast.makeText(context, "AstroFrame is already up to date (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun queryUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        // 1. First check PostHog Feature Flag Payload ("app_update_info")
        try {
            val payload = PostHog.getFeatureFlagPayload("app_update_info")
            if (payload is Map<*, *>) {
                val code = (payload["latest_version_code"] as? Number)?.toInt() ?: 0
                val name = payload["latest_version_name"] as? String ?: ""
                val url = payload["download_url"] as? String ?: ""
                val notes = payload["release_notes"] as? String ?: "New features and performance improvements."
                if (code > 0 && url.isNotBlank()) {
                    Log.d(TAG, "Update detected via PostHog Remote Config: v$name ($code)")
                    return@withContext UpdateInfo(code, name, url, notes)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "PostHog update check error: ${e.message}")
        }

        // 2. Fallback: Query GitHub Releases API directly
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .addHeader("User-Agent", "AstroFrame-AndroidTV")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "").removePrefix("v")
                val releaseNotes = json.optString("body", "New features & bug fixes.")
                val assets = json.optJSONArray("assets") ?: return@withContext null

                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }

                if (tagName.isNotBlank() && downloadUrl.isNotBlank()) {
                    val remoteVersionCode = parseVersionCodeFromTag(tagName)
                    return@withContext UpdateInfo(
                        latestVersionCode = remoteVersionCode,
                        latestVersionName = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub release check error: ${e.message}")
        }
        null
    }

    private fun parseVersionCodeFromTag(tag: String): Int {
        // e.g. "1.3.0" -> 10300 or split parts
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

        // Broadcast receiver for download completion
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
