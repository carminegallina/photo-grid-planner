package com.niwlayr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.niwlayr.app.ui.PhotoGridPlannerApp
import com.niwlayr.app.ui.theme.PhotoGridPlannerTheme
import com.niwlayr.app.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {
    private val plannerViewModel: PlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhotoGridPlannerTheme {
                PhotoGridPlannerApp(viewModel = plannerViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        plannerViewModel.removeDeletedMedia()
    }
}
