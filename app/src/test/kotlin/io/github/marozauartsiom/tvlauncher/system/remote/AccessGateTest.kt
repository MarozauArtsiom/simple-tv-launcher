package io.github.marozauartsiom.tvlauncher.system.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessGateTest {
    private var now = 1_000_000L
    private val gate = AccessGate({ now }).apply { setOpen(setOf(ClientKind.Phone, ClientKind.Assistant)) }

    private fun pairPhone() = gate.pair(gate.code, ClientKind.Phone, "Phone") as PairResult.Paired

    @Test fun codeIsSixDigits() {
        assertTrue(gate.code.matches(Regex("\\d{6}")))
    }

    @Test fun rightCodeGivesAKeyThatAuthorizesOnlyItsKind() {
        val p = pairPhone()
        assertEquals(32, p.key.length)
        assertEquals(p.client, gate.authorize(p.key, ClientKind.Phone))
        assertNull(gate.authorize(p.key, ClientKind.Assistant))
        assertNull(gate.authorize("nope", ClientKind.Phone))
        assertNull(gate.authorize(null, ClientKind.Phone))
    }

    @Test fun fiveWrongCodesLockPairingForAMinute() {
        repeat(4) { assertEquals(PairResult.WrongCode(4 - it), gate.pair("000000", ClientKind.Phone, "x")) }
        assertEquals(PairResult.Locked(60), gate.pair("000000", ClientKind.Phone, "x"))
        // Even the right code is refused while locked.
        now += 30_000
        assertEquals(PairResult.Locked(30), gate.pair(gate.code, ClientKind.Phone, "x"))
        now += 30_000
        assertTrue(gate.pair(gate.code, ClientKind.Phone, "x") is PairResult.Paired)
    }

    @Test fun aNewPhoneReplacesTheOldOne() {
        val first = pairPhone()
        val second = pairPhone()
        assertNull(gate.authorize(first.key, ClientKind.Phone))
        assertNotNull(gate.authorize(second.key, ClientKind.Phone))
        assertEquals(1, gate.clients().count { it.kind == ClientKind.Phone })
    }

    @Test fun severalAssistantsMayPair() {
        val a = gate.pair(gate.code, ClientKind.Assistant, "Claude") as PairResult.Paired
        val b = gate.pair(gate.code, ClientKind.Assistant, "Other") as PairResult.Paired
        assertNotNull(gate.authorize(a.key, ClientKind.Assistant))
        assertNotNull(gate.authorize(b.key, ClientKind.Assistant))
    }

    @Test fun closedKindCannotPairAndSwitchingOffDisconnects() {
        val p = pairPhone()
        var changes = 0
        gate.onChange = { changes++ }
        gate.setOpen(setOf(ClientKind.Assistant))
        assertNull(gate.authorize(p.key, ClientKind.Phone))
        assertEquals(PairResult.Closed, gate.pair(gate.code, ClientKind.Phone, "x"))
        assertEquals(1, changes)
    }

    @Test fun wipeForgetsEveryKeyAndChangesTheCode() {
        val p = pairPhone()
        val a = gate.pair(gate.code, ClientKind.Assistant, "Claude") as PairResult.Paired
        val oldCode = gate.code
        gate.wipe()
        assertTrue(gate.clients().isEmpty())
        assertTrue(gate.open.isEmpty())
        // Turning the switches back on doesn't bring the old keys back.
        gate.setOpen(setOf(ClientKind.Phone, ClientKind.Assistant))
        assertNull(gate.authorize(p.key, ClientKind.Phone))
        assertNull(gate.authorize(a.key, ClientKind.Assistant))
        assertNotEquals(oldCode, gate.code) // 1 in 900 000 to collide
    }

    @Test fun idleTimeCountsFromTheLastUserAction() {
        pairPhone()
        now += 10 * 60_000
        assertEquals(10 * 60_000L, gate.idleMillis())
        gate.touch()
        assertEquals(0L, gate.idleMillis())
    }

    @Test fun disconnectRemovesOneClient() {
        val p = pairPhone()
        gate.disconnect(p.client.id)
        assertNull(gate.authorize(p.key, ClientKind.Phone))
    }

    @Test fun nameIsTrimmedToFortyAndBlankGetsTheKind() {
        val long = gate.pair(gate.code, ClientKind.Assistant, "x".repeat(100)) as PairResult.Paired
        assertEquals(40, long.client.name.length)
        val blank = gate.pair(gate.code, ClientKind.Assistant, " ") as PairResult.Paired
        assertEquals("Assistant", blank.client.name)
    }
}
