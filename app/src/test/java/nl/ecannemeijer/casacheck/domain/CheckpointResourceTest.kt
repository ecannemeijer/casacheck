package nl.ecannemeijer.casacheck.domain

import com.google.common.truth.Truth.assertThat
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import org.junit.Test
import java.io.File

/** Regression for the 1.0.0 crash: Resources$NotFoundException "String resource ID #0x0" in the inspection wizard. */
class CheckpointResourceTest {
    @Test fun mandatoryTextsAreAlwaysSet() {
        CheckpointLibrary.all.forEach { d ->
            listOf(d.title, d.explanation, d.steps, d.lookFor, d.normal, d.abnormal, d.causes, d.consequences, d.repair)
                .forEach { assertThat(it).isNotEqualTo(0) }
        }
    }

    @Test fun optionalResourcesAreGuardedInUi() {
        // warning, expert and illustration are optional (0 = none) for many checkpoints; the UI must never resolve id 0.
        assertThat(CheckpointLibrary.all.any { it.warning == 0 || it.expert == 0 }).isTrue()
        val src = File("src/main/java/nl/ecannemeijer/casacheck/ui/inspection/CheckpointPage.kt").readText()
        for (f in listOf("warning", "expert", "illustration")) {
            val uses = Regex("""(stringResource|painterResource)\(def\.$f\)""").findAll(src).count()
            val guards = Regex("""def\.$f != 0""").findAll(src).count()
            assertThat(guards).isAtLeast(1)
            assertThat(uses).isAtLeast(1)
        }
    }
}