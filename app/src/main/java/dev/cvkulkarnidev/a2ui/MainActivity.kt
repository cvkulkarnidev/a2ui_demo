package dev.cvkulkarnidev.a2ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { A2UIPlayground() } }
    }
}

private data class SurfaceState(
    val id: String,
    val catalogId: String,
    val sendDataModel: Boolean,
    val components: Map<String, JsonObject> = emptyMap(),
    val data: JsonElement = buildJsonObject {}
)

private class A2UIProcessor {
    var surfaces by mutableStateOf<Map<String, SurfaceState>>(emptyMap())
        private set

    fun clear() { surfaces = emptyMap() }

    fun replace(input: String) {
        clear()
        process(input)
    }

    fun process(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val messages = when {
            trimmed.startsWith("[") -> json.parseToJsonElement(trimmed).jsonArray.toList()
            trimmed.startsWith("{") && runCatching {
                json.parseToJsonElement(trimmed).jsonObject["messages"]
            }.getOrNull() != null -> json.parseToJsonElement(trimmed).jsonObject["messages"]!!.jsonArray.toList()
            else -> trimmed.lineSequence().filter { it.isNotBlank() }
                .map { json.parseToJsonElement(it) }.toList()
        }
        messages.forEach { processMessage(it.jsonObject) }
    }

    private fun processMessage(message: JsonObject) {
        require(message["version"]?.jsonPrimitive?.content == "v0.9") {
            "Only A2UI v0.9 is supported"
        }
        when {
            "createSurface" in message -> {
                val body = message.getValue("createSurface").jsonObject
                val id = body.getValue("surfaceId").jsonPrimitive.content
                surfaces = surfaces + (id to SurfaceState(
                    id = id,
                    catalogId = body["catalogId"]?.jsonPrimitive?.content ?: "basic",
                    sendDataModel = body["sendDataModel"]?.jsonPrimitive?.booleanOrNull ?: false
                ))
            }
            "updateComponents" in message -> {
                val body = message.getValue("updateComponents").jsonObject
                val id = body.getValue("surfaceId").jsonPrimitive.content
                val old = surfaces[id] ?: error("Unknown surface: $id")
                val updates = body.getValue("components").jsonArray.associate { element ->
                    val component = element.jsonObject
                    component.getValue("id").jsonPrimitive.content to component
                }
                surfaces = surfaces + (id to old.copy(components = old.components + updates))
            }
            "updateDataModel" in message -> {
                val body = message.getValue("updateDataModel").jsonObject
                val id = body.getValue("surfaceId").jsonPrimitive.content
                val old = surfaces[id] ?: error("Unknown surface: $id")
                val path = body["path"]?.jsonPrimitive?.content ?: ""
                val value = body["value"] ?: JsonNull
                surfaces = surfaces + (id to old.copy(data = setAtPath(old.data, path, value)))
            }
            "deleteSurface" in message -> {
                val id = message.getValue("deleteSurface").jsonObject
                    .getValue("surfaceId").jsonPrimitive.content
                surfaces = surfaces - id
            }
            else -> error("Unsupported A2UI message")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun A2UIPlayground() {
    val processor = remember { A2UIProcessor() }
    var selectedExample by remember { mutableStateOf(A2UI_EXAMPLES.first()) }
    var source by remember { mutableStateOf(selectedExample.jsonl) }
    var menuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastAction by remember { mutableStateOf("No action yet") }

    fun renderCurrent() {
        error = null
        runCatching { processor.replace(source) }.onFailure { error = it.message }
    }

    LaunchedEffect(Unit) { renderCurrent() }

    Scaffold(topBar = { TopAppBar(title = { Text("A2UI Android Renderer") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Example gallery", style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = !menuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedExample.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose an example") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    A2UI_EXAMPLES.forEach { example ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(example.name, fontWeight = FontWeight.SemiBold)
                                    Text(example.description, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                selectedExample = example
                                source = example.jsonl
                                menuExpanded = false
                                lastAction = "No action yet"
                                error = null
                                runCatching { processor.replace(example.jsonl) }
                                    .onFailure { error = it.message }
                            }
                        )
                    }
                }
            }

            Text(selectedExample.description, style = MaterialTheme.typography.bodySmall)
            Text("A2UI v0.9 JSONL", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                label = { Text("Messages") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { renderCurrent() }) { Text("Render") }
                OutlinedButton(onClick = {
                    processor.clear()
                    error = null
                    lastAction = "No action yet"
                }) { Text("Clear") }
                OutlinedButton(onClick = {
                    source = selectedExample.jsonl
                    renderCurrent()
                }) { Text("Reset") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            processor.surfaces.values.forEach { surface ->
                SurfaceView(surface) { action -> lastAction = action.toString() }
            }
            Text("Last action", style = MaterialTheme.typography.labelLarge)
            Text(lastAction, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SurfaceView(surface: SurfaceState, onAction: (JsonObject) -> Unit) {
    val root = surface.components["root"]
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Surface: ${surface.id}", style = MaterialTheme.typography.labelMedium)
            if (root == null) Text("Waiting for root component…")
            else RenderComponent(surface, root, onAction)
        }
    }
}

@Composable
private fun RenderComponent(
    surface: SurfaceState,
    component: JsonObject,
    onAction: (JsonObject) -> Unit
) {
    when (val type = component["component"]?.jsonPrimitive?.content ?: "Unknown") {
        "Text" -> {
            val variant = component["variant"]?.jsonPrimitive?.content
            Text(
                resolveString(component["text"], surface.data),
                style = when (variant) {
                    "h1" -> MaterialTheme.typography.headlineLarge
                    "h2" -> MaterialTheme.typography.headlineMedium
                    "h3" -> MaterialTheme.typography.headlineSmall
                    "caption" -> MaterialTheme.typography.bodySmall
                    else -> MaterialTheme.typography.bodyLarge
                },
                fontWeight = if (variant?.startsWith("h") == true) FontWeight.Bold else null
            )
        }
        "Column" -> Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = parseHorizontal(component["align"]?.jsonPrimitive?.content)
        ) { renderChildren(surface, component, onAction) }
        "Row" -> Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { renderChildren(surface, component, onAction) }
        "Card" -> Card(Modifier.fillMaxWidth()) {
            component["child"]?.jsonPrimitive?.contentOrNull?.let { id ->
                surface.components[id]?.let { child ->
                    Box(Modifier.padding(16.dp)) { RenderComponent(surface, child, onAction) }
                }
            }
        }
        "Button" -> {
            val childId = component["child"]?.jsonPrimitive?.contentOrNull
            Button(onClick = { onAction(buildAction(surface, component)) }) {
                val child = childId?.let(surface.components::get)
                if (child != null) RenderComponent(surface, child, onAction)
                else Text(resolveString(component["label"], surface.data).ifBlank { "Action" })
            }
        }
        "TextField" -> {
            val initial = resolveString(component["value"], surface.data)
            var value by remember(component["id"], initial) { mutableStateOf(initial) }
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(resolveString(component["label"], surface.data)) },
                minLines = if (component["variant"]?.jsonPrimitive?.content == "longText") 3 else 1
            )
        }
        "CheckBox" -> {
            var checked by remember(component["id"]) {
                mutableStateOf(resolveBoolean(component["value"], surface.data))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, onCheckedChange = { checked = it })
                Text(resolveString(component["label"], surface.data))
            }
        }
        "Slider" -> {
            val min = component["min"]?.jsonPrimitive?.floatOrNull ?: 0f
            val max = component["max"]?.jsonPrimitive?.floatOrNull ?: 100f
            var value by remember(component["id"]) {
                mutableFloatStateOf(component["value"]?.jsonPrimitive?.floatOrNull ?: min)
            }
            Column {
                Text(value.toInt().toString())
                Slider(value, { value = it }, valueRange = min..max)
            }
        }
        "Divider" -> HorizontalDivider()
        "Image" -> AsyncImage(
            model = resolveString(component["url"], surface.data),
            contentDescription = resolveString(component["alt"], surface.data),
            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
            contentScale = ContentScale.Crop
        )
        "Icon" -> Text("◉ ${component["name"]?.jsonPrimitive?.content ?: "icon"}")
        else -> Text("Unsupported component: $type", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun renderChildren(
    surface: SurfaceState,
    component: JsonObject,
    onAction: (JsonObject) -> Unit
) {
    component["children"]?.jsonArray
        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        .orEmpty()
        .forEach { id -> surface.components[id]?.let { RenderComponent(surface, it, onAction) } }
}

private fun buildAction(surface: SurfaceState, component: JsonObject): JsonObject {
    val action = component["action"]?.jsonObject ?: buildJsonObject { put("name", "click") }
    return buildJsonObject {
        put("version", "v0.9")
        putJsonObject("action") {
            put("surfaceId", surface.id)
            action.forEach { (key, value) -> put(key, value) }
            if (surface.sendDataModel) put("dataModel", surface.data)
        }
    }
}

private fun resolveString(value: JsonElement?, data: JsonElement): String = when (value) {
    null, JsonNull -> ""
    is JsonPrimitive -> value.content
    is JsonObject -> value["path"]?.jsonPrimitive?.content
        ?.let { lookup(data, it) }?.jsonPrimitive?.contentOrNull ?: ""
    else -> value.toString()
}

private fun resolveBoolean(value: JsonElement?, data: JsonElement): Boolean = when (value) {
    is JsonPrimitive -> value.booleanOrNull ?: false
    is JsonObject -> value["path"]?.jsonPrimitive?.content
        ?.let { lookup(data, it) }?.jsonPrimitive?.booleanOrNull ?: false
    else -> false
}

private fun lookup(root: JsonElement, path: String): JsonElement? {
    if (path.isBlank() || path == "/") return root
    return path.trim('/').split('/').fold(root as JsonElement?) { current, key ->
        when (current) {
            is JsonObject -> current[key]
            is JsonArray -> key.toIntOrNull()?.let(current::getOrNull)
            else -> null
        }
    }
}

private fun setAtPath(root: JsonElement, path: String, value: JsonElement): JsonElement {
    if (path.isBlank() || path == "/") return value
    val keys = path.trim('/').split('/')
    fun update(current: JsonElement?, index: Int): JsonElement {
        if (index == keys.size) return value
        val key = keys[index]
        val obj = current as? JsonObject ?: buildJsonObject {}
        return JsonObject(obj.toMutableMap().apply { put(key, update(obj[key], index + 1)) })
    }
    return update(root, 0)
}

private fun parseHorizontal(value: String?): Alignment.Horizontal = when (value) {
    "center" -> Alignment.CenterHorizontally
    "end" -> Alignment.End
    else -> Alignment.Start
}
