package ru.noxly.baumforms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import ru.alfaintegral.mtrack.ui.theme.BaumFormTheme
import ru.noxly.baumforms.di.ServerEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serverManager = EntryPointAccessors.fromApplication(
            applicationContext,
            ServerEntryPoint::class.java
        ).serverManager()

        setContent {
            BaumFormTheme {
                MainScreen()
            }
        }
    }
}