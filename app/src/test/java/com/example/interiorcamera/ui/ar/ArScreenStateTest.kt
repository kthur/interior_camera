package com.example.interiorcamera.ui.ar

import com.google.ar.core.Anchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class ArScreenStateTest {

  // A simple state container mimicking ArScreen's logic
  class ArScreenStateHolder {
    var placedItems = emptyList<PlacedItem>()
    var selectedItemId: String? = null
    var isPlaneDetected = false

    fun addPlacedItem(anchor: Anchor, widthCm: Float, heightCm: Float, depthCm: Float, modelName: String) {
      val newItem = PlacedItem(
        id = java.util.UUID.randomUUID().toString(),
        anchor = anchor,
        widthCm = widthCm,
        heightCm = heightCm,
        depthCm = depthCm,
        modelName = modelName,
        rotationDegrees = 0f,
        opacity = 0.8f
      )
      placedItems = placedItems + newItem
      selectedItemId = newItem.id
    }

    fun removeSelectedItem() {
      val selectedId = selectedItemId ?: return
      placedItems = placedItems.filter { it.id != selectedId }
      selectedItemId = null
    }

    fun clearAll() {
      placedItems = emptyList()
      selectedItemId = null
    }

    fun rotateSelected(degrees: Float) {
      val selectedId = selectedItemId ?: return
      placedItems = placedItems.map {
        if (it.id == selectedId) {
          it.copy(rotationDegrees = (it.rotationDegrees + degrees + 360f) % 360f)
        } else it
      }
    }

    fun updateSelectedOpacity(newOpacity: Float) {
      val selectedId = selectedItemId ?: return
      val clampedOpacity = newOpacity.coerceIn(0.1f, 1.0f)
      placedItems = placedItems.map {
        if (it.id == selectedId) {
          it.copy(opacity = clampedOpacity)
        } else it
      }
    }
  }

  @Test
  fun testPlacedItemRotationWrapAround() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 100f, 100f, 100f, "cube.glb")

    // Initially 0 degrees
    assertEquals(0f, state.placedItems[0].rotationDegrees, 0.01f)

    // Rotate +15
    state.rotateSelected(15f)
    assertEquals(15f, state.placedItems[0].rotationDegrees, 0.01f)

    // Rotate +350 (should wrap around to 5f)
    state.rotateSelected(350f)
    assertEquals(5f, state.placedItems[0].rotationDegrees, 0.01f)

    // Rotate -15 (should wrap to 350f)
    state.rotateSelected(-15f)
    assertEquals(350f, state.placedItems[0].rotationDegrees, 0.01f)
  }

  @Test
  fun testPlacedItemOpacityConstraints() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 100f, 100f, 100f, "cube.glb")

    // Update opacity to 0.5f (within range)
    state.updateSelectedOpacity(0.5f)
    assertEquals(0.5f, state.placedItems[0].opacity, 0.01f)

    // Try setting to 0.0f (should clamp to 0.1f)
    state.updateSelectedOpacity(0.0f)
    assertEquals(0.1f, state.placedItems[0].opacity, 0.01f)

    // Try setting to 1.5f (should clamp to 1.0f)
    state.updateSelectedOpacity(1.5f)
    assertEquals(1.0f, state.placedItems[0].opacity, 0.01f)
  }

  @Test
  fun testPlacedItemScaleCalculation() {
    val anchor = mock(Anchor::class.java)
    val item = PlacedItem("1", anchor, 50f, 120f, 80f, "cube.glb")

    val w = item.widthCm / 100f
    val h = item.heightCm / 100f
    val d = item.depthCm / 100f

    assertEquals(0.5f, w, 0.01f)
    assertEquals(1.2f, h, 0.01f)
    assertEquals(0.8f, d, 0.01f)
  }

  @Test
  fun testPlacedItemPositionCalculation() {
    val anchor = mock(Anchor::class.java)
    val item = PlacedItem("1", anchor, 50f, 150f, 80f, "cube.glb")

    val h = item.heightCm / 100f
    val positionY = h / 2f

    assertEquals(1.5f, h, 0.01f)
    assertEquals(0.75f, positionY, 0.01f)
  }

  @Test
  fun testAddPlacedItemToList() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 60f, 85f, 60f, "cube.glb")

    assertEquals(1, state.placedItems.size)
    assertEquals(state.placedItems[0].id, state.selectedItemId)
  }

  @Test
  fun testRemovePlacedItemFromList() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 60f, 85f, 60f, "cube.glb")
    assertEquals(1, state.placedItems.size)

    state.removeSelectedItem()
    assertEquals(0, state.placedItems.size)
    assertNull(state.selectedItemId)
  }

  @Test
  fun testSelectPlacedItem() {
    val state = ArScreenStateHolder()
    val anchor1 = mock(Anchor::class.java)
    val anchor2 = mock(Anchor::class.java)
    
    state.addPlacedItem(anchor1, 60f, 85f, 60f, "cube.glb")
    val id1 = state.selectedItemId!!

    state.addPlacedItem(anchor2, 90f, 180f, 80f, "refrigerator.glb")
    val id2 = state.selectedItemId!!

    assertEquals(id2, state.selectedItemId)

    // Select the first one
    state.selectedItemId = id1
    assertEquals(id1, state.selectedItemId)
  }

  @Test
  fun testClearAllPlacedItems() {
    val state = ArScreenStateHolder()
    val anchor1 = mock(Anchor::class.java)
    val anchor2 = mock(Anchor::class.java)

    state.addPlacedItem(anchor1, 60f, 85f, 60f, "cube.glb")
    state.addPlacedItem(anchor2, 90f, 180f, 80f, "refrigerator.glb")
    assertEquals(2, state.placedItems.size)

    state.clearAll()
    assertEquals(0, state.placedItems.size)
    assertNull(state.selectedItemId)
  }

  @Test
  fun testOpacityUpdatedForSelectedItem() {
    val state = ArScreenStateHolder()
    val anchor1 = mock(Anchor::class.java)
    val anchor2 = mock(Anchor::class.java)

    state.addPlacedItem(anchor1, 60f, 85f, 60f, "cube.glb")
    val id1 = state.selectedItemId!!

    state.addPlacedItem(anchor2, 90f, 180f, 80f, "refrigerator.glb")
    val id2 = state.selectedItemId!!

    // selected item is id2
    state.updateSelectedOpacity(0.5f)
    assertEquals(0.5f, state.placedItems.first { it.id == id2 }.opacity, 0.01f)
    assertEquals(0.8f, state.placedItems.first { it.id == id1 }.opacity, 0.01f)
  }

  @Test
  fun testRotationUpdatedForSelectedItem() {
    val state = ArScreenStateHolder()
    val anchor1 = mock(Anchor::class.java)
    val anchor2 = mock(Anchor::class.java)

    state.addPlacedItem(anchor1, 60f, 85f, 60f, "cube.glb")
    val id1 = state.selectedItemId!!

    state.addPlacedItem(anchor2, 90f, 180f, 80f, "refrigerator.glb")
    val id2 = state.selectedItemId!!

    // selected item is id2, rotate by 45 degrees
    state.rotateSelected(45f)
    assertEquals(45f, state.placedItems.first { it.id == id2 }.rotationDegrees, 0.01f)
    assertEquals(0f, state.placedItems.first { it.id == id1 }.rotationDegrees, 0.01f)
  }

  @Test
  fun testMultipleAnchorsPlacedIndependently() {
    val state = ArScreenStateHolder()
    val anchor1 = mock(Anchor::class.java)
    val anchor2 = mock(Anchor::class.java)

    state.addPlacedItem(anchor1, 60f, 85f, 60f, "cube.glb")
    val id1 = state.selectedItemId!!
    
    state.addPlacedItem(anchor2, 90f, 180f, 80f, "refrigerator.glb")
    val id2 = state.selectedItemId!!

    // Manipulate first item
    state.selectedItemId = id1
    state.updateSelectedOpacity(0.3f)
    state.rotateSelected(90f)

    // Manipulate second item
    state.selectedItemId = id2
    state.updateSelectedOpacity(0.7f)
    state.rotateSelected(180f)

    val item1 = state.placedItems.first { it.id == id1 }
    val item2 = state.placedItems.first { it.id == id2 }

    assertEquals(0.3f, item1.opacity, 0.01f)
    assertEquals(90f, item1.rotationDegrees, 0.01f)

    assertEquals(0.7f, item2.opacity, 0.01f)
    assertEquals(180f, item2.rotationDegrees, 0.01f)
  }

  @Test
  fun testDeselectItem() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 60f, 85f, 60f, "cube.glb")
    
    // Deselect
    state.selectedItemId = null
    assertNull(state.selectedItemId)
  }

  @Test
  fun testDeselectBeforeDeletingDoesNotDeleteAny() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 60f, 85f, 60f, "cube.glb")

    state.selectedItemId = null
    state.removeSelectedItem()

    assertEquals(1, state.placedItems.size)
  }

  @Test
  fun testRotateDeselectedNodeDoesNothing() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 60f, 85f, 60f, "cube.glb")

    state.selectedItemId = null
    state.rotateSelected(45f)

    assertEquals(0f, state.placedItems[0].rotationDegrees, 0.01f)
  }

  @Test
  fun testModifyOpacityDeselectedNodeDoesNothing() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 60f, 85f, 60f, "cube.glb")

    state.selectedItemId = null
    state.updateSelectedOpacity(0.2f)

    assertEquals(0.8f, state.placedItems[0].opacity, 0.01f)
  }
}
