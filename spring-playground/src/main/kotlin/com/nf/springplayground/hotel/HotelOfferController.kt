package com.nf.springplayground.hotel

import jakarta.annotation.PostConstruct
import net.logstash.logback.marker.RawJsonAppendingMarker
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
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
    private val redis: StringRedisTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val OFFER_CACHE_KEY = "offers:best"
    }

    private val hotels = listOf(
        Hotel("H01", "Grand Central", "Paris"),
        Hotel("H02", "Riverside Inn", "Paris"),
        Hotel("H03", "The Loft", "Lyon"),
        Hotel("H04", "Harbor View", "Marseille"),
        Hotel("H05", "Sunset Palace", "Nice"),
        Hotel("H06", "Old Town Suites", "Bordeaux"),
    )

    private fun round2(v: Double): Double = round(v * 100) / 100

    @PostConstruct
    fun seedHotels() {
        hotels.forEach { hotel ->
            redis.opsForValue().set("hotel:${hotel.id}", objectMapper.writeValueAsString(hotel))
        }
        log.info("Seeded {} hotels into Redis under keys hotel:*", hotels.size)
    }

    @GetMapping("/best-offer")
    fun bestOffer(): Map<String, Any> {
        val requestId = "req-${(100000..999999).random()}"

        // 1) Check Redis cache for a previously computed best offer
        val cached = redis.opsForValue().get(OFFER_CACHE_KEY)
        if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            val result = objectMapper.readValue(cached, Map::class.java) as Map<String, Any>
            log.atInfo()
                .addKeyValue("event", "hotel_offer_selection")
                .addKeyValue("operation", "cache_hit")
                .addKeyValue("request_id", requestId)
                .log("Returning cached best offer from Redis key={}", OFFER_CACHE_KEY)
            // Inject a fresh requestId into the cached response
            return result + ("requestId" to requestId)
        }

        // 2) Cache miss — read hotels from Redis to build offers
        val hotelsFromRedis = hotels.mapNotNull { hotel ->
            val json = redis.opsForValue().get("hotel:${hotel.id}")
            json?.let { objectMapper.readValue(it, Hotel::class.java) }
        }

        val offersByHotel: Map<String, List<Offer>> = hotelsFromRedis.associate { hotel ->
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

        // 3) Build and cache the response
        val response = mapOf(
            "requestId" to requestId,
            "hotelsConsidered" to hotelsFromRedis.size,
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
        redis.opsForValue().set(OFFER_CACHE_KEY, objectMapper.writeValueAsString(response))

        // 4) Wide log payload (unchanged)
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
            .log("Chose best hotel offer (fresh)")

        return response
    }
}