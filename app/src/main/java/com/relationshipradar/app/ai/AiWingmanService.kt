package com.relationshipradar.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class WingmanVibe(val label: String, val shortTitle: String, val emoji: String, val description: String) {
    LOW_PRESSURE("Low Pressure", "No Stress", "🧘", "Zero pressure, reply whenever"),
    CATCH_UP("Catch Up", "Catch Up", "☕", "Casual plans, coffee, lunch, or hanging out"),
    QUICK_DIRECT("Quick & Direct", "Quick", "⚡", "Short, punchy 1-line text for busy moments"),
    WARM_CASUAL("Warm & Casual", "Warm", "💛", "Friendly check-in about what's new"),
    FUNNY("Playful & Meme", "Playful", "😂", "Lighthearted, humorous reconnection"),
    CELEBRATE("Celebrate & Congrats", "Congrats", "🎉", "Celebrating good news, promotions, or wins"),
    DEEP_RECONNECT("Thoughtful", "Heartfelt", "✨", "Meaningful and sincere check-in")
}

data class ProviderMeta(
    val id: String,
    val name: String,
    val isFreeTierAvailable: Boolean,
    val badge: String,
    val keyUrl: String,
    val defaultModel: String
)

object AiProviders {
    val ALL = listOf(
        ProviderMeta(
            id = "gemini",
            name = "Google Gemini",
            isFreeTierAvailable = true,
            badge = "100% Free · No Card Needed",
            keyUrl = "https://aistudio.google.com/app/apikey",
            defaultModel = "gemini-2.0-flash"
        ),
        ProviderMeta(
            id = "groq",
            name = "Groq Cloud",
            isFreeTierAvailable = true,
            badge = "100% Free · Lightning Fast",
            keyUrl = "https://console.groq.com/keys",
            defaultModel = "llama-3.3-70b-versatile"
        ),
        ProviderMeta(
            id = "openrouter",
            name = "OpenRouter",
            isFreeTierAvailable = true,
            badge = "Free Models Available",
            keyUrl = "https://openrouter.ai/keys",
            defaultModel = "meta-llama/llama-3.3-70b-instruct:free"
        ),
        ProviderMeta(
            id = "grok",
            name = "xAI Grok",
            isFreeTierAvailable = false,
            badge = "xAI Console",
            keyUrl = "https://console.x.ai/",
            defaultModel = "grok-2-mini"
        ),
        ProviderMeta(
            id = "openai",
            name = "OpenAI",
            isFreeTierAvailable = false,
            badge = "GPT-4o mini",
            keyUrl = "https://platform.openai.com/api-keys",
            defaultModel = "gpt-4o-mini"
        ),
        ProviderMeta(
            id = "claude",
            name = "Claude",
            isFreeTierAvailable = false,
            badge = "Claude 3.5 Haiku",
            keyUrl = "https://console.anthropic.com/",
            defaultModel = "claude-3-5-haiku-latest"
        ),
        ProviderMeta(
            id = "custom",
            name = "Custom / Local",
            isFreeTierAvailable = true,
            badge = "Ollama / Local / Proxy",
            keyUrl = "http://localhost:11434",
            defaultModel = "llama3"
        )
    )

    fun find(id: String): ProviderMeta = ALL.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL.first()
}

object AiWingmanService {

    data class SuggestionResult(
        val suggestions: List<String>,
        val isAiPowered: Boolean,
        val providerUsed: String = "Offline Engine"
    )

    suspend fun generateStarters(
        name: String,
        category: String?,
        daysSinceContact: Int?,
        talkingPoints: String?,
        notes: String?,
        vibe: WingmanVibe,
        provider: String,
        apiKey: String,
        customBaseUrl: String = "",
        customModel: String = "",
        customPrompt: String? = null
    ): SuggestionResult = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        val meta = AiProviders.find(provider)

        if (trimmedKey.isNotEmpty() || provider == "custom") {
            try {
                val aiSuggestions = when (provider.lowercase()) {
                    "gemini" -> callGemini(name, category, daysSinceContact, talkingPoints, notes, vibe, trimmedKey, customPrompt)
                    "groq" -> callOpenAiCompatible(
                        endpoint = "https://api.groq.com/openai/v1/chat/completions",
                        model = "llama-3.3-70b-versatile",
                        apiKey = trimmedKey,
                        name = name, category = category, daysSinceContact = daysSinceContact,
                        talkingPoints = talkingPoints, notes = notes, vibe = vibe,
                        customPrompt = customPrompt
                    )
                    "openrouter" -> callOpenAiCompatible(
                        endpoint = "https://openrouter.ai/api/v1/chat/completions",
                        model = "meta-llama/llama-3.3-70b-instruct:free",
                        apiKey = trimmedKey,
                        name = name, category = category, daysSinceContact = daysSinceContact,
                        talkingPoints = talkingPoints, notes = notes, vibe = vibe,
                        customPrompt = customPrompt
                    )
                    "grok" -> callOpenAiCompatible(
                        endpoint = "https://api.x.ai/v1/chat/completions",
                        model = "grok-2-mini",
                        apiKey = trimmedKey,
                        name = name, category = category, daysSinceContact = daysSinceContact,
                        talkingPoints = talkingPoints, notes = notes, vibe = vibe,
                        customPrompt = customPrompt
                    )
                    "openai" -> callOpenAiCompatible(
                        endpoint = "https://api.openai.com/v1/chat/completions",
                        model = "gpt-4o-mini",
                        apiKey = trimmedKey,
                        name = name, category = category, daysSinceContact = daysSinceContact,
                        talkingPoints = talkingPoints, notes = notes, vibe = vibe,
                        customPrompt = customPrompt
                    )
                    "claude" -> callClaude(name, category, daysSinceContact, talkingPoints, notes, vibe, trimmedKey, customPrompt)
                    "custom" -> callOpenAiCompatible(
                        endpoint = if (customBaseUrl.endsWith("/chat/completions")) customBaseUrl else "${customBaseUrl.trimEnd('/')}/chat/completions",
                        model = customModel.ifBlank { "default" },
                        apiKey = trimmedKey,
                        name = name, category = category, daysSinceContact = daysSinceContact,
                        talkingPoints = talkingPoints, notes = notes, vibe = vibe,
                        customPrompt = customPrompt
                    )
                    else -> callGemini(name, category, daysSinceContact, talkingPoints, notes, vibe, trimmedKey, customPrompt)
                }

                if (aiSuggestions.isNotEmpty()) {
                    return@withContext SuggestionResult(
                        suggestions = aiSuggestions.take(3),
                        isAiPowered = true,
                        providerUsed = meta.name
                    )
                }
            } catch (e: Exception) {
                // Network or API failure, gracefully fall back to smart offline templates
            }
        }

        // Smart offline fallback: Immediate, zero latency, highly personalized
        SuggestionResult(
            suggestions = generateSmartOffline(name, category, daysSinceContact, talkingPoints, vibe, customPrompt),
            isAiPowered = false,
            providerUsed = "Tether Smart Template"
        )
    }

    private fun buildPrompt(
        name: String,
        category: String?,
        daysSinceContact: Int?,
        talkingPoints: String?,
        notes: String?,
        vibe: WingmanVibe,
        customPrompt: String? = null
    ): String {
        val daysStr = daysSinceContact?.let { "$it days since we last talked" } ?: "it's been a while"
        val categoryStr = category ?: "close contact"
        val pointsStr = talkingPoints?.takeIf { it.isNotBlank() }?.let { "Talking points to touch on: $it." } ?: ""
        val notesStr = notes?.takeIf { it.isNotBlank() }?.let { "Personal notes: $it." } ?: ""
        val customStr = customPrompt?.trim()?.takeIf { it.isNotBlank() }?.let {
            "\nIMPORTANT: The user specifically wants to convey this thought or topic: \"$it\". Transform and rewrite this into 3 ready-to-send texts matching the requested vibe!"
        } ?: ""

        return """
You are a social wingman for introverts and people with ADHD who want to reach out to someone without social anxiety or awkwardness.
Write exactly 3 distinct, ready-to-send SMS text message options to:
- Name: $name
- Relationship: $categoryStr
- Time elapsed: $daysStr
- Desired Vibe: ${vibe.label} (${vibe.description})
$pointsStr
$notesStr
$customStr

Rules:
1. Keep each text short, authentic, and natural (1 to 2 sentences max).
2. Absolutely NO cheesy corporate greetings, NO formal sign-offs like "Best regards" or "Sincerely".
3. Sound like a real human texting a friend or family member.
4. Output ONLY the 3 text messages, each on its own line, prefixed with "1. ", "2. ", "3. ". Do not add intro or outro explanation.
""".trimIndent()
    }

    private fun callGemini(
        name: String,
        category: String?,
        daysSinceContact: Int?,
        talkingPoints: String?,
        notes: String?,
        vibe: WingmanVibe,
        apiKey: String,
        customPrompt: String? = null
    ): List<String> {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true

        val prompt = buildPrompt(name, category, daysSinceContact, talkingPoints, notes, vibe, customPrompt)
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 250)
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode in 200..299) {
            val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(resp)
            val text = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            return parseList(text)
        }
        return emptyList()
    }

    private fun callOpenAiCompatible(
        endpoint: String,
        model: String,
        apiKey: String,
        name: String,
        category: String?,
        daysSinceContact: Int?,
        talkingPoints: String?,
        notes: String?,
        vibe: WingmanVibe,
        customPrompt: String? = null
    ): List<String> {
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true

        val prompt = buildPrompt(name, category, daysSinceContact, talkingPoints, notes, vibe, customPrompt)
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 250)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode in 200..299) {
            val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(resp)
            val text = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            return parseList(text)
        }
        return emptyList()
    }

    private fun callClaude(
        name: String,
        category: String?,
        daysSinceContact: Int?,
        talkingPoints: String?,
        notes: String?,
        vibe: WingmanVibe,
        apiKey: String,
        customPrompt: String? = null
    ): List<String> {
        val url = URL("https://api.anthropic.com/v1/messages")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true

        val prompt = buildPrompt(name, category, daysSinceContact, talkingPoints, notes, vibe, customPrompt)
        val payload = JSONObject().apply {
            put("model", "claude-3-5-haiku-latest")
            put("max_tokens", 250)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode in 200..299) {
            val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(resp)
            val text = json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
            return parseList(text)
        }
        return emptyList()
    }

    private fun parseList(raw: String): List<String> {
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                line.replace(Regex("^(\\d+\\.|[-*•])\\s*"), "")
                    .removeSurrounding("\"")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .take(3)
    }

    private fun generateSmartOffline(
        name: String,
        category: String?,
        daysSinceContact: Int?,
        talkingPoints: String?,
        vibe: WingmanVibe,
        customPrompt: String? = null
    ): List<String> {
        val firstName = name.split(" ").firstOrNull() ?: name
        val custom = customPrompt?.trim()?.takeIf { it.isNotBlank() }
        val firstPoint = custom ?: talkingPoints?.lines()
            ?.map { it.removePrefix("• ").removePrefix("- ").trim() }
            ?.firstOrNull { it.isNotBlank() }

        return when (vibe) {
            WingmanVibe.LOW_PRESSURE -> listOf(
                if (firstPoint != null)
                    "Hey $firstName! No rush to reply at all, just was thinking about $firstPoint and wanted to send a quick wave 👋"
                else
                    "Hey $firstName! Zero pressure to get back to me, just wanted to say hi and hope you're having a good week!",
                if (custom != null)
                    "Hey! No need to reply right away, but was just thinking about $custom whenever you're free."
                else
                    "Hey $firstName! Thinking of you today, hope life has been treating you kindly lately.",
                "Hey! Hope things are going smoothly on your end, no reply needed!"
            )

            WingmanVibe.CATCH_UP -> listOf(
                if (firstPoint != null)
                    "Hey $firstName! Would love to catch up soon. Are you free sometime to chat about $firstPoint?"
                else
                    "Hey $firstName! It feels like forever. Would love to catch up over coffee or lunch soon if you're free!",
                if (custom != null)
                    "Hey $firstName! Thinking we should catch up on $custom soon, let me know when works for you!"
                else
                    "Hey! We gotta catch up properly soon. How does your schedule look this week or next?",
                "Hey $firstName, was just thinking of you! Let's definitely find time to catch up soon."
            )

            WingmanVibe.QUICK_DIRECT -> listOf(
                if (firstPoint != null)
                    "Hey $firstName, quick check-in on $firstPoint! How's it going?"
                else
                    "Hey $firstName, quick ping to see how you're doing!",
                if (custom != null)
                    "Hey! Quick question about $custom when you get a second."
                else
                    "Hey $firstName! Hope you're having a solid week, talk soon!",
                "Just checking in quickly! Hope all is well."
            )

            WingmanVibe.WARM_CASUAL -> listOf(
                if (firstPoint != null)
                    "Hey $firstName! Hope you're having a great week, how did everything go with $firstPoint?"
                else
                    "Hey $firstName! How have you been? Would love to catch up whenever you get a chance.",
                if (custom != null)
                    "Hey $firstName! Saw something that reminded me of $custom and made me think of you. Hope you're well!"
                else
                    "Hey $firstName! Saw something that reminded me of you today. Hope all is well in your world!",
                "Just checking in on you! How's everything going lately?"
            )

            WingmanVibe.FUNNY -> listOf(
                if (firstPoint != null)
                    "My brain just pinged me about $firstPoint! Proof of life check: how is it going? 😂"
                else
                    "My brain suddenly realized it's been way too long! Proof of life check: how are you doing? 😂",
                if (custom != null)
                    "Emergency alert: we need to discuss $custom immediately haha. How are things?"
                else
                    "Hey! Realized I haven't said hi in forever. Sending good vibes and zero expectations haha",
                "Emerging from the void to say hey $firstName! Hope you're surviving the week."
            )

            WingmanVibe.CELEBRATE -> listOf(
                if (firstPoint != null)
                    "Huge congratulations on $firstPoint $firstName! So excited for you 🎉"
                else
                    "Hey $firstName! Just wanted to send some good energy and celebrate you today! 🎉",
                if (custom != null)
                    "So hyped about $custom! We definitely need to celebrate soon!"
                else
                    "Sending you big high fives and good vibes! Hope you're celebrating your wins.",
                "Just wanted to drop some love and hype your way $firstName! Keep crushing it."
            )

            WingmanVibe.DEEP_RECONNECT -> listOf(
                if (firstPoint != null)
                    "Hey $firstName, I was reflecting on things and was thinking of you and $firstPoint. Really appreciate you."
                else
                    "Hey $firstName, I know it's been a while since we connected, but you've been on my mind. Hope you're taking good care of yourself.",
                if (custom != null)
                    "Hey $firstName, wanted to reach out sincerely about $custom. Really value our relationship."
                else
                    "Hey $firstName! Life gets crazy fast, but I really value our connection and wanted to send love your way.",
                "Just wanted to drop a heartfelt note to let you know I'm thinking of you and rooting for you."
            )
        }
    }
}
