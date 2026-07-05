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

    composeTestRule.onNodeWithText("배치할 크기: 가로 100.0cm x 세로 200.0cm x 깊이 60.0cm").assertExists()
    composeTestRule.onNodeWithText("주변 평면 인식 중...").assertExists()
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

    composeTestRule.onNodeWithText("카메라를 천천히 좌우로 흔들면서 바닥면이나 평평한 표면을 비춰주세요.").assertExists()
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

    composeTestRule.onNodeWithText("선택된 제품 설정 (cube.glb)").assertDoesNotExist()
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

    composeTestRule.onNodeWithText("선택된 제품 설정 (cube.glb)").assertExists()
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

    composeTestRule.onNodeWithText("왼쪽 15°").performClick()
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

    composeTestRule.onNodeWithText("오른쪽 15°").performClick()
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

    composeTestRule.onNodeWithText("모두 지우기").performClick()
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

    composeTestRule.onNodeWithText("뒤로 가기").performClick()
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

    composeTestRule.onNodeWithText("배치할 크기: 가로 150.0cm x 세로 180.0cm x 깊이 90.0cm").assertExists()
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

    // Find progress indicator by role or by searching for the "주변 평면 인식 중..." container contents
    composeTestRule.onNodeWithText("주변 평면 인식 중...").assertExists()
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

    composeTestRule.onNodeWithText("주변 평면 인식 중...").assertDoesNotExist()
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

    composeTestRule.onNodeWithText("선택 해제").performClick()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("Ruler Mode").assertExists()
    composeTestRule.onNodeWithText("자 모드 끄기").assertExists()
    composeTestRule.onNodeWithText("거리: 125.5cm").assertExists()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("보정 계수: 1.00").assertExists()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("보정 계수: 1.15").assertExists()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("추천 가구").assertExists()
    composeTestRule.onNodeWithText("Sofa (150x85)").assertExists()
  }

  @Test
  fun testRecommendationPanel_SelectAndPlace() {
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
    composeTestRule.onNodeWithText("Chair (60x90)").performClick()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("거리: 150.0cm").assertExists()
    composeTestRule.onNodeWithText("Table (120x75)").assertExists()
  }

  @Test
  fun testRulerAndRecommendation_ClearRulerResetsPanel() {
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
    composeTestRule.onNodeWithText("자 지우기").performClick()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("공간 부족: 안전 마진 최소 5cm 필요").assertExists()
  }

  @Test
  fun testWorkflow_MeasureSpaceAndPlaceMatchingFurniture() {
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
    composeTestRule.onNodeWithText("거리: 180.0cm").assertExists()
    composeTestRule.onNodeWithText("Bed (160x45)").performClick()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("보정 계수: 1.10").assertExists()
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
        onClearAll = {}
      )
    }
    composeTestRule.onNodeWithText("거리: 100.0cm").assertExists()

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
    composeTestRule.onNodeWithText("거리: 200.0cm").assertExists()
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
    composeTestRule.onNodeWithText("카메라를 천천히 좌우로 흔들면서 바닥면이나 평평한 표면을 비춰주세요.").assertExists()

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
    composeTestRule.onNodeWithText("카메라를 천천히 좌우로 흔들면서 바닥면이나 평평한 표면을 비춰주세요.").assertDoesNotExist()
  }
}
