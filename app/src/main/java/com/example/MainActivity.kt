package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.TestReportRepository
import com.example.ui.PhoneTestApp
import com.example.ui.PhoneTestViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: PhoneTestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = TestReportRepository(database.testReportDao())

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PhoneTestViewModel(application, repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[PhoneTestViewModel::class.java]

        setContent {
            MyApplicationTheme {
                PhoneTestApp(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (::viewModel.isInitialized && viewModel.onPhysicalButtonPressed(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
