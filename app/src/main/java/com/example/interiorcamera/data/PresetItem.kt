package com.example.interiorcamera.data

import java.util.UUID

data class PresetItem(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val width: Float,
  val height: Float,
  val depth: Float,
  val modelName: String = "cube.glb"
)
