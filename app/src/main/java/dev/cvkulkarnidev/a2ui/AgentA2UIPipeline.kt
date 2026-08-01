package dev.cvkulkarnidev.a2ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface BasicLlmClient {
    fun complete(prompt: String): String
}

class LocalDraftLlmClient : BasicLlmClient {
    override fun complete(prompt: String): String {
        val cleanPrompt = prompt.trim().ifBlank { "this request" }
        return """
            Here is a clear response for: $cleanPrompt

            Key points:
            - Understand the user's intent and preserve the important context.
            - Return a concise answer first so the conversation remains fast.
            - Convert that answer into structured A2UI only after the basic response exists.

            Recommended next steps:
            - Validate the generated A2UI message sequence.
            - Render only registered catalog components.
            - Show an error card when the generated IR is incomplete or unsupported.
        """.trimIndent()
    }
}

data class AgentRenderResult(
    val basicResponse: String,
    val a2uiJsonl: String
)

object A2UIResponseConverter {
    fun convert(prompt: String, basicResponse: String): String {
        val title = prompt.cleanInline().ifBlank { "Assistant response" }.takeWords(9)
        val sections = extractSections(basicResponse)
        val summary = sections.firstOrNull()?.body
            ?: basicResponse.cleanInline().takeChars(220).ifBlank { "No response content." }
        val metrics = extractMetrics(basicResponse)
        val surfaceId = "llm-${(prompt + basicResponse).hashCode().toUInt().toString(16)}"

        val data = buildJsonObject {
            put("prompt", prompt.cleanInline())
            put("title", title)
            put("subtitle", "Converted from a plain LLM response into native A2UI")
            put("summary", summary)
            sections.forEachIndexed { index, section ->
                put("section${index}Title", section.title)
                put("section${index}Body", section.body)
            }
            metrics.forEachIndexed { index, metric ->
                put("metric${index}Value", metric.value)
                put("metric${index}Label", metric.label)
            }
        }

        val rootChildren = mutableListOf("headerCard", "summaryCard")
        if (metrics.isNotEmpty()) rootChildren += "metricRow"
        rootChildren += sections.indices.map { "section${it}Card" }
        rootChildren += listOf("divider", "actionRow")

        val components = mutableListOf<JsonElement>()
        components += component("root", "Column") {
            put("spacing", 16)
            put("children", stringArray(rootChildren))
        }
        components += component("headerCard", "Card") {
            put("variant", "tonal")
            put("child", "headerGroup")
        }
        components += component("headerGroup", "Column") {
            put("spacing", 10)
            put("children", stringArray("statusChip", "title", "subtitle", "promptLabel", "prompt"))
        }
        components += component("statusChip", "Chip") {
            put("label", "LLM -> A2UI")
            put("tone", "success")
        }
        components += text("title", "/title", "h2")
        components += text("subtitle", "/subtitle", tone = "muted")
        components += component("promptLabel", "Text") {
            put("text", "Prompt")
            put("variant", "label")
        }
        components += text("prompt", "/prompt", "caption", "muted")
        components += component("summaryCard", "Card") {
            put("variant", "outlined")
            put("child", "summary")
        }
        components += text("summary", "/summary")

        if (metrics.isNotEmpty()) {
            components += component("metricRow", "Row") {
                put("justify", "spaceBetween")
                put("maxItems", 3)
                put("children", stringArray(metrics.indices.map { "metric$it" }))
            }
            metrics.indices.forEach { index ->
                components += component("metric$index", "Metric") {
                    putPath("value", "/metric${index}Value")
                    putPath("label", "/metric${index}Label")
                }
            }
        }

        sections.forEachIndexed { index, _ ->
            components += component("section${index}Card", "Card") {
                put("child", "section${index}Group")
            }
            components += component("section${index}Group", "Column") {
                put("spacing", 8)
                put("children", stringArray("section${index}Title", "section${index}Body"))
            }
            components += text("section${index}Title", "/section${index}Title", "h3")
            components += text("section${index}Body", "/section${index}Body")
        }

        components += component("divider", "Divider")
        components += component("actionRow", "Row") {
            put("justify", "spaceBetween")
            put("maxItems", 2)
            put("children", stringArray("copyAction", "refineAction"))
        }
        components += component("copyActionLabel", "Text") { put("text", "Copy response") }
        components += component("copyAction", "Button") {
            put("variant", "outlined")
            put("child", "copyActionLabel")
            put("action", action("copy_basic_response", surfaceId))
        }
        components += component("refineActionLabel", "Text") { put("text", "Refine UI") }
        components += component("refineAction", "Button") {
            put("variant", "secondary")
            put("child", "refineActionLabel")
            put("action", action("request_a2ui_refinement", surfaceId))
        }

        val messages = listOf(
            buildJsonObject {
                put("version", "v0.9")
                put("createSurface", buildJsonObject {
                    put("surfaceId", surfaceId)
                    put("catalogId", "basic")
                    put("sendDataModel", true)
                })
            },
            buildJsonObject {
                put("version", "v0.9")
                put("updateDataModel", buildJsonObject {
                    put("surfaceId", surfaceId)
                    put("value", data)
                })
            },
            buildJsonObject {
                put("version", "v0.9")
                put("updateComponents", buildJsonObject {
                    put("surfaceId", surfaceId)
                    put("components", JsonArray(components))
                })
            }
        )

        return messages.joinToString("\n") { it.toString() }
    }

    fun run(prompt: String, llmClient: BasicLlmClient): AgentRenderResult {
        val basic = llmClient.complete(prompt)
        return AgentRenderResult(
            basicResponse = basic,
            a2uiJsonl = convert(prompt, basic)
        )
    }

    private data class Section(val title: String, val body: String)
    private data class MetricCandidate(val value: String, val label: String)

    private fun extractSections(text: String): List<Section> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        if (lines.isEmpty()) return emptyList()

        val sections = mutableListOf<Section>()
        var currentTitle = "Summary"
        val currentBody = mutableListOf<String>()

        fun flush() {
            val body = currentBody.joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .takeChars(420)
            if (body.isNotBlank()) sections += Section(currentTitle, body)
            currentBody.clear()
        }

        lines.forEach { line ->
            val normalized = line.removePrefix("-").removePrefix("*").trim()
            if (line.endsWith(":") && normalized.length <= 60) {
                flush()
                currentTitle = normalized.removeSuffix(":").takeChars(56)
            } else {
                currentBody += normalized
            }
        }
        flush()

        return sections.take(4).ifEmpty {
            listOf(Section("Summary", text.cleanInline().takeChars(420)))
        }
    }

    private fun extractMetrics(text: String): List<MetricCandidate> {
        val regex = Regex("""(?<![A-Za-z0-9])(\d+(?:\.\d+)?%?|\d+/\d+)(?![A-Za-z0-9])""")
        return regex.findAll(text)
            .take(3)
            .mapIndexed { index, match ->
                MetricCandidate(
                    value = match.value,
                    label = when (index) {
                        0 -> "Signal"
                        1 -> "Detail"
                        else -> "Reference"
                    }
                )
            }
            .toList()
    }

    private fun component(
        id: String,
        type: String,
        block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {}
    ) = buildJsonObject {
        put("id", id)
        put("component", type)
        block()
    }

    private fun text(
        id: String,
        path: String,
        variant: String? = null,
        tone: String? = null
    ) = component(id, "Text") {
        putPath("text", path)
        variant?.let { put("variant", it) }
        tone?.let { put("tone", it) }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putPath(key: String, path: String) {
        put(key, buildJsonObject { put("path", path) })
    }

    private fun stringArray(values: List<String>) = buildJsonArray {
        values.forEach { add(JsonPrimitive(it)) }
    }

    private fun stringArray(vararg values: String) = stringArray(values.toList())

    private fun action(name: String, surfaceId: String) = buildJsonObject {
        put("name", name)
        put("context", buildJsonObject { put("surfaceId", surfaceId) })
    }

    private fun String.cleanInline(): String =
        replace(Regex("\\s+"), " ").trim()

    private fun String.takeChars(max: Int): String =
        if (length <= max) this else take(max - 1).trimEnd() + "..."

    private fun String.takeWords(max: Int): String {
        val words = split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size <= max) this else words.take(max).joinToString(" ") + "..."
    }
}
