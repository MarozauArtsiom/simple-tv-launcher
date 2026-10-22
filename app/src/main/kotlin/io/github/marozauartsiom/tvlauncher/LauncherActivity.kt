package io.github.marozauartsiom.tvlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.github.marozauartsiom.tvlauncher.system.CrashGuard
import io.github.marozauartsiom.tvlauncher.ui.LauncherRoot
import io.github.marozauartsiom.tvlauncher.ui.LauncherViewModel

class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private val graph: AppGraph get() = (application as LauncherApp).graph
    private val handler = Handler(Looper.getMainLooper())

    // Language must be applied before any resource is read (below API 33 this wraps the context).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(io.github.marozauartsiom.tvlauncher.i18n.LocaleController(newBase).wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeMode = graph.crashGuard.safeMode()
        graph.apps.start()
        setContent { LauncherRoot(graph, viewModel, safeMode) }
    }

    override fun onResume() {
        super.onResume()
        // If the screen turned on while Home was not in front, reset now.
        if (graph.screenOn.attach { viewModel.onHomePressed() }) viewModel.onHomePressed()
        handler.postDelayed({ graph.crashGuard.markStable() }, CrashGuard.STABLE_AFTER_MS)
    }

    override fun onDestroy() {
        graph.apps.stop()
        super.onDestroy()
    }

    override fun onPause() {
        graph.screenOn.detach()
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    // The Home key while we are already in front arrives here. Nothing from the intent is trusted or forwarded.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.onHomePressed()
    }
}
