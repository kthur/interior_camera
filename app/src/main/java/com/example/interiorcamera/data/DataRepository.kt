package com.example.interiorcamera.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface DataRepository {
  val data: Flow<List<PresetItem>>
  suspend fun savePreset(preset: PresetItem)
}

class DefaultDataRepository(private val context: Context) : DataRepository {
  private val sharedPreferences = context.getSharedPreferences("preset_storage", Context.MODE_PRIVATE)
  private val _data = MutableStateFlow<List<PresetItem>>(emptyList())
  override val data: Flow<List<PresetItem>> = _data.asStateFlow()

  init {
    loadPresets()
  }

  private fun loadPresets() {
    val jsonString = sharedPreferences.getString("presets", null)
    if (jsonString != null) {
      try {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<PresetItem>()
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val name = obj.getString("name")
          val width = obj.getDouble("width").toFloat()
          val height = obj.getDouble("height").toFloat()
          val depth = obj.getDouble("depth").toFloat()
          val modelName = obj.optString("modelName", "cube.glb")
          list.add(PresetItem(name, width, height, depth, modelName))
        }
        _data.value = list
      } catch (e: Exception) {
        _data.value = emptyList()
      }
    } else {
      _data.value = emptyList()
    }
  }

  override suspend fun savePreset(preset: PresetItem) {
    val currentList = _data.value.toMutableList()
    currentList.add(preset)

    val jsonArray = JSONArray()
    for (item in currentList) {
      val obj = JSONObject()
      obj.put("name", item.name)
      obj.put("width", item.width.toDouble())
      obj.put("height", item.height.toDouble())
      obj.put("depth", item.depth.toDouble())
      obj.put("modelName", item.modelName)
      jsonArray.put(obj)
    }

    sharedPreferences.edit().putString("presets", jsonArray.toString()).apply()
    _data.value = currentList
  }
}

