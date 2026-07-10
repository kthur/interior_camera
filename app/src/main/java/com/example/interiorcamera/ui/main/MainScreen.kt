package com.example.interiorcamera.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.interiorcamera.ArItem
import com.example.interiorcamera.ArView
import com.example.interiorcamera.data.PresetItem
import com.example.interiorcamera.data.RoomPreset
import com.example.interiorcamera.theme.InteriorCameraTheme
import com.example.interiorcamera.ui.gallery.GalleryScreen
import com.example.interiorcamera.ui.floorplan.FloorplanCanvas
import com.example.interiorcamera.ui.floorplan.ArPlacedItem
import com.google.ar.core.ArCoreApk
import java.util.UUID

val PRESETS = listOf(
  PresetItem("양문형 냉장고", 91.2f, 178.4f, 75.0f, "refrigerator.glb"),
  PresetItem("드럼 세탁기", 60.0f, 85.0f, 65.0f, "cube.glb"),
  PresetItem("4도어 김치냉장고", 66.6f, 179.7f, 69.5f, "refrigerator.glb"),
  PresetItem("식기세척기 (빌트인)", 59.8f, 84.5f, 57.3f, "cube.glb"),
  PresetItem("옷장 (자작)", 100.0f, 210.0f, 60.0f, "cube.glb")
)

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory)
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  MainScreenContent(
    uiState = uiState,
    onItemClick = onItemClick,
    onSavePreset = { viewModel.savePreset(it) },
    onSaveRoomPreset = { viewModel.saveRoomPreset(it) },
    onDeleteRoomPreset = { viewModel.deleteRoomPreset(it) },
    modifier = modifier
  )
}

@Composable
fun MainScreenContent(
  uiState: MainScreenUiState,
  onItemClick: (NavKey) -> Unit,
  onSavePreset: (PresetItem) -> Unit,
  onSaveRoomPreset: (RoomPreset) -> Unit,
  onDeleteRoomPreset: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(0) }

  var customName by remember { mutableStateOf("") }
  var widthStr by remember { mutableStateOf("91.2") }
  var heightStr by remember { mutableStateOf("178.4") }
  var depthStr by remember { mutableStateOf("75.0") }
  var selectedPresetIndex by remember { mutableStateOf(-1) }
  var modelName by remember { mutableStateOf("cube.glb") }
  var calibrationFactor by remember { mutableStateOf(1.0f) }

  var roomName by remember { mutableStateOf("") }
  var roomWidthStr by remember { mutableStateOf("300") }
  var roomDepthStr by remember { mutableStateOf("350") }

  var activeFloorplanRoom by remember { mutableStateOf<RoomPreset?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      val width = widthStr.toFloatOrNull() ?: 0f
      val height = heightStr.toFloatOrNull() ?: 0f
      val depth = depthStr.toFloatOrNull() ?: 0f
      val arItems = PRESETS.map { ArItem(it.name, it.width, it.height, it.depth, it.modelName) }
      onItemClick(ArView(width, height, depth, modelName, calibrationFactor, arItems))
    } else {
      Toast.makeText(context, "AR 기능을 실행하려면 카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    Spacer(modifier = Modifier.height(12.dp))
    Image(
      painter = androidx.compose.ui.res.painterResource(id = com.example.interiorcamera.R.drawable.ic_app_logo),
      contentDescription = "FitCheck AR Logo",
      modifier = Modifier
        .size(80.dp)
        .align(Alignment.CenterHorizontally)
        .clip(RoundedCornerShape(20.dp))
    )
    Text(
      text = "FitCheck AR",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(top = 8.dp, bottom = 12.dp).align(Alignment.CenterHorizontally)
    )

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier.fillMaxWidth()
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("📐 배치", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("🏠 나의 방", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("🖼 갤러리", fontWeight = FontWeight.Bold) }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (selectedTab) {
      0 -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "가구 및 가전제품의 크기를 입력하고 카메라 화면을 통해 실제 공간에 맞는지 확인해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          HorizontalDivider()

          Text(
            text = "자주 찾는 제품 규격 (프리셋)",
            style = MaterialTheme.typography.titleMedium
          )

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            PRESETS.chunked(2).forEach { rowPresets ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowPresets.forEach { preset ->
                  val index = PRESETS.indexOf(preset)
                  FilterChip(
                    selected = selectedPresetIndex == index,
                    onClick = {
                      selectedPresetIndex = index
                      customName = preset.name
                      widthStr = preset.width.toString()
                      heightStr = preset.height.toString()
                      depthStr = preset.depth.toString()
                      modelName = preset.modelName
                    },
                    label = { Text(preset.name) },
                    modifier = Modifier.weight(1f)
                  )
                }
                if (rowPresets.size < 2) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          if (uiState is MainScreenUiState.Success) {
            val customPresets = uiState.presets
            if (customPresets.isNotEmpty()) {
              HorizontalDivider()
              Text(
                text = "나의 리스트",
                style = MaterialTheme.typography.titleMedium
              )

              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                customPresets.chunked(2).forEach { rowPresets ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    rowPresets.forEach { preset ->
                      val isSelected = selectedPresetIndex == -1 &&
                          customName == preset.name &&
                          widthStr == preset.width.toString() &&
                          heightStr == preset.height.toString() &&
                          depthStr == preset.depth.toString()
                      FilterChip(
                        selected = isSelected,
                        onClick = {
                          selectedPresetIndex = -1
                          customName = preset.name
                          widthStr = preset.width.toString()
                          heightStr = preset.height.toString()
                          depthStr = preset.depth.toString()
                          modelName = preset.modelName
                        },
                        label = { Text(preset.name) },
                        modifier = Modifier.weight(1f)
                      )
                    }
                    if (rowPresets.size < 2) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider()

          Text(
            text = "직접 치수 입력 (cm 단위)",
            style = MaterialTheme.typography.titleMedium
          )

          OutlinedTextField(
            value = customName,
            onValueChange = { customName = it },
            label = { Text("제품명 (Product Name)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = widthStr,
            onValueChange = {
              widthStr = it
              selectedPresetIndex = -1
              modelName = "cube.glb"
            },
            label = { Text("가로 너비 (Width)") },
            suffix = { Text("cm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = heightStr,
            onValueChange = {
              heightStr = it
              selectedPresetIndex = -1
              modelName = "cube.glb"
            },
            label = { Text("높이 (Height)") },
            suffix = { Text("cm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = depthStr,
            onValueChange = {
              depthStr = it
              selectedPresetIndex = -1
              modelName = "cube.glb"
            },
            label = { Text("깊이 (Depth)") },
            suffix = { Text("cm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          val width = widthStr.toFloatOrNull() ?: 0f
          val height = heightStr.toFloatOrNull() ?: 0f
          val depth = depthStr.toFloatOrNull() ?: 0f
          val isValid = width > 0f && height > 0f && depth > 0f
          val isSaveEnabled = customName.isNotBlank() && isValid

          Button(
            onClick = {
              if (isSaveEnabled) {
                onSavePreset(
                  PresetItem(
                    name = customName.trim(),
                    width = width,
                    height = height,
                    depth = depth,
                    modelName = modelName
                  )
                )
                customName = ""
              }
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Save to My List")
          }

          HorizontalDivider()

          Text(
            text = "크기 보정 (선택사항)",
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = "AR 크기가 실제와 다르다면 보정값을 조정하세요. (1.0 = 기본, 0.9 = 10% 작게, 1.1 = 10% 크게)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(onClick = { calibrationFactor = (calibrationFactor - 0.05f).coerceAtLeast(0.5f) }) {
              Text("-0.05")
            }
            Text(
              text = "%.2f".format(calibrationFactor),
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.width(64.dp),
              textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = { calibrationFactor = (calibrationFactor + 0.05f).coerceAtMost(2.0f) }) {
              Text("+0.05")
            }
            OutlinedButton(onClick = { calibrationFactor = 1.0f }) {
              Text("초기화")
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              if (isValid) {
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                  Toast.makeText(context, "이 기기는 ARCore를 지원하지 않아 AR 기능을 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
                  return@Button
                }

                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                  val arItems = PRESETS.map { ArItem(it.name, it.width, it.height, it.depth, it.modelName) }
                  onItemClick(ArView(width, height, depth, modelName, calibrationFactor, arItems))
                } else {
                  permissionLauncher.launch(Manifest.permission.CAMERA)
                }
              }
            },
            enabled = isValid,
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
          ) {
            Text(
              text = "AR 카메라로 확인하기",
              style = MaterialTheme.typography.titleMedium
            )
          }

          Spacer(modifier = Modifier.height(24.dp))
        }
      }
      1 -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "자주 측정하는 방의 규격을 등록해두면, 가구 피팅 화면에서 방 사이즈에 맞는 추천 상품을 더 빨리 확인할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text("🏠 새 방 크기 추가", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

              OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("방 이름 (예: 안방, 거실, 서재)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = roomWidthStr,
                  onValueChange = { roomWidthStr = it },
                  label = { Text("가로 너비") },
                  suffix = { Text("cm") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier.weight(1f),
                  singleLine = true
                )
                OutlinedTextField(
                  value = roomDepthStr,
                  onValueChange = { roomDepthStr = it },
                  label = { Text("세로 깊이") },
                  suffix = { Text("cm") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  modifier = Modifier.weight(1f),
                  singleLine = true
                )
              }

              val roomWidth = roomWidthStr.toFloatOrNull() ?: 0f
              val roomDepth = roomDepthStr.toFloatOrNull() ?: 0f
              val isRoomValid = roomName.isNotBlank() && roomWidth > 0f && roomDepth > 0f

              Button(
                onClick = {
                  if (isRoomValid) {
                    onSaveRoomPreset(
                      RoomPreset(
                        id = UUID.randomUUID().toString(),
                        name = roomName.trim(),
                        widthCm = roomWidth,
                        depthCm = roomDepth,
                        timestamp = System.currentTimeMillis()
                      )
                    )
                    roomName = ""
                  }
                },
                enabled = isRoomValid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("방 크기 저장")
              }
            }
          }

          HorizontalDivider()

          Text("저장된 방 목록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

          if (uiState is MainScreenUiState.Success) {
            val roomPresets = uiState.roomPresets
            if (roomPresets.isEmpty()) {
              Text(
                "등록된 방이 없습니다. 위 양식에서 방을 추가해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
              )
            } else {
              Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                roomPresets.forEach { room ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      val ratio = (room.widthCm / room.depthCm).coerceIn(0.5f, 2.0f)
                      val boxWidth = if (ratio > 1f) 64.dp else (64 * ratio).dp
                      val boxHeight = if (ratio < 1f) 64.dp else (64 / ratio).dp
                      Box(
                        modifier = Modifier
                          .size(72.dp)
                          .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                      ) {
                        Box(
                          modifier = Modifier
                            .size(width = boxWidth, height = boxHeight)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        )
                      }

                      Spacer(modifier = Modifier.width(16.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Text(room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                          text = "${room.widthCm.toInt()} cm × ${room.depthCm.toInt()} cm",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          text = "가구: ${room.items.size}개 배치됨",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary
                        )
                      }

                      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 2D 도면 배치 에디터 실행 버튼
                        OutlinedButton(
                          onClick = { activeFloorplanRoom = room },
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                          shape = RoundedCornerShape(8.dp)
                        ) {
                          Text("📐 도면", fontSize = 11.sp)
                        }

                        IconButton(
                          onClick = {
                            val availability = ArCoreApk.getInstance().checkAvailability(context)
                            if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                              Toast.makeText(context, "이 기기는 ARCore를 지원하지 않아 AR 기능을 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
                              return@IconButton
                            }
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                              val arItems = room.items.map {
                                ArItem(
                                  name = it.name,
                                  widthCm = it.widthCm,
                                  heightCm = it.heightCm,
                                  depthCm = it.depthCm,
                                  modelName = it.modelName,
                                  isFloorplanPlaced = true,
                                  offsetX = it.offsetX,
                                  offsetZ = it.offsetZ,
                                  rotationDegrees = it.rotationDegrees
                                )
                              }
                              onItemClick(ArView(room.widthCm, 100f, room.depthCm, "cube.glb", 1.0f, arItems))
                            } else {
                              permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                          }
                        ) {
                          Text("📷", fontSize = 18.sp)
                        }

                        IconButton(onClick = { onDeleteRoomPreset(room.id) }) {
                          Text("🗑", fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
      2 -> {
        GalleryScreen(modifier = Modifier.fillMaxSize())
      }
    }

    // 2D Floorplan Layout Editor Dialog
    activeFloorplanRoom?.let { room ->
      FloorplanEditorDialog(
        room = room,
        onDismiss = { activeFloorplanRoom = null },
        onSave = { updatedItems ->
          onSaveRoomPreset(room.copy(items = updatedItems))
          activeFloorplanRoom = null
        },
        onLaunchAr = { updatedItems ->
          val arItems = updatedItems.map {
            ArItem(
              name = it.name,
              widthCm = it.widthCm,
              heightCm = it.heightCm,
              depthCm = it.depthCm,
              modelName = it.modelName,
              isFloorplanPlaced = true,
              offsetX = it.offsetX,
              offsetZ = it.offsetZ,
              rotationDegrees = it.rotationDegrees
            )
          }
          val availability = ArCoreApk.getInstance().checkAvailability(context)
          if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Toast.makeText(context, "이 기기는 ARCore를 지원하지 않아 AR 기능을 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
            return@FloorplanEditorDialog
          }
          val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
          if (hasPermission) {
            onItemClick(ArView(room.widthCm, 100f, room.depthCm, "cube.glb", 1.0f, arItems))
          } else {
            Toast.makeText(context, "AR 피팅을 하려면 카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
          }
        }
      )
    }
  }
}

@Composable
fun FloorplanEditorDialog(
  room: RoomPreset,
  onDismiss: () -> Unit,
  onSave: (List<ArPlacedItem>) -> Unit,
  onLaunchAr: (List<ArPlacedItem>) -> Unit
) {
  var placedItems by remember { mutableStateOf(room.items) }
  var selectedItemName by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Action Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("닫기", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "${room.name} 2D 도면 배치",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Button(
            onClick = { onSave(placedItems) },
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("도면 저장")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid canvas (Room layout width/depth proportions)
        FloorplanCanvas(
          roomWidthCm = room.widthCm,
          roomDepthCm = room.depthCm,
          placedItems = placedItems,
          onPlacedItemsChanged = { placedItems = it },
          selectedItemName = selectedItemName,
          onSelectItem = { selectedItemName = it },
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Item Action Tools (Rotate, Delete)
        selectedItemName?.let { selName ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(onClick = {
              placedItems = placedItems.map {
                if (it.name == selName) it.copy(rotationDegrees = (it.rotationDegrees - 15f) % 360f) else it
              }
            }) {
              Text("🔄 좌회전(-15°)", fontSize = 12.sp)
            }

            OutlinedButton(onClick = {
              placedItems = placedItems.map {
                if (it.name == selName) it.copy(rotationDegrees = (it.rotationDegrees + 15f) % 360f) else it
              }
            }) {
              Text("🔄 우회전(+15°)", fontSize = 12.sp)
            }

            Button(
              onClick = {
                placedItems = placedItems.filter { it.name != selName }
                selectedItemName = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
              Text("🗑 삭제", fontSize = 12.sp)
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Add furniture panel
        Text(
          text = "🛋 배치할 가구/가전 추가",
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.align(Alignment.Start)
        )
        
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val availableFurniture = listOf(
            ArPlacedItem("양문형 냉장고", 91.2f, 178.4f, 75.0f, "refrigerator.glb", 0f, 0f, 0f),
            ArPlacedItem("드럼 세탁기", 60.0f, 85.0f, 65.0f, "cube.glb", 0f, 0f, 0f),
            ArPlacedItem("소파 (IKEA)", 160f, 85f, 90f, "cube.glb", 0f, 0f, 0f),
            ArPlacedItem("식탁 (한샘)", 140f, 75f, 80f, "cube.glb", 0f, 0f, 0f),
            ArPlacedItem("싱글 침대", 100f, 45f, 200f, "cube.glb", 0f, 0f, 0f)
          )

          availableFurniture.forEach { item ->
            ElevatedCard(
              onClick = {
                val count = placedItems.count { it.name.startsWith(item.name) }
                val uniqueName = if (count > 0) "${item.name} (${count + 1})" else item.name
                placedItems = placedItems + item.copy(name = uniqueName)
              },
              modifier = Modifier.width(120.dp)
            ) {
              Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${item.widthCm.toInt()}x${item.depthCm.toInt()}cm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Launch AR fitting button
        Button(
          onClick = { onLaunchAr(placedItems) },
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
          Text("📷 이 도면 레이아웃대로 AR 피팅", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  InteriorCameraTheme {
    MainScreenContent(
      uiState = MainScreenUiState.Success(emptyList()),
      onItemClick = {},
      onSavePreset = {},
      onSaveRoomPreset = {},
      onDeleteRoomPreset = {}
    )
  }
}
