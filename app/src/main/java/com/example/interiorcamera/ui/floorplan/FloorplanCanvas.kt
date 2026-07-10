package com.example.interiorcamera.ui.floorplan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

            // Convert to density independent positioning safely
            Box(
                modifier = Modifier
                    .offset(
                        x = ((screenX - itemWidthPx / 2f) / 3f).dp,
                        y = ((screenY - itemDepthPx / 2f) / 3f).dp
                    )
                    .size(width = (itemWidthPx / 3f).dp, height = (itemDepthPx / 3f).dp)
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
                            val newScreenX = screenX + dragAmount.x
                            val newScreenY = screenY + dragAmount.y
                            val (relX, relZ) = FloorplanCoordinator.screenToRelative(
                                newScreenX,
                                newScreenY,
                                canvasWidth,
                                canvasHeight,
                                roomWidthCm,
                                roomDepthCm
                            )
                            val (clampedX, clampedZ) = FloorplanCoordinator.clampToRoomBounds(
                                relX,
                                relZ,
                                item.rotationDegrees,
                                item.widthCm,
                                item.depthCm,
                                roomWidthCm,
                                roomDepthCm
                            )
                            val updatedList = placedItems.map {
                                if (it.name == item.name) {
                                    it.copy(offsetX = clampedX, offsetZ = clampedZ)
                                } else it
                            }
                            onPlacedItemsChanged(updatedList)
                        }
                    }
                    .testTag("FurnitureBlock_${item.name}")
            ) {
                Text(
                    text = item.name,
                    fontSize = 10.sp,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
