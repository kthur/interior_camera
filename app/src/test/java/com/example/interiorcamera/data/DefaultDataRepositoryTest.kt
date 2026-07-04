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
import org.json.JSONException

class DefaultDataRepositoryTest {

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
  fun testSaveNormalPreset() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset = PresetItem("Normal", 10f, 20f, 30f)
    repository.savePreset(preset)

    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])

    verify(editor).putString(eq("presets"), anyString())
  }

  @Test
  fun testSavePresetWithInfinity_throwsException() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    val preset = PresetItem("Infinite", Float.POSITIVE_INFINITY, 20f, 30f)

    var threwException = false
    try {
      repository.savePreset(preset)
    } catch (e: JSONException) {
      threwException = true
    } catch (e: Exception) {
      if (e.message?.contains("JSON") == true || e.cause is JSONException) {
        threwException = true
      } else {
        throw e
      }
    }
    assertTrue("Expected JSONException due to Float.POSITIVE_INFINITY", threwException)
  }

  @Test
  fun testSavePresetWithEmptyName() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val preset = PresetItem("", 10f, 20f, 30f)
    repository.savePreset(preset)
    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSavePresetWithZeroDimensions() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val preset = PresetItem("Zero", 0f, 0f, 0f)
    repository.savePreset(preset)
    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSavePresetWithNegativeDimensions() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val preset = PresetItem("Negative", -10f, -20f, -30f)
    repository.savePreset(preset)
    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSavePresetWithHugeDimensions() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val preset = PresetItem("Huge", 100000f, 200000f, 300000f)
    repository.savePreset(preset)
    val list = repository.data.first()
    assertEquals(1, list.size)
    assertEquals(preset, list[0])
  }

  @Test
  fun testSavePresetMultipleItems() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val preset1 = PresetItem("Item 1", 10f, 20f, 30f)
    val preset2 = PresetItem("Item 2", 40f, 50f, 60f)
    repository.savePreset(preset1)
    
    val jsonStringCaptor = org.mockito.ArgumentCaptor.forClass(String::class.java)
    verify(editor).putString(eq("presets"), jsonStringCaptor.capture())
    `when`(sharedPreferences.getString("presets", null)).thenReturn(jsonStringCaptor.value)
    
    val reloadedRepo = DefaultDataRepository(context)
    reloadedRepo.savePreset(preset2)
    
    val list = reloadedRepo.data.first()
    assertEquals(2, list.size)
    assertEquals(preset1, list[0])
    assertEquals(preset2, list[1])
  }

  @Test
  fun testLoadPresetsWhenEmpty() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }

  @Test
  fun testSaveDuplicatePresetName() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)
    val preset1 = PresetItem("Duplicate", 10f, 20f, 30f)
    val preset2 = PresetItem("Duplicate", 40f, 50f, 60f)
    repository.savePreset(preset1)
    
    val jsonStringCaptor = org.mockito.ArgumentCaptor.forClass(String::class.java)
    verify(editor).putString(eq("presets"), jsonStringCaptor.capture())
    `when`(sharedPreferences.getString("presets", null)).thenReturn(jsonStringCaptor.value)
    
    val reloadedRepo = DefaultDataRepository(context)
    reloadedRepo.savePreset(preset2)
    
    val list = reloadedRepo.data.first()
    assertEquals(2, list.size)
    assertEquals(preset1, list[0])
    assertEquals(preset2, list[1])
  }

  @Test
  fun testLoadCorruptedPresetsFallback() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn("corrupted JSON")
    val repository = DefaultDataRepository(context)
    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }
}

