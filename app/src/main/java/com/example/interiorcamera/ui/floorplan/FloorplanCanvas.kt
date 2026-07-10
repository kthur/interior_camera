package com.example.interiorcamera.ui.floorplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight

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
    var canvasWidth by remember { mutableStateOf(600f) }
    var canvasHeight by remember { mutableStateOf(600f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFFAFAFA))
            .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
            .onGloballyPositioned { coordinates ->
                canvasWidth = coordinates.size.width.toFloat()
                canvasHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    onSelectItem(null)
                }
            }
            .testTag("FloorplanCanvas")
    ) {
        // Draw grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40f
            for (y in 0 until (size.height / gridSpacing).toInt()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(size.width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }
            for (x in 0 until (size.width / gridSpacing).toInt()) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, size.height),
                    strokeWidth = 1f
                )
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

            // Convert to density independent positioning safely
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
                        if (isSelected) Color.Red.copy(alpha = 0.3f) else Color.Blue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.Red else Color.Blue,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .pointerInput(item.name) {
                        detectTapGestures {
                            onSelectItem(item.name)
                        }
                    }
                    .pointerInput(item.name) {
                        detectDragGestures { change, dragAmount ->
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
                            val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
                                relX,
                                relZ,
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
    }
}
