package io.github.marozauartsiom.tvlauncher.system.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import io.github.marozauartsiom.tvlauncher.system.remote.control.AndroidTvControl
import io.github.marozauartsiom.tvlauncher.system.remote.http.LocalHttpServer
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** What the TV's settings page shows. */
data class RemoteState(
    val phoneOn: Boolean = false,
    val aiOn: Boolean = false,
    /** `192.168.1.20:8765` while the server runs, null otherwise. */
    val address: String? = null,
    val code: String = "",
    val clients: List<RemoteClient> = emptyList(),
    val helperReady: Boolean = false,
    /** Real key events (adb) work; null until checked. */
    val fullKeys: Boolean? = null,
    /** A switch is on but the TV has no home-network address. */
    val noNetwork: Boolean = false,
    /** AI access from the internet (through the tunnel) is switched on. */
    val externalOn: Boolean = false,
    /** `https://abc.lhr.life/s/<secret>` once the tunnel is up; null while it connects. */
    val externalUrl: String? = null,
    /** The tunnel could not start or dropped. */
    val externalFailed: Boolean = false,
) {
    val anyOn: Boolean get() = phoneOn || aiOn
}

/**
 * Phone remote and AI access: the two switches, the server, and the rule that **turning the TV off ends
 * everything**. Nothing is saved: the switches, the code and every key live in memory, so a restart forgets
 * them, and the screen turning off, the helper going away or 15 minutes without a user action [shutDown]
 * on purpose. While both switches are off, nothing runs (no socket, no receiver, no timer).
 */
class RemoteAccess(
    private val context: Context,
    private val scope: CoroutineScope,
    private val gate: AccessGate,
    private val control: AndroidTvControl,
    private val api: (host: () -> String) -> RemoteApi,
    private val helperReady: () -> Boolean,
    private val badge: RemoteBadgeHost,
    private val port: Int = PORT,
    private val lanAddress: () -> InetAddress? = ::homeNetworkAddress,
    private val tunnel: () -> Tunnel = { SshTunnel() },
    private val random: SecureRandom = SecureRandom(),
) {
    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(RemoteState(helperReady = helperReady()))
    val state: StateFlow<RemoteState> = _state.asStateFlow()

    private var server: LocalHttpServer? = null
    private var routes: RemoteApi? = null
    private var externalServer: LocalHttpServer? = null
    private var externalTunnel: Tunnel? = null
    private var host: String = ""
    private var idleJob: Job? = null
    private var screenReceiverOn = false

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) shutDown()
        }
    }

    init {
        gate.onChange = { main.post { publish() } }
        LauncherInFront.onChange = { main.post { badge.refresh() } }
        control.onAdbResult = { full -> _state.update { it.copy(fullKeys = full) } }
    }

    fun setPhone(on: Boolean) = reconfigure(_state.value.copy(phoneOn = on))
    fun setAi(on: Boolean) = reconfigure(_state.value.copy(aiOn = on))

    /**
     * AI access from the internet: opens a tunnel to a relay, which gives a random https address. Only for
     * assistants (see [ExternalApi]); needs AI access on; a new address and a new secret every time; gone when
     * the switch, AI access or the TV goes off.
     */
    @Synchronized
    fun setExternal(on: Boolean) {
        if (!on) {
            stopExternal()
            _state.update { it.copy(externalOn = false, externalUrl = null, externalFailed = false) }
            return
        }
        val api = routes
        if (!_state.value.aiOn || server == null || api == null || externalTunnel != null) return
        val secret = ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        // IPv4 loopback on purpose: Android's getLoopbackAddress() is ::1, and the tunnel connects to 127.0.0.1.
        val local = LocalHttpServer(Tunnel.LOCAL_HOST, 0, ExternalApi(api) { secret }::handle)
        if (!local.start()) { _state.update { it.copy(externalOn = false, externalFailed = true) }; return }
        val t = tunnel()
        externalServer = local
        externalTunnel = t
        _state.update { it.copy(externalOn = true, externalUrl = null, externalFailed = false) }
        t.start(
            localPort = local.boundPort,
            onUrl = { url -> if (externalTunnel === t) _state.update { it.copy(externalUrl = "$url/s/$secret") } },
            onDown = { main.post { synchronized(this) { if (externalTunnel === t) { stopExternal(); _state.update { it.copy(externalOn = false, externalUrl = null, externalFailed = true) } } } } },
        )
    }

    private fun stopExternal() {
        externalTunnel?.stop()
        externalTunnel = null
        externalServer?.stop()
        externalServer = null
    }

    /** An assistant ran a tool: outside Home the badge shows for a moment so the owner sees it. Any thread. */
    fun onAction(kind: ClientKind) {
        if (kind == ClientKind.Assistant) main.post { badge.flash() }
    }

    fun disconnect(id: String) = gate.disconnect(id)

    fun disconnectAll() = gate.clients().forEach { gate.disconnect(it.id) }

    /** The TV turned off, the helper went away, or nobody used it for 15 minutes: everything off, every key gone. */
    @Synchronized
    fun shutDown() {
        gate.wipe()
        stopServer()
        _state.value = RemoteState(helperReady = helperReady())
        main.post { badge.hide() }
    }

    /** The settings page opened: refresh what can't be observed (the helper switch, the adb check). */
    fun refresh() {
        _state.update { it.copy(helperReady = helperReady()) }
        if (_state.value.anyOn && _state.value.fullKeys != true) { control.reset(); _state.update { it.copy(fullKeys = null) }; control.adbReady() }
    }

    @Synchronized
    private fun reconfigure(wanted: RemoteState) {
        val kinds = buildSet { if (wanted.phoneOn) add(ClientKind.Phone); if (wanted.aiOn) add(ClientKind.Assistant) }
        if (kinds.isEmpty()) { shutDown(); return }
        if (!wanted.aiOn && externalTunnel != null) stopExternal()
        val starting = server == null
        if (starting) {
            val address = lanAddress()
            if (address == null) { _state.value = wanted.copy(phoneOn = false, aiOn = false, noNetwork = true); return }
            control.reset()
            gate.newPairingCode()
            val routes = api { host }
            this.routes = routes
            val s = LocalHttpServer(address, port, routes::handle)
            if (!s.start()) { _state.value = wanted.copy(phoneOn = false, aiOn = false, noNetwork = true); return }
            host = "${address.hostAddress}:${s.boundPort}"
            server = s
            registerScreenOff()
            startIdleWatch()
            control.adbReady() // checks in the background; the page shows the result
        }
        gate.setOpen(kinds)
        _state.value = wanted.copy(
            address = host, code = gate.code, clients = gate.clients(), helperReady = helperReady(), noNetwork = false,
            externalOn = wanted.externalOn && wanted.aiOn, externalUrl = wanted.externalUrl.takeIf { wanted.aiOn },
        )
    }

    private fun publish() {
        val clients = gate.clients()
        _state.update { it.copy(clients = clients, code = gate.code) }
        if (clients.isEmpty()) badge.hide() else badge.show(phone = clients.any { it.kind == ClientKind.Phone }, ai = clients.any { it.kind == ClientKind.Assistant })
    }

    private fun stopServer() {
        stopExternal()
        server?.stop()
        server = null
        routes = null
        idleJob?.cancel()
        idleJob = null
        control.reset()
        if (screenReceiverOn) { runCatching { context.unregisterReceiver(screenOff) }; screenReceiverOn = false }
    }

    private fun registerScreenOff() {
        if (screenReceiverOn) return
        ContextCompat.registerReceiver(context, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiverOn = true
    }

    private fun startIdleWatch() {
        idleJob?.cancel()
        idleJob = scope.launch {
            while (isActive) {
                delay(IDLE_CHECK_MS)
                if (gate.idleMillis() >= AccessGate.IDLE_OFF_MS) shutDown()
            }
        }
    }

    companion object {
        const val PORT = 8765
        private const val IDLE_CHECK_MS = 30_000L
    }
}

/** The TV's address in the home network (Wi-Fi or Ethernet), IPv4. */
private fun homeNetworkAddress(): InetAddress? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
}.getOrNull()

/** The small "remote / AI is controlling" pill: steady on Home, a short flash over other apps. */
interface RemoteBadgeHost {
    /** Who is connected now (both false: nobody). The badge stays up while Home is in front. Main thread. */
    fun show(phone: Boolean, ai: Boolean)
    fun hide()

    /** Home came to or left the front: show or hide the badge to match. Main thread. */
    fun refresh()

    /** An assistant did something: outside Home, show the badge briefly with an animation. Main thread. */
    fun flash()
}
