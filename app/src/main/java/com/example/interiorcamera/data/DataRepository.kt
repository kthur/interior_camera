package com.example.interiorcamera.data

import android.content.Context
import com.example.interiorcamera.ui.floorplan.ArPlacedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface DataRepository {
  val data: Flow<List<PresetItem>>
  val roomPresets: Flow<List<RoomPreset>>
  suspend fun savePreset(preset: PresetItem)
  suspend fun deletePreset(presetId: String)
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
          val id = obj.optString("id", java.util.UUID.randomUUID().toString())
          val name = obj.getString("name")
          val width = obj.getDouble("width").toFloat()
          val height = obj.getDouble("height").toFloat()
          val depth = obj.getDouble("depth").toFloat()
          val modelName = obj.optString("modelName", "cube.glb")
          list.add(PresetItem(name = name, width = width, height = height, depth = depth, modelName = modelName, id = id))
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

    savePresetsToSharedPrefs(currentList)
  }

  override suspend fun deletePreset(presetId: String) {
    val currentList = _data.value.toMutableList()
    currentList.removeAll { it.id == presetId }

    savePresetsToSharedPrefs(currentList)
  }

  private fun savePresetsToSharedPrefs(list: List<PresetItem>) {
    val jsonArray = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("name", item.name)
      obj.put("width", item.width.toDouble())
      obj.put("height", item.height.toDouble())
      obj.put("depth", item.depth.toDouble())
      obj.put("modelName", item.modelName)
      jsonArray.put(obj)
    }

    sharedPreferences.edit().putString("presets", jsonArray.toString()).apply()
    _data.value = list
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
          
          val itemsList = mutableListOf<ArPlacedItem>()
          val itemsArray = obj.optJSONArray("items")
          if (itemsArray != null) {
            for (j in 0 until itemsArray.length()) {
              val itemObj = itemsArray.getJSONObject(j)
              itemsList.add(
                ArPlacedItem(
                  name = itemObj.getString("name"),
                  widthCm = itemObj.getDouble("widthCm").toFloat(),
                  heightCm = itemObj.getDouble("heightCm").toFloat(),
                  depthCm = itemObj.getDouble("depthCm").toFloat(),
                  modelName = itemObj.getString("modelName"),
                  offsetX = itemObj.getDouble("offsetX").toFloat(),
                  offsetZ = itemObj.getDouble("offsetZ").toFloat(),
                  rotationDegrees = itemObj.getDouble("rotationDegrees").toFloat()
                )
              )
            }
          }
          val isFavorite = obj.optBoolean("isFavorite", false)
          list.add(RoomPreset(id, name, width, depth, timestamp, itemsList, isFavorite))
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
      obj.put("isFavorite", item.isFavorite)
      
      val itemsArray = JSONArray()
      for (arItem in item.items) {
        val itemObj = JSONObject()
        itemObj.put("name", arItem.name)
        itemObj.put("widthCm", arItem.widthCm.toDouble())
        itemObj.put("heightCm", arItem.heightCm.toDouble())
        itemObj.put("depthCm", arItem.depthCm.toDouble())
        itemObj.put("modelName", arItem.modelName)
        itemObj.put("offsetX", arItem.offsetX.toDouble())
        itemObj.put("offsetZ", arItem.offsetZ.toDouble())
        itemObj.put("rotationDegrees", arItem.rotationDegrees.toDouble())
        itemsArray.put(itemObj)
      }
      obj.put("items", itemsArray)
      jsonArray.put(obj)
    }

    sharedPreferences.edit().putString("room_presets", jsonArray.toString()).apply()
    _roomPresets.value = list
  }
}


