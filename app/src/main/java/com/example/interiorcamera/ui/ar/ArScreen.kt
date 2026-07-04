package com.example.interiorcamera.ui.ar

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.filament.RenderableManager
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

data class PlacedItem(
  val id: String,
  val anchor: Anchor,
  val widthCm: Float,
  val heightCm: Float,
  val depthCm: Float,
  val modelName: String,
  val rotationDegrees: Float = 0f,
  val opacity: Float = 0.8f
)

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
  
  var placedItems by remember { mutableStateOf(emptyList<PlacedItem>()) }
  var selectedItemId by remember { mutableStateOf<String?>(null) }
  var isPlaneDetected by remember { mutableStateOf(false) }

  ArScreenContent(
    widthCm = widthCm,
    heightCm = heightCm,
    depthCm = depthCm,
    hasCameraPermission = hasCameraPermission,
    placedItems = placedItems,
    selectedItemId = selectedItemId,
    isPlaneDetected = isPlaneDetected,
    onBack = onBack,
    onOpacityChange = { item, newOpacity ->
      placedItems = placedItems.map {
        if (it.id == item.id) it.copy(opacity = newOpacity) else it
      }
    },
    onRotateLeft = { item ->
      placedItems = placedItems.map {
        if (it.id == item.id) {
          it.copy(rotationDegrees = (it.rotationDegrees - 15f + 360f) % 360f)
        } else it
      }
    },
    onRotateRight = { item ->
      placedItems = placedItems.map {
        if (it.id == item.id) {
          it.copy(rotationDegrees = (it.rotationDegrees + 15f) % 360f)
        } else it
      }
    },
    onDeselect = { selectedItemId = null },
    onDelete = { item ->
      placedItems = placedItems.filter { it.id != item.id }
      selectedItemId = null
    },
    onClearAll = {
      placedItems = emptyList()
      selectedItemId = null
    },
    modifier = modifier,
    arSceneViewContent = {
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
          onSessionUpdated = { session, updatedFrame ->
            frame = updatedFrame
            if (!isPlaneDetected) {
              val planes = session.getAllTrackables(com.google.ar.core.Plane::class.java)
              if (planes.isNotEmpty()) {
                isPlaneDetected = true
              }
            }
          },
          onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
              if (node != null) {
                var parentNode: Node? = node
                while (parentNode != null && parentNode !is AnchorNode) {
                  parentNode = parentNode.parent
                }
                if (parentNode is AnchorNode) {
                  val anchorRef = parentNode.anchor
                  val found = placedItems.find { it.anchor == anchorRef }
                  if (found != null) {
                    selectedItemId = found.id
                  }
                }
              } else {
                val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                val newAnchor = hitResults?.firstOrNull()?.createAnchor()
                if (newAnchor != null) {
                  val newItem = PlacedItem(
                    id = java.util.UUID.randomUUID().toString(),
                    anchor = newAnchor,
                    widthCm = widthCm,
                    heightCm = heightCm,
                    depthCm = depthCm,
                    modelName = modelName,
                    rotationDegrees = 0f,
                    opacity = 0.8f
                  )
                  placedItems = placedItems + newItem
                  selectedItemId = newItem.id
                } else {
                  selectedItemId = null
                }
              }
              true
            }
          )
        ) {
          placedItems.forEach { item ->
            key(item.id) {
              AnchorNode(anchor = item.anchor) {
                val w = item.widthCm / 100f
                val h = item.heightCm / 100f
                val d = item.depthCm / 100f

                val modelInstance = rememberModelInstance(modelLoader, "models/${item.modelName}")

                // Apply opacity to all material instances.
                // Each GLB material instance exposes a "baseColorFactor" float4
                // whose alpha component is honoured when the material's blending is
                // TRANSPARENT.  SceneView's glTF importer already marks materials
                // with alphaMode == BLEND as transparent, so setting the alpha here
                // works for those primitives.  For OPAQUE primitives we fall back
                // to the Filament RenderableManager layer to forcibly set the
                // priority — at worst the model stays opaque, which is acceptable.
                LaunchedEffect(item.opacity, modelInstance) {
                  modelInstance?.let { mi ->
                    mi.materialInstances.forEach { mat ->
                      try {
                        // Standard glTF parameter name used by SceneView's Filament ubershader
                        mat.setParameter("baseColorFactor", item.opacity, item.opacity, item.opacity, item.opacity)
                      } catch (_: Exception) { /* parameter may not exist on some materials */ }
                    }
                    // Force-set blend priority on the entity so semi-transparent models
                    // sort correctly against the AR camera background.
                    try {
                      val rm = engine.renderableManager
                      val inst = rm.getInstance(mi.root)
                      if (rm.hasComponent(mi.root)) {
                        rm.setLayerMask(inst, 0xff, 0xff)
                      }
                    } catch (_: Exception) { }
                  }
                }

                if (modelInstance != null) {
                  ModelNode(
                    modelInstance = modelInstance,
                    scale = Scale(w, h, d),
                    rotation = Rotation(0f, item.rotationDegrees, 0f),
                    position = Position(0f, h / 2f, 0f)
                  )
                }
              }
            }
          }
        }
      }
    }
  )
}

@Composable
fun ArScreenContent(
  widthCm: Float,
  heightCm: Float,
  depthCm: Float,
  hasCameraPermission: Boolean,
  placedItems: List<PlacedItem>,
  selectedItemId: String?,
  isPlaneDetected: Boolean,
  onBack: () -> Unit,
  onOpacityChange: (PlacedItem, Float) -> Unit,
  onRotateLeft: (PlacedItem) -> Unit,
  onRotateRight: (PlacedItem) -> Unit,
  onDeselect: () -> Unit,
  onDelete: (PlacedItem) -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier,
  arSceneViewContent: @Composable BoxScope.() -> Unit = {}
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (hasCameraPermission) {
      Box(modifier = Modifier.fillMaxSize()) {
        arSceneViewContent()
      }

      val selectedItem = placedItems.find { it.id == selectedItemId }

      Card(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 48.dp, start = 16.dp, end = 16.dp)
          .fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "배치할 크기: 가로 ${widthCm}cm x 세로 ${heightCm}cm x 깊이 ${depthCm}cm",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "바닥을 터치하여 원하는 개수만큼 가구를 배치해 보세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (selectedItem != null) {
          Card(
            modifier = Modifier.padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "선택된 제품 설정 (${selectedItem.modelName})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
              )
              
              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "투명도 조절: ${(selectedItem.opacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
              )
              Slider(
                value = selectedItem.opacity,
                onValueChange = { newOpacity ->
                  onOpacityChange(selectedItem, newOpacity)
                },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = "정밀 회전 조작",
                style = MaterialTheme.typography.bodyMedium
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { onRotateLeft(selectedItem) },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("왼쪽 15°")
                }
                Button(
                  onClick = { onRotateRight(selectedItem) },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("오른쪽 15°")
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = onDeselect,
                  modifier = Modifier.weight(1f)
                ) {
                  Text("선택 해제")
                }
                Button(
                  onClick = { onDelete(selectedItem) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("삭제")
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (placedItems.isNotEmpty() && selectedItemId == null) {
            Button(
              onClick = onClearAll,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text("모두 지우기")
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

      if (!isPlaneDetected) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
              )
              Text(
                text = "주변 평면 인식 중...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "카메라를 천천히 좌우로 흔들면서 바닥면이나 평평한 표면을 비춰주세요.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
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

