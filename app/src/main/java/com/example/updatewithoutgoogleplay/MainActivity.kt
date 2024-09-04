package com.example.updatewithoutgoogleplay

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.updatewithoutgoogleplay.remote.RetrofitClient
import com.example.updatewithoutgoogleplay.remote.VersionCheck
import com.example.updatewithoutgoogleplay.ui.theme.UpdateWithoutGooglePlayTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UpdateWithoutGooglePlayTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val versionCheckState = remember { mutableStateOf<VersionCheck?>(null) }
                val showUpdateDialog = remember { mutableStateOf(false) }
                val downloadProgress = remember { mutableStateOf<Int?>(null) }

                LaunchedEffect(Unit) {
                    scope.launch {
                        try {
                            val versionCheck = RetrofitClient.service.getVersionCheck()
                            val currentVersionCode = BuildConfig.VERSION_CODE
                            if (versionCheck.versionCode > currentVersionCode) {
                                versionCheckState.value = versionCheck
                                showUpdateDialog.value = true
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                            Log.e("Error", e.message.toString())
                        }
                    }
                }

                if (showUpdateDialog.value && versionCheckState.value != null) {
                    UpdateCheckDialog(
                        context = context,
                        versionCheck = versionCheckState.value!!,
                        onDismiss = { showUpdateDialog.value = false },
                        onDownloadStart = { downloadProgress.value = it },
                        downloadProgress = downloadProgress
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(text = "Primera version")
                }
            }
        }
    }
}

@Composable
fun UpdateCheckDialog(
    context: Context,
    versionCheck: VersionCheck,
    onDismiss: () -> Unit,
    onDownloadStart: (Int) -> Unit,
    downloadProgress: MutableState<Int?>
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(text = "Update Available") },
        text = {
            Column {
                Text(text = "Version ${versionCheck.versionName} is available.\n\n${versionCheck.releaseNotes}")
                downloadProgress.value?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = it / 100f)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onDownloadStart(0)
                downloadAndInstallApk(context, versionCheck.apkUrl, onDownloadStart)
            }) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Later")
            }
        }
    )
}

private fun downloadAndInstallApk(
    context: Context,
    apkUrl: String,
    onDownloadStart: (Int) -> Unit
) {
    val request = DownloadManager.Request(Uri.parse(apkUrl))
    request.setTitle("Downloading Update")
    request.setDescription("Please wait...")
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yourapp.apk")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
    request.setMimeType("application/vnd.android.package-archive")

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (downloadId == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)) {
                val uri = manager.getUriForDownloadedFile(downloadId)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(installIntent)
            }
        }
    }

    val onProgress = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = manager.query(query)
            if (cursor.moveToFirst()) {
                val totalSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val downloadedSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                if (totalSize > 0) {
                    val progress = (downloadedSize * 100L / totalSize).toInt()
                    onDownloadStart(progress)
                }
            }
        }
    }

    context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    context.registerReceiver(onProgress, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))
}