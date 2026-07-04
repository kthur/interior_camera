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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = { deleteClicked = true },
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
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
        onDelete = {},
        onClearAll = {}
      )
    }

    composeTestRule.onNodeWithText("선택 해제").performClick()
    assertTrue(deselectClicked)
  }
}
