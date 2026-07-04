package com.example.interiorcamera.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ChallengerPresetStorageTest {

  private lateinit var context: Context
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var editor: SharedPreferences.Editor

  @Before
  fun setUp() {
    context = mock(Context::class.java)
    sharedPreferences = mock(SharedPreferences::class.java)
    editor = mock(SharedPreferences.Editor::class.java)

    `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
    `when`(sharedPreferences.edit()).thenReturn(editor)
    `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
  }

  @Test
  fun testLoadPresets_nullJSON_returnsEmptyList() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }

  @Test
  fun testLoadPresets_corruptedJSON_returnsEmptyList() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn("{invalid json string")
    val repository = DefaultDataRepository(context)

    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }

  @Test
  fun testLoadPresets_missingFieldsJSON_returnsEmptyList() = runTest {
    // missing width, height, depth
    val corruptedArray = JSONArray().apply {
      put(JSONObject().apply {
        put("name", "Test Item")
      })
    }
    `when`(sharedPreferences.getString("presets", null)).thenReturn(corruptedArray.toString())
    val repository = DefaultDataRepository(context)

    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }

  @Test
  fun testSavePreset_savesMultipleCorrectly() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset1 = PresetItem("Desk", 120f, 75f, 60f)
    val preset2 = PresetItem("Chair", 50f, 90f, 50f)

    repository.savePreset(preset1)

    // Capture the JSON string saved in SharedPreferences
    val jsonStringCaptor = org.mockito.ArgumentCaptor.forClass(String::class.java)
    verify(editor).putString(eq("presets"), jsonStringCaptor.capture())

    // Mock that SharedPreferences now returns the saved JSON array
    val savedJson = jsonStringCaptor.value
    `when`(sharedPreferences.getString("presets", null)).thenReturn(savedJson)

    // Reload repository to test deserialization
    val reloadedRepo = DefaultDataRepository(context)
    assertEquals(1, reloadedRepo.data.first().size)
    assertEquals(preset1, reloadedRepo.data.first()[0])

    // Save second preset
    reloadedRepo.savePreset(preset2)

    val jsonStringCaptor2 = org.mockito.ArgumentCaptor.forClass(String::class.java)
    verify(editor, times(2)).putString(eq("presets"), jsonStringCaptor2.capture())

    // Verify both are present in the final list
    val finalList = reloadedRepo.data.first()
    assertEquals(2, finalList.size)
    assertEquals(preset1, finalList[0])
    assertEquals(preset2, finalList[1])
  }

  @Test
  fun testSavePreset_handlesExtremelyLargeAndSmallDimensions() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val largePreset = PresetItem("Huge", Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
    val smallPreset = PresetItem("Tiny", 0.0001f, 0.0001f, 0.0001f)

    repository.savePreset(largePreset)
    repository.savePreset(smallPreset)

    val list = repository.data.first()
    assertEquals(2, list.size)
    assertEquals(largePreset, list[0])
    assertEquals(smallPreset, list[1])
  }

  @Test
  fun testSavePreset_handlesEmptyNameAndNegativeDimensions() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    // Although the UI prevents these, the database repository should handle storing/deserializing them
    val negativePreset = PresetItem("", -10.5f, 0f, -5f)

    repository.savePreset(negativePreset)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(negativePreset, list[0])
  }
}
