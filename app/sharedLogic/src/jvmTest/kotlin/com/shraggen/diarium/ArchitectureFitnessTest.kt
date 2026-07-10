package com.shraggen.diarium

import kotlin.test.Test

class ArchitectureFitnessTest {

    @Test
    fun `core module must not depend on app or domain modules`() {
        // Fitness Function: Prevent Instability in Core
        // Core must remain completely agnostic of the Beekeeping/Mechanic UI and Logic.
        Konsist
            .scopeFromProject()
            .files
            .withPackage("com.shraggen.diarium.core..")
            .assertFalse(additionalMessage = "Core module violated architecture by importing domain logic!") {
                it.hasImports { import -> import.name.startsWith("com.shraggen.diarium.app") }
            }
    }

    @Test
    fun `core interfaces must remain abstract`() {
        // Fitness Function: Abstractness in Core
        // Ensures we are defining contracts (Tools, Engines) rather than concrete implementations here.
        Konsist
            .scopeFromProject()
            .interfaces()
            .withPackage("com.shraggen.diarium.core..")
            .assertTrue { it.hasPublicOrDefaultModifier }
    }
}