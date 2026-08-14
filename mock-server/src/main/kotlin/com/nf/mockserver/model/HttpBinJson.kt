package com.nf.mockserver.model

/**
 * Data model mirroring the payload served by httpbin.org/json.
 *
 * See https://httpbin.org/json — a literal "Sample Slide Show" document.
 */
data class Slideshow(
    val title: String,
    val date: String,
    val author: String,
    val slides: List<Slide>,
)

data class Slide(
    val type: String,
    val title: String,
    val items: List<String>? = null,
)

/** Top-level envelope returned by the mock server /json endpoint. */
data class JsonResponse(
    val slideshow: Slideshow,
)

/** Builds a copy of the canonical httpbin.org/json document. */
object HttpBinJson {
    fun sample(): JsonResponse = JsonResponse(
        slideshow = Slideshow(
            title = "Sample Slide Show",
            date = "date of publication",
            author = "Yours Truly",
            slides = listOf(
                Slide(type = "all", title = "Wake up to WonderWidgets!"),
                Slide(
                    type = "all",
                    title = "Overview",
                    items = listOf(
                        "Why <em>WonderWidgets</em> are great",
                        "Who <em>buys</em> WonderWidgets",
                    ),
                ),
            ),
        ),
    )
}
