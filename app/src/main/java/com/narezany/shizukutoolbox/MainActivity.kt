package com.narezany.shizukutoolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.narezany.shizukutoolbox.core.ShizukuManager
import com.narezany.shizukutoolbox.ui.ToolboxApp
import com.narezany.shizukutoolbox.ui.ToolboxTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShizukuManager.init(this)
        setContent {
            ToolboxTheme {
                ToolboxApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.refresh()
    }
}
