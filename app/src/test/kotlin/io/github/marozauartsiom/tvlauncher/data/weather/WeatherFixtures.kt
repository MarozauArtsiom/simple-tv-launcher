package io.github.marozauartsiom.tvlauncher.data.weather

/** Reads a fixture from `app/src/test/resources/weather/`. */
fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.classLoader?.getResourceAsStream("weather/$name")) { "missing fixture $name" }
        .bufferedReader().use { it.readText() }

/** A fake network: the first rule whose text the URL contains answers; no rule means "offline". */
class FakeHttp : HttpFetcher {
    private class Rule(val contains: String, val answers: ArrayDeque<HttpResult?>)

    private val rules = mutableListOf<Rule>()
    val requests = mutableListOf<String>()

    /** [answers] are given one per request, the last one repeats. */
    fun on(contains: String, vararg answers: HttpResult?): FakeHttp = apply { rules += Rule(contains, ArrayDeque(answers.toList())) }
    fun ok(contains: String, body: String) = on(contains, HttpResult(200, body))

    override suspend fun get(url: String): HttpResult? {
        requests += url
        val rule = rules.firstOrNull { url.contains(it.contains) } ?: return null
        return if (rule.answers.size > 1) rule.answers.removeFirst() else rule.answers.firstOrNull()
    }
}
