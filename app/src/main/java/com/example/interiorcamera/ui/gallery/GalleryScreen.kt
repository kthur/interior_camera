package com.example.interiorcamera.ui.gallery

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class SnapshotItem(
  val file: File,
  val bitmap: Bitmap?,
  val dateFormatted: String
)

@Composable
fun GalleryScreen(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val snapshotDir = remember { File(context.filesDir, "snapshots") }
  var snapshots by remember { mutableStateOf<List<SnapshotItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var selectedSnapshot by remember { mutableStateOf<SnapshotItem?>(null) }
  val scope = rememberCoroutineScope()

  val loadSnapshots = suspend {
    isLoading = true
    withContext(Dispatchers.IO) {
      if (!snapshotDir.exists()) {
        snapshotDir.mkdirs()
      }
      val files = snapshotDir.listFiles { _, name -> name.endsWith(".jpg") || name.endsWith(".jpeg") } ?: emptyArray()
      files.sortByDescending { it.lastModified() }

      val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
      snapshots = files.map { file ->
        val options = BitmapFactory.Options().apply {
          inSampleSize = 4
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        SnapshotItem(
          file = file,
          bitmap = bitmap,
          dateFormatted = sdf.format(Date(file.lastModified()))
        )
      }
    }
    isLoading = false
  }

  LaunchedEffect(Unit) {
    loadSnapshots()
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    if (isLoading) {
      CircularProgressIndicator()
    } else if (snapshots.isEmpty()) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
      ) {
        Text("📷", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "아직 촬영된 AR 스냅샷이 없습니다.",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "AR 카메라 화면에서 📷 버튼을 눌러 실제 공간에 가구를 배치한 모습을 저장해보세요.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(snapshots) { item ->
          Box(
            modifier = Modifier
              .aspectRatio(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { selectedSnapshot = item }
          ) {
            if (item.bitmap != null) {
              Image(
                bitmap = item.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                Text("Error", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }

    selectedSnapshot?.let { snapshot ->
      DetailDialog(
        snapshot = snapshot,
        onDismiss = { selectedSnapshot = null },
        onDelete = {
          if (snapshot.file.exists()) {
            snapshot.file.delete()
          }
          selectedSnapshot = null
          scope.launch {
            loadSnapshots()
          }
        },
        context = context
      )
    }
  }
}

@Composable
fun DetailDialog(
  snapshot: SnapshotItem,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  context: Context
) {
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("닫기", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = snapshot.dateFormatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        val fullBitmap = remember(snapshot.file) {
          BitmapFactory.decodeFile(snapshot.file.absolutePath)
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (fullBitmap != null) {
            Image(
              bitmap = fullBitmap.asImageBitmap(),
              contentDescription = null,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Text("이미지를 로드할 수 없습니다.")
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Button(
            onClick = {
              val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                snapshot.file
              )
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(intent, "AR 스냅샷 공유"))
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("공유하기", style = MaterialTheme.typography.titleMedium)
          }

          Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("삭제", style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    }
  }
}
