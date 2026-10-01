package com.example.data.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import com.example.data.model.ToolExecution

class DeviceToolManager(private val context: Context) {

    private var isFlashlightOn = false

    fun openWebsite(url: String, title: String? = null): ToolExecution {
        val safeUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecution(
                name = "openWebsite",
                description = "Opened ${title ?: safeUrl}",
                detail = safeUrl,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e("DeviceToolManager", "Failed to open url: $safeUrl", e)
            ToolExecution(
                name = "openWebsite",
                description = "Failed to open link",
                detail = e.message ?: "",
                isSuccess = false
            )
        }
    }

    fun searchWeb(query: String): ToolExecution {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            }
            ToolExecution(
                name = "searchWeb",
                description = "Searched '$query'",
                detail = query,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e("DeviceToolManager", "Failed to search web: $query", e)
            ToolExecution(
                name = "searchWeb",
                description = "Web search failed",
                detail = e.message ?: "",
                isSuccess = false
            )
        }
    }

    fun toggleFlashlight(turnOn: Boolean): ToolExecution {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                isFlashlightOn = turnOn
                ToolExecution(
                    name = "toggleFlashlight",
                    description = if (turnOn) "Flashlight turned ON ✨" else "Flashlight turned OFF 🌙",
                    detail = if (turnOn) "Torch active" else "Torch inactive",
                    isSuccess = true
                )
            } else {
                ToolExecution(
                    name = "toggleFlashlight",
                    description = "Flashlight unavailable on this device",
                    detail = "No torch hardware found",
                    isSuccess = false
                )
            }
        } catch (e: CameraAccessException) {
            Log.e("DeviceToolManager", "Camera error toggling torch", e)
            ToolExecution(
                name = "toggleFlashlight",
                description = "Torch access denied or busy",
                detail = e.message ?: "",
                isSuccess = false
            )
        } catch (e: Exception) {
            Log.e("DeviceToolManager", "Generic error toggling torch", e)
            ToolExecution(
                name = "toggleFlashlight",
                description = "Failed to toggle flashlight",
                detail = e.message ?: "",
                isSuccess = false
            )
        }
    }

    fun setTimer(seconds: Int, label: String = "Mahi Assistant Timer"): ToolExecution {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecution(
                name = "setTimer",
                description = "Timer set for ${seconds}s ($label)",
                detail = "$seconds seconds",
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e("DeviceToolManager", "Failed to set timer", e)
            ToolExecution(
                name = "setTimer",
                description = "Couldn't set system timer",
                detail = e.message ?: "",
                isSuccess = false
            )
        }
    }

    fun openApp(appName: String): ToolExecution {
        val pm = context.packageManager
        val queryLower = appName.lowercase().trim()
        val targetPackage = when {
            queryLower.contains("youtube") -> "com.google.android.youtube"
            queryLower.contains("spotify") -> "com.spotify.music"
            queryLower.contains("chrome") -> "com.android.chrome"
            queryLower.contains("camera") -> "com.google.android.GoogleCamera"
            queryLower.contains("maps") -> "com.google.android.apps.maps"
            queryLower.contains("clock") -> "com.google.android.deskclock"
            else -> null
        }

        return try {
            val launchIntent = if (targetPackage != null) {
                pm.getLaunchIntentForPackage(targetPackage)
            } else {
                // Search installed apps by label
                val installed = pm.getInstalledApplications(0)
                val matched = installed.firstOrNull {
                    pm.getApplicationLabel(it).toString().lowercase().contains(queryLower)
                }
                matched?.packageName?.let { pm.getLaunchIntentForPackage(it) }
            }

            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                ToolExecution(
                    name = "openApp",
                    description = "Opened $appName",
                    detail = appName,
                    isSuccess = true
                )
            } else {
                // Fallback to web search
                searchWeb(appName)
            }
        } catch (e: Exception) {
            Log.e("DeviceToolManager", "Failed to launch $appName", e)
            ToolExecution(
                name = "openApp",
                description = "Could not open $appName",
                detail = e.message ?: "",
                isSuccess = false
            )
        }
    }

    fun cleanup() {
        if (isFlashlightOn) {
            toggleFlashlight(false)
        }
    }
}
