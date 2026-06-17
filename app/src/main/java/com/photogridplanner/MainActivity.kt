package com.photogridplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photogridplanner.ui.PhotoGridPlannerApp
import com.photogridplanner.ui.theme.PhotoGridPlannerTheme
import com.photogridplanner.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhotoGridPlannerTheme {
                val viewModel: PlannerViewModel = viewModel()
                PhotoGridPlannerApp(viewModel = viewModel)
            }
        }
    }
}
