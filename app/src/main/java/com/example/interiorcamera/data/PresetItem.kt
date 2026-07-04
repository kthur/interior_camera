package com.example.interiorcamera.data

data class PresetItem(
  val name: String,
  val width: Float,
  val height: Float,
  val depth: Float,
  val modelName: String = "cube.glb"
)
