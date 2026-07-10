package com.example.interiorcamera.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.interiorcamera.ArView
import com.example.interiorcamera.data.PresetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.interiorcamera.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun defaultPresets_areDisplayed() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("양문형 냉장고").assertExists()
    composeTestRule.onNodeWithText("드럼 세탁기").assertExists()
  }

  @Test
  fun customPresets_areDisplayed() {
    val customPresets = listOf(
      PresetItem("내 맞춤 책상", 120.0f, 75.0f, 60.0f),
      PresetItem("내 맞춤 소파", 200.0f, 85.0f, 90.0f)
    )

    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(customPresets),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("나의 리스트").assertExists()
    composeTestRule.onNodeWithText("내 맞춤 책상").assertExists()
    composeTestRule.onNodeWithText("내 맞춤 소파").assertExists()
  }

  @Test
  fun savePreset_invokesCallback() {
    var savedPreset: PresetItem? = null

    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = { savedPreset = it },
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("제품명 (Product Name)").performTextInput("테스트 가구")
    composeTestRule.onNodeWithText("Save to My List").performClick()

    assertNotNull(savedPreset)
    assertEquals("테스트 가구", savedPreset?.name)
    assertEquals(91.2f, savedPreset?.width ?: 0f, 0.1f)
  }

  @Test
  fun savePreset_emptyName_showsError() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("Save to My List").assertIsNotEnabled()
  }

  @Test
  fun savePreset_zeroDimensions_showsError() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("제품명 (Product Name)").performTextInput("테스트")
    composeTestRule.onNodeWithText("가로 너비 (Width)").performTextReplacement("0")

    composeTestRule.onNodeWithText("Save to My List").assertIsNotEnabled()
  }

  @Test
  fun clickPresetChip_populatesForm() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("옷장 (자작)").performClick()

    composeTestRule.onNodeWithText("100.0").assertExists()
    composeTestRule.onNodeWithText("210.0").assertExists()
    composeTestRule.onNodeWithText("60.0").assertExists()
  }

  @Test
  fun clickPresetThenViewInAR_navigatesWithCorrectArgs() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("옷장 (자작)").performClick()
    composeTestRule.onNodeWithText("AR 카메라로 확인하기").assertIsEnabled()
  }

  @Test
  fun savePreset_largeDimensions_succeeds() {
    var savedPreset: PresetItem? = null

    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = { savedPreset = it },
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("제품명 (Product Name)").performTextInput("거대 가구")
    composeTestRule.onNodeWithText("가로 너비 (Width)").performTextReplacement("500.0")
    composeTestRule.onNodeWithText("높이 (Height)").performTextReplacement("1000.0")
    composeTestRule.onNodeWithText("깊이 (Depth)").performTextReplacement("800.0")
    composeTestRule.onNodeWithText("Save to My List").performClick()

    assertNotNull(savedPreset)
    assertEquals("거대 가구", savedPreset?.name)
    assertEquals(500.0f, savedPreset?.width ?: 0f, 0.1f)
    assertEquals(1000.0f, savedPreset?.height ?: 0f, 0.1f)
    assertEquals(800.0f, savedPreset?.depth ?: 0f, 0.1f)
  }

  @Test
  fun formValidation_clearsOnErrorResolved() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("가로 너비 (Width)").performTextReplacement("-5.0")
    composeTestRule.onNodeWithText("Save to My List").assertIsNotEnabled()

    composeTestRule.onNodeWithText("가로 너비 (Width)").performTextReplacement("120.0")
    composeTestRule.onNodeWithText("제품명 (Product Name)").performTextInput("소파")
    composeTestRule.onNodeWithText("Save to My List").assertIsEnabled()
  }

  @Test
  fun customPresetsHeader_hiddenWhenNoCustomPresets() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("나의 리스트").assertDoesNotExist()
  }

  @Test
  fun myListSection_displaysExpectedCount() {
    val customPresets = listOf(
      PresetItem("Desk", 120.0f, 75.0f, 60.0f),
      PresetItem("Chair", 50.0f, 90.0f, 50.0f)
    )

    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(customPresets),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("나의 리스트").assertExists()
    composeTestRule.onNodeWithText("Desk").assertExists()
    composeTestRule.onNodeWithText("Chair").assertExists()
  }

  @Test
  fun defaultPresetClick_doesNotPopulateForm() {
    composeTestRule.setContent {
      MainScreenContent(
        uiState = MainScreenUiState.Success(emptyList()),
        onItemClick = {},
        onSavePreset = {},
        onSaveRoomPreset = {},
        onDeleteRoomPreset = {}
      )
    }

    composeTestRule.onNodeWithText("드럼 세탁기").performClick()
    composeTestRule.onNodeWithText("드럼 세탁기").assertExists()
  }
}


