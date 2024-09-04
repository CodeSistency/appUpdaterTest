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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
                    UpdateCheckDialog(context, versionCheckState.value!!)
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Primera version")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UpdateWithoutGooglePlayTheme {
        Greeting("Android")
    }
}

@Composable
fun UpdateCheckDialog(context: Context, versionCheck: VersionCheck) {
    AlertDialog(
        onDismissRequest = { /* handle dismiss */ },
        title = { Text(text = "Update Available") },
        text = { Text(text = "Version ${versionCheck.versionName} is available.\n\n${versionCheck.releaseNotes}") },
        confirmButton = {
            Button(onClick = { downloadAndInstallApk(context, versionCheck.apkUrl) }) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = { /* handle dismiss */ }) {
                Text("Later")
            }
        }
    )
}

private fun downloadAndInstallApk(context: Context, apkUrl: String) {
    val request = DownloadManager.Request(Uri.parse(apkUrl))
    request.setTitle("Downloading Update")
    request.setDescription("Please wait...")
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yourapp.apk")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setMimeType("application/vnd.android.package-archive")

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    Log.d("Update", "Download started with ID: $downloadId")

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val receivedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == receivedDownloadId) {
                Log.d("Update", "Download completed for ID: $downloadId")

                val uri = manager.getUriForDownloadedFile(downloadId)
                if (uri != null) {
                    Log.d("Update", "APK Uri: $uri")
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(installIntent)
                } else {
                    Log.e("Update", "Failed to get URI for downloaded file")
                }
            } else {
                Log.e("Update", "Download ID mismatch")
            }
        }
    }

    context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
}