package com.example.interiorcamera.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.json.JSONArray
import org.json.JSONObject

class DefaultDataRepositoryExtraTest {

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
  fun testSaveEmptyNamePreset() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset = PresetItem("", 10f, 20f, 30f)
    repository.savePreset(preset)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSaveNegativeDimensionsPreset() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset = PresetItem("Negative", -10f, -20f, -30f)
    repository.savePreset(preset)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSaveZeroDimensionsPreset() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset = PresetItem("Zero", 0f, 0f, 0f)
    repository.savePreset(preset)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSaveLargeDimensionsPreset() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset = PresetItem("Large", 999999f, 999999f, 999999f)
    repository.savePreset(preset)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSaveMultiplePresetsAndLoad() = runTest {
    val initialJson = JSONArray().apply {
      put(JSONObject().apply {
        put("name", "Desk")
        put("width", 120.0)
        put("height", 75.0)
        put("depth", 60.0)
        put("modelName", "cube.glb")
      })
    }.toString()

    `when`(sharedPreferences.getString("presets", null)).thenReturn(initialJson)
    val repository = DefaultDataRepository(context)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals("Desk", list[0].name)

    val preset2 = PresetItem("Chair", 50f, 90f, 50f)
    repository.savePreset(preset2)

    val updatedList = repository.data.first()
    assertEquals(2, updatedList.size)
    assertEquals("Desk", updatedList[0].name)
    assertEquals("Chair", updatedList[1].name)
  }

  @Test
  fun testLoadMalformedJson() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn("malformed json {")
    val repository = DefaultDataRepository(context)

    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }
}
