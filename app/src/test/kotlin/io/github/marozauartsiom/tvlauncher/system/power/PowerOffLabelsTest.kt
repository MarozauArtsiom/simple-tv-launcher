package io.github.marozauartsiom.tvlauncher.system.power

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerOffLabelsTest {
    @Test fun matchesThePowerOffItemInAnyCase() {
        assertTrue(PowerOffLabels.matches("Выключить"))
        assertTrue(PowerOffLabels.matches(" power off "))
    }

    @Test fun neverMatchesRestartOrLongerTexts() {
        assertFalse(PowerOffLabels.matches("Перезагрузить"))
        assertFalse(PowerOffLabels.matches("Restart"))
        assertFalse(PowerOffLabels.matches("Выключить экран"))
        assertFalse(PowerOffLabels.matches(null))
    }
}
