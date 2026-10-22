package io.github.marozauartsiom.tvlauncher.system.hdmi

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.marozauartsiom.tvlauncher.data.prefs.HdmiSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A TV whose HDMI inputs and states the test controls. */
class FakeInputs(vararg inputs: HdmiInput) : TvInputs {
    var list = inputs.toList()
    val states = HashMap<String, Int?>()
    var callback: ((String, Int?) -> Unit)? = null
    var registered = 0

    override fun hdmiInputs() = list
    override fun state(id: String) = states[id]
    override fun register(onChange: (String, Int?) -> Unit): () -> Unit { registered++; callback = onChange; return { callback = null } }

    /** The system reports a new state. */
    fun report(id: String, state: Int?) { states[id] = state; callback?.invoke(id, state) }
    /** The state changes without a callback reaching us (the process was away). */
    fun changeQuietly(id: String, state: Int?) { states[id] = state }
}

private val HDMI1 = HdmiInput("hdmi1", "HDMI 1", null)
private val HDMI2 = HdmiInput("hdmi2", "HDMI 2", null)

class HdmiWatcherTest {
    private val tv = FakeInputs(HDMI1, HDMI2).apply { states["hdmi1"] = TvInputs.DISCONNECTED; states["hdmi2"] = TvInputs.DISCONNECTED }
    private var now = 1_000_000L
    private val signals = mutableListOf<String>()
    private var allowed: (HdmiInput) -> Boolean = { true }
    private val watcher = HdmiWatcher(tv, { now }, allow = { allowed(it) }) { signals += it.id }.also { it.start() }

    @Test fun aSignalAppearingAsksOnce() {
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2"), signals)
        tv.report("hdmi2", TvInputs.CONNECTED) // the same state again is not news
        assertEquals(listOf("hdmi2"), signals)
    }

    @Test fun whatIsAlreadyConnectedWhenWeStartIsNotNews() {
        val busy = FakeInputs(HDMI1).apply { states["hdmi1"] = TvInputs.CONNECTED }
        val seen = mutableListOf<String>()
        HdmiWatcher(busy, { now }) { seen += it.id }.start()
        assertTrue(seen.isEmpty())
    }

    @Test fun aDeviceWakingFromStandbyCountsAsASignal() {
        tv.report("hdmi1", TvInputs.STANDBY)
        assertTrue(signals.isEmpty())
        tv.report("hdmi1", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi1"), signals)
    }

    @Test fun aFlickerWithinThirtySecondsDoesNotAskAgain() {
        tv.report("hdmi2", TvInputs.CONNECTED)
        tv.report("hdmi2", TvInputs.DISCONNECTED)
        now += 29_000
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2"), signals)
        tv.report("hdmi2", TvInputs.DISCONNECTED)
        now += 2_000 // 31 s after the first ask
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2", "hdmi2"), signals)
    }

    @Test fun afterNotNowTheInputIsLeftAloneUntilItDisconnects() {
        tv.report("hdmi2", TvInputs.CONNECTED)
        watcher.dismiss("hdmi2")
        tv.report("hdmi2", TvInputs.STANDBY) // standby is not a disconnect
        now += 60_000
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2"), signals)
        tv.report("hdmi2", TvInputs.DISCONNECTED) // now it is
        now += 60_000
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2", "hdmi2"), signals)
    }

    @Test fun inputsAreIndependent() {
        tv.report("hdmi1", TvInputs.CONNECTED)
        watcher.dismiss("hdmi1")
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi1", "hdmi2"), signals)
    }

    @Test fun theOwnersSwitchesDecide() {
        allowed = { it.id != "hdmi1" } // HDMI 1 is muted
        tv.report("hdmi1", TvInputs.CONNECTED)
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2"), signals)
    }

    @Test fun aMutedAskIsNotUsedUpByTheDebounce() {
        allowed = { false }
        tv.report("hdmi2", TvInputs.CONNECTED)
        tv.report("hdmi2", TvInputs.DISCONNECTED)
        allowed = { true }
        tv.report("hdmi2", TvInputs.CONNECTED) // right after: the earlier one never counted
        assertEquals(listOf("hdmi2"), signals)
    }

    @Test fun refreshCatchesWhatHappenedWhileWeWereAway() {
        tv.changeQuietly("hdmi2", TvInputs.CONNECTED)
        watcher.refresh()
        assertEquals(listOf("hdmi2"), signals)
        watcher.refresh() // nothing new
        assertEquals(listOf("hdmi2"), signals)
    }

    @Test fun refreshAlsoSeesDisconnectsSoTheNextConnectAsksAgain() {
        tv.report("hdmi2", TvInputs.CONNECTED)
        watcher.dismiss("hdmi2")
        tv.changeQuietly("hdmi2", TvInputs.DISCONNECTED)
        watcher.refresh()
        now += 60_000
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(2, signals.size)
    }

    @Test fun chipsFollowTheStates() {
        assertEquals(listOf(false, false), watcher.status.value.map { it.connected })
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi1" to false, "hdmi2" to true), watcher.status.value.map { it.input.id to it.connected })
        tv.report("hdmi2", TvInputs.STANDBY)
        assertFalse(watcher.status.value.last().connected)
    }

    @Test fun aHiddenOrUnknownInputNeverAsks() {
        tv.list = listOf(HDMI1) // HDMI 2 was hidden in the TV's settings
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertTrue(signals.isEmpty())
    }

    @Test fun startIsIdempotentAndStopUnregisters() {
        watcher.start()
        assertEquals(1, tv.registered)
        watcher.stop()
        assertNull(tv.callback)
    }
}

class HdmiControllerTest {
    private val tv = FakeInputs(HDMI1, HDMI2).apply { states["hdmi1"] = TvInputs.DISCONNECTED; states["hdmi2"] = TvInputs.DISCONNECTED }
    private var settings = HdmiSettings()
    private var canDraw = false
    private val overApps = mutableListOf<String>()
    private var now = 1_000_000L
    private val controller = HdmiController(tv, { settings }, { now }, { canDraw }, { overApps += it.id }).also { it.start() }

    @Test fun inFrontTheSignalWaitsForHomeToShowIt() {
        controller.onHomeVisible()
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals("hdmi2", controller.pending.value?.id)
        assertTrue(overApps.isEmpty())
    }

    @Test fun overAnotherAppItUsesTheWindowWhenAllowedAndGranted() {
        settings = HdmiSettings(overOtherApps = true)
        canDraw = true
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals(listOf("hdmi2"), overApps)
        assertNull(controller.pending.value)
    }

    @Test fun withoutThePermissionTheSignalWaitsForTheOwnerToComeHome() {
        settings = HdmiSettings(overOtherApps = true)
        canDraw = false
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertTrue(overApps.isEmpty())
        assertEquals("hdmi2", controller.pending.value?.id)
    }

    @Test fun ifTheOwnerSwitchedOverAppsOffItWaitsToo() {
        settings = HdmiSettings(overOtherApps = false)
        canDraw = true
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertTrue(overApps.isEmpty())
        assertEquals("hdmi2", controller.pending.value?.id)
    }

    @Test fun aWaitingSignalIsDroppedIfItIsGoneWhenHomeComesBack() {
        tv.report("hdmi2", TvInputs.CONNECTED) // launcher not in front
        tv.states["hdmi2"] = TvInputs.DISCONNECTED
        controller.onHomeVisible()
        assertNull(controller.pending.value)
    }

    @Test fun aWaitingSignalSurvivesIfItIsStillThere() {
        tv.report("hdmi2", TvInputs.CONNECTED)
        controller.onHomeVisible()
        assertEquals("hdmi2", controller.pending.value?.id)
    }

    @Test fun comingHomeFindsASignalThatArrivedWhileTheProcessWasAway() {
        tv.changeQuietly("hdmi1", TvInputs.CONNECTED)
        controller.onHomeVisible()
        assertEquals("hdmi1", controller.pending.value?.id)
    }

    @Test fun consumingClearsItSoItIsAskedOnce() {
        controller.onHomeVisible()
        tv.report("hdmi2", TvInputs.CONNECTED)
        controller.consume(controller.pending.value!!)
        assertNull(controller.pending.value)
    }

    @Test fun promptOffMeansNothingHappensAnywhere() {
        settings = HdmiSettings(promptEnabled = false, overOtherApps = true)
        canDraw = true
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertTrue(overApps.isEmpty())
        assertNull(controller.pending.value)
    }

    @Test fun aMutedInputIsSilent() {
        settings = HdmiSettings(mutedInputs = setOf("hdmi2"))
        controller.onHomeVisible()
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertNull(controller.pending.value)
        tv.report("hdmi1", TvInputs.CONNECTED)
        assertEquals("hdmi1", controller.pending.value?.id)
    }

    @Test fun notNowIsRememberedUntilDisconnect() {
        controller.onHomeVisible()
        tv.report("hdmi2", TvInputs.CONNECTED)
        controller.consume(controller.pending.value!!)
        controller.dismiss("hdmi2")
        tv.report("hdmi2", TvInputs.DISCONNECTED)
        now += 60_000
        tv.report("hdmi2", TvInputs.CONNECTED)
        assertEquals("hdmi2", controller.pending.value?.id) // disconnect cleared the "not now"
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class InputLauncherTest {
    private val started = mutableListOf<Intent>()
    private val picker = Intent("vendor.INPUT_PICKER")

    private fun launcher(vararg handled: String, fail: Boolean = false) = InputLauncher(
        canHandle = { (it.action == Intent.ACTION_VIEW && "view" in handled) || it.action in handled },
        start = { if (fail) throw ActivityNotFoundException() else started += it },
        picker = listOf(picker),
    )

    @Test fun theStandardAddressIsOpenedWithView() {
        assertTrue(launcher("view").open("com.vendor/.HdmiInputService/HW5"))
        val intent = started.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("content://android.media.tv/passthrough/com.vendor%2F.HdmiInputService%2FHW5", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun withoutAHandlerTheVendorPickerIsTried() {
        assertTrue(launcher("vendor.INPUT_PICKER").open("x"))
        assertEquals("vendor.INPUT_PICKER", started.single().action)
    }

    @Test fun nothingToOpenReportsFalse() {
        assertFalse(launcher().open("x"))
        assertTrue(started.isEmpty())
    }

    @Test fun aRefusedStartReportsFalseNotACrash() {
        assertFalse(launcher("view", fail = true).open("x"))
    }

    @Test fun aResolverThatThrowsIsAMiss() {
        assertFalse(InputLauncher({ error("binder") }, {}).open("x"))
    }
}
