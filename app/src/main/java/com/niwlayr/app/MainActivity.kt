package com.niwlayr.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.niwlayr.app.data.AppLanguage
import com.niwlayr.app.share.SharedImageImporter
import com.niwlayr.app.ui.PhotoGridPlannerApp
import com.niwlayr.app.ui.theme.PhotoGridPlannerTheme
import com.niwlayr.app.viewmodel.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val plannerViewModel: PlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        setContent {
            PhotoGridPlannerTheme {
                PhotoGridPlannerApp(viewModel = plannerViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        plannerViewModel.removeDeletedMedia()
    }

    /** Imports images sent to the app via the Android share sheet into the grid. */
    private fun handleShareIntent(intent: Intent?) {
        intent ?: return
        if (intent.type?.startsWith("image/") != true) return
        val shared = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.parcelableStream())
            Intent.ACTION_SEND_MULTIPLE -> intent.parcelableStreamList()
            else -> emptyList()
        }
        if (shared.isEmpty()) return
        // Consume the payload so a configuration change does not re-import it.
        intent.action = null

        lifecycleScope.launch {
            val local = withContext(Dispatchers.IO) {
                SharedImageImporter.importToLocal(this@MainActivity, shared)
            }
            if (local.isEmpty()) return@launch
            plannerViewModel.addImages(local)
            val english = plannerViewModel.state.value.language == AppLanguage.English
            val message = if (english) {
                "Added ${local.size} image(s) to the grid"
            } else {
                "Aggiunte ${local.size} immagini alla griglia"
            }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableStream(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableStreamList(): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }.orEmpty()
}
