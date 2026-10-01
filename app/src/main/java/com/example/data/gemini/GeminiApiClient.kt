package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.SassLevel
import com.example.data.model.ToolExecution
import com.example.data.tools.DeviceToolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResponse {
    data class Success(val spokenText: String, val toolExecution: ToolExecution? = null) : GeminiResponse()
    data class Error(val message: String, val isKeyMissing: Boolean = false) : GeminiResponse()
}

class GeminiApiClient(private val toolManager: DeviceToolManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    var customApiKey: String = ""

    private fun getEffectiveApiKey(): String {
        if (customApiKey.isNotBlank()) return customApiKey
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (buildKey != "MY_GEMINI_API_KEY" && buildKey.isNotBlank()) buildKey else ""
    }

    fun isApiKeyConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    suspend fun sendVoicePrompt(
        userPrompt: String,
        sassLevel: SassLevel,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            // Local fallback with Mahi personality and local tool execution
            val (fallbackText, toolTrigger) = MahiPersona.getLocalResponse(userPrompt, sassLevel)
            var toolExec: ToolExecution? = null
            if (toolTrigger != null) {
                toolExec = executeLocalTool(toolTrigger)
            }
            return@withContext GeminiResponse.Success(fallbackText, toolExec)
        }

        val model = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val root = JSONObject()

            // System Instruction
            val sysParts = JSONArray().apply {
                put(JSONObject().put("text", MahiPersona.getSystemPrompt(sassLevel)))
            }
            root.put("systemInstruction", JSONObject().put("parts", sysParts))

            // Contents (History + Latest user prompt)
            val contentsArray = JSONArray()
            for ((role, text) in conversationHistory.takeLast(4)) {
                val turnParts = JSONArray().apply {
                    put(JSONObject().put("text", text))
                }
                contentsArray.put(
                    JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", turnParts)
                    }
                )
            }
            // Add current prompt
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                    })
                }
            )
            root.put("contents", contentsArray)

            // Function Calling Tools
            val funcDeclarations = JSONArray().apply {
                put(createToolDeclaration(
                    name = "openWebsite",
                    desc = "Open a website in the device web browser",
                    params = mapOf("url" to "URL of website, e.g. https://youtube.com", "title" to "Site display name"),
                    required = listOf("url")
                ))
                put(createToolDeclaration(
                    name = "searchWeb",
                    desc = "Search the web or Google for a query",
                    params = mapOf("query" to "Search query terms"),
                    required = listOf("query")
                ))
                put(createToolDeclaration(
                    name = "toggleFlashlight",
                    desc = "Turn the phone's flashlight / torch on or off",
                    params = mapOf("turnOn" to "true to turn on, false to turn off (boolean)"),
                    required = listOf("turnOn")
                ))
                put(createToolDeclaration(
                    name = "setTimer",
                    desc = "Set a countdown timer on the device",
                    params = mapOf("seconds" to "Duration in seconds (integer)", "label" to "Optional timer name"),
                    required = listOf("seconds")
                ))
                put(createToolDeclaration(
                    name = "openApp",
                    desc = "Open an installed app like YouTube, Spotify, Camera, Maps",
                    params = mapOf("appName" to "Name of the app to launch"),
                    required = listOf("appName")
                ))
            }
            root.put("tools", JSONArray().apply {
                put(JSONObject().put("functionDeclarations", funcDeclarations))
            })

            // Generation Config
            val genConfig = JSONObject().apply {
                put("temperature", 0.85)
                put("topP", 0.95)
            }
            root.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "API error: ${response.code} $responseBody")
                // Use smart fallback with persona
                val (fallbackText, toolTrigger) = MahiPersona.getLocalResponse(userPrompt, sassLevel)
                val toolExec = toolTrigger?.let { executeLocalTool(it) }
                return@withContext GeminiResponse.Success(fallbackText, toolExec)
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var spokenText = ""
            var executedTool: ToolExecution? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.has("text")) {
                        spokenText += part.optString("text") + " "
                    }
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.getString("name")
                        val args = fn.optJSONObject("args") ?: JSONObject()
                        executedTool = executeFunctionCall(fnName, args)
                    }
                }
            }

            spokenText = spokenText.trim()
            if (spokenText.isEmpty()) {
                spokenText = when (executedTool?.name) {
                    "openWebsite" -> "Opened that for you, babe! Anything else you need? 😉"
                    "searchWeb" -> "Got it! Looked that up for you, handsome. 💅"
                    "toggleFlashlight" -> "Let there be light, trouble! ✨"
                    "setTimer" -> "Timer set! Don't let your day slip away now. ⏱️"
                    "openApp" -> "Launched! You're welcome, darling. 💋"
                    else -> "All done! What's next on your mind, trouble? ✨"
                }
            }

            GeminiResponse.Success(spokenText, executedTool)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Call failure", e)
            val (fallbackText, toolTrigger) = MahiPersona.getLocalResponse(userPrompt, sassLevel)
            val toolExec = toolTrigger?.let { executeLocalTool(it) }
            GeminiResponse.Success(fallbackText, toolExec)
        }
    }

    private fun createToolDeclaration(
        name: String,
        desc: String,
        params: Map<String, String>,
        required: List<String>
    ): JSONObject {
        val props = JSONObject()
        for ((key, descText) in params) {
            props.put(key, JSONObject().apply {
                put("type", if (key == "turnOn") "BOOLEAN" else if (key == "seconds") "INTEGER" else "STRING")
                put("description", descText)
            })
        }
        val reqArr = JSONArray().apply { required.forEach { put(it) } }
        val parameters = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", props)
            put("required", reqArr)
        }
        return JSONObject().apply {
            put("name", name)
            put("description", desc)
            put("parameters", parameters)
        }
    }

    private fun executeFunctionCall(name: String, args: JSONObject): ToolExecution {
        return when (name) {
            "openWebsite" -> {
                val url = args.optString("url", "https://google.com")
                val title = args.optString("title", "Site")
                toolManager.openWebsite(url, title)
            }
            "searchWeb" -> {
                val query = args.optString("query", "Android")
                toolManager.searchWeb(query)
            }
            "toggleFlashlight" -> {
                val turnOn = args.optBoolean("turnOn", true)
                toolManager.toggleFlashlight(turnOn)
            }
            "setTimer" -> {
                val sec = args.optInt("seconds", 60)
                val label = args.optString("label", "Mahi Timer")
                toolManager.setTimer(sec, label)
            }
            "openApp" -> {
                val app = args.optString("appName", "YouTube")
                toolManager.openApp(app)
            }
            else -> {
                ToolExecution(name, "Executed tool $name", isSuccess = true)
            }
        }
    }

    private fun executeLocalTool(trigger: String): ToolExecution {
        val parts = trigger.split(":", limit = 2)
        val action = parts[0]
        val param = parts.getOrNull(1) ?: ""
        return when (action) {
            "openWebsite" -> toolManager.openWebsite(param)
            "searchWeb" -> toolManager.searchWeb(param)
            "toggleFlashlight" -> toolManager.toggleFlashlight(param.toBoolean())
            "setTimer" -> toolManager.setTimer(param.toIntOrNull() ?: 60)
            "saveNote" -> ToolExecution("saveNote", "Note saved: $param", param, true)
            else -> ToolExecution("unknown", "Action done")
        }
    }
}
