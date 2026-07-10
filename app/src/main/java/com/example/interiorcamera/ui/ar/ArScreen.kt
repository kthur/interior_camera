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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.google.ar.core.LightEstimate
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
import com.example.interiorcamera.data.DefaultFurnitureRepository
import com.example.interiorcamera.ui.ar.OnboardingTutorial
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos

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
  var ambientColorCorrection by remember { mutableStateOf(floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)) }

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
    DefaultFurnitureRepository().getRecommendedFurniture()
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

          // R7. Elegant Watermark synthesis
          val watermarkPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#A6000000") // Dark translucent
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
          }
          val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
          }
          val subTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#CCCCCC")
            textSize = 18f
            isAntiAlias = true
          }

          val cardWidth = 440f
          val cardHeight = 110f
          val margin = 40f
          val left = bitmap.width - cardWidth - margin
          val top = bitmap.height - cardHeight - margin
          val right = bitmap.width - margin
          val bottom = bitmap.height - margin

          val rectF = android.graphics.RectF(left, top, right, bottom)
          canvas.drawRoundRect(rectF, 12f, 12f, watermarkPaint)

          canvas.drawText("FitCheck AR", left + 24f, top + 42f, textPaint)
          val countInfo = if (placedItems.isNotEmpty()) "${placedItems.size}개 가구 배치됨" else "인테리어 공간 측정"
          val sizeInfo = "방 크기: ${widthCm.toInt()}x${depthCm.toInt()}cm"
          canvas.drawText("$countInfo • $sizeInfo", left + 24f, top + 80f, subTextPaint)

          // Save cache copy for quick sharing intent
          val dir = File(context.cacheDir, "screenshots")
          dir.mkdirs()
          val file = File(dir, "fitcheck_${System.currentTimeMillis()}.png")
          FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
          }

          // Save permanent copy to Snapshots Gallery directory
          val snapshotDir = File(context.filesDir, "snapshots")
          snapshotDir.mkdirs()
          val galleryFile = File(snapshotDir, "snapshot_${System.currentTimeMillis()}.jpg")
          FileOutputStream(galleryFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
          }

          val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, "스크린샷 공유"))
          Toast.makeText(context, "스냅샷 갤러리에 저장 완료", Toast.LENGTH_LONG).show()
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
        var targetRotationDegrees = item.rotationDegrees
        
        // R3: Wall snapping math integration
        val snappedPose = findNearestVerticalPlaneSnap(arSession, targetPose, 0.15f)
        if (snappedPose != null) {
          targetPose = snappedPose
          triggerHapticFeedback(context) // Haptic feedback on snap
          val qx = targetPose.qx(); val qy = targetPose.qy(); val qz = targetPose.qz(); val qw = targetPose.qw()
          val yaw = atan2(2f * (qw * qy + qx * qz), 1f - 2f * (qy * qy + qz * qz))
          targetRotationDegrees = yaw * (180f / 3.14159265f)
        }

        // Real-time collision detection (SAT) and resolution (MTV)
        val activeObb = Obb2D(
          x = targetPose.tx(),
          z = targetPose.tz(),
          hw = (item.widthCm / 100f * calibrationFactor) / 2f,
          hd = (item.depthCm / 100f * calibrationFactor) / 2f,
          rotationDegrees = targetRotationDegrees
        )

        var collidingResult: CollisionResult? = null
        for (other in placedItems) {
          if (other.id == item.id) continue
          val otherObb = Obb2D(
            x = other.anchor.pose.tx(),
            z = other.anchor.pose.tz(),
            hw = (other.widthCm / 100f * calibrationFactor) / 2f,
            hd = (other.depthCm / 100f * calibrationFactor) / 2f,
            rotationDegrees = other.rotationDegrees
          )
          val res = CollisionDetection.checkCollision(activeObb, otherObb)
          if (res.collides) {
            collidingResult = res
            break
          }
        }

        if (collidingResult != null && collidingResult.collides) {
          triggerHapticFeedback(context)
          targetPose = Pose(
            floatArrayOf(targetPose.tx() + collidingResult.mtvX, targetPose.ty(), targetPose.tz() + collidingResult.mtvZ),
            targetPose.rotationQuaternion
          )
        }

        val newAnchor = arSession?.createAnchor(targetPose)
        if (newAnchor != null) {
          placedItems = placedItems.map {
            if (it.id == item.id) it.copy(anchor = newAnchor, rotationDegrees = targetRotationDegrees) else it
          }
          try { item.anchor.detach() } catch (_: Exception) {}
        }
      } catch (_: Exception) {}
    },
    onConfirmGhost = {
      // R5. Max placement limit check
      if (placedItems.size >= 8) {
        Toast.makeText(context, "안정적인 실행을 위해 가구는 최대 8개까지 배치할 수 있습니다.", Toast.LENGTH_LONG).show()
      } else {
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
                session.configure(session.config.apply {
                  depthMode = Config.DepthMode.AUTOMATIC
                  lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                })
              } catch (_: Exception) { }
              depthConfigured = true
            }
            frame = updatedFrame
            trackingState = updatedFrame.camera.trackingState

            val lightEstimate = updatedFrame.lightEstimate
            if (lightEstimate.state == LightEstimate.State.VALID) {
                val correction = FloatArray(4)
                lightEstimate.getColorCorrection(correction, 0)
                ambientColorCorrection = correction
            }

            if (!isPlaneDetected) {
              val planes = session.getAllTrackables(Plane::class.java)
              val horizontalPlane = planes.find { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING || it.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING } ?: planes.firstOrNull()
              if (horizontalPlane != null) {
                isPlaneDetected = true
                val floorplanItems = availableModels.filter { it.isFloorplanPlaced }
                if (floorplanItems.isNotEmpty()) {
                  val centerPose = horizontalPlane.centerPose
                  val spawnedList = mutableListOf<PlacedItem>()
                  for (arItem in floorplanItems) {
                    val relativeX = arItem.offsetX
                    val relativeZ = arItem.offsetZ
                    val yawRad = Math.toRadians(arItem.rotationDegrees.toDouble())
                    val qRot = floatArrayOf(
                      0f,
                      sin(yawRad / 2.0).toFloat(),
                      0f,
                      cos(yawRad / 2.0).toFloat()
                    )
                    val localOffset = floatArrayOf(relativeX, 0f, relativeZ)
                    val targetWorldPoint = centerPose.transformPoint(localOffset)
                    val targetPose = Pose(targetWorldPoint, multiplyQuaternions(centerPose.rotationQuaternion, qRot))
                    val newAnchor = session.createAnchor(targetPose)
                    if (newAnchor != null) {
                      spawnedList.add(
                        PlacedItem(
                          id = java.util.UUID.randomUUID().toString(),
                          anchor = newAnchor,
                          widthCm = arItem.widthCm,
                          heightCm = arItem.heightCm,
                          depthCm = arItem.depthCm,
                          modelName = arItem.modelName,
                          rotationDegrees = arItem.rotationDegrees
                        )
                      )
                    }
                  }
                  if (spawnedList.isNotEmpty()) {
                    placedItems = placedItems + spawnedList
                  }
                }
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
              LaunchedEffect(ghostInstance, ambientColorCorrection) {
                ghostInstance?.let { mi ->
                  mi.materialInstances.forEach { mat ->
                    try { mat.setParameter("baseColorFactor", 0.3f * ambientColorCorrection[0], 0.3f * ambientColorCorrection[1], 0.3f * ambientColorCorrection[2], 0.3f * ambientColorCorrection[3]) } catch (_: Exception) {}
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

                LaunchedEffect(item.opacity, modelInstance, ambientColorCorrection) {
                  modelInstance?.let { mi ->
                    mi.materialInstances.forEach { mat ->
                      try {
                        mat.setParameter("baseColorFactor", item.opacity * ambientColorCorrection[0], item.opacity * ambientColorCorrection[1], item.opacity * ambientColorCorrection[2], item.opacity * ambientColorCorrection[3])
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
                    position = Position(0f, h / 2f, 0f),
                    apply = {
                      isShadowCaster = true
                      isShadowReceiver = true
                    }
                  )
                }

                // Legacy flat shadow mesh disabled/removed for premium soft shadows
                /*
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
                */
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
  val context = LocalContext.current
  var viewportWidth by remember { mutableStateOf(0) }
  var viewportHeight by remember { mutableStateOf(0) }
  var showOnboarding by remember { mutableStateOf(true) }

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
                // Check if selected item is currently colliding with any other item
                var hasCollision = false
                val activeObb = Obb2D(
                  x = pose.tx(),
                  z = pose.tz(),
                  hw = (item.widthCm / 100f * calibrationFactor) / 2f,
                  hd = (item.depthCm / 100f * calibrationFactor) / 2f,
                  rotationDegrees = item.rotationDegrees
                )
                for (other in placedItems) {
                  if (other.id == item.id) continue
                  val otherObb = Obb2D(
                    x = other.anchor.pose.tx(),
                    z = other.anchor.pose.tz(),
                    hw = (other.widthCm / 100f * calibrationFactor) / 2f,
                    hd = (other.depthCm / 100f * calibrationFactor) / 2f,
                    rotationDegrees = other.rotationDegrees
                  )
                  if (CollisionDetection.checkCollision(activeObb, otherObb).collides) {
                    hasCollision = true
                    break
                  }
                }
                val ringColor = if (hasCollision) Color.Red else Color.Cyan

                val density = LocalContext.current.resources.displayMetrics.density
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val ringRadius = (item.widthCm.coerceAtLeast(item.depthCm) / 2f) * 4f * density
                  val constrainedRadius = ringRadius.coerceIn(50f, 150f)

                  // 대시 효과가 가미된 세련된 가이드 링 (충돌 시 빨간색, 평상시 청록색)
                  drawCircle(
                    color = ringColor.copy(alpha = 0.85f),
                    radius = constrainedRadius,
                    center = androidx.compose.ui.geometry.Offset(floorPt.x, floorPt.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                      width = 3f * density,
                      pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                  )
                  // 링 내부 글로우
                  drawCircle(
                    color = ringColor.copy(alpha = 0.12f),
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

      // ──────────────────────────────────────────────────────────────
      // 패널 표시 상태: null=닫힘, "ruler"/"calibration"/"recommend"=해당 패널 열림
      // ──────────────────────────────────────────────────────────────
      var activePanelKey by remember { mutableStateOf<String?>(null) }
      var uiVisible by remember { mutableStateOf(true) }

      // 선택된 아이템이 있으면 다른 패널 닫기
      LaunchedEffect(selectedItemId) {
        if (selectedItemId != null) activePanelKey = null
      }

      // ── UI 토글 버튼 ─────────────────────────────────────────────
      IconButton(
        onClick = { uiVisible = !uiVisible },
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 48.dp, end = 12.dp)
          .size(36.dp)
          .background(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(10.dp)
          )
      ) {
        Text(
          text = if (uiVisible) "✕" else "☰",
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // ── 상단 미니멀 상태 바 ───────────────────────────────────────
      Row(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 48.dp, start = 12.dp, end = 56.dp)
          .fillMaxWidth()
          .graphicsLayer { alpha = if (uiVisible) 1f else 0f }
          .then(if (!uiVisible) Modifier.pointerInput(Unit) { detectTapGestures {} } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // 뒤로가기
        FilledTonalIconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
          )
        ) {
          Text("←", style = MaterialTheme.typography.titleMedium)
        }
        // 상태 칩
        val statusText = when {
          isRulerModeActive && measuredDistanceCm > 0f -> "📏 ${"%.1f".format(measuredDistanceCm)}cm"
          isRulerModeActive -> "📏 자 모드 — 바닥을 터치하세요"
          trackingState != TrackingState.TRACKING -> "⚠ 추적 중단"
          placedItems.isEmpty() -> "바닥을 터치해 배치"
          else -> "${placedItems.size}개 배치됨${if (hasVerticalPlane) "  벽 감지" else ""}"
        }
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (trackingState != TrackingState.TRACKING)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
          else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
          modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
        ) {
          Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1
          )
        }
        // 모델 선택 (availableModels 있을 때만)
        if (availableModels.isNotEmpty() && availableModels.size <= 3) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            availableModels.forEachIndexed { index, model ->
              FilterChip(
                selected = selectedModelIndex == index,
                onClick = { onSelectModel(index) },
                label = {
                  Text(
                    model.name.ifEmpty { "${model.widthCm.toInt()}cm" },
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                modifier = Modifier.height(32.dp)
              )
            }
          }
        }
      }

      // ── 배치 확인 다이얼로그 ─────────────────────────────────────
      if (showGhost) {
        Card(
          modifier = Modifier
            .align(Alignment.Center)
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

      // ── 우측 아이콘 FAB 툴바 ─────────────────────────────────────
      Column(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 12.dp)
          .padding(top = 100.dp)
          .graphicsLayer { alpha = if (uiVisible) 1f else 0f }
          .then(if (!uiVisible) Modifier.pointerInput(Unit) { detectTapGestures {} } else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Ruler 토글
        ArFabButton(
          emoji = "📏",
          label = if (isRulerModeActive) "자\nON" else "자",
          isActive = isRulerModeActive,
          onClick = { onToggleRulerMode(); activePanelKey = if (isRulerModeActive) null else "ruler" }
        )
        // Calibration
        ArFabButton(
          emoji = "🎚",
          label = "보정",
          isActive = activePanelKey == "calibration",
          onClick = { activePanelKey = if (activePanelKey == "calibration") null else "calibration" }
        )
        // 추천 가구
        ArFabButton(
          emoji = "🛋",
          label = "추천",
          isActive = activePanelKey == "recommend",
          onClick = { activePanelKey = if (activePanelKey == "recommend") null else "recommend" }
        )
        // 캡처
        ArFabButton(
          emoji = "📷",
          label = "캡처",
          isActive = false,
          onClick = onScreenshot
        )
        // Undo
        ArFabButton(
          emoji = "↩",
          label = "취소",
          isActive = false,
          enabled = canUndo,
          onClick = onUndo
        )
        // Redo
        ArFabButton(
          emoji = "↪",
          label = "재실행",
          isActive = false,
          enabled = canRedo,
          onClick = onRedo
        )
        // 모두 지우기 (배치된 것 있고 선택 없을 때)
        if (placedItems.isNotEmpty() && selectedItemId == null && !showGhost) {
          ArFabButton(
            emoji = "🗑",
            label = "전체\n삭제",
            isActive = false,
            tint = MaterialTheme.colorScheme.error,
            onClick = onClearAll
          )
        }
      }

      // ── 하단 슬라이드업 패널 영역 ─────────────────────────────────
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
          .graphicsLayer { alpha = if (uiVisible) 1f else 0f }
          .then(if (!uiVisible) Modifier.pointerInput(Unit) { detectTapGestures {} } else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {

        // [선택된 가구 설정 패널] — 가구 선택 시 항상 표시
        AnimatedVisibility(
          visible = selectedItem != null,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
          if (selectedItem != null) {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
              ),
              elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = selectedItem.modelName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                      onClick = onDeselect,
                      modifier = Modifier.height(32.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("해제", style = MaterialTheme.typography.labelSmall) }
                    Button(
                      onClick = { onDeleteItem(selectedItem) },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                      modifier = Modifier.height(32.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("삭제", style = MaterialTheme.typography.labelSmall) }
                  }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 이동 패드
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedIconButton(onClick = { onNudge(selectedItem, -NUDGE_STEP_M, 0f) }, modifier = Modifier.size(36.dp)) {
                    Text("←", style = MaterialTheme.typography.labelMedium)
                  }
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                  ) {
                    OutlinedIconButton(onClick = { onNudge(selectedItem, 0f, -NUDGE_STEP_M) }, modifier = Modifier.size(36.dp)) {
                      Text("↑", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedIconButton(onClick = { onNudge(selectedItem, 0f, NUDGE_STEP_M) }, modifier = Modifier.size(36.dp)) {
                      Text("↓", style = MaterialTheme.typography.labelMedium)
                    }
                  }
                  OutlinedIconButton(onClick = { onNudge(selectedItem, NUDGE_STEP_M, 0f) }, modifier = Modifier.size(36.dp)) {
                    Text("→", style = MaterialTheme.typography.labelMedium)
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  // 회전
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedIconButton(onClick = { onRotateLeft(selectedItem) }, modifier = Modifier.size(36.dp)) {
                      Text("↺", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedIconButton(onClick = { onRotateRight(selectedItem) }, modifier = Modifier.size(36.dp)) {
                      Text("↻", style = MaterialTheme.typography.labelMedium)
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  // 투명도 + 벽 정렬
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                  ) {
                    Text(
                      text = "투명도 ${(selectedItem.opacity * 100).toInt()}%",
                      style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                      value = selectedItem.opacity,
                      onValueChange = { onOpacityChange(selectedItem, it) },
                      valueRange = 0.1f..1.0f,
                      modifier = Modifier.fillMaxWidth()
                    )
                    if (hasVerticalPlane) {
                      OutlinedButton(
                        onClick = { onAlignToWall(selectedItem) },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                      ) { Text("벽에 정렬", style = MaterialTheme.typography.labelSmall) }
                    }
                  }
                }
              }
            }
          }
        }

        // [Ruler 패널] — Ruler FAB 탭 시 표시
        AnimatedVisibility(
          visible = activePanelKey == "ruler" && isRulerModeActive,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("📏 자 모드", style = MaterialTheme.typography.titleSmall)
                if (measuredDistanceCm > 0f) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "측정 거리: ${"%.1f".format(measuredDistanceCm)} cm",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // R4. Clipboard Copy Button
                    IconButton(
                      onClick = {
                        val textToCopy = "[FitCheck AR] 측정 공간 거리: ${"%.1f".format(measuredDistanceCm)} cm"
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FitCheck AR Distance", textToCopy)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "측정 거리가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Text("📋", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // R4. External Share Intent Button
                    IconButton(
                      onClick = {
                        val textToShare = "[FitCheck AR] 인테리어 가구 배치용 측정 거리: ${"%.1f".format(measuredDistanceCm)} cm"
                        val sendIntent = Intent().apply {
                          action = Intent.ACTION_SEND
                          putExtra(Intent.EXTRA_TEXT, textToShare)
                          type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "측정 거리 공유"))
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Text("🔗", fontSize = 14.sp)
                    }
                  }
                } else {
                  Text("바닥을 두 번 탭하세요", style = MaterialTheme.typography.bodySmall)
                }
              }
              OutlinedButton(
                onClick = onClearRuler,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
              ) { Text("자 지우기", style = MaterialTheme.typography.labelSmall) }
            }
          }
        }

        // [Calibration 패널] — 보정 FAB 탭 시 표시
        AnimatedVisibility(
          visible = activePanelKey == "calibration",
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("🎚 크기 보정", style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${"%.0f".format(calibrationFactor * 100)}%",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Slider(
                value = calibrationFactor,
                onValueChange = onCalibrationFactorChange,
                valueRange = 0.8f..1.2f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("80%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("100%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("120%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        // [추천 가구 패널] — 추천 FAB 탭 시 표시
        AnimatedVisibility(
          visible = activePanelKey == "recommend",
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
              Text("🛋 추천 가구", style = MaterialTheme.typography.titleSmall)
              if (safetyWarning != null) {
                Text(safetyWarning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
              }
              if (measuredDistanceCm > 0f) {
                Text(
                  text = "공간 ${"%.0f".format(measuredDistanceCm)}cm 기준 필터링",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              var selectedCategory by remember { mutableStateOf("전체") }
              val categories = listOf("전체", "주방", "다용도실", "침실/거실", "기타")

              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                categories.forEach { category ->
                  FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              val filteredFurniture = remember(recommendedFurniture, selectedCategory) {
                if (selectedCategory == "전체") recommendedFurniture else recommendedFurniture.filter { getPresetCategory(it.name) == selectedCategory }
              }

              Row(
                modifier = Modifier
                  .horizontalScroll(rememberScrollState())
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                filteredFurniture.forEach { furniture ->
                  ElevatedButton(
                    onClick = {
                      onSelectRecommended(furniture)
                      activePanelKey = null
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        text = "[${furniture.brand}] ${furniture.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        maxLines = 1
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "${furniture.widthCm.toInt()}×${furniture.depthCm.toInt()}cm  •  ${furniture.price}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                      )
                    }
                  }
                }
                if (filteredFurniture.isEmpty()) {
                  Text("해당 조건에 맞는 가구가 없습니다", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
              }
            }
          }
        }
      }

      if (!isPlaneDetected) {
        ScanGuideOverlay()
      }

      if (showOnboarding) {
        OnboardingTutorial(onDismiss = { showOnboarding = false })
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
fun ArFabButton(
  emoji: String,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  enabled: Boolean = true,
  tint: Color = Color.Unspecified
) {
  val containerColor = if (isActive)
    MaterialTheme.colorScheme.primary
  else
    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
  val contentColor = if (isActive)
    MaterialTheme.colorScheme.onPrimary
  else if (tint != Color.Unspecified) tint
  else MaterialTheme.colorScheme.onSurface

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    shadowElevation = if (isActive) 6.dp else 2.dp,
    modifier = Modifier
      .size(width = 52.dp, height = 52.dp)
      .then(
        if (enabled) Modifier.clickable { onClick() }
        else Modifier
      )
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = emoji,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        color = if (!enabled) contentColor.copy(alpha = 0.38f) else contentColor
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
        color = if (!enabled) contentColor.copy(alpha = 0.38f) else contentColor,
        textAlign = TextAlign.Center,
        maxLines = 2,
        lineHeight = 9.sp
      )
    }
  }
}

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

private fun multiplyQuaternions(q1: FloatArray, q2: FloatArray): FloatArray {
  val x1 = q1[0]; val y1 = q1[1]; val z1 = q1[2]; val w1 = q1[3]
  val x2 = q2[0]; val y2 = q2[1]; val z2 = q2[2]; val w2 = q2[3]
  return floatArrayOf(
    x1 * w2 + y1 * z2 - z1 * y2 + w1 * x2,
    -x1 * z2 + y1 * w2 + z1 * x2 + w1 * y2,
    x1 * y2 - y1 * x2 + z1 * w2 + w1 * z2,
    -x1 * x2 - y1 * y2 - z1 * z2 + w1 * w2
  )
}

fun getPresetCategory(name: String): String {
  return when {
    name.contains("냉장고") || name.contains("식기세척기") -> "주방"
    name.contains("세탁기") -> "다용도실"
    name.contains("옷장") || name.contains("침대") || name.contains("소파") || name.contains("식탁") -> "침실/거실"
    else -> "기타"
  }
}

