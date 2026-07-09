package com.example.interiorcamera.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface DataRepository {
  val data: Flow<List<PresetItem>>
  val roomPresets: Flow<List<RoomPreset>>
  suspend fun savePreset(preset: PresetItem)
  suspend fun saveRoomPreset(preset: RoomPreset)
  suspend fun deleteRoomPreset(presetId: String)
}

class DefaultDataRepository(private val context: Context) : DataRepository {
  private val sharedPreferences = context.getSharedPreferences("preset_storage", Context.MODE_PRIVATE)
  private val _data = MutableStateFlow<List<PresetItem>>(emptyList())
  override val data: Flow<List<PresetItem>> = _data.asStateFlow()

  private val _roomPresets = MutableStateFlow<List<RoomPreset>>(emptyList())
  override val roomPresets: Flow<List<RoomPreset>> = _roomPresets.asStateFlow()

  init {
    loadPresets()
    loadRoomPresets()
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

  private fun loadRoomPresets() {
    val jsonString = sharedPreferences.getString("room_presets", null)
    if (jsonString != null) {
      try {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<RoomPreset>()
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val id = obj.getString("id")
          val name = obj.getString("name")
          val width = obj.getDouble("width").toFloat()
          val depth = obj.getDouble("depth").toFloat()
          val timestamp = obj.getLong("timestamp")
          list.add(RoomPreset(id, name, width, depth, timestamp))
        }
        _roomPresets.value = list
      } catch (e: Exception) {
        _roomPresets.value = emptyList()
      }
    } else {
      _roomPresets.value = emptyList()
    }
  }

  override suspend fun saveRoomPreset(preset: RoomPreset) {
    val currentList = _roomPresets.value.toMutableList()
    currentList.removeAll { it.id == preset.id }
    currentList.add(preset)

    saveRoomPresetsToSharedPrefs(currentList)
  }

  override suspend fun deleteRoomPreset(presetId: String) {
    val currentList = _roomPresets.value.toMutableList()
    currentList.removeAll { it.id == presetId }

    saveRoomPresetsToSharedPrefs(currentList)
  }

  private fun saveRoomPresetsToSharedPrefs(list: List<RoomPreset>) {
    val jsonArray = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("name", item.name)
      obj.put("width", item.widthCm.toDouble())
      obj.put("depth", item.depthCm.toDouble())
      obj.put("timestamp", item.timestamp)
      jsonArray.put(obj)
    }

    sharedPreferences.edit().putString("room_presets", jsonArray.toString()).apply()
    _roomPresets.value = list
  }
}


