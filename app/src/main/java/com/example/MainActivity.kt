package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserPreferencesRepository
import com.example.data.local.EmiPulseDatabase
import com.example.data.local.SavedSessionRepository
import com.example.ui.EmiPulseApp
import com.example.ui.EmiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val database = EmiPulseDatabase.getInstance(applicationContext)
        val savedSessionRepository = SavedSessionRepository(database.savedEmiSessionDao())
        setContent {
            val viewModel: EmiViewModel = viewModel(
                factory = EmiViewModel.factory(prefsRepository, savedSessionRepository)
            )
            EmiPulseApp(viewModel = viewModel)
        }
    }
}
