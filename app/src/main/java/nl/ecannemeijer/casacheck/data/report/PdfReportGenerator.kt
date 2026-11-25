package nl.ecannemeijer.casacheck.data.report

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.CheckStatus
import nl.ecannemeijer.casacheck.data.db.DefectStatus
import nl.ecannemeijer.casacheck.data.db.DocumentStatus
import nl.ecannemeijer.casacheck.data.db.Photo
import nl.ecannemeijer.casacheck.data.db.PhotoKind
import nl.ecannemeijer.casacheck.data.db.Report
import nl.ecannemeijer.casacheck.data.db.Severity
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.repo.InspectionRepository
import nl.ecannemeijer.casacheck.data.repo.SnapshotRepository
import nl.ecannemeijer.casacheck.data.settings.SettingsStore
import nl.ecannemeijer.casacheck.domain.checklist.CheckpointLibrary
import nl.ecannemeijer.casacheck.domain.content.ContentLibrary
import nl.ecannemeijer.casacheck.domain.moisture.MoistureAnalyzer
import nl.ecannemeijer.casacheck.domain.score.PropertySnapshot
import nl.ecannemeijer.casacheck.domain.score.ScoreCalculator
import nl.ecannemeijer.casacheck.ui.common.Fmt
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class ReportType { FULL, SUMMARY, DEFECTS, COSTS, MOISTURE, COMPARISON, GEOMETRA, AGENT }

data class ReportOptions(
    val type: ReportType,
    val language: String, // nl, it, en
    val includePhotos: Boolean = true,
    val includeNotes: Boolean = true,
    /** Listing photos only when the user explicitly allows it (copyright of the advertiser). */
    val includeListingPhotos: Boolean = false,
)

/** Offline PDF reports (android.graphics.pdf), A4, in nl/it/en independent of the app language. */
class PdfReportGenerator(
    private val context: Context,
    private val db: CasaDatabase,
    private val files: FileStore,
    private val snapshots: SnapshotRepository,
    private val inspections: InspectionRepository,
    private val settings: SettingsStore,
) {
    private fun localized(lang: String): Context {
        val cfg = Configuration(context.resources.configuration); cfg.setLocale(Locale(lang))
        return context.createConfigurationContext(cfg)
    }

    suspend fun generate(propertyIds: List<Long>, opt: ReportOptions): File = withContext(Dispatchers.IO) {
        val ctx = localized(opt.language)
        val locale = Locale(opt.language)
        val snaps = propertyIds.mapNotNull { snapshots.snapshot(it) }
        require(snaps.isNotEmpty()) { "no property" }
        val w = PdfWriter(ctx, locale)
        val weights = settings.current().weightMap()
        if (opt.type == ReportType.COMPARISON) comparison(w, ctx, snaps, weights)
        else snaps.forEachIndexed { i, s -> if (i > 0) w.newPage(); single(w, ctx, s, opt, weights) }
        val name = "casacheck_${opt.type.name.lowercase()}_${opt.language}_${System.currentTimeMillis()}.pdf"
        val out = File(files.reportsDir(), name)
        w.finish(out)
        val title = if (opt.type == ReportType.COMPARISON) ctx.getString(R.string.report_type_COMPARISON) else snaps.first().property.title
        db.reportDao().insert(Report(propertyId = if (snaps.size == 1) snaps.first().property.id else null, type = opt.type.name,
            language = opt.language, filePath = files.relative(out), title = title))
        out
    }

    private fun statusLabel(ctx: Context, s: CheckStatus) = ctx.getString(when (s) {
        CheckStatus.NOT_CHECKED -> R.string.status_not_checked; CheckStatus.OK -> R.string.status_ok; CheckStatus.MINOR -> R.string.status_minor
        CheckStatus.MAJOR -> R.string.status_major; CheckStatus.SERIOUS -> R.string.status_serious; CheckStatus.NOT_APPLICABLE -> R.string.status_na
        CheckStatus.UNSURE -> R.string.status_unsure
    })
    private fun sevLabel(ctx: Context, s: Severity) = ctx.getString(when (s) { Severity.MINOR -> R.string.status_minor; Severity.MAJOR -> R.string.status_major; Severity.SERIOUS -> R.string.status_serious })
    private fun docStatus(ctx: Context, s: DocumentStatus) = ctx.getString(when (s) {
        DocumentStatus.NOT_REQUESTED -> R.string.doc_status_not_requested; DocumentStatus.REQUESTED -> R.string.doc_status_requested
        DocumentStatus.RECEIVED -> R.string.doc_status_received; DocumentStatus.CHECKED -> R.string.doc_status_checked
        DocumentStatus.MISSING -> R.string.doc_status_missing; DocumentStatus.DEVIATION -> R.string.doc_status_deviation
        DocumentStatus.SPECIALIST_NEEDED -> R.string.doc_status_specialist
    })

    private fun yesNo(ctx: Context, b: Boolean?) = when (b) { true -> ctx.getString(R.string.yes); false -> ctx.getString(R.string.no); null -> "–" }

    private suspend fun single(w: PdfWriter, ctx: Context, s: PropertySnapshot, opt: ReportOptions, weights: Map<nl.ecannemeijer.casacheck.domain.score.ScoreGroup, Int>) {
        val p = s.property
        val t = opt.type
        val score = ScoreCalculator.calculate(s, weights, CheckpointLibrary.byCode)
        w.title(ctx.getString(R.string.report_header, ctx.getString(reportTypeName(t))))
        w.heading1(p.title.ifBlank { ctx.getString(R.string.untitled_property) })
        w.small(listOfNotNull(p.address, p.city, p.province, p.region, p.country).joinToString(", "))
        w.small(ctx.getString(R.string.report_date, DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, w.locale).format(Date())))
        val photos = db.photoDao().list(p.id)
        if (t in setOf(ReportType.FULL, ReportType.SUMMARY, ReportType.GEOMETRA, ReportType.AGENT)) {
            val main = photos.firstOrNull { it.id == p.mainPhotoId } ?: photos.firstOrNull { it.kind == PhotoKind.INSPECTION }
            if (main != null && opt.includePhotos && (main.kind != PhotoKind.LISTING || opt.includeListingPhotos)) w.image(files.resolve(main.editedPath ?: main.filePath), 260f)
        }
        w.disclaimer(ctx.getString(R.string.report_disclaimer))

        if (t in setOf(ReportType.FULL, ReportType.SUMMARY, ReportType.GEOMETRA, ReportType.AGENT)) {
            w.heading2(ctx.getString(R.string.section_property_data))
            w.table(listOf(
                ctx.getString(R.string.f_asking_price) to Fmt.euro(p.askingPrice, w.locale),
                ctx.getString(R.string.f_living_area) to Fmt.sqm(p.livingArea),
                ctx.getString(R.string.f_plot_area) to Fmt.sqm(p.plotArea),
                ctx.getString(R.string.f_build_year) to (p.buildYear?.toString() ?: "–"),
                ctx.getString(R.string.f_property_type) to (p.propertyType ?: "–"),
                ctx.getString(R.string.f_rooms) to (p.rooms?.toString() ?: "–"),
                ctx.getString(R.string.f_bedrooms) to (p.bedrooms?.toString() ?: "–"),
                ctx.getString(R.string.f_bathrooms) to (p.bathrooms?.toString() ?: "–"),
                ctx.getString(R.string.f_energy_label) to (p.energyLabel ?: "–"),
                ctx.getString(R.string.f_heating) to (p.heatingType ?: "–"),
                ctx.getString(R.string.f_cellar) to yesNo(ctx, p.cellar),
                ctx.getString(R.string.f_garage) to yesNo(ctx, p.garage),
                ctx.getString(R.string.f_garden) to yesNo(ctx, p.garden),
                ctx.getString(R.string.f_listing_url) to (p.listingUrl ?: "–"),
                ctx.getString(R.string.f_reference) to (p.referenceNumber ?: "–"),
                ctx.getString(R.string.f_agent_name) to (p.agentName ?: "–"),
            ))
        }

        if (t in setOf(ReportType.FULL, ReportType.SUMMARY)) {
            w.heading2(ctx.getString(R.string.section_assessment))
            w.table(listOf(
                ctx.getString(R.string.score_total) to (score.total?.let { "$it / 100" } ?: "–"),
                ctx.getString(R.string.inspection_completion) to "${score.completion}%",
                ctx.getString(R.string.status_minor) to s.defects.count { it.severity == Severity.MINOR && it.status != DefectStatus.RESOLVED }.toString(),
                ctx.getString(R.string.status_major) to s.defects.count { it.severity == Severity.MAJOR && it.status != DefectStatus.RESOLVED }.toString(),
                ctx.getString(R.string.status_serious) to s.defects.count { it.severity == Severity.SERIOUS && it.status != DefectStatus.RESOLVED }.toString(),
                ctx.getString(R.string.specialist_needed) to s.items.count { it.specialistNeeded == true }.toString(),
            ))
            w.paragraph(ctx.getString(R.string.score_not_guarantee))
            if (score.critical.isNotEmpty()) {
                w.heading3(ctx.getString(R.string.critical_issues))
                score.critical.forEach { w.bullet(ctx.getString(criticalName(it.kind)) + ": " + it.title, warn = true) }
            }
        }

        val active = s.defects.filter { it.status != DefectStatus.RESOLVED }.sortedByDescending { it.severity.ordinal }
        val defectsToShow = when (t) {
            ReportType.GEOMETRA -> active.filter { it.isStructural || it.isLegal || it.isMoisture || it.specialistNeeded }
            ReportType.SUMMARY, ReportType.AGENT -> active.filter { it.severity != Severity.MINOR }
            ReportType.COSTS, ReportType.MOISTURE -> emptyList()
            else -> active
        }
        if (t in setOf(ReportType.FULL, ReportType.SUMMARY, ReportType.DEFECTS, ReportType.GEOMETRA, ReportType.AGENT)) {
            w.heading2(ctx.getString(R.string.section_defects))
            if (defectsToShow.isEmpty()) w.paragraph(ctx.getString(R.string.no_defects_registered))
            val rooms = db.roomDao().rooms(p.id).associateBy { it.id }
            val links = db.photoDao().linksForProperty(p.id)
            for (d in defectsToShow) {
                w.heading3("${sevLabel(ctx, d.severity)} – ${d.title}")
                val lines = mutableListOf<Pair<String, String>>()
                d.roomId?.let { rooms[it] }?.let { lines += ctx.getString(R.string.room) to it.name }
                d.cause?.let { lines += ctx.getString(R.string.possible_cause) to it }
                d.solution?.let { lines += ctx.getString(R.string.possible_solution) to it }
                CostLineText(d.costMin, d.costMax, d.costOwn, w.locale)?.let { lines += ctx.getString(R.string.cost_estimate) to it }
                if (d.specialistNeeded) lines += ctx.getString(R.string.specialist_needed) to ctx.getString(R.string.yes)
                if (opt.includeNotes) d.notes?.let { lines += ctx.getString(R.string.notes) to it }
                w.table(lines)
                if (opt.includePhotos && t != ReportType.SUMMARY) {
                    val ph = photos.filter { it.includeInReport && (it.defectId == d.id || (d.itemId != null && links.any { l -> l.itemId == d.itemId && l.photoId == it.id })) }
                        .filter { it.kind != PhotoKind.LISTING || opt.includeListingPhotos }
                    w.imageRow(ph.take(4).map { files.resolve(it.editedPath ?: it.filePath) })
                }
            }
        }

        if (t == ReportType.FULL || t == ReportType.GEOMETRA) {
            val structural = s.items.filter { it.categoryCode in setOf("FOUNDATION", "STRUCTURE", "CRACKS", "RETAINING", "SLOPES") && it.effectiveStatus.isChecked }
            w.heading2(ctx.getString(R.string.section_structural))
            if (structural.isEmpty()) w.paragraph(ctx.getString(R.string.nothing_checked_yet))
            structural.forEach { w.bullet("${statusLabel(ctx, it.effectiveStatus)}: ${titleIn(ctx, it.templateCode, it.customTitle)}" + if (it.specialistNeeded == true) " (" + ctx.getString(R.string.specialist_needed) + ")" else "") }
            val cracks = db.measurementDao().cracks(p.id)
            if (cracks.isNotEmpty()) {
                w.heading3(ctx.getString(R.string.section_cracks))
                cracks.forEach { c -> w.bullet("${c.location}: ${ctx.getString(crackDirName(c.direction))}, ${c.widthMm?.let { "$it mm" } ?: "–"} × ${c.lengthCm?.let { "$it cm" } ?: "–"}, ${DateFormat.getDateInstance(DateFormat.SHORT, w.locale).format(Date(c.measuredAt))}" + (c.changeNotes?.let { " – $it" } ?: "")) }
                w.paragraph(ctx.getString(R.string.crack_width_note))
            }
        }

        if (t in setOf(ReportType.FULL, ReportType.MOISTURE, ReportType.GEOMETRA)) {
            val ms = db.measurementDao().moisture(p.id)
            w.heading2(ctx.getString(R.string.section_moisture))
            if (ms.isEmpty()) w.paragraph(ctx.getString(R.string.no_measurements))
            val rooms = db.roomDao().rooms(p.id).associateBy { it.id }
            for (m in ms) {
                val pts = db.measurementDao().points(m.id).filter { it.value != null }.map { it.heightCm to it.value!! }
                w.heading3(listOfNotNull(m.roomId?.let { rooms[it]?.name }, m.wall).joinToString(" – "))
                w.table(listOfNotNull(
                    ctx.getString(R.string.m_date) to DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, w.locale).format(Date(m.measuredAt)),
                    m.meterType?.let { ctx.getString(R.string.m_meter) to it }, m.method?.let { ctx.getString(R.string.m_method) to it },
                    m.unit?.let { ctx.getString(R.string.m_unit) to it }, m.material?.let { ctx.getString(R.string.m_material) to it },
                    m.referenceValue?.let { ctx.getString(R.string.m_reference) to it.toString() },
                    m.temperature?.let { ctx.getString(R.string.m_temperature) to "$it °C" }, m.relativeHumidity?.let { ctx.getString(R.string.m_rh) to "$it %" },
                ))
                w.barChart(pts, m.referenceValue, m.unit ?: "")
                val a = MoistureAnalyzer.analyze(pts, m.referenceValue)
                w.paragraph(ctx.getString(patternText(a.pattern)))
                if (opt.includeNotes) m.notes?.let { w.paragraph(it) }
            }
            w.paragraph(ctx.getString(R.string.moisture_disclaimer))
        }

        if (t in setOf(ReportType.FULL, ReportType.COSTS, ReportType.SUMMARY)) {
            val c = s.costs
            w.heading2(ctx.getString(R.string.section_costs))
            w.table(listOf(
                ctx.getString(R.string.f_asking_price) to Fmt.euro(c.askingPrice, w.locale),
                ctx.getString(R.string.cost_renovation) to Fmt.range(c.renovation.min, c.renovation.max, w.locale),
                ctx.getString(R.string.cost_immediate) to Fmt.range(c.immediate.min, c.immediate.max, w.locale),
                ctx.getString(R.string.cost_within_year) to Fmt.range(c.withinYear.min, c.withinYear.max, w.locale),
                ctx.getString(R.string.cost_optional) to Fmt.range(c.optional.min, c.optional.max, w.locale),
                ctx.getString(R.string.cost_contingency) to Fmt.range(c.contingency.min, c.contingency.max, w.locale),
                ctx.getString(R.string.cost_purchase) to Fmt.euro(c.purchaseCosts, w.locale),
                ctx.getString(R.string.cost_total) to if (c.totalMin != null) Fmt.range(c.totalMin, c.totalMax!!, w.locale) else "–",
                ctx.getString(R.string.cost_own_offer) to Fmt.euro(c.ownOffer, w.locale),
                ctx.getString(R.string.cost_total_own_offer) to if (c.totalOwnOfferMin != null) Fmt.range(c.totalOwnOfferMin, c.totalOwnOfferMax!!, w.locale) else "–",
            ))
            if (t != ReportType.SUMMARY) {
                w.heading3(ctx.getString(R.string.cost_lines))
                c.lines.forEach { l -> w.bullet("${l.title}: " + if (l.hasAmount) Fmt.range(l.range.min, l.range.max, w.locale) else ctx.getString(R.string.no_cost_estimate)) }
            }
            if (c.indicative) w.paragraph(ctx.getString(R.string.costs_indicative))
        }

        if (t in setOf(ReportType.FULL, ReportType.GEOMETRA, ReportType.AGENT, ReportType.SUMMARY)) {
            w.heading2(ctx.getString(R.string.section_documents))
            val docs = s.documents.associateBy { it.typeCode }
            for (d in ContentLibrary.documents) {
                val st = docs[d.code]?.status ?: DocumentStatus.NOT_REQUESTED
                if (t == ReportType.SUMMARY && st == DocumentStatus.NOT_REQUESTED) continue
                w.bullet("${d.italianName} (${ctx.getString(d.name)}): ${docStatus(ctx, st)}", warn = st == DocumentStatus.DEVIATION || st == DocumentStatus.MISSING)
                if (t == ReportType.AGENT && st != DocumentStatus.CHECKED && st != DocumentStatus.RECEIVED) w.small("   ${d.questionIt}")
            }
            w.paragraph(ctx.getString(R.string.catasto_vs_urbanistica))
        }

        if (t == ReportType.AGENT || t == ReportType.GEOMETRA) {
            w.heading2(ctx.getString(R.string.section_agent_questions))
            val qs = db.agentDao().questions(); val ans = db.agentDao().answers(p.id).associateBy { it.questionId }
            val defs = ContentLibrary.agentQuestions.associateBy { it.code }
            for (q in qs) {
                val it = defs[q.code]?.italian ?: q.customIt ?: q.customNl ?: continue
                val a = ans[q.id]
                w.bullet(it + (defs[q.code]?.let { d -> if (ctx.resources.configuration.locales[0].language != "it") " – " + ctx.getString(d.text) else "" } ?: ""))
                a?.answer?.let { w.small("   → $it") }
            }
        }

        if (t == ReportType.FULL) {
            val todo = s.items.filter { it.effectiveStatus == CheckStatus.NOT_CHECKED }
            w.heading2(ctx.getString(R.string.section_todo))
            w.paragraph(ctx.getString(R.string.todo_count, todo.size))
            todo.groupBy { it.categoryCode }.forEach { (cat, list) ->
                val catName = CheckpointLibrary.categories.firstOrNull { it.code == cat }?.let { ctx.getString(it.name) } ?: cat
                w.bullet("$catName: ${list.size}")
            }
            if (opt.includeNotes && !p.notes.isNullOrBlank()) { w.heading2(ctx.getString(R.string.section_personal_notes)); w.paragraph(p.notes) }
            if (opt.includePhotos) {
                val extra = photos.filter { it.includeInReport && it.important && it.defectId == null && (it.kind != PhotoKind.LISTING || opt.includeListingPhotos) }
                if (extra.isNotEmpty()) { w.heading2(ctx.getString(R.string.section_important_photos)); extra.chunked(2).forEach { row -> w.imageRow(row.map { files.resolve(it.editedPath ?: it.filePath) }) } }
            }
            w.heading2(ctx.getString(R.string.section_final))
            w.paragraph(finalText(ctx, score.total, score.completion, score.critical.size))
        }
    }

    private fun finalText(ctx: Context, total: Int?, completion: Int, critical: Int): String = buildString {
        append(ctx.getString(R.string.final_score_line, total?.toString() ?: "–", completion))
        if (critical > 0) append(' ').append(ctx.getString(R.string.final_critical_line, critical))
        if (completion < 80) append(' ').append(ctx.getString(R.string.final_incomplete_line))
        append(' ').append(ctx.getString(R.string.score_not_guarantee))
    }

    private fun comparison(w: PdfWriter, ctx: Context, snaps: List<PropertySnapshot>, weights: Map<nl.ecannemeijer.casacheck.domain.score.ScoreGroup, Int>) {
        w.title(ctx.getString(R.string.report_header, ctx.getString(R.string.report_type_COMPARISON)))
        w.small(DateFormat.getDateInstance(DateFormat.LONG, w.locale).format(Date()))
        w.disclaimer(ctx.getString(R.string.report_disclaimer))
        for (s in snaps) {
            val sc = ScoreCalculator.calculate(s, weights, CheckpointLibrary.byCode)
            val p = s.property
            w.heading2(p.title.ifBlank { ctx.getString(R.string.untitled_property) })
            w.table(listOf(
                ctx.getString(R.string.score_total) to (sc.total?.let { "$it / 100" } ?: "–"),
                ctx.getString(R.string.inspection_completion) to "${sc.completion}%",
                ctx.getString(R.string.f_asking_price) to Fmt.euro(p.askingPrice, w.locale),
                ctx.getString(R.string.f_living_area) to Fmt.sqm(p.livingArea),
                ctx.getString(R.string.f_plot_area) to Fmt.sqm(p.plotArea),
                ctx.getString(R.string.cost_renovation) to Fmt.range(s.costs.renovation.min, s.costs.renovation.max, w.locale),
                ctx.getString(R.string.cost_total) to if (s.costs.totalMin != null) Fmt.range(s.costs.totalMin, s.costs.totalMax!!, w.locale) else "–",
                ctx.getString(R.string.status_serious) to s.defects.count { it.severity == Severity.SERIOUS && it.status != DefectStatus.RESOLVED }.toString(),
                ctx.getString(R.string.critical_issues) to sc.critical.size.toString(),
            ))
            sc.critical.forEach { w.bullet(ctx.getString(criticalName(it.kind)) + ": " + it.title, warn = true) }
        }
        w.paragraph(ctx.getString(R.string.score_not_guarantee))
    }

    private fun titleIn(ctx: Context, code: String, custom: String?) = custom ?: CheckpointLibrary.byCode[code]?.let { ctx.getString(it.title) } ?: code

    companion object {
        fun reportTypeName(t: ReportType) = when (t) {
            ReportType.FULL -> R.string.report_type_FULL; ReportType.SUMMARY -> R.string.report_type_SUMMARY; ReportType.DEFECTS -> R.string.report_type_DEFECTS
            ReportType.COSTS -> R.string.report_type_COSTS; ReportType.MOISTURE -> R.string.report_type_MOISTURE; ReportType.COMPARISON -> R.string.report_type_COMPARISON
            ReportType.GEOMETRA -> R.string.report_type_GEOMETRA; ReportType.AGENT -> R.string.report_type_AGENT
        }
        fun criticalName(k: nl.ecannemeijer.casacheck.domain.score.CriticalKind) = when (k) {
            nl.ecannemeijer.casacheck.domain.score.CriticalKind.STRUCTURAL -> R.string.critical_structural
            nl.ecannemeijer.casacheck.domain.score.CriticalKind.SAFETY -> R.string.critical_safety
            nl.ecannemeijer.casacheck.domain.score.CriticalKind.LEGAL -> R.string.critical_legal
            nl.ecannemeijer.casacheck.domain.score.CriticalKind.MOISTURE -> R.string.critical_moisture
        }
        fun crackDirName(d: nl.ecannemeijer.casacheck.data.db.CrackDirection) = when (d) {
            nl.ecannemeijer.casacheck.data.db.CrackDirection.HORIZONTAL -> R.string.crack_HORIZONTAL
            nl.ecannemeijer.casacheck.data.db.CrackDirection.VERTICAL -> R.string.crack_VERTICAL
            nl.ecannemeijer.casacheck.data.db.CrackDirection.DIAGONAL -> R.string.crack_DIAGONAL
            nl.ecannemeijer.casacheck.data.db.CrackDirection.STEPPED -> R.string.crack_STEPPED
            nl.ecannemeijer.casacheck.data.db.CrackDirection.AROUND_WINDOW -> R.string.crack_AROUND_WINDOW
            nl.ecannemeijer.casacheck.data.db.CrackDirection.AROUND_DOOR -> R.string.crack_AROUND_DOOR
            nl.ecannemeijer.casacheck.data.db.CrackDirection.CEILING -> R.string.crack_CEILING
            nl.ecannemeijer.casacheck.data.db.CrackDirection.FOUNDATION -> R.string.crack_FOUNDATION
            nl.ecannemeijer.casacheck.data.db.CrackDirection.RETAINING_WALL -> R.string.crack_RETAINING_WALL
            nl.ecannemeijer.casacheck.data.db.CrackDirection.FACADE -> R.string.crack_FACADE
        }
        fun patternText(p: nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern) = when (p) {
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.INSUFFICIENT -> R.string.pattern_insufficient
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.UNIFORM_LOW -> R.string.pattern_uniform_low
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.DECREASING_FROM_FLOOR -> R.string.pattern_decreasing
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.HIGHEST_AT_TOP -> R.string.pattern_top
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.LOCAL_PEAK -> R.string.pattern_peak
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.UNIFORM_ELEVATED -> R.string.pattern_uniform_high
            nl.ecannemeijer.casacheck.domain.moisture.MoisturePattern.IRREGULAR -> R.string.pattern_irregular
        }
    }
}

@Suppress("FunctionName")
private fun CostLineText(min: Long?, max: Long?, own: Long?, locale: Locale): String? = when {
    own != null -> Fmt.euro(own, locale)
    min != null || max != null -> Fmt.range(min ?: max!!, max ?: min!!, locale)
    else -> null
}

/** Small flowing-layout PDF writer: A4 portrait, automatic page breaks, footer with page numbers. */
class PdfWriter(private val ctx: Context, val locale: Locale) {
    private val doc = PdfDocument()
    private val pageW = 595; private val pageH = 842; private val margin = 40f
    private var pageNo = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = 0f
    private val contentW get() = pageW - 2 * margin
    private val accent = Color.rgb(0x1F, 0x5C, 0x4B)

    private fun tp(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    init { newPage() }

    fun newPage() {
        finishPage()
        pageNo++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        canvas = page!!.canvas
        y = margin
    }

    private fun finishPage() {
        val pg = page ?: return
        val c = pg.canvas
        val f = tp(8f, color = Color.GRAY)
        c.drawText("CasaCheck – " + ctx.getString(R.string.report_footer), margin, pageH - 20f, f)
        c.drawText("$pageNo", pageW - margin - 10f, pageH - 20f, f)
        doc.finishPage(pg); page = null
    }

    private fun ensure(h: Float) { if (y + h > pageH - 50f) newPage() }

    private fun text(s: String, paint: TextPaint, indent: Float = 0f, spacingAfter: Float = 4f) {
        val layout = StaticLayout.Builder.obtain(s, 0, s.length, paint, (contentW - indent).toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        var line = 0
        while (line < layout.lineCount) {
            // split across pages line by line
            val remaining = pageH - 50f - y
            var end = line
            while (end < layout.lineCount && layout.getLineBottom(end) - layout.getLineTop(line) <= remaining) end++
            if (end == line) { newPage(); continue }
            val c = canvas!!
            c.save(); c.translate(margin + indent, y - layout.getLineTop(line))
            c.clipRect(0f, layout.getLineTop(line).toFloat(), contentW, layout.getLineBottom(end - 1).toFloat())
            layout.draw(c); c.restore()
            y += (layout.getLineBottom(end - 1) - layout.getLineTop(line)).toFloat()
            line = end
            if (line < layout.lineCount) newPage()
        }
        y += spacingAfter
    }

    fun title(s: String) { text(s, tp(11f, true, accent), spacingAfter = 2f) }
    fun heading1(s: String) { ensure(30f); text(s, tp(18f, true)) }
    fun heading2(s: String) {
        ensure(40f); y += 8f
        canvas!!.drawRect(margin, y, margin + contentW, y + 1.2f, Paint().apply { color = accent }); y += 6f
        text(s, tp(14f, true, accent))
    }
    fun heading3(s: String) { ensure(24f); y += 2f; text(s, tp(11f, true)) }
    fun paragraph(s: String) = text(s, tp(10f))
    fun small(s: String) = text(s, tp(8.5f, color = Color.DKGRAY), spacingAfter = 2f)
    fun bullet(s: String, warn: Boolean = false) = text("•  $s", tp(10f, warn, if (warn) Color.rgb(0xB0, 0x1E, 0x1E) else Color.BLACK), indent = 6f, spacingAfter = 2f)
    fun disclaimer(s: String) {
        ensure(40f)
        val p = tp(8.5f, color = Color.rgb(0x55, 0x55, 0x55))
        val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, (contentW - 12).toInt()).build()
        canvas!!.drawRoundRect(RectF(margin, y, margin + contentW, y + layout.height + 10f), 4f, 4f, Paint().apply { color = Color.rgb(0xF1, 0xF4, 0xF2) })
        canvas!!.save(); canvas!!.translate(margin + 6, y + 5); layout.draw(canvas!!); canvas!!.restore()
        y += layout.height + 16f
    }

    fun table(rows: List<Pair<String, String>>) {
        val kp = tp(9.5f, true, Color.DKGRAY); val vp = tp(9.5f)
        val keyW = contentW * 0.38f
        for ((k, v) in rows) {
            val kl = StaticLayout.Builder.obtain(k, 0, k.length, kp, keyW.toInt() - 6).build()
            val vl = StaticLayout.Builder.obtain(v, 0, v.length, vp, (contentW - keyW).toInt()).build()
            val h = maxOf(kl.height, vl.height) + 4f
            if (h > pageH - 100) { paragraph("$k: $v"); continue }
            ensure(h)
            val c = canvas!!
            c.save(); c.translate(margin, y); kl.draw(c); c.restore()
            c.save(); c.translate(margin + keyW, y); vl.draw(c); c.restore()
            y += h
            c.drawLine(margin, y - 1, margin + contentW, y - 1, Paint().apply { color = Color.rgb(0xE0, 0xE0, 0xE0) })
        }
        y += 4f
    }

    private fun decode(f: File?, maxSide: Int): Bitmap? {
        if (f == null || !f.exists()) return null
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(f.absolutePath, b)
        var s = 1; while (b.outWidth / (s * 2) >= maxSide && b.outHeight / (s * 2) >= maxSide) s *= 2
        return BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
    }

    fun image(f: File?, maxH: Float) {
        val bmp = decode(f, 1200) ?: return
        val scale = minOf(contentW / bmp.width, maxH / bmp.height)
        val w = bmp.width * scale; val h = bmp.height * scale
        ensure(h + 6)
        canvas!!.drawBitmap(bmp, null, RectF(margin, y, margin + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        bmp.recycle(); y += h + 6
    }

    fun imageRow(fs: List<File?>) {
        val list = fs.filterNotNull(); if (list.isEmpty()) return
        val cell = (contentW - (list.size - 1) * 6f) / maxOf(list.size, 2)
        val h = cell * 0.75f
        ensure(h + 6)
        list.forEachIndexed { i, f ->
            val bmp = decode(f, 800) ?: return@forEachIndexed
            val scale = minOf(cell / bmp.width, h / bmp.height)
            val x = margin + i * (cell + 6f)
            canvas!!.drawBitmap(bmp, null, RectF(x, y, x + bmp.width * scale, y + bmp.height * scale), Paint(Paint.FILTER_BITMAP_FLAG))
            bmp.recycle()
        }
        y += h + 6
    }

    /** Horizontal bars per measuring height (relative scale; no universal thresholds). */
    fun barChart(points: List<Pair<Int, Double>>, reference: Double?, unit: String) {
        if (points.isEmpty()) return
        val rowH = 14f
        val h = points.size * rowH + 12
        ensure(h)
        val c = canvas!!
        val max = maxOf(points.maxOf { it.second }, reference ?: 0.0).takeIf { it > 0 } ?: 1.0
        val labelW = 60f; val barMax = contentW - labelW - 60f
        val lp = tp(8.5f)
        points.sortedByDescending { it.first }.forEachIndexed { i, (hgt, v) ->
            val top = y + i * rowH
            c.drawText("$hgt cm", margin, top + 10f, lp)
            val w = (v / max * barMax).toFloat()
            val elevated = reference != null && v >= reference * 1.5
            c.drawRect(margin + labelW, top + 2f, margin + labelW + w, top + rowH - 2f, Paint().apply { color = if (elevated) Color.rgb(0xD9, 0x6C, 0x1F) else Color.rgb(0x3E, 0x7C, 0xB1) })
            c.drawText("$v $unit", margin + labelW + w + 4f, top + 10f, lp)
        }
        reference?.let {
            val x = margin + labelW + (it / max * barMax).toFloat()
            c.drawLine(x, y, x, y + points.size * rowH, Paint().apply { color = Color.rgb(0x2E, 0x7D, 0x32); strokeWidth = 1.2f })
        }
        y += h
    }

    fun finish(out: File) {
        finishPage()
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}
