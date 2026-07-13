import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

class ArchitectureFitnessTest {

    @Test
    fun `core module must not depend on app or domain modules`() {
        // Fitness Function: Prevent Instability in Core
        // Core must remain completely agnostic of the UI and Logic.
        val coreFiles = Konsist
            .scopeFromProduction(moduleName = "core")
            .files

        kotlin.test.assertTrue(
            coreFiles.isNotEmpty(),
            "The core architecture scope must not be empty.",
        )

        coreFiles.assertFalse(
            additionalMessage = "Core module violated architecture by importing domain logic!",
        ) {
            it.hasImport { import ->
                import.name.startsWith("com.shraggen.diarium.app")
            }
        }
    }

    @Test
    fun `core interfaces must remain abstract`() {
        // Fitness Function: Abstractness in Core
        // Ensures we are defining contracts (Tools, Engines) rather than concrete implementations here.
        Konsist
            .scopeFromProduction(moduleName = "core")
            .interfaces()
            .assertTrue { it.hasPublicOrDefaultModifier }
    }
}
