package com.example.interiorcamera.data

import java.util.UUID

data class PresetItem(
  val name: String,
  val width: Float,
  val height: Float,
  val depth: Float,
  val modelName: String = "cube.glb",
  val id: String = java.util.UUID.randomUUID().toString(),
  val isFavorite: Boolean = false
)
