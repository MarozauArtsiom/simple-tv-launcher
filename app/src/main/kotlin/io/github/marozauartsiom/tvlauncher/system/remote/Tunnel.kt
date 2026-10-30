package io.github.marozauartsiom.tvlauncher.system.remote

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * A way to reach the TV's local server from the internet. The TV connects *out* to a relay, which hands back a
 * public https address; nothing on the home router is opened. One tunnel = one address, new every time.
 */
interface Tunnel {
    /**
     * Connects and forwards the relay's port 80/443 to 127.0.0.1:[localPort]. [onUrl] gets the public address once
     * (`https://…`, no trailing slash); [onDown] is called once when the tunnel ends or could not start.
     * Both may be called from another thread.
     */
    fun start(localPort: Int, onUrl: (String) -> Unit, onDown: () -> Unit)

    fun stop()

    companion object {
        /** Where tunnels deliver connections: the TV's IPv4 loopback (not ::1, which Android's loopback default is). */
        val LOCAL_HOST: java.net.InetAddress = java.net.InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    }
}

/**
 * The free anonymous tunnel of [localhost.run](https://localhost.run): an SSH reverse forward with the user name
 * `nokey` (any key is accepted, so a throw-away one is made each time) that answers with a random `*.lhr.life`
 * address. Traffic is TLS-terminated by the relay, so the relay can see it; the access code and the secret in the
 * path are what keep strangers out (see [ExternalApi]).
 */
class SshTunnel(
    private val host: String = "localhost.run",
    private val port: Int = 22,
    private val user: String = "nokey",
) : Tunnel {
    @Volatile private var session: Session? = null
    @Volatile private var stopped = false

    override fun start(localPort: Int, onUrl: (String) -> Unit, onDown: () -> Unit) {
        thread(name = "remote-tunnel", isDaemon = true) {
            try {
                val jsch = JSch()
                val pair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048)
                val priv = ByteArrayOutputStream().also { pair.writePrivateKey(it) }.toByteArray()
                val pub = ByteArrayOutputStream().also { pair.writePublicKey(it, "tv") }.toByteArray()
                pair.dispose()
                jsch.addIdentity("tv", priv, pub, null)
                val s = jsch.getSession(user, host, port)
                s.setConfig("StrictHostKeyChecking", "no") // anonymous relay; the traffic is TLS-terminated there anyway
                s.setServerAliveInterval(20_000)
                s.setServerAliveCountMax(3)
                s.connect(CONNECT_MS)
                session = s
                if (stopped) { s.disconnect(); return@thread }
                s.setPortForwardingR(80, Tunnel.LOCAL_HOST.hostAddress, localPort)
                // The relay announces the address on the session's output; keep reading until it closes.
                val channel = s.openChannel("shell")
                val out = channel.inputStream
                // No timeout here: with one, JSch waits for a reply to the shell request, and this relay never sends it.
                channel.connect()
                var announced = false
                out.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (!announced) TunnelBanner.parse(line)?.let { announced = true; onUrl(it) }
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "tunnel ended: $e")
            } finally {
                runCatching { session?.disconnect() }
                session = null
                if (!stopped) onDown()
            }
        }
    }

    override fun stop() {
        stopped = true
        runCatching { session?.disconnect() }
        session = null
    }

    private companion object {
        const val TAG = "RemoteTunnel"
        const val CONNECT_MS = 15_000
    }
}

/** Finds the public address in the relay's session output. */
object TunnelBanner {
    private val URL = Regex("""https://[a-z0-9][a-z0-9-]*\.(?:lhr\.life|localhost\.run)\b""")
    private val NOT_A_TUNNEL = setOf("https://admin.localhost.run", "https://localhost.run")

    fun parse(line: String): String? {
        // "86e0f64868e2d7.lhr.life tunneled with tls termination, https://86e0f64868e2d7.lhr.life"
        if (!line.contains("tunneled with")) return null
        return URL.findAll(line).map { it.value }.firstOrNull { it !in NOT_A_TUNNEL }
    }
}
