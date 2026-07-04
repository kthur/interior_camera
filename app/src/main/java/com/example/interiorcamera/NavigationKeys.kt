package com.example.interiorcamera

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable
data class ArItem(
  val name: String = "",
  val widthCm: Float,
  val heightCm: Float,
  val depthCm: Float,
  val modelName: String
)

@Serializable
data class ArView(
  val widthCm: Float,
  val heightCm: Float,
  val depthCm: Float,
  val modelName: String,
  val calibrationFactor: Float = 1.0f,
  val items: List<ArItem> = emptyList()
) : NavKey
