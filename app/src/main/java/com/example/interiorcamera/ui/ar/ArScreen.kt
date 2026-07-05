package com.example.interiorcamera.ui.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.MotionEvent
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import com.google.android.filament.Box
import com.google.android.filament.RenderableManager
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Rotation
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import com.example.interiorcamera.ArItem
import com.example.interiorcamera.data.RecommendedFurniture
import kotlin.math.atan2
import kotlin.math.sqrt

private const val MAX_HIT_DISTANCE_M = 5.0f
private const val NUDGE_STEP_M = 0.05f

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

sealed interface ArAction {
  data class Place(val item: PlacedItem) : ArAction
  data class Delete(val item: PlacedItem) : ArAction
  data class ClearAll(val items: List<PlacedItem>) : ArAction
  data class Rotate(val itemId: String, val oldDeg: Float, val newDeg: Float) : ArAction
  data class Opacity(val itemId: String, val oldVal: Float, val newVal: Float) : ArAction
}

private fun selectBestHit(hitResults: List<HitResult>?): HitResult? {
  if (hitResults.isNullOrEmpty()) return null
  return hitResults
    .filter { it.distance < MAX_HIT_DISTANCE_M }
    .filter { it.trackable is Plane }
    .minByOrNull {
      val planePref = when ((it.trackable as? Plane)?.type) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> 0
        Plane.Type.VERTICAL -> 1
        else -> 2
      }
      planePref * 10000 + (it.distance * 100).toInt()
    }
}

@Composable
fun ArScreen(
  widthCm: Float,
  heightCm: Float,
  depthCm: Float,
  modelName: String,
  onBack: () -> Unit,
  calibrationFactor: Float = 1.0f,
  availableModels: List<ArItem> = emptyList(),
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
  var arSession by remember { mutableStateOf<com.google.ar.core.Session?>(null) }
  var depthConfigured by remember { mutableStateOf(false) }
  var trackingState by remember { mutableStateOf(TrackingState.TRACKING) }
  var ghostAnchor by remember { mutableStateOf<Anchor?>(null) }
  var showGhost by remember { mutableStateOf(false) }
  var actionHistory by remember { mutableStateOf(listOf<ArAction>()) }
  var redoStack by remember { mutableStateOf(listOf<ArAction>()) }
  var captureRequested by remember { mutableStateOf(false) }
  var selectedModelIndex by remember { mutableStateOf(-1) }
  var hasVerticalPlane by remember { mutableStateOf(false) }

  var isRulerMode by remember { mutableStateOf(false) }
  var rulerPointA by remember { mutableStateOf<Anchor?>(null) }
  var rulerPointB by remember { mutableStateOf<Anchor?>(null) }
  var currentCalibrationFactor by remember { mutableStateOf(calibrationFactor) }

  LaunchedEffect(calibrationFactor) {
    currentCalibrationFactor = calibrationFactor
  }

  val catalog = remember {
    listOf(
      RecommendedFurniture("Cozy Sofa", 160f, 85f, 90f, "cube.glb"),
      RecommendedFurniture("Wooden Dining Table", 140f, 75f, 80f, "cube.glb"),
      RecommendedFurniture("Modern Desk", 120f, 75f, 60f, "cube.glb"),
      RecommendedFurniture("Kitchen Cabinet", 80f, 180f, 50f, "refrigerator.glb"),
      RecommendedFurniture("Single Bed", 100f, 45f, 200f, "cube.glb")
    )
  }

  val measuredDistance = remember(rulerPointA, rulerPointB, currentCalibrationFactor) {
    if (rulerPointA != null && rulerPointB != null) {
      val poseA = rulerPointA!!.pose
      val poseB = rulerPointB!!.pose
      val dx = poseB.tx() - poseA.tx()
      val dy = poseB.ty() - poseA.ty()
      val dz = poseB.tz() - poseA.tz()
      val dist = sqrt(dx * dx + dy * dy + dz * dz)
      dist * 100f * currentCalibrationFactor
    } else 0f
  }

  val recommendedFurniture = remember(measuredDistance) {
    if (measuredDistance > 0f) {
      catalog.filter { it.widthCm <= (measuredDistance - 5f) }
    } else {
      catalog
    }
  }

  val safetyWarning = remember(measuredDistance, recommendedFurniture) {
    if (measuredDistance > 0f && recommendedFurniture.isEmpty()) {
      "측정된 공간(${measuredDistance.toInt()}cm)이 가구 크기에 비해 좁습니다 (안전 마진 5cm 기준)."
    } else null
  }

  val multiWidthCm = if (selectedModelIndex in availableModels.indices) availableModels[selectedModelIndex].widthCm else widthCm
  val multiHeightCm = if (selectedModelIndex in availableModels.indices) availableModels[selectedModelIndex].heightCm else heightCm
  val multiDepthCm = if (selectedModelIndex in availableModels.indices) availableModels[selectedModelIndex].depthCm else depthCm
  val currentModelName = if (selectedModelIndex in availableModels.indices) availableModels[selectedModelIndex].modelName else modelName
  val effectiveWidthCm = if (availableModels.isNotEmpty()) multiWidthCm else widthCm
  val effectiveHeightCm = if (availableModels.isNotEmpty()) multiHeightCm else heightCm
  val effectiveDepthCm = if (availableModels.isNotEmpty()) multiDepthCm else depthCm
  val effectiveModelName = if (availableModels.isNotEmpty()) currentModelName else modelName

  fun pushAction(action: ArAction) {
    actionHistory = actionHistory + action
    redoStack = emptyList()
  }

  fun undo() {
    if (actionHistory.isEmpty()) return
    val action = actionHistory.last()
    actionHistory = actionHistory.dropLast(1)
    redoStack = redoStack + action
    when (action) {
      is ArAction.Place -> {
        placedItems = placedItems.filter { it.id != action.item.id }
        try { action.item.anchor.detach() } catch (_: Exception) {}
        selectedItemId = null
      }
      is ArAction.Delete -> {
        placedItems = placedItems + action.item
      }
      is ArAction.ClearAll -> {
        placedItems = action.items
      }
      is ArAction.Rotate -> {
        placedItems = placedItems.map {
          if (it.id == action.itemId) it.copy(rotationDegrees = action.oldDeg) else it
        }
      }
      is ArAction.Opacity -> {
        placedItems = placedItems.map {
          if (it.id == action.itemId) it.copy(opacity = action.oldVal) else it
        }
      }
    }
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val action = redoStack.last()
    redoStack = redoStack.dropLast(1)
    actionHistory = actionHistory + action
    when (action) {
      is ArAction.Place -> {
        placedItems = placedItems + action.item
        selectedItemId = action.item.id
      }
      is ArAction.Delete -> {
        placedItems = placedItems.filter { it.id != action.item.id }
        try { action.item.anchor.detach() } catch (_: Exception) {}
        selectedItemId = null
      }
      is ArAction.ClearAll -> {
        placedItems = emptyList()
        selectedItemId = null
      }
      is ArAction.Rotate -> {
        placedItems = placedItems.map {
          if (it.id == action.itemId) it.copy(rotationDegrees = action.newDeg) else it
        }
      }
      is ArAction.Opacity -> {
        placedItems = placedItems.map {
          if (it.id == action.itemId) it.copy(opacity = action.newVal) else it
        }
      }
    }
  }

  LaunchedEffect(captureRequested) {
    if (captureRequested) {
      try {
        val activity = context as? android.app.Activity
        if (activity != null) {
          val view = activity.window.decorView
          val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
          val canvas = AndroidCanvas(bitmap)
          view.draw(canvas)
          val dir = File(context.cacheDir, "screenshots")
          dir.mkdirs()
          val file = File(dir, "fircheck_${System.currentTimeMillis()}.png")
          FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          }
          val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, "스크린샷 공유"))
          Toast.makeText(context, "캡처 저장 완료", Toast.LENGTH_LONG).show()
        }
      } catch (e: Exception) {
        Toast.makeText(context, "캡처 실패: ${e.message}", Toast.LENGTH_SHORT).show()
      }
      captureRequested = false
    }
  }

  ArScreenContent(
    widthCm = effectiveWidthCm,
    heightCm = effectiveHeightCm,
    depthCm = effectiveDepthCm,
    hasCameraPermission = hasCameraPermission,
    placedItems = placedItems,
    selectedItemId = selectedItemId,
    isPlaneDetected = isPlaneDetected,
    trackingState = trackingState,
    frame = frame,
    showGhost = showGhost,
    canUndo = actionHistory.isNotEmpty(),
    canRedo = redoStack.isNotEmpty(),
    onBack = onBack,
    onPlaceItem = { item ->
      placedItems = placedItems + item
      selectedItemId = item.id
      showGhost = false
      ghostAnchor = null
      pushAction(ArAction.Place(item))
    },
    onDeleteItem = { item ->
      placedItems = placedItems.filter { it.id != item.id }
      selectedItemId = null
      pushAction(ArAction.Delete(item))
    },
    onClearAll = {
      val snapshot = placedItems
      placedItems = emptyList()
      selectedItemId = null
      pushAction(ArAction.ClearAll(snapshot))
    },
    onUndo = { undo() },
    onRedo = { redo() },
    onOpacityChange = { item, newOpacity ->
      val oldOpacity = item.opacity
      placedItems = placedItems.map {
        if (it.id == item.id) it.copy(opacity = newOpacity) else it
      }
      pushAction(ArAction.Opacity(item.id, oldOpacity, newOpacity))
    },
    onRotateLeft = { item ->
      val oldDeg = item.rotationDegrees
      val newDeg = (item.rotationDegrees - 15f + 360f) % 360f
      placedItems = placedItems.map {
        if (it.id == item.id) it.copy(rotationDegrees = newDeg) else it
      }
      pushAction(ArAction.Rotate(item.id, oldDeg, newDeg))
    },
    onRotateRight = { item ->
      val oldDeg = item.rotationDegrees
      val newDeg = (item.rotationDegrees + 15f) % 360f
      placedItems = placedItems.map {
        if (it.id == item.id) it.copy(rotationDegrees = newDeg) else it
      }
      pushAction(ArAction.Rotate(item.id, oldDeg, newDeg))
    },
    onNudge = { item, dx, dz ->
      try {
        val pose = item.anchor.pose
        val offset = Pose(floatArrayOf(dx, 0f, dz), floatArrayOf(0f, 0f, 0f, 1f))
        var targetPose = offset.compose(pose)
        
        // R3: Wall snapping math integration
        val snappedPose = findNearestVerticalPlaneSnap(arSession, targetPose, 0.15f)
        if (snappedPose != null) {
          targetPose = snappedPose
          triggerHapticFeedback(context) // Haptic feedback on snap
        }

        val newAnchor = arSession?.createAnchor(targetPose)
        if (newAnchor != null) {
          placedItems = placedItems.map {
            if (it.id == item.id) it.copy(anchor = newAnchor) else it
          }
          try { item.anchor.detach() } catch (_: Exception) {}
        }
      } catch (_: Exception) {}
    },
    onConfirmGhost = {
      ghostAnchor?.let { anchor ->
        val newItem = PlacedItem(
          id = java.util.UUID.randomUUID().toString(),
          anchor = anchor,
          widthCm = effectiveWidthCm,
          heightCm = effectiveHeightCm,
          depthCm = effectiveDepthCm,
          modelName = effectiveModelName,
          rotationDegrees = 0f,
          opacity = 0.8f
        )
        placedItems = placedItems + newItem
        selectedItemId = newItem.id
        pushAction(ArAction.Place(newItem))
      }
      ghostAnchor = null
      showGhost = false
    },
    onCancelGhost = {
      ghostAnchor?.let { anchor ->
        try { anchor.detach() } catch (_: Exception) {}
      }
      ghostAnchor = null
      showGhost = false
    },
    onScreenshot = { captureRequested = true },
    onDeselect = { selectedItemId = null },
    availableModels = availableModels,
    selectedModelIndex = selectedModelIndex,
    onSelectModel = { selectedModelIndex = it },
    hasVerticalPlane = hasVerticalPlane,
    onAlignToWall = { item ->
      try {
        val planes = arSession?.getAllTrackables(Plane::class.java)?.filter { it.type == Plane.Type.VERTICAL } ?: emptyList()
        if (planes.isNotEmpty()) {
          val plane = planes.first()
          val p = plane.centerPose
          val qx = p.qx(); val qy = p.qy(); val qz = p.qz(); val qw = p.qw()
          val yaw = atan2(2f * (qw * qy + qx * qz), 1f - 2f * (qy * qy + qz * qz))
          val wallDeg = yaw * (180f / 3.14159265f)
          placedItems = placedItems.map {
            if (it.id == item.id) it.copy(rotationDegrees = wallDeg) else it
          }
        }
      } catch (_: Exception) {}
    },
    isRulerModeActive = isRulerMode,
    measuredDistanceCm = measuredDistance,
    calibrationFactor = currentCalibrationFactor,
    rulerPointA = rulerPointA,
    rulerPointB = rulerPointB,
    recommendedFurniture = recommendedFurniture,
    safetyWarning = safetyWarning,
    onToggleRulerMode = {
      isRulerMode = !isRulerMode
    },
    onClearRuler = {
      try { rulerPointA?.detach() } catch (_: Exception) {}
      try { rulerPointB?.detach() } catch (_: Exception) {}
      rulerPointA = null
      rulerPointB = null
    },
    onCalibrationFactorChange = {
      currentCalibrationFactor = it
    },
    onSelectRecommended = { furniture ->
      try {
        var targetAnchor: Anchor? = rulerPointB
        if (targetAnchor == null) {
          targetAnchor = rulerPointA
        }
        var finalAnchor = targetAnchor
        if (finalAnchor == null) {
          frame?.camera?.pose?.let { camPose ->
            val forwardVec = FloatArray(3)
            camPose.getTransformedAxis(2, -1.0f, forwardVec, 0)
            val tx = camPose.tx() + forwardVec[0]
            val ty = camPose.ty() + forwardVec[1]
            val tz = camPose.tz() + forwardVec[2]
            val floorY = arSession?.getAllTrackables(Plane::class.java)
              ?.filter { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
              ?.minOfOrNull { it.centerPose.ty() } ?: ty
            
            val targetPose = Pose(floatArrayOf(tx, floorY, tz), camPose.rotationQuaternion)
            finalAnchor = arSession?.createAnchor(targetPose)
          }
        }
        
        finalAnchor?.let { anchor ->
          val newAnchor = arSession?.createAnchor(anchor.pose)
          if (newAnchor != null) {
            val newItem = PlacedItem(
              id = java.util.UUID.randomUUID().toString(),
              anchor = newAnchor,
              widthCm = furniture.widthCm,
              heightCm = furniture.heightCm,
              depthCm = furniture.depthCm,
              modelName = furniture.modelName,
              rotationDegrees = 0f,
              opacity = 0.8f
            )
            placedItems = placedItems + newItem
            selectedItemId = newItem.id
            pushAction(ArAction.Place(newItem))
            Toast.makeText(context, "${furniture.name}이 배치되었습니다.", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (_: Exception) {}
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
              "AR 실행 실패: 이 기기가 ARCore를 지원하지 않거나 필수 서비스가 설치되어 있지 않습니다.",
              Toast.LENGTH_LONG
            ).show()
            onBack()
          },
          onSessionUpdated = { session, updatedFrame ->
            arSession = session
            if (!depthConfigured) {
              try {
                session.configure(session.config.apply { depthMode = Config.DepthMode.AUTOMATIC })
              } catch (_: Exception) { }
              depthConfigured = true
            }
            frame = updatedFrame
            trackingState = updatedFrame.camera.trackingState
            if (!isPlaneDetected) {
              val planes = session.getAllTrackables(Plane::class.java)
              if (planes.isNotEmpty()) {
                isPlaneDetected = true
              }
            }
            hasVerticalPlane = session.getAllTrackables(Plane::class.java).any { it.type == Plane.Type.VERTICAL }
          },
          onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
              if (isRulerMode) {
                val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                val bestHit = selectBestHit(hitResults)
                if (bestHit != null) {
                  if (rulerPointA == null) {
                    rulerPointA = bestHit.createAnchor()
                  } else if (rulerPointB == null) {
                    rulerPointB = bestHit.createAnchor()
                  } else {
                    try { rulerPointA?.detach() } catch (_: Exception) {}
                    try { rulerPointB?.detach() } catch (_: Exception) {}
                    rulerPointA = bestHit.createAnchor()
                    rulerPointB = null
                  }
                }
              } else {
                if (node != null) {
                  var parentNode: Node? = node
                  while (parentNode != null && parentNode !is AnchorNode) {
                    parentNode = parentNode.parent
                  }
                  if (parentNode is AnchorNode) {
                    val anchorRef = parentNode.anchor
                    val found = placedItems.find { it.anchor == anchorRef }
                    if (found != null) {
                      ghostAnchor?.let { try { it.detach() } catch (_: Exception) {} }
                      ghostAnchor = null
                      showGhost = false
                      selectedItemId = found.id
                    }
                  }
                } else {
                  val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                  val bestHit = selectBestHit(hitResults)
                  if (bestHit != null) {
                    ghostAnchor?.let { try { it.detach() } catch (_: Exception) {} }
                    ghostAnchor = bestHit.createAnchor()
                    showGhost = true
                    selectedItemId = null
                  } else {
                    selectedItemId = null
                  }
                }
              }
              true
            }
          )
        ) {
          if (showGhost && ghostAnchor != null) {
            AnchorNode(anchor = ghostAnchor!!) {
              val w = effectiveWidthCm / 100f * calibrationFactor
              val h = effectiveHeightCm / 100f * calibrationFactor
              val d = effectiveDepthCm / 100f * calibrationFactor
              val ghostInstance = rememberModelInstance(modelLoader, "models/$effectiveModelName")
              LaunchedEffect(ghostInstance) {
                ghostInstance?.let { mi ->
                  mi.materialInstances.forEach { mat ->
                    try { mat.setParameter("baseColorFactor", 0.3f, 0.3f, 0.3f, 0.3f) } catch (_: Exception) {}
                  }
                }
              }
              if (ghostInstance != null) {
                ModelNode(
                  modelInstance = ghostInstance,
                  scale = Scale(w, h, d),
                  rotation = Rotation(0f, 0f, 0f),
                  position = Position(0f, h / 2f, 0f)
                )
              }
            }
          }
          placedItems.forEach { item ->
            key(item.id) {
              AnchorNode(anchor = item.anchor) {
                val w = item.widthCm / 100f * calibrationFactor
                val h = item.heightCm / 100f * calibrationFactor
                val d = item.depthCm / 100f * calibrationFactor

                val modelInstance = rememberModelInstance(modelLoader, "models/${item.modelName}")

                var effectiveScale by remember { mutableStateOf(Scale(w, h, d)) }

                LaunchedEffect(modelInstance, w, h, d) {
                  modelInstance?.let { mi ->
                    try {
                      val rm = engine.renderableManager
                      val inst = rm.getInstance(mi.root)
                      if (rm.hasComponent(mi.root)) {
                        val outBox = Box()
                        rm.getAxisAlignedBoundingBox(inst, outBox)
                        val mw = (outBox.halfExtent[0] * 2f).coerceAtLeast(0.001f)
                        val mh = (outBox.halfExtent[1] * 2f).coerceAtLeast(0.001f)
                        val md = (outBox.halfExtent[2] * 2f).coerceAtLeast(0.001f)
                        effectiveScale = Scale(w / mw, h / mh, d / md)
                      }
                    } catch (_: Exception) {}
                  }
                }

                LaunchedEffect(item.opacity, modelInstance) {
                  modelInstance?.let { mi ->
                    mi.materialInstances.forEach { mat ->
                      try {
                        mat.setParameter("baseColorFactor", item.opacity, item.opacity, item.opacity, item.opacity)
                      } catch (_: Exception) {}
                    }
                    try {
                      val rm = engine.renderableManager
                      val inst = rm.getInstance(mi.root)
                      if (rm.hasComponent(mi.root)) {
                        rm.setLayerMask(inst, 0xff, 0xff)
                      }
                    } catch (_: Exception) {}
                  }
                }

                if (modelInstance != null) {
                  ModelNode(
                    modelInstance = modelInstance,
                    scale = effectiveScale,
                    rotation = Rotation(0f, item.rotationDegrees, 0f),
                    position = Position(0f, h / 2f, 0f)
                  )
                }

                val shadowInstance = rememberModelInstance(modelLoader, "models/cube.glb")
                LaunchedEffect(shadowInstance) {
                  shadowInstance?.let { mi ->
                    mi.materialInstances.forEach { mat ->
                      try {
                        mat.setParameter("baseColorFactor", 0.0f, 0.0f, 0.0f, 0.15f)
                      } catch (_: Exception) {}
                    }
                  }
                }
                if (shadowInstance != null) {
                  ModelNode(
                    modelInstance = shadowInstance,
                    scale = Scale(w * 1.2f, 0.01f, d * 1.2f),
                    rotation = Rotation(0f, item.rotationDegrees, 0f),
                    position = Position(0f, 0.005f, 0f)
                  )
                }
              }
            }
          }

          if (rulerPointA != null) {
            AnchorNode(anchor = rulerPointA!!) {
              val pointInstance = rememberModelInstance(modelLoader, "models/cube.glb")
              LaunchedEffect(pointInstance) {
                pointInstance?.let { mi ->
                  mi.materialInstances.forEach { mat ->
                    try { mat.setParameter("baseColorFactor", 1.0f, 0.0f, 0.0f, 1.0f) } catch (_: Exception) {}
                  }
                }
              }
              if (pointInstance != null) {
                ModelNode(
                  modelInstance = pointInstance,
                  scale = Scale(0.015f, 0.015f, 0.015f),
                  position = Position(0f, 0f, 0f)
                )
              }
            }
          }

          if (rulerPointB != null) {
            AnchorNode(anchor = rulerPointB!!) {
              val pointInstance = rememberModelInstance(modelLoader, "models/cube.glb")
              LaunchedEffect(pointInstance) {
                pointInstance?.let { mi ->
                  mi.materialInstances.forEach { mat ->
                    try { mat.setParameter("baseColorFactor", 1.0f, 0.0f, 0.0f, 1.0f) } catch (_: Exception) {}
                  }
                }
              }
              if (pointInstance != null) {
                ModelNode(
                  modelInstance = pointInstance,
                  scale = Scale(0.015f, 0.015f, 0.015f),
                  position = Position(0f, 0f, 0f)
                )
              }
            }
          }

          if (rulerPointA != null && rulerPointB != null) {
            val poseA = rulerPointA!!.pose
            val poseB = rulerPointB!!.pose

            val dx = poseB.tx() - poseA.tx()
            val dy = poseB.ty() - poseA.ty()
            val dz = poseB.tz() - poseA.tz()
            val dist = sqrt(dx * dx + dy * dy + dz * dz)

            val yaw = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
            val pitch = Math.toDegrees(-atan2(dy.toDouble(), sqrt((dx * dx + dz * dz).toDouble()))).toFloat()

            AnchorNode(anchor = rulerPointA!!) {
              val lineInstance = rememberModelInstance(modelLoader, "models/cube.glb")
              LaunchedEffect(lineInstance) {
                lineInstance?.let { mi ->
                  mi.materialInstances.forEach { mat ->
                    try { mat.setParameter("baseColorFactor", 1.0f, 0.0f, 0.0f, 1.0f) } catch (_: Exception) {}
                  }
                }
              }
              if (lineInstance != null) {
                ModelNode(
                  modelInstance = lineInstance,
                  scale = Scale(0.005f, 0.005f, dist),
                  rotation = Rotation(pitch, yaw, 0f),
                  position = Position(dx / 2f, dy / 2f, dz / 2f)
                )
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
  trackingState: TrackingState = TrackingState.TRACKING,
  frame: Frame? = null,
  showGhost: Boolean = false,
  canUndo: Boolean = false,
  canRedo: Boolean = false,
  onBack: () -> Unit,
  onPlaceItem: (PlacedItem) -> Unit = {},
  onDeleteItem: (PlacedItem) -> Unit = {},
  onClearAll: () -> Unit = {},
  onUndo: () -> Unit = {},
  onRedo: () -> Unit = {},
  onOpacityChange: (PlacedItem, Float) -> Unit,
  onRotateLeft: (PlacedItem) -> Unit,
  onRotateRight: (PlacedItem) -> Unit,
  onNudge: (PlacedItem, Float, Float) -> Unit = { _, _, _ -> },
  onConfirmGhost: () -> Unit = {},
  onCancelGhost: () -> Unit = {},
  onScreenshot: () -> Unit = {},
  onDeselect: () -> Unit,
  availableModels: List<ArItem> = emptyList(),
  selectedModelIndex: Int = -1,
  onSelectModel: (Int) -> Unit = {},
  hasVerticalPlane: Boolean = false,
  onAlignToWall: (PlacedItem) -> Unit = {},
  isRulerModeActive: Boolean = false,
  measuredDistanceCm: Float = 0f,
  calibrationFactor: Float = 1.0f,
  rulerPointA: Anchor? = null,
  rulerPointB: Anchor? = null,
  recommendedFurniture: List<RecommendedFurniture> = emptyList(),
  safetyWarning: String? = null,
  onToggleRulerMode: () -> Unit = {},
  onClearRuler: () -> Unit = {},
  onCalibrationFactorChange: (Float) -> Unit = {},
  onSelectRecommended: (RecommendedFurniture) -> Unit = {},
  modifier: Modifier = Modifier,
  arSceneViewContent: @Composable BoxScope.() -> Unit = {}
) {
  var viewportWidth by remember { mutableStateOf(0) }
  var viewportHeight by remember { mutableStateOf(0) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .onGloballyPositioned { coords ->
        viewportWidth = coords.size.width
        viewportHeight = coords.size.height
      }
  ) {
    if (hasCameraPermission) {
      Box(modifier = Modifier.fillMaxSize()) {
        arSceneViewContent()

        // --- UX 개선 R2 & R1: 3D 치수 레이블 및 바닥 선택 링 오버레이 ---
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        var matricesValid = false

        frame?.camera?.let { camera ->
          if (camera.trackingState == TrackingState.TRACKING) {
            camera.getViewMatrix(viewMatrix, 0)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
            matricesValid = true
          }
        }

        if (matricesValid && viewportWidth > 0 && viewportHeight > 0) {
          Box(modifier = Modifier.fillMaxSize()) {
            placedItems.forEach { item ->
              val pose = item.anchor.pose

              // 가구의 바닥 중앙 3D 월드 좌표 투영
              val floorPt = projectWorldToScreen(
                pose.tx(), pose.ty(), pose.tz(),
                viewMatrix, projectionMatrix, viewportWidth, viewportHeight
              )

              // 가구의 상단 중앙 3D 월드 좌표 투영 (높이만큼 보정)
              val topPt = projectWorldToScreen(
                pose.tx(), pose.ty() + (item.heightCm / 100f), pose.tz(),
                viewMatrix, projectionMatrix, viewportWidth, viewportHeight
              )

              // R1. 선택된 가구 바닥 선택 링 (Cyan glowing halo)
              val isSelected = item.id == selectedItemId
              if (floorPt != null && isSelected) {
                val density = LocalContext.current.resources.displayMetrics.density
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val ringRadius = (item.widthCm.coerceAtLeast(item.depthCm) / 2f) * 4f * density
                  val constrainedRadius = ringRadius.coerceIn(50f, 150f)

                  // 대시 효과가 가미된 세련된 청록 가이드 링
                  drawCircle(
                    color = Color.Cyan.copy(alpha = 0.85f),
                    radius = constrainedRadius,
                    center = androidx.compose.ui.geometry.Offset(floorPt.x, floorPt.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                      width = 3f * density,
                      pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                  )
                  // 링 내부 글로우
                  drawCircle(
                    color = Color.Cyan.copy(alpha = 0.12f),
                    radius = constrainedRadius,
                    center = androidx.compose.ui.geometry.Offset(floorPt.x, floorPt.y)
                  )
                }
              }

              // R2. 3D 치수 말풍선 카드 그리기
              if (topPt != null) {
                val density = LocalContext.current.resources.displayMetrics.density
                val leftDp = (topPt.x / density).dp
                val topDp = (topPt.y / density).dp

                Box(
                  modifier = Modifier
                    .offset(x = leftDp - 70.dp, y = topDp - 50.dp)
                    .wrapContentSize()
                ) {
                  Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text(
                        text = "${item.widthCm.toInt()}×${item.heightCm.toInt()}×${item.depthCm.toInt()}cm",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      if (isSelected) {
                        Text(
                          text = "선택됨",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                      }
                    }
                  }
                }
              }
            }

            if (rulerPointA != null && rulerPointB != null) {
              val poseA = rulerPointA.pose
              val poseB = rulerPointB.pose
              val dx = poseB.tx() - poseA.tx()
              val dy = poseB.ty() - poseA.ty()
              val dz = poseB.tz() - poseA.tz()
              val dist = sqrt(dx * dx + dy * dy + dz * dz)

              val midX = (poseA.tx() + poseB.tx()) / 2f
              val midY = (poseA.ty() + poseB.ty()) / 2f
              val midZ = (poseA.tz() + poseB.tz()) / 2f

              val midPt = projectWorldToScreen(
                midX, midY, midZ,
                viewMatrix, projectionMatrix, viewportWidth, viewportHeight
              )

              if (midPt != null) {
                val density = LocalContext.current.resources.displayMetrics.density
                val leftDp = (midPt.x / density).dp
                val topDp = (midPt.y / density).dp

                Box(
                  modifier = Modifier
                    .offset(x = leftDp - 70.dp, y = topDp - 30.dp)
                    .wrapContentSize()
                ) {
                  Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                  ) {
                    Text(
                      text = "${"%.1f".format(dist * 100f * calibrationFactor)} cm",
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                      ),
                      color = MaterialTheme.colorScheme.onTertiary
                    )
                  }
                }
              }
            }
          }
        }
      }

      val selectedItem = placedItems.find { it.id == selectedItemId }

      Column(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 48.dp, start = 16.dp, end = 16.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Card(
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
            if (placedItems.isEmpty()) {
              Text(
                text = "바닥을 터치하여 원하는 개수만큼 가구를 배치해 보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              Text(
                text = "배치된 아이템: ${placedItems.size}개",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
            if (hasVerticalPlane) {
              Text(
                text = "벽면 감지됨 ✓ (벽에 정렬 가능)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }
        }
        if (trackingState != TrackingState.TRACKING) {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            )
          ) {
            Text(
              text = if (trackingState == TrackingState.PAUSED) "⚠ 카메라 추적이 일시 중지되었습니다. 천천히 움직여 주세요."
                     else "⚠ AR 추적이 중단되었습니다. 카메라를 천천히 움직여 주세요.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
        if (availableModels.isNotEmpty()) {
          Text(
            text = "모델 선택",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            availableModels.forEachIndexed { index, model ->
              FilterChip(
                selected = selectedModelIndex == index,
                onClick = { onSelectModel(index) },
                label = {
                  Text(
                    model.name.ifEmpty { "${model.widthCm.toInt()}×${model.heightCm.toInt()}cm" },
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      if (showGhost) {
        Card(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = if (availableModels.isNotEmpty()) 220.dp else 160.dp)
            .padding(horizontal = 48.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
          )
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("이 위치에 배치하시겠습니까?", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = onConfirmGhost) { Text("확인") }
              OutlinedButton(onClick = onCancelGhost) { Text("취소") }
            }
          }
        }
      }

      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Ruler Mode Overlay
        Card(
          modifier = Modifier.padding(bottom = 8.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Ruler Mode", style = MaterialTheme.typography.titleMedium)
              Button(onClick = onToggleRulerMode) {
                Text(if (isRulerModeActive) "자 모드 끄기" else "자 모드 켜기")
              }
            }
            if (isRulerModeActive) {
              Spacer(modifier = Modifier.height(8.dp))
              Text("거리: ${"%.1f".format(measuredDistanceCm)}cm", style = MaterialTheme.typography.bodyLarge)
              Spacer(modifier = Modifier.height(4.dp))
              Button(onClick = onClearRuler) {
                Text("자 지우기")
              }
            }
          }
        }

        // Calibration Card
        if (selectedItemId == null) {
          Card(
            modifier = Modifier.padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("보정 계수: ${"%.2f".format(calibrationFactor)}", style = MaterialTheme.typography.bodyMedium)
              Slider(
                value = calibrationFactor,
                onValueChange = onCalibrationFactorChange,
                valueRange = 0.8f..1.2f,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        // Recommendation Panel Card
        Card(
          modifier = Modifier.padding(bottom = 8.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("추천 가구", style = MaterialTheme.typography.titleMedium)
            if (safetyWarning != null) {
              Text(safetyWarning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              recommendedFurniture.forEach { furniture ->
                Button(onClick = { onSelectRecommended(furniture) }) {
                  Text("${furniture.name} (${furniture.widthCm.toInt()}x${furniture.heightCm.toInt()})")
                }
              }
            }
          }
        }

        if (selectedItem != null) {
          Card(
            modifier = Modifier.padding(bottom = 8.dp),
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

              if (placedItems.size > 1) {
                val nearest = placedItems
                  .filter { it.id != selectedItem.id }
                  .minByOrNull {
                    val p1 = selectedItem.anchor.pose
                    val p2 = it.anchor.pose
                    val dx = p1.tx() - p2.tx()
                    val dy = p1.ty() - p2.ty()
                    val dz = p1.tz() - p2.tz()
                    dx * dx + dy * dy + dz * dz
                  }
                if (nearest != null) {
                  val p1 = selectedItem.anchor.pose
                  val p2 = nearest.anchor.pose
                  val dx = p1.tx() - p2.tx()
                  val dy = p1.ty() - p2.ty()
                  val dz = p1.tz() - p2.tz()
                  val distCm = sqrt(dx * dx + dy * dy + dz * dz) * 100f
                  Text(
                    text = "가장 가까운 아이템과 거리: ${"%.1f".format(distCm)}cm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "위치 미세 조정 (5cm)",
                style = MaterialTheme.typography.bodySmall
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
              ) {
                OutlinedButton(onClick = { onNudge(selectedItem, -NUDGE_STEP_M, 0f) }, modifier = Modifier.padding(2.dp)) { Text("←") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  OutlinedButton(onClick = { onNudge(selectedItem, 0f, -NUDGE_STEP_M) }, modifier = Modifier.padding(2.dp)) { Text("↑") }
                  OutlinedButton(onClick = { onNudge(selectedItem, 0f, NUDGE_STEP_M) }, modifier = Modifier.padding(2.dp)) { Text("↓") }
                }
                OutlinedButton(onClick = { onNudge(selectedItem, NUDGE_STEP_M, 0f) }, modifier = Modifier.padding(2.dp)) { Text("→") }
              }

              if (hasVerticalPlane) {
                OutlinedButton(
                  onClick = { onAlignToWall(selectedItem) },
                  modifier = Modifier.fillMaxWidth()
                ) { Text("벽에 정렬") }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "투명도 조절: ${(selectedItem.opacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
              )
              Slider(
                value = selectedItem.opacity,
                onValueChange = { newOpacity -> onOpacityChange(selectedItem, newOpacity) },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                Button(onClick = { onRotateLeft(selectedItem) }, modifier = Modifier.weight(1f)) { Text("◀ 15°") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onRotateRight(selectedItem) }, modifier = Modifier.weight(1f)) { Text("15° ▶") }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(onClick = onDeselect, modifier = Modifier.weight(1f)) { Text("선택 해제") }
                Button(
                  onClick = { onDeleteItem(selectedItem) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.weight(1f)
                ) { Text("삭제") }
              }
            }
          }
        }

        if (placedItems.isNotEmpty() && selectedItemId == null) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 4.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
          ) {
            Row(
              modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              placedItems.forEachIndexed { idx, item ->
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = "#${idx+1} ${item.widthCm.toInt()}×${item.heightCm.toInt()}×${item.depthCm.toInt()}cm",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.weight(1f)
          ) { Text("실행 취소") }
          Button(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier.weight(1f)
          ) { Text("다시 실행") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (placedItems.isNotEmpty() && selectedItemId == null && !showGhost) {
            Button(
              onClick = onClearAll,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.weight(1f)
            ) { Text("모두 지우기") }
          }
          Button(
            onClick = onScreenshot,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
          ) { Text("캡처") }
          Button(
            onClick = onBack,
            modifier = Modifier.weight(1f)
          ) { Text("뒤로 가기") }
        }
      }

      if (!isPlaneDetected) {
        ScanGuideOverlay()
      }
    } else {
      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text("카메라 권한이 없습니다. 메인 화면으로 돌아가 권한을 수락해 주세요.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) { Text("돌아가기") }
      }
    }
  }
}

// =============================================================================
// Premium UX Helpers & Animated Onboarding
// =============================================================================

@Composable
fun ScanGuideOverlay() {
  val transition = rememberInfiniteTransition(label = "ScanGuide")
  val tiltAngle by transition.animateFloat(
    initialValue = -20f,
    targetValue = 20f,
    animationSpec = infiniteRepeatable(
      animation = tween(1600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PhoneTilt"
  )
  val sweepOffset by transition.animateFloat(
    initialValue = -50f,
    targetValue = 50f,
    animationSpec = infiniteRepeatable(
      animation = tween(1600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PhoneSweep"
  )
  val waveRadius by transition.animateFloat(
    initialValue = 15f,
    targetValue = 85f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "RadarWave"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.65f)),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier.padding(horizontal = 32.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Canvas(modifier = Modifier.size(150.dp)) {
          val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
          val baseLineY = center.y + 35f

          // 1. Perspective Floor Grid
          drawOval(
            color = Color.Cyan.copy(alpha = 0.15f),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - 75f, baseLineY - 15f),
            size = androidx.compose.ui.geometry.Size(150f, 30f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
          )

          // 2. Pulse radar wave
          drawOval(
            color = Color.Cyan.copy(alpha = (1.0f - (waveRadius / 85f)).coerceIn(0f, 1f) * 0.45f),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - waveRadius, baseLineY - (waveRadius * 0.2f)),
            size = androidx.compose.ui.geometry.Size(waveRadius * 2f, waveRadius * 0.4f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
          )

          // 3. Device motion
          drawContext.canvas.save()
          drawContext.canvas.translate(center.x + sweepOffset, center.y - 20f)
          drawContext.canvas.rotate(tiltAngle)

          val phoneW = 40f
          val phoneH = 76f
          val rrect = androidx.compose.ui.geometry.RoundRect(
            left = -phoneW / 2f,
            top = -phoneH / 2f,
            right = phoneW / 2f,
            bottom = phoneH / 2f,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
          )
          val path = androidx.compose.ui.graphics.Path().apply { addRoundRect(rrect) }

          drawPath(path = path, color = Color.Gray, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
          drawPath(path = path, color = Color.DarkGray.copy(alpha = 0.7f))
          drawCircle(color = Color.LightGray, radius = 3f, center = androidx.compose.ui.geometry.Offset(0f, -phoneH / 2f + 8f))
          drawLine(color = Color.LightGray, start = androidx.compose.ui.geometry.Offset(-10f, phoneH / 2f - 6f), end = androidx.compose.ui.geometry.Offset(10f, phoneH / 2f - 6f), strokeWidth = 2f)

          drawContext.canvas.restore()
        }

        Text("주변 공간 인식 중", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          "스마트폰을 천천히 흔들며 바닥면과 주변 벽면을 비춰 공간을 스캔하세요.",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

fun projectWorldToScreen(
  worldX: Float, worldY: Float, worldZ: Float,
  viewMatrix: FloatArray,
  projectionMatrix: FloatArray,
  viewportWidth: Int,
  viewportHeight: Int
): android.graphics.PointF? {
  val worldCoord = floatArrayOf(worldX, worldY, worldZ, 1.0f)
  val cameraCoord = FloatArray(4)
  val clipCoord = FloatArray(4)

  android.opengl.Matrix.multiplyMV(cameraCoord, 0, viewMatrix, 0, worldCoord, 0)
  android.opengl.Matrix.multiplyMV(clipCoord, 0, projectionMatrix, 0, cameraCoord, 0)

  val w = clipCoord[3]
  if (w < 0.001f) {
    return null
  }

  val ndcX = clipCoord[0] / w
  val ndcY = clipCoord[1] / w
  val ndcZ = clipCoord[2] / w

  if (ndcX < -1.3f || ndcX > 1.3f || ndcY < -1.3f || ndcY > 1.3f || ndcZ > 1.0f || ndcZ < -1.0f) {
    return null
  }

  val screenX = ((ndcX + 1.0f) / 2.0f) * viewportWidth
  val screenY = ((1.0f - ndcY) / 2.0f) * viewportHeight

  return android.graphics.PointF(screenX, screenY)
}

fun findNearestVerticalPlaneSnap(
  session: com.google.ar.core.Session?,
  currentPose: Pose,
  thresholdM: Float = 0.15f
): Pose? {
  if (session == null) return null
  val verticalPlanes = session.getAllTrackables(Plane::class.java).filter {
    it.type == Plane.Type.VERTICAL && it.trackingState == TrackingState.TRACKING
  }
  if (verticalPlanes.isEmpty()) return null

  var bestPose: Pose? = null
  var minDistance = Float.MAX_VALUE

  for (plane in verticalPlanes) {
    val planePose = plane.centerPose
    val normal = FloatArray(3)
    planePose.getTransformedAxis(2, 1f, normal, 0) // Local Z axis is plane normal

    val dx = currentPose.tx() - planePose.tx()
    val dy = currentPose.ty() - planePose.ty()
    val dz = currentPose.tz() - planePose.tz()

    val dist = Math.abs(dx * normal[0] + dy * normal[1] + dz * normal[2])

    if (dist < thresholdM && dist < minDistance) {
      minDistance = dist
      val dot = dx * normal[0] + dy * normal[1] + dz * normal[2]
      val sign = if (dot >= 0) 1.0f else -1.0f

      val snapX = currentPose.tx() - normal[0] * dist * sign
      val snapZ = currentPose.tz() - normal[2] * dist * sign

      bestPose = Pose(
        floatArrayOf(snapX, currentPose.ty(), snapZ),
        planePose.rotationQuaternion
      )
    }
  }
  return bestPose
}

fun triggerHapticFeedback(context: android.content.Context) {
  val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
  vibrator?.let {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      it.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
      @Suppress("DEPRECATION")
      it.vibrate(45)
    }
  }
}

