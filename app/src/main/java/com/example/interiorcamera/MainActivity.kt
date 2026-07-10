package com.example.interiorcamera

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.example.interiorcamera.theme.InteriorCameraTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val deepLinkKey = parseDeepLink(intent)

    enableEdgeToEdge()
    setContent {
      InteriorCameraTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(deepLinkKey = deepLinkKey)
        }
      }
    }
  }

  private fun parseDeepLink(intent: Intent?): NavKey? {
    val data = intent?.data ?: return null
    if (data.scheme == "fitcheck" && data.host == "ar") {
      val name = data.getQueryParameter("name") ?: "Custom Item"
      val w = data.getQueryParameter("w")?.toFloatOrNull() ?: 100f
      val h = data.getQueryParameter("h")?.toFloatOrNull() ?: 100f
      val d = data.getQueryParameter("d")?.toFloatOrNull() ?: 100f
      val model = data.getQueryParameter("model") ?: "cube.glb"
      
      val arItem = ArItem(name = name, widthCm = w, heightCm = h, depthCm = d, modelName = model)
      return ArView(
        widthCm = w,
        heightCm = h,
        depthCm = d,
        modelName = model,
        calibrationFactor = 1.0f,
        items = listOf(arItem)
      )
    }
    return null
  }
}
