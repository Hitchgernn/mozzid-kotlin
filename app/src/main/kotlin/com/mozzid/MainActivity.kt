package com.mozzid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.mozzid.data.permission.PermissionBridge
import com.mozzid.domain.model.AppSettings
import com.mozzid.domain.model.ThemeBrightness
import com.mozzid.presentation.HomeScreen
import com.mozzid.presentation.ProvideAppLanguage
import com.mozzid.presentation.theme.AppAccent
import com.mozzid.presentation.theme.MozzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Build the graph once, off the UI construction path.
            val boot by produceState<Bootstrap?>(initialValue = null) {
                value = Bootstrap.create(applicationContext)
            }

            boot?.let { app ->
                // Persisted settings drive language, brightness and accent, so
                // changing any of them relabels or recolours the whole tree live.
                val settings by app.settingsRepository.watch()
                    .collectAsState(initial = AppSettings())

                ProvideAppLanguage(settings.language) {
                    MozzTheme(
                        dark = settings.brightness == ThemeBrightness.DARK,
                        accent = AppAccent.fromName(settings.accentId),
                    ) {
                        BindPermissions(app.permissions)
                        HomeScreen(app)
                    }
                }
            }
        }
    }
}

/**
 * Hands the [PermissionBridge] a launcher for as long as this Activity is
 * composed, so data-layer services can suspend on a system permission dialog.
 * Unbinding on dispose keeps a destroyed Activity's launcher from being reused.
 */
@Composable
private fun BindPermissions(permissions: PermissionBridge) {
    // Only one request is ever in flight — PermissionBridge serialises callers.
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        // Location asks for coarse+fine together; either one granted is a win.
        val granted = results.values.any { it }
        pending[0]?.invoke(granted)
        pending[0] = null
    }

    DisposableEffect(permissions, launcher) {
        permissions.bind { requested, onResult ->
            pending[0] = onResult
            launcher.launch(requested)
        }
        onDispose {
            permissions.unbind()
            // Release any caller still suspended on a dialog we can no longer show.
            pending[0]?.invoke(false)
            pending[0] = null
        }
    }
}
