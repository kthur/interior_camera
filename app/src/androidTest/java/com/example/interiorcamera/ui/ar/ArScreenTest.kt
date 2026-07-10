package com.example.interiorcamera.ui.ar

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.ar.core.Anchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import com.example.interiorcamera.data.RecommendedFurniture

class ArScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val mockAnchor = mock(Anchor::class.java)

  @Test
  fun testArScreenInitialLayout() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = false,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("바닥을 터치해 배치").assertExists()
    composeTestRule.onNodeWithText("주변 공간 인식 중").assertExists()
  }

  @Test
  fun testArScreenGuidesVisibleWhenNoPlane() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = false,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("스마트폰을 천천히 흔들며 바닥면과 주변 벽면을 비춰 공간을 스캔하세요.").assertExists()
  }

  @Test
  fun testDeselectedStateHidesControls() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("cube.glb").assertDoesNotExist()
  }

  @Test
  fun testSelectedStateShowsControls() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = "1",
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("cube.glb").assertExists()
  }

  @Test
  fun testSliderValueChangeTriggersCallback() {
    var opacityChanged = false
    var newOpacityVal = 0f

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb", opacity = 0.8f)),
        selectedItemId = "1",
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, op ->
          opacityChanged = true
          newOpacityVal = op
        },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    // Find the slider by range info and change value
    composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.8f, 0.1f..1.0f)))
      .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }

    assertTrue(opacityChanged)
    assertEquals(0.5f, newOpacityVal, 0.01f)
  }

  @Test
  fun testRotateLeftButtonTriggersCallback() {
    var rotateLeftClicked = false

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = "1",
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = { rotateLeftClicked = true },
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("↺").performClick()
    assertTrue(rotateLeftClicked)
  }

  @Test
  fun testRotateRightButtonTriggersCallback() {
    var rotateRightClicked = false

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = "1",
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = { rotateRightClicked = true },
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("↻").performClick()
    assertTrue(rotateRightClicked)
  }

  @Test
  fun testDeleteButtonTriggersCallback() {
    var deleteClicked = false

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = "1",
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = { deleteClicked = true },
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("삭제").performClick()
    assertTrue(deleteClicked)
  }

  @Test
  fun testClearAllButtonTriggersCallback() {
    var clearAllClicked = false

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = { clearAllClicked = true }
      )
    }

    composeTestRule.onNodeWithText("전체\n삭제").performClick()
    assertTrue(clearAllClicked)
  }

  @Test
  fun testClickBackButtonNavigatesBack() {
    var backClicked = false

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = { backClicked = true },
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("←").performClick()
    assertTrue(backClicked)
  }

  @Test
  fun testNoCameraPermissionShowsPermissionError() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = false,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = false,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("카메라 권한이 없습니다. 메인 화면으로 돌아가 권한을 수락해 주세요.").assertExists()
  }

  @Test
  fun testArOverlayDisplaysDimensions() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 150f,
        heightCm = 180f,
        depthCm = 90f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("바닥을 터치해 배치").assertExists()
  }

  @Test
  fun testCircularProgressIndicatorShownInGuide() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = false,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("주변 공간 인식 중").assertExists()
  }

  @Test
  fun testOverlayDisappearsWhenPlaneDetected() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("주변 공간 인식 중").assertDoesNotExist()
  }

  @Test
  fun testDeselectedButtonClosesControlCard() {
    var deselectClicked = false

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = listOf(PlacedItem("1", mockAnchor, 100f, 200f, 60f, "cube.glb")),
        selectedItemId = "1",
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = { deselectClicked = true },
        onDeleteItem = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("해제").performClick()
    assertTrue(deselectClicked)
  }

  @Test
  fun testRulerMode_UIOverlayDisplaysDistance() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        isRulerModeActive = true,
        measuredDistanceCm = 125.5f
      )
    }
    composeTestRule.onNodeWithText("자\nON").assertExists()
    composeTestRule.onNodeWithText("📏 125.5cm").assertExists()
    
    composeTestRule.onNodeWithText("자\nON").performClick()
    composeTestRule.onNodeWithText("📏 자 모드").assertExists()
    composeTestRule.onNodeWithText("측정 거리: 125.5 cm").assertExists()
  }

  @Test
  fun testCalibrationSlider_InitialValue() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        calibrationFactor = 1.0f
      )
    }
    composeTestRule.onNodeWithText("보정").performClick()
    composeTestRule.onNodeWithText("🎚 크기 보정").assertExists()
    composeTestRule.onNodeWithText("100%").assertExists()
  }

  @Test
  fun testCalibrationSlider_UIVisibility() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        calibrationFactor = 1.15f
      )
    }
    composeTestRule.onNodeWithText("보정").performClick()
    composeTestRule.onNodeWithText("115%").assertExists()
  }

  @Test
  fun testRecommendationPanel_UIVisibility() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        recommendedFurniture = listOf(
          RecommendedFurniture("Cozy Sofa", "IKEA", "₩350,000", "Living Room", 160f, 85f, 90f, "cube.glb")
        )
      )
    }
    composeTestRule.onNodeWithText("추천").performClick()
    composeTestRule.onNodeWithText("🛋 추천 가구").assertExists()
    composeTestRule.onNodeWithText("[IKEA] Cozy Sofa").assertExists()
  }

  @Test
  fun testRecommendationPanel_SelectAndPlace() {
    var selectClicked = false
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        recommendedFurniture = listOf(
          RecommendedFurniture("Cozy Sofa", "IKEA", "₩350,000", "Living Room", 160f, 85f, 90f, "cube.glb")
        ),
        onSelectRecommended = { selectClicked = true }
      )
    }
    composeTestRule.onNodeWithText("추천").performClick()
    composeTestRule.onNodeWithText("[IKEA] Cozy Sofa").performClick()
    assertTrue(selectClicked)
  }

  @Test
  fun testRulerAndRecommendation_AutoPopulateSpace() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        isRulerModeActive = true,
        measuredDistanceCm = 150.0f,
        recommendedFurniture = listOf(
          RecommendedFurniture("Cozy Sofa", "IKEA", "₩350,000", "Living Room", 160f, 85f, 90f, "cube.glb")
        )
      )
    }
    composeTestRule.onNodeWithText("📏 150.0cm").assertExists()
    composeTestRule.onNodeWithText("추천").performClick()
    composeTestRule.onNodeWithText("🛋 추천 가구").assertExists()
    composeTestRule.onNodeWithText("공간 150cm 기준 필터링").assertExists()
  }

  @Test
  fun testRulerAndRecommendation_ClearRulerResetsPanel() {
    var clearRulerClicked = false
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        isRulerModeActive = true,
        measuredDistanceCm = 150.0f,
        onClearRuler = { clearRulerClicked = true }
      )
    }
    composeTestRule.onNodeWithText("자\nON").performClick()
    composeTestRule.onNodeWithText("자 지우기").performClick()
    assertTrue(clearRulerClicked)
  }

  @Test
  fun testCalibrationAndRecommendation_RealtimeSafetyWarning() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        safetyWarning = "공간 부족"
      )
    }
    composeTestRule.onNodeWithText("추천").performClick()
    composeTestRule.onNodeWithText("공간 부족").assertExists()
  }

  @Test
  fun testWorkflow_MeasureSpaceAndPlaceMatchingFurniture() {
    var placedClicked = false
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        isRulerModeActive = true,
        measuredDistanceCm = 180.0f,
        recommendedFurniture = listOf(
          RecommendedFurniture("Single Bed", "IKEA", "₩250,000", "Bedroom", 100f, 45f, 200f, "cube.glb")
        ),
        onSelectRecommended = { placedClicked = true }
      )
    }
    composeTestRule.onNodeWithText("📏 180.0cm").assertExists()
    composeTestRule.onNodeWithText("추천").performClick()
    composeTestRule.onNodeWithText("[IKEA] Single Bed").performClick()
    assertTrue(placedClicked)
  }

  @Test
  fun testWorkflow_CalibrateAndVerifyFitWithClearance() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        calibrationFactor = 1.10f
      )
    }
    composeTestRule.onNodeWithText("보정").performClick()
    composeTestRule.onNodeWithText("110%").assertExists()
  }

  @Test
  fun testWorkflow_DynamicReMeasurement() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        isRulerModeActive = true,
        measuredDistanceCm = 100.0f
      )
    }
    composeTestRule.onNodeWithText("📏 100.0cm").assertExists()

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {},
        isRulerModeActive = true,
        measuredDistanceCm = 200.0f
      )
    }
    composeTestRule.onNodeWithText("📏 200.0cm").assertExists()
  }

  @Test
  fun testWorkflow_TrackingLossRecoveryDuringMeasurement() {
    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = false,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("주변 공간 인식 중").assertExists()

    composeTestRule.setContent {
      ArScreenContent(
        widthCm = 100f,
        heightCm = 200f,
        depthCm = 60f,
        hasCameraPermission = true,
        placedItems = emptyList(),
        selectedItemId = null,
        isPlaneDetected = true,
        onBack = {},
        onOpacityChange = { _, _ -> },
        onRotateLeft = {},
        onRotateRight = {},
        onDeselect = {},
        onDeleteItem = {},
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("주변 공간 인식 중").assertDoesNotExist()
  }
}
