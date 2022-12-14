package flank.instrument.log

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

// Stage 1 ============================================
/**
 * Group instrumentation output logs
 * @receiver Flow of lines
 * @return Flow of groups where the last line always contains status code
 */
internal fun Flow<String>.groupLines(): Flow<List<Line>> {
    val accumulator = mutableListOf<Line>()
    return transform { line ->
        val (prefix, text) = line.parsePrefix()
        accumulator += Line(prefix, text)
        when (prefix) {
            Type.StatusCode.text,
            Type.Code.text -> {
                emit(accumulator.toList())
                accumulator.clear()
            }
        }
    }
}

/**
 * Parsed line of instrumentation output. For example:
 * ```
 * INSTRUMENTATION_STATUS: test=ignoredTestWithIgnore
 * ```
 * @property prefix `"INSTRUMENTATION_STATUS: "`
 * @property text `"test=ignoredTestWithIgnore"`
 */
internal data class Line(
    val prefix: String?,
    val text: String,
)

private fun String.parsePrefix(): Pair<String?, String> {
    val prefix = Type.values().firstOrNull { startsWith(it.text) }?.text
    return prefix to (prefix?.let { removePrefix(it) } ?: this)
}

private enum class Type(val text: String) {
    Status("INSTRUMENTATION_STATUS: "),
    StatusCode("INSTRUMENTATION_STATUS_CODE: "),
    Result("INSTRUMENTATION_RESULT: "),
    Code("INSTRUMENTATION_CODE: "),
}

// Stage 2 ============================================
/**
 * Parse previously grouped lines into chunks
 * @receiver [Flow] of [Line] groups
 * @return [Flow] of [Chunk]
 */
internal fun Flow<List<Line>>.parseChunks(): Flow<Chunk> = map { group ->
    val reversed = group.reversed().toMutableList()
    val (prefix, text) = reversed.removeFirst()
    requireNotNull(prefix) {
        "Invalid last line in group, expected code but was: $text"
    }
    val linesAccumulator = mutableListOf<String>()
    val map = mutableMapOf<String, List<String>>()
    reversed.forEach { line ->
        if (line.prefix == null)
            linesAccumulator += line.text
        else
            linesAccumulator.apply {
                val (key, value) = line.text.split("=", limit = 2)
                add(value.trim())
                map[key.trim()] = reversed()
                clear()
            }
    }
    Chunk(
        type = prefix,
        code = text.trim().toInt(),
        map = map
    )
}

/**
 * The structured representation of instrumentation output lines followed by result code.
 * @property type The prefix of status code line: [INSTRUMENTATION_STATUS_CODE | INSTRUMENTATION_CODE]
 * @property code The result code.
 * @property map The properties for the specific group of lines.
 */
internal data class Chunk(
    val type: String,
    val code: Int,
    val map: RawProperties,
    val timestamp: Long = System.currentTimeMillis(),
)

private typealias RawProperties = Map<String, List<String>>

// Stage 3 ============================================

internal fun Flow<Chunk>.parseStatusResult(): Flow<Instrument> {
    val emptyChunk = Chunk(
        type = "",
        code = 0,
        map = mapOf("current" to listOf("0"))
    )

    var prev = emptyChunk

    return transform { next ->
        when (next.type) {

            // Handling the regular chunk which is representing the half of Status.
            Type.StatusCode.text -> when {
                prev.id == next.id -> emit(createStatus(prev, next))
                prev.id < next.id -> prev = next
                else -> throw IllegalArgumentException("Inconsistent stream of chunks.\nexpected pair for: $prev\nbut was $next")
            }

            // Handling the final chunk which is representing the Result.
            Type.Code.text -> {
                prev = emptyChunk
                emit(createResult(next))
            }

            else -> throw IllegalArgumentException("Unknown type of Chunk: ${next.type}")
        }
    }
}

private val Chunk.id: Int get() = map.getValue("current").first().trim().toInt()

private fun createStatus(first: Chunk, second: Chunk) = Instrument.Status(
    code = second.code,
    startTime = first.timestamp,
    endTime = second.timestamp,
    details = (first.map to second.map).parseValues().createDetails()
)

private fun Pair<RawProperties, RawProperties>.parseValues(): Map<String, Any> =
    (first.keys + second.keys).associateWith { key ->
        listOfNotNull(first[key], second[key]).flatten()
        (first[key] ?: emptyList()) + (second[key] ?: emptyList())
    }.mapValues { (key, value: List<String>) ->
        when (key) {
            "id",
            "test",
            "class",
            -> value.first() // all values are the same so we need only one

            "current",
            "numtests",
            -> value.first().trim().toInt() // same here but we want int

            else -> value // merged lines from stream or stack
        }
    }

private fun Map<String, Any>.createDetails() = Instrument.Status.Details(
    raw = this,
    className = get("class") as String,
    testName = get("test") as String,
    stack = get("stack")?.run {
        require(this is List<*>)
        joinToString("\n") { line -> line.toString().trim() }
    }
)

private fun createResult(chunk: Chunk) = Instrument.Result(
    code = chunk.code,
    details = chunk.map,
    time = chunk.timestamp
)
