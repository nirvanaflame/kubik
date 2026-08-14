package com.nf.springplayground.hotel

import net.logstash.logback.marker.RawJsonAppendingMarker
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import kotlin.math.round
import kotlin.random.Random

data class Hotel(
    val id: String,
    val name: String,
    val city: String,
)

data class Offer(
    val hotelId: String,
    val hotelName: String,
    val price: Double,
    val currency: String,
    val nights: Int,
)

@RestController
class HotelOfferController(
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val hotels = listOf(
        Hotel("H01", "Grand Central", "Paris"),
        Hotel("H02", "Riverside Inn", "Paris"),
        Hotel("H03", "The Loft", "Lyon"),
        Hotel("H04", "Harbor View", "Marseille"),
        Hotel("H05", "Sunset Palace", "Nice"),
        Hotel("H06", "Old Town Suites", "Bordeaux"),
    )

    private fun round2(v: Double): Double = round(v * 100) / 100

    @GetMapping("/best-offer")
    fun bestOffer(): Map<String, Any> {

        val requestId = "req-${(100000..999999).random()}"

        // random offers returned per hotel (each hotel can have 1..4 offers)
        val offersByHotel: Map<String, List<Offer>> = hotels.associate { hotel ->
            val count = (1..4).random()
            hotel.id to (1..count).map {
                Offer(
                    hotelId = hotel.id,
                    hotelName = hotel.name,
                    price = round2(Random.nextDouble(80.0, 320.0)),
                    currency = "EUR",
                    nights = listOf(1, 2, 3, 7).random(),
                )
            }
        }

        val allOffers = offersByHotel.values.flatten()
        val best = allOffers.minByOrNull { it.price }!!
        val avg = round2(allOffers.map { it.price }.average())
        val savingsPct = round2(if (avg > 0) ((avg - best.price) / avg * 100) else 0.0)

        // the WIDE payload: full input + output, JSON-encoded into one marker
        val payload = mapOf(
            "requestId" to requestId,
            "hotelsConsidered" to hotels,
            "offersByHotel" to offersByHotel,
            "chosenOffer" to best,
            "stats" to mapOf(
                "hotelCount" to hotels.size,
                "offerCount" to allOffers.size,
                "cheapestPrice" to best.price,
                "avgPrice" to avg,
                "savingsVsAvgPct" to savingsPct,
            ),
        )

        // ONE wide event: filterable scalars + the full context marker
        log.atInfo()
            .addKeyValue("event", "hotel_offer_selection")
            .addKeyValue("operation", "choose_best_offer")
            .addKeyValue("requestId", requestId)
            .addKeyValue("hotelCount", hotels.size)
            .addKeyValue("offerCount", allOffers.size)
            .addKeyValue("chosenHotelId", best.hotelId)
            .addKeyValue("chosenHotelName", best.hotelName)
            .addKeyValue("chosenPrice", best.price)
            .addKeyValue("currency", best.currency)
            .addKeyValue("avgPrice", avg)
            .addKeyValue("savingsVsAvgPct", savingsPct)
            .addMarker(RawJsonAppendingMarker("comparison", objectMapper.writeValueAsString(payload)))
            .log("Chose best hotel offer")

        // static summary returned to the caller
        return mapOf(
            "requestId" to requestId,
            "hotelsConsidered" to hotels.size,
            "offersConsidered" to allOffers.size,
            "chosen" to mapOf(
                "hotelId" to best.hotelId,
                "hotelName" to best.hotelName,
                "price" to best.price,
                "currency" to best.currency,
                "nights" to best.nights,
                "avgPrice" to avg,
                "savingsVsAvgPct" to savingsPct,
            ),
        )
    }
}
