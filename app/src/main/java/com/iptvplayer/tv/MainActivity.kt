package com.iptvplayer.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.iptvplayer.tv.core.player.PlayerViewModel
import com.iptvplayer.tv.feature.home.HomeScreen
import com.iptvplayer.tv.feature.home.PlaylistViewModel
import com.iptvplayer.tv.feature.player.IPTVPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            IPTVPlayerTheme {
                HomeScreen(viewModel = playlistViewModel, playerViewModel = playerViewModel)
            }
        }
    }
}
