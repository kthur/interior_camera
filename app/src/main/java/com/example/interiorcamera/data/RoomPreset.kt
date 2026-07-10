package com.example.interiorcamera.data

import com.example.interiorcamera.ui.floorplan.ArPlacedItem

data class RoomPreset(
  val id: String,
  val name: String,
  val widthCm: Float,
  val depthCm: Float,
  val timestamp: Long,
  val items: List<ArPlacedItem> = emptyList()
)
