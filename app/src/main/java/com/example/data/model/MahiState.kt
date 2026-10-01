package com.example.data.model

enum class VoiceState {
    DISCONNECTED,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class SassLevel(val displayName: String, val emoji: String, val tagline: String) {
    PLAYFUL("Sweet & Playful", "🥰", "Flirty teasing and affectionate girlfriend energy"),
    SASSY("Sassy & Witty", "💅", "Confident banter, smart sarcasm, and playful roasts"),
    SAVAGE("Ultra Savage", "🔥", "Zero chill, ruthless wit, and iconic queen energy")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCall: ToolExecution? = null
) {
    enum class Role { USER, MAHI, SYSTEM }
}

data class ToolExecution(
    val name: String,
    val description: String,
    val detail: String = "",
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class MahiNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
