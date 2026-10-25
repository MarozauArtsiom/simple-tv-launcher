package io.github.marozauartsiom.tvlauncher.system.remote

import java.security.MessageDigest
import java.security.SecureRandom

enum class ClientKind { Phone, Assistant }

/** A paired phone or AI assistant. [id] is public (shown on the TV); the key never leaves [AccessGate]. */
data class RemoteClient(val id: String, val kind: ClientKind, val name: String, val pairedAt: Long)

sealed interface PairResult {
    data class Paired(val key: String, val client: RemoteClient) : PairResult
    data class WrongCode(val attemptsLeft: Int) : PairResult
    data class Locked(val secondsLeft: Int) : PairResult
    /** Pairing for this kind is switched off on the TV. */
    data object Closed : PairResult
}

/**
 * Who may control the TV. Everything lives in memory only: a process restart forgets every key, and [wipe]
 * (called when the TV's screen turns off) forgets them on purpose. Pure logic, so it is unit-tested.
 *
 * - A 6-digit [code] is shown on the TV. Pairing exchanges it for a random 128-bit key per client.
 * - 5 wrong codes lock pairing for 60 s.
 * - One phone at a time: a new phone replaces the old one. Several assistants may be paired.
 * - Keys are stored as SHA-256 hashes and compared in constant time.
 */
class AccessGate(private val clock: () -> Long, private val random: SecureRandom = SecureRandom()) {
    private class Entry(val keyHash: ByteArray, val client: RemoteClient)

    private val entries = mutableListOf<Entry>()
    private var wrong = 0
    private var lockedUntil = 0L
    private var lastActivity = clock()

    /** Which kinds may pair right now (the two switches on the TV). */
    @Volatile var open: Set<ClientKind> = emptySet()
        private set

    @Volatile var code: String = newCode()
        private set

    /** Called whenever the client list changes, so the TV's page and badge can update. */
    var onChange: () -> Unit = {}

    @Synchronized
    fun setOpen(kinds: Set<ClientKind>) {
        open = kinds
        val before = entries.size
        entries.removeAll { it.client.kind !in kinds } // switching a kind off disconnects it
        touch()
        if (entries.size != before) onChange()
    }

    @Synchronized
    fun pair(code: String, kind: ClientKind, name: String): PairResult {
        val now = clock()
        if (kind !in open) return PairResult.Closed
        if (now < lockedUntil) return PairResult.Locked(((lockedUntil - now + 999) / 1000).toInt())
        if (!MessageDigest.isEqual(code.trim().toByteArray(), this.code.toByteArray())) {
            wrong++
            if (wrong >= MAX_WRONG) { wrong = 0; lockedUntil = now + LOCK_MS; return PairResult.Locked((LOCK_MS / 1000).toInt()) }
            return PairResult.WrongCode(MAX_WRONG - wrong)
        }
        wrong = 0
        if (kind == ClientKind.Phone) entries.removeAll { it.client.kind == ClientKind.Phone }
        val key = token(16)
        val client = RemoteClient(id = token(6), kind = kind, name = name.take(40).ifBlank { kind.name }, pairedAt = now)
        entries += Entry(sha256(key), client)
        lastActivity = now
        onChange()
        return PairResult.Paired(key, client)
    }

    /** The client this key belongs to (of the given kind), or null. Does not count as activity. */
    @Synchronized
    fun authorize(key: String?, kind: ClientKind): RemoteClient? {
        if (key.isNullOrEmpty() || kind !in open) return null
        val hash = sha256(key)
        return entries.firstOrNull { MessageDigest.isEqual(it.keyHash, hash) && it.client.kind == kind }?.client
    }

    /** A user action (a key, a tap, a tool call): resets the idle timer. Frames and state polls are not actions. */
    @Synchronized
    fun touch() { lastActivity = clock() }

    @Synchronized
    fun idleMillis(): Long = clock() - lastActivity

    @Synchronized
    fun clients(): List<RemoteClient> = entries.map { it.client }

    @Synchronized
    fun disconnect(id: String) {
        if (entries.removeAll { it.client.id == id }) onChange()
    }

    /** Forgets every key and makes a new code. The TV turned off, the switches went off, or the process is going away. */
    @Synchronized
    fun wipe() {
        val had = entries.isNotEmpty()
        entries.clear()
        open = emptySet()
        wrong = 0
        lockedUntil = 0
        code = newCode()
        if (had) onChange()
    }

    /** A fresh code, e.g. each time a switch is turned on. */
    @Synchronized
    fun newPairingCode() { code = newCode() }

    private fun newCode(): String = (random.nextInt(900_000) + 100_000).toString()

    private fun token(bytes: Int): String {
        val b = ByteArray(bytes).also(random::nextBytes)
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun sha256(s: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())

    companion object {
        const val MAX_WRONG = 5
        const val LOCK_MS = 60_000L
        const val IDLE_OFF_MS = 15 * 60_000L
    }
}
