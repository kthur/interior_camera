package com.example.interiorcamera

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable
data class ArView(
  val widthCm: Float,
  val heightCm: Float,
  val depthCm: Float,
  val modelName: String
) : NavKey
