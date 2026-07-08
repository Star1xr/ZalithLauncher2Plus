/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.crashlogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class TestCrashLogAnalyzer {

    @Test
    fun testDetectsKnownSodiumDependency() {
        val log = """
            java.lang.NoClassDefFoundError: net/caffeinemc/mods/sodium/client/gui/SodiumOptionsGUI
                at knot//net.minecraft.class_429.handler${'$'}fji000${'$'}planifolia${'$'}openVanillaMenu(class_429.java:1016)
            Caused by: java.lang.ClassNotFoundException: net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI
        """.trimIndent()

        val hint = CrashLogAnalyzer.analyze(log)

        assertEquals("net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI", hint?.missingClass)
        assertEquals("Sodium", hint?.dependencyName)
    }

    @Test
    fun testUnknownMissingClassHasNoDependencyName() {
        val log = "java.lang.NoClassDefFoundError: com/example/somemod/SomeClass"

        val hint = CrashLogAnalyzer.analyze(log)

        assertEquals("com.example.somemod.SomeClass", hint?.missingClass)
        assertNull(hint?.dependencyName)
    }

    @Test
    fun testNoHintWhenNoMatchingException() {
        val log = "java.lang.NullPointerException: Cannot invoke method on null object"

        val hint = CrashLogAnalyzer.analyze(log)

        assertNull(hint)
    }

    @Test
    fun testMatchesClassNameFollowedByTrailingNoise() {
        // Class names in real logs are often followed by punctuation, parens, or line breaks
        val log = "Caused by: java.lang.ClassNotFoundException: net.irisshaders.iris.gui.IrisConfigScreen\n\tat java.base/..."

        val hint = CrashLogAnalyzer.analyze(log)

        assertEquals("net.irisshaders.iris.gui.IrisConfigScreen", hint?.missingClass)
        assertEquals("Iris", hint?.dependencyName)
    }

    @Test
    fun testFileAnalysisRespectsTailByteCap() {
        val file = File.createTempFile("crash-log-tail-test", ".log")
        try {
            // Prefix filler far larger than the tail cap so the real exception is only
            // reachable if the tail read correctly seeks near the end of the file.
            val filler = "x".repeat(50 * 1024)
            val realException = "java.lang.NoClassDefFoundError: com/example/somemod/SomeClass\n"
            file.writeText(filler + realException)

            val hint = CrashLogAnalyzer.analyze(file, maxBytes = 4 * 1024L)

            assertEquals("com.example.somemod.SomeClass", hint?.missingClass)
        } finally {
            file.delete()
        }
    }
}
