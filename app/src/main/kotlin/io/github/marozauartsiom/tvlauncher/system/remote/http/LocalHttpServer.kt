package io.github.marozauartsiom.tvlauncher.system.remote.http

import android.util.Log
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * A tiny HTTP/1.1 server for the home network: one connection = one request (`Connection: close`), a few
 * worker threads, and a socket timeout so a slow client can't hold a worker. Connections from addresses that
 * are not local ([isLocalAddress]) are closed before anything is read. Nothing runs before [start] or after [stop].
 */
class LocalHttpServer(
    private val bindAddress: InetAddress,
    private val port: Int,
    private val handler: (HttpRequest) -> HttpResponse,
) {
    @Volatile private var socket: ServerSocket? = null
    private var pool: ThreadPoolExecutor? = null

    val isRunning: Boolean get() = socket?.isClosed == false

    /** The port actually bound (differs from the requested one only when that was 0). */
    val boundPort: Int get() = socket?.localPort ?: port

    /** Starts listening; false if the port is taken or the address is gone. */
    @Synchronized
    fun start(): Boolean {
        if (isRunning) return true
        val s = try {
            ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(bindAddress, port), 16) }
        } catch (e: java.io.IOException) {
            Log.w(TAG, "can't listen on $bindAddress:$port: $e")
            return false
        }
        val p = ThreadPoolExecutor(0, WORKERS, 30, TimeUnit.SECONDS, ArrayBlockingQueue(QUEUE)) { r -> Thread(r, "remote-http").apply { isDaemon = true } }
        socket = s
        pool = p
        thread(name = "remote-http-accept", isDaemon = true) {
            while (!s.isClosed) {
                val client = try { s.accept() } catch (e: java.io.IOException) { break }
                try {
                    p.execute { serve(client) }
                } catch (e: RejectedExecutionException) {
                    runCatching { client.close() }
                }
            }
        }
        return true
    }

    @Synchronized
    fun stop() {
        runCatching { socket?.close() }
        socket = null
        pool?.shutdownNow()
        pool = null
    }

    private fun serve(client: Socket) {
        client.use { c ->
            if (!isLocalAddress(c.inetAddress)) return
            c.soTimeout = READ_TIMEOUT_MS
            val response = try {
                val request = HttpParser.parse(c.getInputStream()) ?: return
                handler(request)
            } catch (e: HttpError) {
                HttpResponse.text(e.status, e.message.orEmpty())
            } catch (e: java.io.IOException) {
                return
            } catch (e: RuntimeException) {
                Log.w(TAG, "handler failed", e)
                HttpResponse.text(500, "internal error")
            }
            runCatching { c.getOutputStream().apply { write(HttpParser.write(response)); flush() } }
        }
    }

    private companion object {
        const val TAG = "RemoteHttp"
        const val WORKERS = 4
        const val QUEUE = 16
        const val READ_TIMEOUT_MS = 10_000
    }
}
