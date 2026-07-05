package com.example.interiorcamera.ui.ar

import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.math.sqrt

class ArScreenStateTest {

  // A simple state container mimicking ArScreen's logic
  class ArScreenStateHolder {
    var placedItems = emptyList<PlacedItem>()
    var selectedItemId: String? = null
    var isPlaneDetected = false

    // Ruler Mode state
    var isRulerModeActive = false
      set(value) {
        field = value
        clearRuler()
      }
    var rulerStartPoint: Pose? = null
    var rulerEndPoint: Pose? = null
    var isTrackingLost = false

    // Calibration Slider state
    var calibrationFactor = 1.0f
    private val originalDimensions = mutableMapOf<String, Triple<Float, Float, Float>>()

    // Ghost Preview state
    var ghostWidthCm: Float = 0f
    var ghostHeightCm: Float = 0f
    var ghostDepthCm: Float = 0f

    // Action history for undo/redo
    var actionHistory = emptyList<ArAction>()
    var redoStack = emptyList<ArAction>()

    fun setGhostDimensions(width: Float, height: Float, depth: Float) {
      ghostWidthCm = width
      ghostHeightCm = height
      ghostDepthCm = depth
    }

    fun getScaledGhostWidth(): Float = ghostWidthCm * calibrationFactor
    fun getScaledGhostHeight(): Float = ghostHeightCm * calibrationFactor
    fun getScaledGhostDepth(): Float = ghostDepthCm * calibrationFactor

    fun addPlacedItem(anchor: Anchor, widthCm: Float, heightCm: Float, depthCm: Float, modelName: String) {
      val newItem = PlacedItem(
        id = java.util.UUID.randomUUID().toString(),
        anchor = anchor,
        widthCm = widthCm * calibrationFactor,
        heightCm = heightCm * calibrationFactor,
        depthCm = depthCm * calibrationFactor,
        modelName = modelName,
        rotationDegrees = 0f,
        opacity = 0.8f
      )
      originalDimensions[newItem.id] = Triple(widthCm, heightCm, depthCm)
      placedItems = placedItems + newItem
      selectedItemId = newItem.id
      pushAction(ArAction.Place(newItem))
    }

    fun removeSelectedItem() {
      val selectedId = selectedItemId ?: return
      val itemToRemove = placedItems.firstOrNull { it.id == selectedId } ?: return
      placedItems = placedItems.filter { it.id != selectedId }
      selectedItemId = null
      pushAction(ArAction.Delete(itemToRemove))
    }

    fun clearAll() {
      if (placedItems.isEmpty()) return
      val oldItems = placedItems
      placedItems = emptyList()
      selectedItemId = null
      pushAction(ArAction.ClearAll(oldItems))
    }

    fun rotateSelected(degrees: Float) {
      val selectedId = selectedItemId ?: return
      val oldItem = placedItems.firstOrNull { it.id == selectedId } ?: return
      val newDegrees = (oldItem.rotationDegrees + degrees + 360f) % 360f
      placedItems = placedItems.map {
        if (it.id == selectedId) {
          it.copy(rotationDegrees = newDegrees)
        } else it
      }
      pushAction(ArAction.Rotate(selectedId, oldItem.rotationDegrees, newDegrees))
    }

    fun updateSelectedOpacity(newOpacity: Float) {
      val selectedId = selectedItemId ?: return
      val oldItem = placedItems.firstOrNull { it.id == selectedId } ?: return
      val clampedOpacity = newOpacity.coerceIn(0.1f, 1.0f)
      placedItems = placedItems.map {
        if (it.id == selectedId) {
          it.copy(opacity = clampedOpacity)
        } else it
      }
      pushAction(ArAction.Opacity(selectedId, oldItem.opacity, clampedOpacity))
    }

    fun handleRulerTap(pose: Pose, distanceToCamera: Float = 0f) {
      if (!isRulerModeActive) return
      if (isTrackingLost) return
      if (distanceToCamera > 5.0f) return // MAX_HIT_DISTANCE_M

      if (rulerStartPoint == null) {
        rulerStartPoint = pose
      } else if (rulerEndPoint == null) {
        rulerEndPoint = pose
      } else {
        rulerStartPoint = pose
        rulerEndPoint = null
      }
    }

    fun clearRuler() {
      rulerStartPoint = null
      rulerEndPoint = null
    }

    fun getMeasuredDistanceCm(): Float {
      val start = rulerStartPoint ?: return 0f
      val end = rulerEndPoint ?: return 0f
      val dx = start.tx() - end.tx()
      val dy = start.ty() - end.ty()
      val dz = start.tz() - end.tz()
      val rawDistance = sqrt(dx * dx + dy * dy + dz * dz)
      return rawDistance * 100f * calibrationFactor
    }

    fun updateCalibrationFactor(factor: Float) {
      calibrationFactor = factor.coerceIn(0.8f, 1.2f)
      placedItems = placedItems.map { item ->
        val original = originalDimensions[item.id] ?: Triple(item.widthCm, item.heightCm, item.depthCm)
        item.copy(
          widthCm = original.first * calibrationFactor,
          heightCm = original.second * calibrationFactor,
          depthCm = original.third * calibrationFactor
        )
      }
    }

    fun pushAction(action: ArAction) {
      actionHistory = actionHistory + action
      redoStack = emptyList()
    }

    fun undo() {
      if (actionHistory.isEmpty()) return
      val action = actionHistory.last()
      actionHistory = actionHistory.dropLast(1)
      redoStack = redoStack + action
      when (action) {
        is ArAction.Place -> {
          placedItems = placedItems.filter { it.id != action.item.id }
          selectedItemId = null
        }
        is ArAction.Delete -> {
          val original = originalDimensions[action.item.id] ?: Triple(action.item.widthCm, action.item.heightCm, action.item.depthCm)
          val restoredItem = action.item.copy(
            widthCm = original.first * calibrationFactor,
            heightCm = original.second * calibrationFactor,
            depthCm = original.third * calibrationFactor
          )
          placedItems = placedItems + restoredItem
        }
        is ArAction.ClearAll -> {
          placedItems = action.items
        }
        is ArAction.Rotate -> {
          placedItems = placedItems.map {
            if (it.id == action.itemId) it.copy(rotationDegrees = action.oldDeg) else it
          }
        }
        is ArAction.Opacity -> {
          placedItems = placedItems.map {
            if (it.id == action.itemId) it.copy(opacity = action.oldVal) else it
          }
        }
      }
    }

    fun redo() {
      if (redoStack.isEmpty()) return
      val action = redoStack.last()
      redoStack = redoStack.dropLast(1)
      actionHistory = actionHistory + action
      when (action) {
        is ArAction.Place -> {
          val original = originalDimensions[action.item.id] ?: Triple(action.item.widthCm, action.item.heightCm, action.item.depthCm)
          val restoredItem = action.item.copy(
            widthCm = original.first * calibrationFactor,
            heightCm = original.second * calibrationFactor,
            depthCm = original.third * calibrationFactor
          )
          placedItems = placedItems + restoredItem
          selectedItemId = restoredItem.id
        }
        is ArAction.Delete -> {
          placedItems = placedItems.filter { it.id != action.item.id }
          selectedItemId = null
        }
        is ArAction.ClearAll -> {
          placedItems = emptyList()
          selectedItemId = null
        }
        is ArAction.Rotate -> {
          placedItems = placedItems.map {
            if (it.id == action.itemId) it.copy(rotationDegrees = action.newDeg) else it
          }
        }
        is ArAction.Opacity -> {
          placedItems = placedItems.map {
            if (it.id == action.itemId) it.copy(opacity = action.newVal) else it
          }
        }
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

  private fun mockPose(x: Float, y: Float, z: Float): Pose {
    val pose = mock(Pose::class.java)
    `when`(pose.tx()).thenReturn(x)
    `when`(pose.ty()).thenReturn(y)
    `when`(pose.tz()).thenReturn(z)
    return pose
  }

  @Test
  fun testRulerMode_ToggleState() {
    val state = ArScreenStateHolder()
    assertEquals(false, state.isRulerModeActive)
    state.isRulerModeActive = true
    assertEquals(true, state.isRulerModeActive)
    state.isRulerModeActive = false
    assertEquals(false, state.isRulerModeActive)
  }

  @Test
  fun testRulerMode_FirstTapSetsStartPoint() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    val pose = mockPose(1.0f, 2.0f, 3.0f)
    state.handleRulerTap(pose)
    assertEquals(pose, state.rulerStartPoint)
    assertNull(state.rulerEndPoint)
  }

  @Test
  fun testRulerMode_SecondTapCalculatesDistance() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    val pose1 = mockPose(0.0f, 0.0f, 0.0f)
    val pose2 = mockPose(3.0f, 4.0f, 0.0f)
    state.handleRulerTap(pose1)
    state.handleRulerTap(pose2)
    assertEquals(pose1, state.rulerStartPoint)
    assertEquals(pose2, state.rulerEndPoint)
    assertEquals(500f, state.getMeasuredDistanceCm(), 0.01f)
  }

  @Test
  fun testRulerMode_ClearMeasurement() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    state.handleRulerTap(mockPose(0f, 0f, 0f))
    state.handleRulerTap(mockPose(1f, 1f, 1f))
    state.clearRuler()
    assertNull(state.rulerStartPoint)
    assertNull(state.rulerEndPoint)
  }

  @Test
  fun testCalibrationSlider_UpdateState() {
    val state = ArScreenStateHolder()
    assertEquals(1.0f, state.calibrationFactor, 0.01f)
    state.updateCalibrationFactor(1.1f)
    assertEquals(1.1f, state.calibrationFactor, 0.01f)
  }

  @Test
  fun testCalibrationSlider_ScaleGhostPreview() {
    val state = ArScreenStateHolder()
    state.setGhostDimensions(100f, 80f, 60f)
    state.updateCalibrationFactor(1.1f)
    assertEquals(110f, state.getScaledGhostWidth(), 0.01f)
    assertEquals(88f, state.getScaledGhostHeight(), 0.01f)
    assertEquals(66f, state.getScaledGhostDepth(), 0.01f)
  }

  @Test
  fun testCalibrationSlider_ScalePlacedItems() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 100f, 100f, 100f, "cube.glb")
    state.updateCalibrationFactor(1.1f)
    assertEquals(110f, state.placedItems[0].widthCm, 0.01f)
    assertEquals(110f, state.placedItems[0].heightCm, 0.01f)
    assertEquals(110f, state.placedItems[0].depthCm, 0.01f)
  }

  @Test
  fun testRulerMode_TapInTrackingLoss() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    state.isTrackingLost = true
    state.handleRulerTap(mockPose(1f, 1f, 1f))
    assertNull(state.rulerStartPoint)
  }

  @Test
  fun testRulerMode_ZeroDistanceTaps() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    val pose = mockPose(1f, 2f, 3f)
    state.handleRulerTap(pose)
    state.handleRulerTap(pose)
    assertEquals(0f, state.getMeasuredDistanceCm(), 0.01f)
  }

  @Test
  fun testRulerMode_ExtremeDistanceTaps() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    val pose1 = mockPose(0f, 0f, 0f)
    val pose2 = mockPose(100f, 0f, 0f)
    state.handleRulerTap(pose1)
    state.handleRulerTap(pose2)
    assertEquals(10000f, state.getMeasuredDistanceCm(), 0.01f)
  }

  @Test
  fun testRulerMode_SwitchModeClearsTaps() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    state.handleRulerTap(mockPose(0f, 0f, 0f))
    state.handleRulerTap(mockPose(1f, 1f, 1f))
    state.isRulerModeActive = false
    assertNull(state.rulerStartPoint)
    assertNull(state.rulerEndPoint)
  }

  @Test
  fun testRulerMode_MaxHitDistanceExceeded() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    state.handleRulerTap(mockPose(1f, 1f, 1f), 6.0f)
    assertNull(state.rulerStartPoint)
  }

  @Test
  fun testCalibrationSlider_ClampMinBound() {
    val state = ArScreenStateHolder()
    state.updateCalibrationFactor(0.5f)
    assertEquals(0.8f, state.calibrationFactor, 0.01f)
  }

  @Test
  fun testCalibrationSlider_ClampMaxBound() {
    val state = ArScreenStateHolder()
    state.updateCalibrationFactor(1.5f)
    assertEquals(1.2f, state.calibrationFactor, 0.01f)
  }

  @Test
  fun testCalibrationSlider_NoSelectedObject() {
    val state = ArScreenStateHolder()
    state.selectedItemId = null
    state.updateCalibrationFactor(1.1f)
    assertEquals(1.1f, state.calibrationFactor, 0.01f)
  }

  @Test
  fun testCalibrationSlider_FloatingPointPrecision() {
    val state = ArScreenStateHolder()
    state.updateCalibrationFactor(1.000001f)
    assertEquals(1.000001f, state.calibrationFactor, 1e-6f)
  }

  @Test
  fun testCalibrationSlider_ZeroDimensionError() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 0f, 100f, 100f, "cube.glb")
    state.updateCalibrationFactor(1.2f)
    assertEquals(0f, state.placedItems[0].widthCm, 0.01f)
    assertEquals(120f, state.placedItems[0].heightCm, 0.01f)
  }

  @Test
  fun testRulerAndCalibration_DistanceScaling() {
    val state = ArScreenStateHolder()
    state.isRulerModeActive = true
    state.handleRulerTap(mockPose(0f, 0f, 0f))
    state.handleRulerTap(mockPose(0f, 2f, 0f))
    assertEquals(200f, state.getMeasuredDistanceCm(), 0.01f)
    state.updateCalibrationFactor(1.2f)
    assertEquals(240f, state.getMeasuredDistanceCm(), 0.01f)
  }

  @Test
  fun testRulerAndCalibration_ObjectPlacementScale() {
    val state = ArScreenStateHolder()
    state.updateCalibrationFactor(1.2f)
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 100f, 50f, 80f, "cube.glb")
    assertEquals(120f, state.placedItems[0].widthCm, 0.01f)
    assertEquals(60f, state.placedItems[0].heightCm, 0.01f)
    assertEquals(96f, state.placedItems[0].depthCm, 0.01f)
  }

  @Test
  fun testWorkflow_UndoRedoAcrossRulerAndCalibration() {
    val state = ArScreenStateHolder()
    val anchor = mock(Anchor::class.java)
    state.addPlacedItem(anchor, 100f, 100f, 100f, "cube.glb")
    assertEquals(1, state.placedItems.size)

    state.updateCalibrationFactor(1.1f)
    assertEquals(110f, state.placedItems[0].widthCm, 0.01f)

    state.undo()
    assertEquals(0, state.placedItems.size)

    state.redo()
    assertEquals(1, state.placedItems.size)
    assertEquals(110f, state.placedItems[0].widthCm, 0.01f)
  }
}
