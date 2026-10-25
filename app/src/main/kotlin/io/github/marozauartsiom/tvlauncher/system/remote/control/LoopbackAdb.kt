package io.github.marozauartsiom.tvlauncher.system.remote.control

import android.util.Log
import java.io.File

/** Runs one shell command on the TV and returns whether it exited with 0. Fakes replace it in tests. */
fun interface ShellRunner {
    fun run(command: String): Boolean
}

/**
 * The only commands the remote sends to the TV's shell. Built from fixed templates: a key code is a number,
 * and text is limited to a safe Latin set, so nothing a client sends can become a shell command.
 */
object AdbCommands {
    fun key(keyCode: Int): String {
        require(keyCode in 1..400) { "bad key code" }
        return "input keyevent $keyCode"
    }

    /**
     * `input text` for apps whose fields the accessibility service can't see (YouTube's search). It types
     * key events, so only Latin letters, digits and simple punctuation; returns null for anything else.
     */
    fun text(value: String): String? {
        if (value.isEmpty() || value.length > 200) return null
        if (!value.all { it in SAFE }) return null
        // `input text` reads %s as a space; every other character here needs no quoting inside single quotes.
        return "input text '" + value.replace(" ", "%s") + "'"
    }

    private val SAFE: Set<Char> = (('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(' ', '.', ',', '-', '_', '!', '?', ':', '@', '+', '=', '/')).toSet()
}

/**
 * A client for the TV's own adbd at 127.0.0.1:5555 (the "network debugging" switch). It is how the remote
 * sends real key events, which reach every app, YouTube included. The key pair lives in the app's private
 * storage; the connection is opened on the first command and closed by [close] (when access turns off).
 * When adb is off the remote still works in the reduced mode (accessibility actions).
 */
class LoopbackAdb(private val keyDir: File, private val port: Int = 5555) : ShellRunner {
    @Volatile private var connection: dadb.Dadb? = null
    @Volatile var lastError: String? = null
        private set

    val isConnected: Boolean get() = connection != null

    @Synchronized
    override fun run(command: String): Boolean = try {
        val d = connection ?: open().also { connection = it }
        val ok = d.shell(command).exitCode == 0
        lastError = null
        ok
    } catch (e: Exception) {
        Log.i(TAG, "adb unavailable: $e")
        lastError = e.toString()
        close()
        false
    }

    /** Tries to connect; true when keys can be sent this way. */
    fun probe(): Boolean = run("true")

    @Synchronized
    fun close() {
        runCatching { connection?.close() }
        connection = null
    }

    private fun open(): dadb.Dadb {
        keyDir.mkdirs()
        val priv = File(keyDir, "adbkey")
        val pub = File(keyDir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) dadb.AdbKeyPair.generate(priv, pub)
        // The first connection waits for the owner to allow it on the TV ("Allow debugging?"), hence the long read timeout.
        return dadb.Dadb.create("127.0.0.1", port, dadb.AdbKeyPair.read(priv, pub), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
    }

    private companion object {
        const val TAG = "LoopbackAdb"
        const val CONNECT_TIMEOUT_MS = 3_000
        const val READ_TIMEOUT_MS = 60_000
    }
}
