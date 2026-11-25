package nl.ecannemeijer.casacheck.domain

import com.google.common.truth.Truth.assertThat
import nl.ecannemeijer.casacheck.data.backup.BackupException
import nl.ecannemeijer.casacheck.data.backup.ZipSafety
import nl.ecannemeijer.casacheck.data.db.*
import nl.ecannemeijer.casacheck.domain.checklist.ChecklistGenerator
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.cost.CostCalculator
import nl.ecannemeijer.casacheck.domain.moisture.MoistureAnalyzer
import nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern
import nl.ecannemeijer.casacheck.domain.score.PrefOutcome
import nl.ecannemeijer.casacheck.domain.score.PreferenceEvaluator
import nl.ecannemeijer.casacheck.domain.score.PropertySnapshot
import nl.ecannemeijer.casacheck.domain.score.ScoreCalculator
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DomainTest {
    private fun item(id: Long, cat: String, st: CheckStatus?, spec: Boolean? = null) =
        ItemWithResult(id, 1, "t$id", cat, null, null, null, null, false, 0, st, spec, null, null, null, null, 0)

    private fun snap(p: Property = Property(), items: List<ItemWithResult> = emptyList(), defects: List<Defect> = emptyList(),
                     projects: List<RepairProject> = emptyList(), est: List<CostEstimate> = emptyList()) =
        PropertySnapshot(p, emptyMap(), items, defects, CostCalculator.summarize(p.askingPrice, p.ownOffer, 10.0, 10.0, defects, projects, est),
            emptyList(), emptyList(), emptyList(), emptyList())

    // ---- costs
    @Test fun projectDefectsAreNotDoubleCounted() {
        val proj = RepairProject(id = 5, propertyId = 1, name = "Dak", costCategory = "ROOF", costMin = 10_000, costMax = 20_000)
        val d1 = Defect(id = 1, propertyId = 1, title = "lek", costMin = 1_000, costMax = 2_000, repairProjectId = 5)
        val d2 = Defect(id = 2, propertyId = 1, title = "pannen", costMin = 3_000, costMax = 4_000, repairProjectId = 5)
        val d3 = Defect(id = 3, propertyId = 1, title = "los", costMin = 100, costMax = 200)
        val s = CostCalculator.summarize(100_000, null, 10.0, 10.0, listOf(d1, d2, d3), listOf(proj), emptyList())
        assertThat(s.renovation.min).isEqualTo(10_100)
        assertThat(s.renovation.max).isEqualTo(20_200)
        assertThat(s.lines).hasSize(2)
        assertThat(s.purchaseCosts).isEqualTo(10_000)
        assertThat(s.totalMax).isEqualTo(100_000 + 10_000 + 20_200 + 2_020)
    }

    @Test fun projectWithoutAmountUsesLargestDefectNotSum() {
        val proj = RepairProject(id = 5, propertyId = 1, name = "Dak", costCategory = "ROOF")
        val d1 = Defect(id = 1, propertyId = 1, title = "a", costMin = 1_000, costMax = 2_000, repairProjectId = 5)
        val d2 = Defect(id = 2, propertyId = 1, title = "b", costMin = 3_000, costMax = 4_000, repairProjectId = 5)
        val r = CostCalculator.projectRange(proj, emptyList(), listOf(d1, d2))!!
        assertThat(r.min).isEqualTo(3_000); assertThat(r.max).isEqualTo(4_000)
    }

    @Test fun resolvedDefectsAndQuotes() {
        val resolved = Defect(id = 1, propertyId = 1, title = "x", costMin = 500, costMax = 900, status = DefectStatus.RESOLVED)
        val noCost = Defect(id = 2, propertyId = 1, title = "y")
        val s = CostCalculator.summarize(null, null, 10.0, 10.0, listOf(resolved, noCost), emptyList(), emptyList())
        assertThat(s.renovation.max).isEqualTo(0)
        assertThat(s.defectsWithoutCost).isEqualTo(1)
        assertThat(s.totalMin).isNull()
        val est = CostEstimate(propertyId = 1, costCategory = "FLOORS", description = "vloer", type = CostType.PER_SQM, quantity = 20.0, unitPriceMin = 30, unitPriceMax = 50)
        assertThat(CostCalculator.estimateRange(est)!!.max).isEqualTo(1_000)
        assertThat(CostCalculator.estimateRange(est.copy(quoteAmount = 777, quoteReceived = true))!!.min).isEqualTo(777)
    }

    // ---- score
    @Test fun scoreExcludesNotApplicableAndCapsSpecialist() {
        assertThat(ScoreCalculator.itemPoints(CheckStatus.OK, true)).isEqualTo(0.5)
        val items = listOf(item(1, "ROOF", CheckStatus.OK), item(2, "ROOF", CheckStatus.NOT_APPLICABLE), item(3, "ROOF", null))
        val r = ScoreCalculator.calculate(snap(items = items), ScoreCalculator.DEFAULT_WEIGHTS, emptyMap())
        assertThat(r.groups.first { it.group.name == "ROOF" }.score).isEqualTo(50)
        assertThat(r.completion).isEqualTo(50)
    }

    @Test fun seriousStructuralDefectIsCritical() {
        val d = Defect(propertyId = 1, title = "scheur", severity = Severity.SERIOUS, isStructural = true)
        val r = ScoreCalculator.calculate(snap(defects = listOf(d)), ScoreCalculator.DEFAULT_WEIGHTS, emptyMap())
        assertThat(r.critical).hasSize(1)
    }

    // ---- preferences
    @Test fun preferencesUnknownWhenDataMissing() {
        val s = snap(Property(askingPrice = 150_000, garage = null))
        assertThat(PreferenceEvaluator.outcome(s, "max_purchase", 200_000.0)).isEqualTo(PrefOutcome.MET)
        assertThat(PreferenceEvaluator.outcome(s, "max_purchase", 100_000.0)).isEqualTo(PrefOutcome.NOT_MET)
        assertThat(PreferenceEvaluator.outcome(s, "garage", null)).isEqualTo(PrefOutcome.UNKNOWN)
        assertThat(PreferenceEvaluator.outcome(s, "min_living", 80.0)).isEqualTo(PrefOutcome.UNKNOWN)
        val prefs = listOf(PropertyPreference("max_purchase", PreferenceType.OFF, 1.0), PropertyPreference("garage", PreferenceType.HARD))
        assertThat(PreferenceEvaluator.evaluate(s, prefs).map { it.key }).containsExactly("garage")
    }

    // ---- moisture
    @Test fun moisturePatterns() {
        val rising = MoistureAnalyzer.analyze(listOf(10 to 40.0, 50 to 30.0, 100 to 15.0, 150 to 10.0), null)
        assertThat(rising.pattern).isEqualTo(MoisturePattern.DECREASING_FROM_FLOOR)
        assertThat(rising.elevatedHeights).containsAtLeast(10, 50, 100)
        assertThat(MoistureAnalyzer.analyze(listOf(10 to 12.0, 50 to 12.5, 100 to 12.0), null).pattern).isEqualTo(MoisturePattern.UNIFORM_LOW)
        assertThat(MoistureAnalyzer.analyze(listOf(10 to 12.0), null).pattern).isEqualTo(MoisturePattern.INSUFFICIENT)
        assertThat(MoistureAnalyzer.parseHeights("200, 10;50 x")).containsExactly(10, 50, 200).inOrder()
    }

    // ---- checklist
    @Test fun libraryIsConsistent() {
        val all = CheckpointLibrary.all
        assertThat(all.size).isAtLeast(200)
        assertThat(all.map { it.code }.toSet()).hasSize(all.size)
    }

    @Test fun apartmentAndNoCellarSkipChecks() {
        val lib = CheckpointLibrary.all
        val house = ChecklistGenerator.desired(lib, Property(id = 1), emptyMap(), emptyList())
        val flat = ChecklistGenerator.desired(lib, Property(id = 1, propertyType = "Appartamento", cellar = false), emptyMap(), emptyList())
        assertThat(flat.size).isLessThan(house.size)
        assertThat(flat.none { (d, _) -> "CELLAR" in d.conditions }).isTrue()
        val rooms = listOf(Room(id = 7, propertyId = 1, name = "Bagno", type = RoomType.BATHROOM))
        val existing = ChecklistGenerator.newItems(lib, Property(id = 1), emptyMap(), rooms, emptyList())
        assertThat(ChecklistGenerator.newItems(lib, Property(id = 1), emptyMap(), rooms, existing)).isEmpty()
    }

    // ---- zip safety
    @Test fun zipSlipIsRejected() {
        assertThat(ZipSafety.isSafeName("photos/a.jpg")).isTrue()
        listOf("../x", "a/../../x", "/etc/passwd", "C:/x", "a\\b").forEach { assertThat(ZipSafety.isSafeName(it)).isFalse() }
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z -> z.putNextEntry(ZipEntry("../evil.txt")); z.write(1); z.closeEntry() }
        val dir = Files.createTempDirectory("cc").toFile()
        assertThrows(BackupException::class.java) { ZipSafety.extract(ByteArrayInputStream(bos.toByteArray()), dir) }
        assertThat(dir.parentFile!!.resolve("evil.txt").exists()).isFalse()
    }
}
