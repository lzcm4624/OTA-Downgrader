package com.chumor.downgrader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chumor.downgrader.presentation.DowngraderViewModel
import com.chumor.downgrader.presentation.DowngraderViewModelFactory
import com.chumor.downgrader.presentation.ui.DowngraderRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val factory = remember { DowngraderViewModelFactory(applicationContext) }
            val viewModel: DowngraderViewModel = viewModel(factory = factory)

            DowngraderRoute(viewModel = viewModel)
        }
    }
}
