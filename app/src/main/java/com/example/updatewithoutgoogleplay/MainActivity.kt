package com.example.updatewithoutgoogleplay

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.updatewithoutgoogleplay.ui.theme.UpdateWithoutGooglePlayTheme
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UpdateWithoutGooglePlayTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
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

private fun checkForUpdates(context: Context) {
    val appUpdater = AppUpdater(context)
        .setDisplay(Display.DIALOG)
        .setUpdateFrom(UpdateFrom.JSON)
        .setGitHubUserAndRepo("your-github-username", "your-repo-name")
        .setTitleOnUpdateAvailable("Update Available")
        .setContentOnUpdateAvailable("A new version is available. Do you want to update?")
        .setButtonUpdate("Update")
        .setButtonDismiss("Later")

    appUpdater.start()
}