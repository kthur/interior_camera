package com.example.interiorcamera.ui.ar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingStep(
  val emoji: String,
  val title: String,
  val description: String
)

@Composable
fun OnboardingTutorial(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val sharedPreferences = remember { context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE) }
  var isCompleted by remember { mutableStateOf(sharedPreferences.getBoolean("completed_ar", false)) }

  if (isCompleted) return

  val steps = listOf(
    OnboardingStep("📱", "바닥 스캔", "스마트폰 카메라를 비추고 천천히 움직여 바닥 평면을 인식시킵니다."),
    OnboardingStep("🛋", "가구 배치 & 조작", "인식된 바닥을 터치하면 가구가 배치됩니다. 터치 후 이동, 회전, 투명도 변경이 가능합니다."),
    OnboardingStep("📏", "자(Ruler) 모드", "📏 버튼을 활성화한 후 바닥의 두 지점을 터치하면 실제 직선거리를 측정할 수 있습니다."),
    OnboardingStep("🎚", "오차 보정", "🎚 보정 슬라이더를 움직여 실제 크기와 가구의 3D 크기 비율을 정밀하게 맞출 수 있습니다.")
  )

  var currentStepIndex by remember { mutableStateOf(0) }
  val currentStep = steps[currentStepIndex]

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.75f)),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth(0.85f)
        .padding(16.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Step Indicator
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 16.dp)
        ) {
          steps.indices.forEach { index ->
            Box(
              modifier = Modifier
                .size(if (index == currentStepIndex) 10.dp else 8.dp)
                .background(
                  color = if (index == currentStepIndex)
                    MaterialTheme.colorScheme.primary
                  else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                  shape = CircleShape
                )
            )
          }
        }

        // Large Emoji representation
        Text(
          text = currentStep.emoji,
          fontSize = 64.sp,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        // Title
        Text(
          text = currentStep.title,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 8.dp)
        )

        // Description
        Text(
          text = currentStep.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .padding(bottom = 24.dp)
            .height(60.dp)
        )

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (currentStepIndex > 0) {
            TextButton(onClick = { currentStepIndex-- }) {
              Text("이전", style = MaterialTheme.typography.labelLarge)
            }
          } else {
            Spacer(modifier = Modifier.width(48.dp))
          }

          Button(
            onClick = {
              if (currentStepIndex < steps.size - 1) {
                currentStepIndex++
              } else {
                sharedPreferences.edit().putBoolean("completed_ar", true).apply()
                isCompleted = true
                onDismiss()
              }
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = if (currentStepIndex == steps.size - 1) "시작하기" else "다음",
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }
    }
  }
}
