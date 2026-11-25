package nl.ecannemeijer.casacheck.domain.score

import nl.ecannemeijer.casacheck.data.db.Defect
import nl.ecannemeijer.casacheck.data.db.Destination
import nl.ecannemeijer.casacheck.data.db.DestinationDistance
import nl.ecannemeijer.casacheck.data.db.Document
import nl.ecannemeijer.casacheck.data.db.EnvironmentRating
import nl.ecannemeijer.casacheck.data.db.ItemWithResult
import nl.ecannemeijer.casacheck.data.db.Property
import nl.ecannemeijer.casacheck.domain.cost.CostSummary

/** Everything needed to score, compare and report one property. */
data class PropertySnapshot(
    val property: Property,
    val features: Map<String, String?>,
    val items: List<ItemWithResult>,
    val defects: List<Defect>,
    val costs: CostSummary,
    val documents: List<Document>,
    val environment: List<EnvironmentRating>,
    val destinations: List<Destination>,
    val distances: List<DestinationDistance>,
)
