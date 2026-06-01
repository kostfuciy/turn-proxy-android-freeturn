package com.freeturn.app

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class ProxyTileService : TileService() {

    private var scope: CoroutineScope? = null
    private var isProxyRunning = false

    override fun onStartListening() {
        super.onStartListening()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        ProxyServiceState.isRunning.onEach { running ->
            isProxyRunning = running
            updateTileState()
        }.launchIn(scope!!)
    }

    override fun onStopListening() {
        super.onStopListening()
        scope?.cancel()
        scope = null
    }

    override fun onClick() {
        super.onClick()
        if (isProxyRunning) {
            val intent = Intent(this, ProxyReceiver::class.java).apply {
                action = "com.freeturn.app.STOP_PROXY"
            }
            sendBroadcast(intent)
        } else {
            val intent = Intent(this, ProxyReceiver::class.java).apply {
                action = "com.freeturn.app.START_PROXY"
            }
            sendBroadcast(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        if (isProxyRunning) {
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.state = Tile.STATE_INACTIVE
        }
        tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_qs_tile_nearby)
        tile.updateTile()
    }
}
