package nl.ecannemeijer.casacheck.data.importer

import kotlinx.serialization.Serializable

/** A photo found in a listing. [position] keeps the original gallery order (0-based). */
@Serializable
data class ImportPhoto(
    val url: String,
    val position: Int,
    val caption: String? = null,
    val isFloorplan: Boolean = false,
    val thumbUrl: String? = null,
)

/**
 * Everything an import provider could find. Every field is nullable: unknown data stays empty
 * and is never guessed. [found] lists the field names that were actually found.
 */
@Serializable
data class ListingData(
    val site: String,
    val url: String? = null,
    val externalId: String? = null,
    val title: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val price: Long? = null,
    val pricePerSqm: Long? = null,
    val buildYear: Int? = null,
    val propertyType: String? = null,
    val livingArea: Double? = null,
    val plotArea: Double? = null,
    val rooms: Int? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val floors: Int? = null,
    val condition: String? = null,
    val energyLabel: String? = null,
    val energyConsumption: String? = null,
    val heatingType: String? = null,
    val garage: Boolean? = null,
    val parkingSpaces: Int? = null,
    val cellar: Boolean? = null,
    val attic: Boolean? = null,
    val balcony: Boolean? = null,
    val terrace: Boolean? = null,
    val garden: Boolean? = null,
    val airConditioning: Boolean? = null,
    val description: String? = null,
    val referenceNumber: String? = null,
    val agentName: String? = null,
    val agentPhone: String? = null,
    val agentEmail: String? = null,
    val photos: List<ImportPhoto> = emptyList(),
    /** Extra raw features (e.g. FLOOR_TEXT, LIFT, ORIENTATION, ROOMS_TEXT). */
    val extraFeatures: Map<String, String> = emptyMap(),
    val listingActive: Boolean? = null,
) {
    /** Names of fields that contain a value. Used to show "automatically found" vs "not found". */
    fun foundFields(): Set<String> = buildSet {
        if (!title.isNullOrBlank()) add("title")
        if (!address.isNullOrBlank()) add("address")
        if (!city.isNullOrBlank()) add("city")
        if (!province.isNullOrBlank()) add("province")
        if (!region.isNullOrBlank()) add("region")
        if (!country.isNullOrBlank()) add("country")
        if (latitude != null && longitude != null) add("gps")
        if (price != null) add("price")
        if (pricePerSqm != null) add("pricePerSqm")
        if (buildYear != null) add("buildYear")
        if (!propertyType.isNullOrBlank()) add("propertyType")
        if (livingArea != null) add("livingArea")
        if (plotArea != null) add("plotArea")
        if (rooms != null) add("rooms")
        if (bedrooms != null) add("bedrooms")
        if (bathrooms != null) add("bathrooms")
        if (floors != null) add("floors")
        if (!condition.isNullOrBlank()) add("condition")
        if (!energyLabel.isNullOrBlank()) add("energyLabel")
        if (!energyConsumption.isNullOrBlank()) add("energyConsumption")
        if (!heatingType.isNullOrBlank()) add("heatingType")
        if (garage != null) add("garage")
        if (parkingSpaces != null) add("parkingSpaces")
        if (cellar != null) add("cellar")
        if (attic != null) add("attic")
        if (balcony != null) add("balcony")
        if (terrace != null) add("terrace")
        if (garden != null) add("garden")
        if (airConditioning != null) add("airConditioning")
        if (!description.isNullOrBlank()) add("description")
        if (!referenceNumber.isNullOrBlank()) add("referenceNumber")
        if (!agentName.isNullOrBlank()) add("agentName")
        if (!agentPhone.isNullOrBlank()) add("agentPhone")
        if (!agentEmail.isNullOrBlank()) add("agentEmail")
        if (!externalId.isNullOrBlank()) add("externalId")
    }

    /** Merge: values of [other] only fill gaps in this listing. */
    fun fillFrom(other: ListingData): ListingData = copy(
        title = title ?: other.title, address = address ?: other.address, city = city ?: other.city,
        province = province ?: other.province, region = region ?: other.region, country = country ?: other.country,
        latitude = latitude ?: other.latitude, longitude = longitude ?: other.longitude, price = price ?: other.price,
        pricePerSqm = pricePerSqm ?: other.pricePerSqm, buildYear = buildYear ?: other.buildYear,
        propertyType = propertyType ?: other.propertyType, livingArea = livingArea ?: other.livingArea,
        plotArea = plotArea ?: other.plotArea, rooms = rooms ?: other.rooms, bedrooms = bedrooms ?: other.bedrooms,
        bathrooms = bathrooms ?: other.bathrooms, floors = floors ?: other.floors, condition = condition ?: other.condition,
        energyLabel = energyLabel ?: other.energyLabel, energyConsumption = energyConsumption ?: other.energyConsumption,
        heatingType = heatingType ?: other.heatingType, garage = garage ?: other.garage,
        parkingSpaces = parkingSpaces ?: other.parkingSpaces, cellar = cellar ?: other.cellar, attic = attic ?: other.attic,
        balcony = balcony ?: other.balcony, terrace = terrace ?: other.terrace, garden = garden ?: other.garden,
        airConditioning = airConditioning ?: other.airConditioning, description = description ?: other.description,
        referenceNumber = referenceNumber ?: other.referenceNumber, agentName = agentName ?: other.agentName,
        agentPhone = agentPhone ?: other.agentPhone, agentEmail = agentEmail ?: other.agentEmail,
        photos = photos.ifEmpty { other.photos }, extraFeatures = other.extraFeatures + extraFeatures,
        externalId = externalId ?: other.externalId, url = url ?: other.url,
    )
}

sealed interface ImportOutcome {
    data class Success(val data: ListingData, val partial: Boolean) : ImportOutcome
    /** The website refused automated access (CAPTCHA / anti-bot / 403). We never try to bypass this. */
    data class Blocked(val site: String, val httpCode: Int, val reason: String) : ImportOutcome
    data class NotFound(val site: String, val httpCode: Int) : ImportOutcome
    data class Failed(val message: String) : ImportOutcome
    data object Cancelled : ImportOutcome
}

enum class ImportStage { VALIDATING, FETCHING, PARSING, PHOTOS, DONE }

data class ImportProgress(val stage: ImportStage, val fieldsFound: Int = 0, val photosFound: Int = 0)
