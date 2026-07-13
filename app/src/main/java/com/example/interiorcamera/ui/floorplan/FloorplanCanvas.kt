package com.example.interiorcamera.ui.floorplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun FloorplanCanvas(
    roomWidthCm: Float,
    roomDepthCm: Float,
    placedItems: List<ArPlacedItem>,
    onPlacedItemsChanged: (List<ArPlacedItem>) -> Unit,
    selectedItemName: String?,
    onSelectItem: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    // R3. Guideline offsets state variables (in Relative cm coordinates)
    var snapHorizontalGuide by remember { mutableStateOf<Float?>(null) }
    var snapVerticalGuide by remember { mutableStateOf<Float?>(null) }

    // State variables to track absolute canvas coordinates for rotation dragging
    var currentAnglePointerCanvasX by remember { mutableStateOf(0f) }
    var currentAnglePointerCanvasY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { coords ->
                canvasWidth = coords.size.width.toFloat()
                canvasHeight = coords.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    onSelectItem(null)
                }
            }
            .testTag("FloorplanCanvas")
    ) {
        // Draw grid and snapping dashed guide lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40f
            // Normal grid
            for (y in 0 until (size.height / gridSpacing).toInt()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(0f, y * gridSpacing),
                    end = androidx.compose.ui.geometry.Offset(size.width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }
            for (x in 0 until (size.width / gridSpacing).toInt()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(x * gridSpacing, 0f),
                    end = androidx.compose.ui.geometry.Offset(x * gridSpacing, size.height),
                    strokeWidth = 1f
                )
            }

            // R3. Draw dashed cyan/magenta alignment guide lines if snapping is active
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)

            snapVerticalGuide?.let { relX ->
                val (screenX, _) = FloorplanCoordinator.relativeToScreen(
                    relX, 0f, size.width, size.height, roomWidthCm, roomDepthCm
                )
                drawLine(
                    color = Color(0xFF00BCD4), // Cyan guide
                    start = androidx.compose.ui.geometry.Offset(screenX, 0f),
                    end = androidx.compose.ui.geometry.Offset(screenX, size.height),
                    strokeWidth = 3f,
                    pathEffect = dashEffect
                )
            }

            snapHorizontalGuide?.let { relZ ->
                val (_, screenY) = FloorplanCoordinator.relativeToScreen(
                    0f, relZ, size.width, size.height, roomWidthCm, roomDepthCm
                )
                drawLine(
                    color = Color(0xFFE91E63), // Pink/Magenta guide
                    start = androidx.compose.ui.geometry.Offset(0f, screenY),
                    end = androidx.compose.ui.geometry.Offset(size.width, screenY),
                    strokeWidth = 3f,
                    pathEffect = dashEffect
                )
            }

            // Draw connection line from selected item to rotation handle
            selectedItemName?.let { selName ->
                placedItems.find { it.name == selName }?.let { item ->
                    val (screenX, screenY) = FloorplanCoordinator.relativeToScreen(
                        item.offsetX,
                        item.offsetZ,
                        size.width,
                        size.height,
                        roomWidthCm,
                        roomDepthCm
                    )
                    val itemDepthPx = (item.depthCm / roomDepthCm) * size.height
                    
                    val rad = Math.toRadians(item.rotationDegrees.toDouble())
                    val sinVal = sin(rad).toFloat()
                    val cosVal = cos(rad).toFloat()
                    val handleDistancePx = with(density) { 35.dp.toPx() }
                    val topEdgeX = screenX + (itemDepthPx / 2f) * sinVal
                    val topEdgeY = screenY - (itemDepthPx / 2f) * cosVal
                    val handleScreenX = screenX + (itemDepthPx / 2f + handleDistancePx) * sinVal
                    val handleScreenY = screenY - (itemDepthPx / 2f + handleDistancePx) * cosVal
                    
                    drawLine(
                        color = Color(0xFF3F51B5), // Brand primary Indigo
                        start = androidx.compose.ui.geometry.Offset(topEdgeX, topEdgeY),
                        end = androidx.compose.ui.geometry.Offset(handleScreenX, handleScreenY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                    )
                }
            }
        }

        // Render furniture blocks
        placedItems.forEach { item ->
            val isSelected = item.name == selectedItemName
            val itemWidthPx = (item.widthCm / roomWidthCm) * canvasWidth
            val itemDepthPx = (item.depthCm / roomDepthCm) * canvasHeight

            val (screenX, screenY) = FloorplanCoordinator.relativeToScreen(
                item.offsetX,
                item.offsetZ,
                canvasWidth,
                canvasHeight,
                roomWidthCm,
                roomDepthCm
            )

            val currentPlacedItems by rememberUpdatedState(placedItems)
            val currentRoomWidth by rememberUpdatedState(roomWidthCm)
            val currentRoomDepth by rememberUpdatedState(roomDepthCm)
            val currentCanvasWidth by rememberUpdatedState(canvasWidth)
            val currentCanvasHeight by rememberUpdatedState(canvasHeight)

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (screenX - itemWidthPx / 2f).toDp() },
                        y = with(density) { (screenY - itemDepthPx / 2f).toDp() }
                    )
                    .size(
                        width = with(density) { itemWidthPx.toDp() },
                        height = with(density) { itemDepthPx.toDp() }
                    )
                    .graphicsLayer {
                        rotationZ = item.rotationDegrees
                    }
                    .background(
                        if (isSelected) Color(0xFF3F51B5).copy(alpha = 0.2f) else Color(0xFFECEFF1).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color(0xFF3F51B5) else Color(0xFFB0BEC5),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .pointerInput(item.name) {
                        detectTapGestures {
                            onSelectItem(item.name)
                        }
                    }
                    .pointerInput(item.name) {
                        detectDragGestures(
                            onDragStart = {
                                onSelectItem(item.name)
                            },
                            onDragEnd = {
                                // Clear visual guidelines on drag release
                                snapHorizontalGuide = null
                                snapVerticalGuide = null
                            },
                            onDragCancel = {
                                snapHorizontalGuide = null
                                snapVerticalGuide = null
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val latestItem = currentPlacedItems.find { it.name == item.name } ?: item
                            val (currScreenX, currScreenY) = FloorplanCoordinator.relativeToScreen(
                                latestItem.offsetX,
                                latestItem.offsetZ,
                                currentCanvasWidth,
                                currentCanvasHeight,
                                currentRoomWidth,
                                currentRoomDepth
                            )
                            val newScreenX = currScreenX + dragAmount.x
                            val newScreenY = currScreenY + dragAmount.y
                            val (relX, relZ) = FloorplanCoordinator.screenToRelative(
                                newScreenX,
                                newScreenY,
                                currentCanvasWidth,
                                currentCanvasHeight,
                                currentRoomWidth,
                                currentRoomDepth
                            )

                            // R3. Implement Snapping boundaries logic (10cm snap threshold)
                            val snapThreshold = 0.10f
                            var targetX = relX
                            var targetZ = relZ
                            var activeVert: Float? = null
                            var activeHoriz: Float? = null

                            // 1) Snap to Room Center
                            if (abs(relX) < snapThreshold) {
                                targetX = 0f
                                activeVert = 0f
                            }
                            if (abs(relZ) < snapThreshold) {
                                targetZ = 0f
                                activeHoriz = 0f
                            }

                            // 2) Snap to other items' coordinates to facilitate neat row alignment
                            currentPlacedItems.filter { it.name != item.name }.forEach { other ->
                                if (abs(relX - other.offsetX) < snapThreshold) {
                                    targetX = other.offsetX
                                    activeVert = other.offsetX
                                }
                                if (abs(relZ - other.offsetZ) < snapThreshold) {
                                    targetZ = other.offsetZ
                                    activeHoriz = other.offsetZ
                                }
                            }

                            snapVerticalGuide = activeVert
                            snapHorizontalGuide = activeHoriz

                            val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
                                targetX,
                                targetZ,
                                latestItem.rotationDegrees,
                                latestItem.widthCm,
                                latestItem.depthCm,
                                currentRoomWidth,
                                currentRoomDepth
                            )
                            val updatedList = currentPlacedItems.map {
                                if (it.name == item.name) {
                                    it.copy(offsetX = clampedX, offsetZ = clampedZ)
                                } else it
                            }
                            onPlacedItemsChanged(updatedList)
                        }
                    }
                    .testTag("FurnitureBlock_${item.name}")
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "${item.widthCm.toInt()}x${item.depthCm.toInt()} cm",
                        fontSize = 8.sp,
                        color = Color.DarkGray,
                        maxLines = 1
                    )
                }
            }
        }

        // Floating rotation handle at root level if an item is selected
        selectedItemName?.let { selName ->
            val item = placedItems.find { it.name == selName }
            if (item != null) {
                val itemWidthPx = (item.widthCm / roomWidthCm) * canvasWidth
                val itemDepthPx = (item.depthCm / roomDepthCm) * canvasHeight
                val (screenX, screenY) = FloorplanCoordinator.relativeToScreen(
                    item.offsetX,
                    item.offsetZ,
                    canvasWidth,
                    canvasHeight,
                    roomWidthCm,
                    roomDepthCm
                )
                val rad = Math.toRadians(item.rotationDegrees.toDouble())
                val sinVal = sin(rad).toFloat()
                val cosVal = cos(rad).toFloat()
                val handleDistancePx = with(density) { 35.dp.toPx() }
                val totalDistancePx = itemDepthPx / 2f + handleDistancePx
                val handleScreenX = screenX + totalDistancePx * sinVal
                val handleScreenY = screenY - totalDistancePx * cosVal

                val handleSizeDp = 28.dp
                val handleRadiusPx = with(density) { (handleSizeDp / 2f).toPx() }

                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (handleScreenX - handleRadiusPx).toDp() },
                            y = with(density) { (handleScreenY - handleRadiusPx).toDp() }
                        )
                        .size(handleSizeDp)
                        .background(Color.White, shape = CircleShape)
                        .border(2.dp, Color(0xFF3F51B5), shape = CircleShape)
                        .pointerInput(item.name) {
                            detectDragGestures(
                                onDragStart = {
                                    val r = Math.toRadians(item.rotationDegrees.toDouble())
                                    val s = sin(r).toFloat()
                                    val c = cos(r).toFloat()
                                    val totalDist = itemDepthPx / 2f + with(density) { 35.dp.toPx() }
                                    currentAnglePointerCanvasX = screenX + totalDist * s
                                    currentAnglePointerCanvasY = screenY - totalDist * c
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val r = Math.toRadians(item.rotationDegrees.toDouble())
                                val c = cos(r).toFloat()
                                val s = sin(r).toFloat()
                                val canvasDragX = dragAmount.x * c - dragAmount.y * s
                                val canvasDragY = dragAmount.x * s + dragAmount.y * c
                                
                                currentAnglePointerCanvasX += canvasDragX
                                currentAnglePointerCanvasY += canvasDragY
                                
                                val dx = currentAnglePointerCanvasX - screenX
                                val dy = currentAnglePointerCanvasY - screenY
                                
                                val newAngleRad = kotlin.math.atan2(dy, dx)
                                var newAngleDeg = Math.toDegrees(newAngleRad.toDouble()).toFloat() + 90f
                                newAngleDeg = (newAngleDeg % 360f + 360f) % 360f
                                
                                // Snap to nearest 90 deg axes if close (threshold 5 degrees)
                                val snapThreshold = 5f
                                val nearest90 = kotlin.math.round(newAngleDeg / 90f) * 90f
                                val finalAngle = if (abs(newAngleDeg - nearest90) < snapThreshold || abs(newAngleDeg - nearest90 - 360f) < snapThreshold) {
                                    (nearest90 % 360f + 360f) % 360f
                                } else {
                                    newAngleDeg
                                }
                                
                                val updatedList = placedItems.map {
                                    if (it.name == item.name) {
                                        it.copy(rotationDegrees = finalAngle)
                                    } else it
                                }
                                onPlacedItemsChanged(updatedList)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↻",
                        color = Color(0xFF3F51B5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
