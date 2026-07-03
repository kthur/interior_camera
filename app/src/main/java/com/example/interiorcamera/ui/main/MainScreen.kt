package com.example.interiorcamera.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.interiorcamera.ArView
import com.example.interiorcamera.theme.InteriorCameraTheme

data class PresetItem(
  val name: String,
  val width: Float,
  val height: Float,
  val depth: Float
)

val PRESETS = listOf(
  PresetItem("양문형 냉장고", 91.2f, 178.4f, 75.0f),
  PresetItem("드럼 세탁기", 60.0f, 85.0f, 65.0f),
  PresetItem("4도어 김치냉장고", 66.6f, 179.7f, 69.5f),
  PresetItem("식기세척기 (빌트인)", 59.8f, 84.5f, 57.3f),
  PresetItem("옷장 (자작)", 100.0f, 210.0f, 60.0f)
)

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  var widthStr by remember { mutableStateOf("91.2") }
  var heightStr by remember { mutableStateOf("178.4") }
  var depthStr by remember { mutableStateOf("75.0") }
  
  var selectedPresetIndex by remember { mutableStateOf(-1) }

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
                widthStr = preset.width.toString()
                heightStr = preset.height.toString()
                depthStr = preset.depth.toString()
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

    HorizontalDivider()

    Text(
      text = "직접 치수 입력 (cm 단위)",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.align(Alignment.Start)
    )

    OutlinedTextField(
      value = widthStr,
      onValueChange = {
        widthStr = it
        selectedPresetIndex = -1
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
      },
      label = { Text("깊이 (Depth)") },
      suffix = { Text("cm") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    val width = widthStr.toFloatOrNull() ?: 0f
    val height = heightStr.toFloatOrNull() ?: 0f
    val depth = depthStr.toFloatOrNull() ?: 0f
    val isValid = width > 0f && height > 0f && depth > 0f

    Button(
      onClick = {
        if (isValid) {
          onItemClick(ArView(width, height, depth))
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
    MainScreen(onItemClick = {})
  }
}
