package com.example.interiorcamera.ui.ar

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

@Composable
fun ArScreen(
  widthCm: Float,
  heightCm: Float,
  depthCm: Float,
  modelName: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

  val engine = rememberEngine()
  val modelLoader = rememberModelLoader(engine)
  var frame by remember { mutableStateOf<Frame?>(null) }
  var anchor by remember { mutableStateOf<Anchor?>(null) }

  Box(modifier = modifier.fillMaxSize()) {
    if (hasCameraPermission) {
      ARSceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        planeRenderer = true,
        onSessionFailed = { exception ->
          Toast.makeText(
            context,
            "AR 실행 실패: 이 기기가 ARCore를 지원하지 않거나 필수 서비스(Google Play Services for AR)가 설치되어 있지 않습니다.",
            Toast.LENGTH_LONG
          ).show()
          onBack()
        },
        onSessionUpdated = { _, updatedFrame ->
          frame = updatedFrame
        },
        onGestureListener = rememberOnGestureListener(
          onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
            if (node == null) {
              val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
              val newAnchor = hitResults?.firstOrNull()?.createAnchor()
              if (newAnchor != null) {
                anchor = newAnchor
              }
            }
            true
          }
        )
      ) {
        anchor?.let { currentAnchor ->
          AnchorNode(anchor = currentAnchor) {
            val w = widthCm / 100f
            val h = heightCm / 100f
            val d = depthCm / 100f

            val modelInstance = rememberModelInstance(modelLoader, "models/$modelName")
            if (modelInstance != null) {
              ModelNode(
                modelInstance = modelInstance,
                scale = Scale(w, h, d),
                position = Position(0f, h / 2f, 0f)
              )
            }
          }
        }
      }

      // UI Overlay
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Card(
          modifier = Modifier.padding(bottom = 16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
          )
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "가구/가전 크기:",
              style = MaterialTheme.typography.titleMedium
            )
            Text(
              text = "가로: ${widthCm}cm x 세로: ${heightCm}cm x 깊이: ${depthCm}cm",
              style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = anchor?.let { "화면을 터치하여 위치를 이동할 수 있습니다." }
                ?: "바닥(평면)을 비추고 점들이 나타나면 터치하여 배치하세요.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (anchor != null) {
            Button(
              onClick = { anchor = null },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text("지우기")
            }
          }
          Button(
            onClick = onBack,
            modifier = Modifier.weight(1f)
          ) {
            Text("뒤로 가기")
          }
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("카메라 권한이 없습니다. 메인 화면으로 돌아가 권한을 수락해 주세요.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
          Text("돌아가기")
        }
      }
    }
  }
}
