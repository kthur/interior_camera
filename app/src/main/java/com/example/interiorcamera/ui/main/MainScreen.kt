package com.example.interiorcamera.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.interiorcamera.ArItem
import com.example.interiorcamera.ArView
import com.example.interiorcamera.data.PresetItem
import com.example.interiorcamera.theme.InteriorCameraTheme
import com.google.ar.core.ArCoreApk

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
    modifier = modifier
  )
}

@Composable
fun MainScreenContent(
  uiState: MainScreenUiState,
  onItemClick: (NavKey) -> Unit,
  onSavePreset: (PresetItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var customName by remember { mutableStateOf("") }
  var widthStr by remember { mutableStateOf("91.2") }
  var heightStr by remember { mutableStateOf("178.4") }
  var depthStr by remember { mutableStateOf("75.0") }

  var selectedPresetIndex by remember { mutableStateOf(-1) }
  var modelName by remember { mutableStateOf("cube.glb") }
  var calibrationFactor by remember { mutableStateOf(1.0f) }
  var showCalibrationDialog by remember { mutableStateOf(false) }

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

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "FitCheck AR",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(top = 16.dp)
    )

    Text(
      text = "가구 및 가전제품의 크기를 입력하고 카메라 화면을 통해 실제 공간에 맞는지 확인해 보세요.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    HorizontalDivider()

    Text(
      text = "자주 찾는 제품 규격 (프리셋)",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.align(Alignment.Start)
    )

    // Preset selection flow layout or row
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

    // New section: 나의 리스트
    if (uiState is MainScreenUiState.Success) {
      val customPresets = uiState.presets
      if (customPresets.isNotEmpty()) {
        HorizontalDivider()
        Text(
          text = "나의 리스트",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.align(Alignment.Start)
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
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.align(Alignment.Start)
    )

    OutlinedTextField(
      value = customName,
      onValueChange = {
        customName = it
      },
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

    Spacer(modifier = Modifier.height(8.dp))

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

    Spacer(modifier = Modifier.height(4.dp))

    HorizontalDivider()
    Text(
      text = "크기 보정 (선택사항)",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.align(Alignment.Start)
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

    Spacer(modifier = Modifier.height(8.dp))

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
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  InteriorCameraTheme {
    MainScreenContent(
      uiState = MainScreenUiState.Success(emptyList()),
      onItemClick = {},
      onSavePreset = {}
    )
  }
}

