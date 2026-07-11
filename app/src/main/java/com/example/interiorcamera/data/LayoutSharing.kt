package com.example.interiorcamera.data

import android.util.Base64
import com.example.interiorcamera.ui.floorplan.ArPlacedItem
import org.json.JSONArray
import org.json.JSONObject

object LayoutSharing {
  fun exportToCode(items: List<ArPlacedItem>): String {
    val jsonArray = JSONArray()
    for (item in items) {
      val obj = JSONObject()
      obj.put("n", item.name)
      obj.put("w", item.widthCm.toDouble())
      obj.put("h", item.heightCm.toDouble())
      obj.put("d", item.depthCm.toDouble())
      obj.put("m", item.modelName)
      obj.put("x", item.offsetX.toDouble())
      obj.put("z", item.offsetZ.toDouble())
      obj.put("r", item.rotationDegrees.toDouble())
      jsonArray.put(obj)
    }
    val rawBytes = jsonArray.toString().toByteArray(Charsets.UTF_8)
    return Base64.encodeToString(rawBytes, Base64.NO_WRAP or Base64.URL_SAFE)
  }

  fun importFromCode(code: String): List<ArPlacedItem> {
    if (code.isBlank()) return emptyList()
    return try {
      val decodedBytes = Base64.decode(code, Base64.NO_WRAP or Base64.URL_SAFE)
      val jsonString = String(decodedBytes, Charsets.UTF_8)
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<ArPlacedItem>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          ArPlacedItem(
            name = obj.getString("n"),
            widthCm = obj.getDouble("w").toFloat(),
            heightCm = obj.getDouble("h").toFloat(),
            depthCm = obj.getDouble("d").toFloat(),
            modelName = obj.getString("m"),
            offsetX = obj.getDouble("x").toFloat(),
            offsetZ = obj.getDouble("z").toFloat(),
            rotationDegrees = obj.getDouble("r").toFloat()
          )
        )
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }
}
