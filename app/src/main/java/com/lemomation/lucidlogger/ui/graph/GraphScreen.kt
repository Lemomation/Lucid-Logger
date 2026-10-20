package com.lemomation.lucidlogger.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lemomation.lucidlogger.AppContainer
import com.lemomation.lucidlogger.data.graph.EntityCategory
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.ui.components.EmptyState
import com.lemomation.lucidlogger.ui.components.LucidCard
import com.lemomation.lucidlogger.ui.components.LucidTopBar
import com.lemomation.lucidlogger.ui.components.SectionHeader
import com.lemomation.lucidlogger.ui.components.TagChip
import com.lemomation.lucidlogger.ui.theme.LucidAmber
import com.lemomation.lucidlogger.ui.theme.LucidBorder
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidDreamSign
import com.lemomation.lucidlogger.ui.theme.LucidEmotion
import com.lemomation.lucidlogger.ui.theme.LucidLocation
import com.lemomation.lucidlogger.ui.theme.LucidPeople
import com.lemomation.lucidlogger.ui.theme.LucidPrimary
import com.lemomation.lucidlogger.ui.theme.LucidSurface
import com.lemomation.lucidlogger.ui.theme.LucidText
import com.lemomation.lucidlogger.ui.theme.LucidTextSubtle
import com.lemomation.lucidlogger.ui.theme.LucidTextVariant
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun GraphScreen(
    container: AppContainer,
    notes: List<DreamNote>,
    onOpenNote: (DreamNote) -> Unit,
) {
    val storedCategories by container.entityCategories.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var enabledNames by rememberSaveable { mutableStateOf(EntityCategory.entries.joinToString(",") { it.name }) }
    val enabled = remember(enabledNames) {
        enabledNames.split(',').mapNotNull { runCatching { EntityCategory.valueOf(it) }.getOrNull() }.toSet()
    }

    val graph = remember(notes, storedCategories, enabled) { buildGraph(notes, storedCategories, enabled) }
    val selected = graph.nodes.firstOrNull { it.name == selectedName }
    val related = remember(notes, selected) {
        selected?.let { node ->
            notes.filter { note -> node.name in note.links || node.name in note.tags }
                .sortedByDescending { it.created.toInstant() }
        }.orEmpty()
    }

    Column(Modifier.fillMaxSize().background(LucidCanvas)) {
        LucidTopBar(title = "Dream Graph")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${graph.nodes.size} recurring motifs",
                    style = MaterialTheme.typography.headlineSmall,
                    color = LucidText,
                )
                Text(
                    "· ${graph.edges.size} links",
                    style = MaterialTheme.typography.bodySmall,
                    color = LucidTextSubtle,
                )
            }
            Text(
                "Dream signs that keep returning. Tap a node to see the nights it appeared.",
                style = MaterialTheme.typography.bodySmall,
                color = LucidTextVariant,
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                EntityCategory.entries.forEach { category ->
                    CategoryPill(
                        category = category,
                        selected = category in enabled,
                        onClick = {
                            val next = if (category in enabled) {
                                if (enabled.size == 1) EntityCategory.entries.toSet() else enabled - category
                            } else {
                                enabled + category
                            }
                            enabledNames = next.joinToString(",") { it.name }
                        },
                    )
                }
            }

            if (graph.nodes.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Hub,
                    title = "No motifs yet",
                    message = "Add [[wikilinks]] or #tags to your dreams. Recurring people, places, and feelings will gather here.",
                )
            } else {
                GraphCanvas(
                    graph = graph,
                    selectedName = selectedName,
                    onSelect = { selectedName = it },
                )

                if (selected == null) {
                    Text(
                        "Tap any star to inspect it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LucidTextSubtle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LucidCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor(selected.category)),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                selected.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = LucidText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${selected.count} ${if (selected.count == 1) "night" else "nights"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LucidTextSubtle,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Classify as",
                            style = MaterialTheme.typography.labelSmall,
                            color = LucidTextSubtle,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            EntityCategory.entries.forEach { category ->
                                CategoryPill(
                                    category = category,
                                    selected = category == selected.category,
                                    onClick = {
                                        scope.launch { container.entityCategories.setCategory(selected.name, category) }
                                    },
                                )
                            }
                        }
                        if (related.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            SectionHeader("Appears in")
                            Spacer(Modifier.height(4.dp))
                            related.take(5).forEach { note ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenNote(note) }
                                        .padding(vertical = 7.dp, horizontal = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        note.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LucidTextVariant,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        note.date.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LucidTextSubtle,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GraphCanvas(graph: GraphData, selectedName: String?, onSelect: (String?) -> Unit) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(graph, size, density) {
        computeLayout(graph, size.width.toFloat(), size.height.toFloat(), density)
    }
    val labelStyle = TextStyle(fontSize = 10.sp, color = LucidTextVariant, fontWeight = FontWeight.Medium)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LucidSurface)
            .border(1.dp, LucidBorder, RoundedCornerShape(18.dp))
            .onSizeChanged { size = it }
            .pointerInput(layout) {
                detectTapGestures { tap ->
                    val hit = layout.nodes.minByOrNull { node ->
                        (node.position - tap).getDistance()
                    }
                    onSelect(
                        hit?.takeIf { (it.position - tap).getDistance() <= it.radius * 2.4f }?.node?.name,
                    )
                }
            },
    ) {
        drawCircle(
            color = LucidPrimary.copy(alpha = 0.05f),
            radius = this.size.minDimension * 0.44f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = LucidPrimary.copy(alpha = 0.035f),
            radius = this.size.minDimension * 0.29f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )

        layout.edges.forEach { edge ->
            val a = edge.first.position
            val b = edge.second.position
            drawLine(
                color = LucidPrimary.copy(alpha = 0.18f),
                start = a,
                end = b,
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        layout.nodes.forEach { positioned ->
            val selected = positioned.node.name == selectedName
            val color = categoryColor(positioned.node.category)
            if (selected) {
                drawCircle(color = color.copy(alpha = 0.16f), radius = positioned.radius * 2.4f, center = positioned.position)
            }
            drawCircle(
                color = color.copy(alpha = if (selected) 0.95f else 0.78f),
                radius = positioned.radius,
                center = positioned.position,
            )
            drawCircle(
                color = if (selected) LucidText else color.copy(alpha = 0.35f),
                radius = positioned.radius,
                center = positioned.position,
                style = Stroke(width = if (selected) 2.dp.toPx() else 1.dp.toPx()),
            )
            val label = textMeasurer.measure(text = positioned.node.name, style = labelStyle, maxLines = 1)
            val x = (positioned.position.x - label.size.width / 2f)
                .coerceIn(6.dp.toPx(), this.size.width - label.size.width - 6.dp.toPx())
            val y = (positioned.position.y + positioned.radius + 5.dp.toPx())
                .coerceAtMost(this.size.height - label.size.height - 2.dp.toPx())
            drawText(textLayoutResult = label, topLeft = Offset(x, y))
        }
    }
}

@Composable
private fun CategoryPill(category: EntityCategory, selected: Boolean, onClick: () -> Unit) {
    val color = categoryColor(category)
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (selected) color.copy(alpha = 0.45f) else LucidBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (selected) color else LucidTextSubtle))
        Text(
            category.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else LucidTextVariant,
        )
    }
}

private data class GraphNode(
    val name: String,
    val count: Int,
    val category: EntityCategory,
)

private data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<Pair<GraphNode, GraphNode>>,
)

private data class PositionedNode(
    val node: GraphNode,
    val position: Offset,
    val radius: Float,
)

private data class GraphLayout(
    val nodes: List<PositionedNode>,
    val edges: List<Pair<PositionedNode, PositionedNode>>,
)

private fun buildGraph(
    notes: List<DreamNote>,
    storedCategories: Map<String, EntityCategory>,
    enabled: Set<EntityCategory>,
): GraphData {
    val frequencies = mutableMapOf<String, Int>()
    notes.forEach { note ->
        (note.links + note.tags).distinct().forEach { name ->
            if (name.isNotBlank()) frequencies[name] = (frequencies[name] ?: 0) + 1
        }
    }
    val allNodes = frequencies.map { (name, count) ->
        val category = storedCategories[name.lowercase()] ?: inferCategory(name)
        GraphNode(name = name, count = count, category = category)
    }.filter { it.category in enabled }
        .sortedWith(compareBy({ it.category.ordinal }, { -it.count }, { it.name.lowercase() }))

    val byName = allNodes.associateBy { it.name }
    val edgeKeys = mutableSetOf<Pair<String, String>>()
    notes.forEach { note ->
        val names = (note.links + note.tags).distinct().filter { it in byName }
        for (i in names.indices) {
            for (j in i + 1 until names.size) {
                val a = names[i]
                val b = names[j]
                edgeKeys += if (a <= b) a to b else b to a
            }
        }
    }
    val edges = edgeKeys.mapNotNull { (a, b) ->
        val first = byName[a] ?: return@mapNotNull null
        val second = byName[b] ?: return@mapNotNull null
        first to second
    }
    return GraphData(allNodes, edges)
}

private fun computeLayout(graph: GraphData, width: Float, height: Float, density: androidx.compose.ui.unit.Density): GraphLayout {
    if (width <= 0f || height <= 0f || graph.nodes.isEmpty()) return GraphLayout(emptyList(), emptyList())
    val count = graph.nodes.size
    val pad = with(density) { 34.dp.toPx() }
    val center = Offset(width / 2f, height / 2f)
    val initialRadius = minOf(width, height) * 0.31f
    val points = graph.nodes.mapIndexed { index, _ ->
        val angle = (Math.PI * 2 * index / count) - Math.PI / 2
        Offset(
            center.x + (Math.cos(angle) * initialRadius).toFloat(),
            center.y + (Math.sin(angle) * initialRadius).toFloat(),
        )
    }.toMutableList()
    val indexOf = graph.nodes.withIndex().associate { (index, node) -> node.name to index }
    val edgePairs = graph.edges.mapNotNull { (a, b) ->
        val ia = indexOf[a.name] ?: return@mapNotNull null
        val ib = indexOf[b.name] ?: return@mapNotNull null
        ia to ib
    }
    val k = sqrt((width * height) / (count * 1.8f))
    repeat(90) {
        val forces = MutableList(count) { Offset.Zero }
        for (i in 0 until count) {
            for (j in i + 1 until count) {
                var dx = points[i].x - points[j].x
                var dy = points[i].y - points[j].y
                var dist = sqrt(dx * dx + dy * dy)
                if (dist < 0.5f) {
                    dx = (i + 1) * 0.7f
                    dy = (j + 1) * 0.7f
                    dist = sqrt(dx * dx + dy * dy)
                }
                val force = (k * k) / dist.coerceAtLeast(1f)
                val fx = dx / dist * force
                val fy = dy / dist * force
                forces[i] += Offset(fx, fy)
                forces[j] -= Offset(fx, fy)
            }
        }
        edgePairs.forEach { (a, b) ->
            val dx = points[a].x - points[b].x
            val dy = points[a].y - points[b].y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val force = (dist * dist) / k
            val fx = dx / dist * force
            val fy = dy / dist * force
            forces[a] -= Offset(fx, fy)
            forces[b] += Offset(fx, fy)
        }
        for (i in 0 until count) {
            val pull = Offset((center.x - points[i].x) * 0.018f, (center.y - points[i].y) * 0.018f)
            val step = forces[i] + pull
            val length = sqrt(step.x * step.x + step.y * step.y)
            val limited = if (length > 12f) Offset(step.x / length * 12f, step.y / length * 12f) else step
            points[i] = Offset(
                (points[i].x + limited.x).coerceIn(pad, width - pad),
                (points[i].y + limited.y).coerceIn(pad, height - pad),
            )
        }
    }
    val positioned = graph.nodes.mapIndexed { index, node ->
        PositionedNode(
            node = node,
            position = points[index],
            radius = with(density) { (8 + minOf(node.count, 7) * 1.15f).dp.toPx() },
        )
    }
    val edges = edgePairs.mapNotNull { (a, b) ->
        val first = positioned.getOrNull(a) ?: return@mapNotNull null
        val second = positioned.getOrNull(b) ?: return@mapNotNull null
        first to second
    }
    return GraphLayout(positioned, edges)
}

private fun inferCategory(name: String): EntityCategory {
    val value = name.lowercase()
    return when {
        listOf("ocean", "sea", "house", "home", "city", "forest", "mountain", "school", "room", "beach", "train", "station", "garden").any { value.contains(it) } -> EntityCategory.LOCATION
        listOf("mother", "father", "sister", "brother", "friend", "teacher", "stranger", "child", "woman", "man", "doctor").any { value.contains(it) } -> EntityCategory.PERSON
        listOf("fear", "joy", "anxiety", "calm", "love", "grief", "panic", "wonder", "sadness", "anger", "peace", "terror").any { value.contains(it) } -> EntityCategory.EMOTION
        else -> EntityCategory.DREAM_SIGN
    }
}

private fun categoryColor(category: EntityCategory): Color = when (category) {
    EntityCategory.LOCATION -> LucidLocation
    EntityCategory.DREAM_SIGN -> LucidDreamSign
    EntityCategory.PERSON -> LucidPeople
    EntityCategory.EMOTION -> LucidEmotion
}
