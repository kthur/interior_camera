package com.example.interiorcamera.ui.floorplan

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class FloorplanScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testFloorplanCanvas_rendersPlacedItems() {
        val items = listOf(
            ArPlacedItem("Refrigerator", 90f, 180f, 80f, "refrigerator.glb", 0f, 0f, 0f),
            ArPlacedItem("WashingMachine", 60f, 85f, 60f, "cube.glb", 0.5f, 0.5f, 90f)
        )

        composeTestRule.setContent {
            var stateItems by remember { mutableStateOf(items) }
            var selectedItemName by remember { mutableStateOf<String?>(null) }
            FloorplanCanvas(
                roomWidthCm = 300f,
                roomDepthCm = 300f,
                placedItems = stateItems,
                onPlacedItemsChanged = { stateItems = it },
                selectedItemName = selectedItemName,
                onSelectItem = { selectedItemName = it }
            )
        }

        // Verify that canvas renders the furniture blocks with appropriate names
        composeTestRule.onNodeWithTag("FloorplanCanvas").assertExists()
        composeTestRule.onNodeWithTag("FurnitureBlock_Refrigerator").assertExists()
        composeTestRule.onNodeWithTag("FurnitureBlock_WashingMachine").assertExists()
    }

    @Test
    fun testFloorplanCanvas_itemSelectionAndTapDeselect() {
        val items = listOf(
            ArPlacedItem("Fridge", 90f, 180f, 80f, "refrigerator.glb", 0f, 0f, 0f)
        )

        var selectedItemName: String? = null

        composeTestRule.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            FloorplanCanvas(
                roomWidthCm = 300f,
                roomDepthCm = 300f,
                placedItems = items,
                onPlacedItemsChanged = {},
                selectedItemName = selected,
                onSelectItem = {
                    selected = it
                    selectedItemName = it
                }
            )
        }

        // Click on furniture block to select
        composeTestRule.onNodeWithTag("FurnitureBlock_Fridge").performClick()
        assertEquals("Fridge", selectedItemName)

        // Click on background canvas to deselect
        composeTestRule.onNodeWithTag("FloorplanCanvas").performClick()
        assertEquals(null, selectedItemName)
    }

    @Test
    fun testFloorplanCanvas_dragItemChangesCoordinates() {
        val items = listOf(
            ArPlacedItem("Fridge", 90f, 180f, 80f, "refrigerator.glb", 0f, 0f, 0f)
        )

        var updatedItems = listOf<ArPlacedItem>()

        composeTestRule.setContent {
            var stateItems by remember { mutableStateOf(items) }
            FloorplanCanvas(
                roomWidthCm = 300f,
                roomDepthCm = 300f,
                placedItems = stateItems,
                onPlacedItemsChanged = {
                    stateItems = it
                    updatedItems = it
                },
                selectedItemName = null,
                onSelectItem = {}
            )
        }

        // Drag the furniture block
        composeTestRule.onNodeWithTag("FurnitureBlock_Fridge").performTouchInput {
            down(center)
            moveBy(Offset(50f, 50f))
            up()
        }

        // Verify the coordinates have changed
        assertNotEquals(0f, updatedItems.first().offsetX)
        assertNotEquals(0f, updatedItems.first().offsetZ)
    }
}
