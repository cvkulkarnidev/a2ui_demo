package dev.cvkulkarnidev.a2ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { A2UITheme { A2UIPlayground() } }
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

    fun clear() {
        surfaces = emptyMap()
    }

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
            }.getOrNull() != null -> json.parseToJsonElement(trimmed)
                .jsonObject.getValue("messages").jsonArray.toList()
            else -> trimmed.lineSequence()
                .filter { it.isNotBlank() }
                .map { json.parseToJsonElement(it) }
                .toList()
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
    var showJsonIr by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastAction by remember { mutableStateOf("No action yet") }

    fun renderCurrent() {
        error = null
        runCatching { processor.replace(source) }
            .onFailure { error = it.message }
    }

    LaunchedEffect(Unit) { renderCurrent() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("A2UI Studio", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Native Android renderer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = OneUiTokens.screenHorizontalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXl)
        ) {
            Text("Examples", style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = !menuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedExample.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose a scenario") },
                    supportingText = { Text(selectedExample.description) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
                    shape = RoundedCornerShape(OneUiTokens.radiusLarge),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    A2UI_EXAMPLES.forEach { example ->
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXs)) {
                                    Text(example.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        example.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

            error?.let { UnsupportedMessage(it) }

            if (processor.surfaces.isEmpty()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(OneUiTokens.radiusSurface)
                ) {
                    Text("No active surface", Modifier.padding(OneUiTokens.spaceXxl))
                }
            } else {
                processor.surfaces.values.forEach { surface ->
                    A2UISurfaceHost(surface) { action -> lastAction = action.toString() }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(OneUiTokens.radiusLarge),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(OneUiTokens.spaceXl),
                    verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceMd)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Developer tools", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Inspect or edit the JSON IR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = { showJsonIr = !showJsonIr }) {
                            Text(if (showJsonIr) "Hide IR" else "Show IR")
                        }
                    }

                    if (showJsonIr) {
                        OutlinedTextField(
                            value = source,
                            onValueChange = { source = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                            label = { Text("A2UI v0.9 JSONL") },
                            shape = RoundedCornerShape(OneUiTokens.radiusMedium)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(OneUiTokens.spaceSm),
                            verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceSm)
                        ) {
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
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(OneUiTokens.radiusMedium),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            ) {
                Column(
                    Modifier.padding(OneUiTokens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXs)
                ) {
                    Text("Last action", style = MaterialTheme.typography.labelLarge)
                    Text(lastAction, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(OneUiTokens.screenBottomPadding))
        }
    }
}

@Composable
private fun A2UISurfaceHost(surface: SurfaceState, onAction: (JsonObject) -> Unit) {
    val catalog = A2UI_CATALOGS[surface.catalogId]
    val root = surface.components["root"]

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneUiTokens.radiusSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(OneUiTokens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Live surface", style = MaterialTheme.typography.labelLarge)
                    Text(
                        surface.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            catalog?.displayName ?: surface.catalogId,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            when {
                catalog == null -> UnsupportedMessage("Catalog '${surface.catalogId}' is not registered")
                root == null -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Waiting for the root component…")
                }
                else -> RenderCatalogComponent(surface, root, catalog, onAction)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderCatalogComponent(
    surface: SurfaceState,
    component: JsonObject,
    catalog: A2UICatalog,
    onAction: (JsonObject) -> Unit
) {
    val type = component["component"]?.jsonPrimitive?.content ?: "Unknown"
    if (!catalog.supports(type)) {
        UnsupportedMessage("Component '$type' is not registered in ${catalog.displayName}")
        return
    }

    when (type) {
        "Text" -> {
            val variant = component["variant"]?.jsonPrimitive?.content
            Text(
                text = resolveString(component["text"], surface.data),
                style = when (variant) {
                    "h1" -> MaterialTheme.typography.headlineLarge
                    "h2" -> MaterialTheme.typography.headlineMedium
                    "h3" -> MaterialTheme.typography.headlineSmall
                    "caption" -> MaterialTheme.typography.bodySmall
                    "label" -> MaterialTheme.typography.labelLarge
                    else -> MaterialTheme.typography.bodyLarge
                },
                color = when (component["tone"]?.jsonPrimitive?.content) {
                    "secondary" -> MaterialTheme.colorScheme.secondary
                    "muted" -> MaterialTheme.colorScheme.onSurfaceVariant
                    "accent" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (variant?.startsWith("h") == true) FontWeight.Bold else null
            )
        }

        "Column" -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(
                component["spacing"]?.jsonPrimitive?.intOrNull?.dp ?: OneUiTokens.spaceMd
            ),
            horizontalAlignment = parseHorizontal(component["align"]?.jsonPrimitive?.content)
        ) {
            renderChildren(surface, component, catalog, onAction)
        }

        "Row" -> {
            val horizontal = when (component["justify"]?.jsonPrimitive?.content) {
                "spaceBetween" -> Arrangement.SpaceBetween
                "center" -> Arrangement.Center
                "end" -> Arrangement.End
                else -> Arrangement.spacedBy(OneUiTokens.spaceMd)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = horizontal,
                verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceMd),
                maxItemsInEachRow = component["maxItems"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
            ) {
                renderChildren(surface, component, catalog, onAction)
            }
        }

        "Card" -> {
            val variant = component["variant"]?.jsonPrimitive?.content
            val childId = component["child"]?.jsonPrimitive?.contentOrNull
            val cardContent: @Composable ColumnScope.() -> Unit = {
                childId?.let(surface.components::get)?.let { child ->
                    Box(Modifier.padding(OneUiTokens.spaceLg)) {
                        RenderCatalogComponent(surface, child, catalog, onAction)
                    }
                }
            }

            when (variant) {
                "outlined" -> OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(OneUiTokens.radiusLarge),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    ),
                    content = cardContent
                )

                "tonal" -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(OneUiTokens.radiusLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    content = cardContent
                )

                else -> ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(OneUiTokens.radiusLarge),
                    content = cardContent
                )
            }
        }

        "Button" -> {
            val childId = component["child"]?.jsonPrimitive?.contentOrNull
            val buttonContent: @Composable RowScope.() -> Unit = {
                val child = childId?.let(surface.components::get)
                if (child != null) {
                    RenderCatalogComponent(surface, child, catalog, onAction)
                } else {
                    Text(resolveString(component["label"], surface.data).ifBlank { "Action" })
                }
            }

            when (component["variant"]?.jsonPrimitive?.content) {
                "borderless" -> TextButton(
                    onClick = { onAction(buildAction(surface, component)) },
                    content = buttonContent
                )

                "secondary" -> FilledTonalButton(
                    onClick = { onAction(buildAction(surface, component)) },
                    content = buttonContent
                )

                "outlined" -> OutlinedButton(
                    onClick = { onAction(buildAction(surface, component)) },
                    content = buttonContent
                )

                else -> Button(
                    onClick = { onAction(buildAction(surface, component)) },
                    shape = RoundedCornerShape(OneUiTokens.radiusMedium),
                    content = buttonContent
                )
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
                minLines = if (component["variant"]?.jsonPrimitive?.content == "longText") 3 else 1,
                shape = RoundedCornerShape(OneUiTokens.radiusMedium)
            )
        }

        "CheckBox" -> {
            var checked by remember(component["id"]) {
                mutableStateOf(resolveBoolean(component["value"], surface.data))
            }
            Surface(
                shape = RoundedCornerShape(OneUiTokens.radiusMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = OneUiTokens.minTouchTarget)
                        .padding(horizontal = OneUiTokens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked, onCheckedChange = { checked = it })
                    Text(resolveString(component["label"], surface.data))
                }
            }
        }

        "Slider" -> {
            val min = component["min"]?.jsonPrimitive?.floatOrNull ?: 0f
            val max = component["max"]?.jsonPrimitive?.floatOrNull ?: 100f
            var value by remember(component["id"]) {
                mutableFloatStateOf(component["value"]?.jsonPrimitive?.floatOrNull ?: min)
            }
            Column(verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXs)) {
                Text(
                    resolveString(component["label"], surface.data)
                        .ifBlank { value.toInt().toString() },
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(value, { value = it }, valueRange = min..max)
            }
        }

        "Divider" -> HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        "Image" -> AsyncImage(
            model = resolveString(component["url"], surface.data),
            contentDescription = resolveString(component["alt"], surface.data),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = OneUiTokens.heroMinHeight,
                    max = OneUiTokens.heroMaxHeight
                )
                .clip(RoundedCornerShape(OneUiTokens.radiusLarge)),
            contentScale = ContentScale.Crop
        )

        "Icon" -> Surface(
            shape = RoundedCornerShape(OneUiTokens.radiusSmall),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = "● ${component["name"]?.jsonPrimitive?.content ?: "icon"}",
                modifier = Modifier.padding(
                    horizontal = OneUiTokens.spaceMd,
                    vertical = OneUiTokens.spaceSm
                ),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        "Chip" -> {
            val tone = component["tone"]?.jsonPrimitive?.content
            Surface(
                modifier = Modifier.widthIn(max = OneUiTokens.compactChipMaxWidth),
                shape = RoundedCornerShape(50),
                color = when (tone) {
                    "success" -> MaterialTheme.colorScheme.secondaryContainer
                    "warning" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Text(
                    text = resolveString(component["label"], surface.data),
                    modifier = Modifier.padding(
                        horizontal = OneUiTokens.spaceMd,
                        vertical = OneUiTokens.spaceSm
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        "ProgressBar" -> {
            val value = component["value"]?.jsonPrimitive?.floatOrNull
                ?.coerceIn(0f, 1f) ?: 0f
            Column(verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceSm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        resolveString(component["label"], surface.data),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(OneUiTokens.spaceSm))
                    Text(
                        "${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                LinearProgressIndicator(
                    progress = { value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        "Metric" -> Surface(
            modifier = Modifier
                .widthIn(
                    min = OneUiTokens.metricMinWidth,
                    max = OneUiTokens.metricMaxWidth
                )
                .defaultMinSize(minHeight = 112.dp),
            shape = RoundedCornerShape(OneUiTokens.radiusLarge),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier.padding(OneUiTokens.spaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = resolveString(component["value"], surface.data),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(OneUiTokens.spaceXs))
                Text(
                    text = resolveString(component["label"], surface.data),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UnsupportedMessage(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(OneUiTokens.radiusMedium)
    ) {
        Text(
            message,
            Modifier.fillMaxWidth().padding(OneUiTokens.spaceLg),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun renderChildren(
    surface: SurfaceState,
    component: JsonObject,
    catalog: A2UICatalog,
    onAction: (JsonObject) -> Unit
) {
    component["children"]?.jsonArray
        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        .orEmpty()
        .forEach { id ->
            surface.components[id]?.let {
                RenderCatalogComponent(surface, it, catalog, onAction)
            }
        }
}

private fun buildAction(surface: SurfaceState, component: JsonObject): JsonObject {
    val action = component["action"]?.jsonObject
        ?: buildJsonObject { put("name", "click") }

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
        ?.let { lookup(data, it) }
        ?.jsonPrimitive
        ?.contentOrNull ?: ""
    else -> value.toString()
}

private fun resolveBoolean(value: JsonElement?, data: JsonElement): Boolean = when (value) {
    is JsonPrimitive -> value.booleanOrNull ?: false
    is JsonObject -> value["path"]?.jsonPrimitive?.content
        ?.let { lookup(data, it) }
        ?.jsonPrimitive
        ?.booleanOrNull ?: false
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
        return JsonObject(
            obj.toMutableMap().apply {
                put(key, update(obj[key], index + 1))
            }
        )
    }

    return update(root, 0)
}

private fun parseHorizontal(value: String?): Alignment.Horizontal = when (value) {
    "center" -> Alignment.CenterHorizontally
    "end" -> Alignment.End
    else -> Alignment.Start
}
