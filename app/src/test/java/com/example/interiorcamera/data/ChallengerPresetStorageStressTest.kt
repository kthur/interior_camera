package com.example.interiorcamera.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ChallengerPresetStorageStressTest {

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
  fun testSavePresetWithSpecialCharacters() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    // Name with quotes, backslashes, tabs, newlines, emojis, and non-ASCII characters
    val specialName = "Desk \n\t \"' \\ / <xml> 😀 한국어"
    val preset = PresetItem(specialName, 120.5f, 75.0f, 60.2f, "refrigerator.glb")

    repository.savePreset(preset)

    // Capture saved JSON and mock it for reload
    val jsonStringCaptor = org.mockito.ArgumentCaptor.forClass(String::class.java)
    verify(editor).putString(eq("presets"), jsonStringCaptor.capture())
    val savedJson = jsonStringCaptor.value
    `when`(sharedPreferences.getString("presets", null)).thenReturn(savedJson)

    // Reload repository
    val reloadedRepo = DefaultDataRepository(context)
    val list = reloadedRepo.data.first()

    assertEquals(1, list.size)
    assertEquals(specialName, list[0].name)
    assertEquals(120.5f, list[0].width)
    assertEquals(75.0f, list[0].height)
    assertEquals(60.2f, list[0].depth)
    assertEquals("refrigerator.glb", list[0].modelName)
  }

  @Test
  fun testLoadPresetsWithNullNameJson() = runTest {
    // Construct JSON where "name" is null
    val jsonString = "[{\"name\":null,\"width\":10.0,\"height\":20.0,\"depth\":30.0,\"modelName\":\"cube.glb\"}]"
    `when`(sharedPreferences.getString("presets", null)).thenReturn(jsonString)

    val repository = DefaultDataRepository(context)
    val list = repository.data.first()

    // Since name is null, JSON parsing should fail or return "null" as string (JSONObject.getString("name") handles null differently in various environments).
    // If it fails, loadPresets falls back to emptyList(). If it succeeds, it might contain "null".
    // Either way, it should not crash the app.
    // Let's assert it doesn't crash, and is either empty or has name "null".
    assertTrue(list.isEmpty() || list[0].name == "null")
  }

  @Test
  fun testLoadPresetsWithMissingFieldsJson() = runTest {
    // Construct JSON missing "width"
    val jsonString = "[{\"name\":\"Desk\",\"height\":20.0,\"depth\":30.0,\"modelName\":\"cube.glb\"}]"
    `when`(sharedPreferences.getString("presets", null)).thenReturn(jsonString)

    val repository = DefaultDataRepository(context)
    val list = repository.data.first()

    // Missing fields should throw JSONException and fallback to emptyList()
    assertTrue(list.isEmpty())
  }

  @Test
  fun testLoadPresetsWithInvalidTypesJson() = runTest {
    // Construct JSON where "width" is a string "invalid" instead of double
    val jsonString = "[{\"name\":\"Desk\",\"width\":\"invalid\",\"height\":20.0,\"depth\":30.0,\"modelName\":\"cube.glb\"}]"
    `when`(sharedPreferences.getString("presets", null)).thenReturn(jsonString)

    val repository = DefaultDataRepository(context)
    val list = repository.data.first()

    // Invalid types should throw JSONException and fallback to emptyList()
    assertTrue(list.isEmpty())
  }

  @Test
  fun testSaveAndLoad1000Presets() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    var currentJsonString: String? = null
    `when`(sharedPreferences.getString("presets", null)).thenAnswer { currentJsonString }

    val editorMock = mock(SharedPreferences.Editor::class.java)
    `when`(sharedPreferences.edit()).thenReturn(editorMock)
    `when`(editorMock.putString(anyString(), anyString())).thenAnswer { invocation ->
      currentJsonString = invocation.arguments[1] as String
      editorMock
    }

    // Save 1000 presets
    for (i in 1..1000) {
      repository.savePreset(PresetItem("Item $i", i.toFloat(), i.toFloat(), i.toFloat()))
    }

    // Reload repository
    val reloadedRepo = DefaultDataRepository(context)
    val list = reloadedRepo.data.first()

    assertEquals(1000, list.size)
    for (i in 1..1000) {
      val item = list[i - 1]
      assertEquals("Item $i", item.name)
      assertEquals(i.toFloat(), item.width)
      assertEquals(i.toFloat(), item.height)
      assertEquals(i.toFloat(), item.depth)
    }
  }

  @Test
  fun testSavePresetWithHugeName() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn(null)
    val repository = DefaultDataRepository(context)

    // Name size: 100,000 characters
    val hugeName = "A".repeat(100000)
    val preset = PresetItem(hugeName, 10f, 20f, 30f)

    repository.savePreset(preset)

    val jsonStringCaptor = org.mockito.ArgumentCaptor.forClass(String::class.java)
    verify(editor).putString(eq("presets"), jsonStringCaptor.capture())
    val savedJson = jsonStringCaptor.value
    `when`(sharedPreferences.getString("presets", null)).thenReturn(savedJson)

    val reloadedRepo = DefaultDataRepository(context)
    val list = reloadedRepo.data.first()

    assertEquals(1, list.size)
    assertEquals(hugeName, list[0].name)
  }

  @Test
  fun testLoadPresetsWithEmptyArrayJson() = runTest {
    `when`(sharedPreferences.getString("presets", null)).thenReturn("[]")
    val repository = DefaultDataRepository(context)
    val list = repository.data.first()
    assertTrue(list.isEmpty())
  }
}
