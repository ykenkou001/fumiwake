package dev.fumiwake.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * M0 placeholder activity. Just proves the Compose + AGP + alphaTab
 * dependency wiring compiles in CI. Real screens land in M2a/M2b.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FumiwakeApp()
        }
    }
}

@Composable
fun FumiwakeApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "fumiwake")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FumiwakeAppPreview() {
    FumiwakeApp()
}
