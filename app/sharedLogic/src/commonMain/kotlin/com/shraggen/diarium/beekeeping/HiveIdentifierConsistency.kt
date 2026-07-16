@file:Suppress("MagicNumber")

package com.shraggen.diarium.beekeeping

import com.shraggen.diarium.tool.ToolCall
import kotlinx.serialization.json.JsonPrimitive

const val RECORD_INSPECTION_TOOL_NAME = "record_inspection"
const val HIVE_ID_ARGUMENT_NAME = "hive_id"

sealed interface HiveIdentifierConsistency {
    val proposedIdentifier: String?

    data class Verified(
        val transcriptIdentifier: String,
        override val proposedIdentifier: String,
    ) : HiveIdentifierConsistency

    data class Mismatch(
        val transcriptIdentifier: String,
        override val proposedIdentifier: String,
    ) : HiveIdentifierConsistency

    data class CannotVerify(
        val transcriptIdentifiers: Set<String>,
        override val proposedIdentifier: String?,
    ) : HiveIdentifierConsistency
}

class HiveIdentifierExtractor {

    fun extract(transcript: String): Set<String> =
        extractHiveIdentifiers(transcript)
}

class HiveIdentifierConsistencyChecker(
    private val extractor: HiveIdentifierExtractor = HiveIdentifierExtractor(),
) {

    fun check(
        transcript: String,
        call: ToolCall,
    ): HiveIdentifierConsistency {
        val proposedIdentifier = call.proposedHiveIdentifier()
        val transcriptIdentifiers = extractor.extract(transcript)

        if (call.toolName != RECORD_INSPECTION_TOOL_NAME ||
            proposedIdentifier == null ||
            transcriptIdentifiers.size != 1
        ) {
            return HiveIdentifierConsistency.CannotVerify(
                transcriptIdentifiers = transcriptIdentifiers,
                proposedIdentifier = proposedIdentifier,
            )
        }

        val transcriptIdentifier = transcriptIdentifiers.single()
        return if (transcriptIdentifier == proposedIdentifier) {
            HiveIdentifierConsistency.Verified(
                transcriptIdentifier = transcriptIdentifier,
                proposedIdentifier = proposedIdentifier,
            )
        } else {
            HiveIdentifierConsistency.Mismatch(
                transcriptIdentifier = transcriptIdentifier,
                proposedIdentifier = proposedIdentifier,
            )
        }
    }
}

private fun ToolCall.proposedHiveIdentifier(): String? {
    val primitive = arguments[HIVE_ID_ARGUMENT_NAME] as? JsonPrimitive
    return primitive
        ?.takeIf(JsonPrimitive::isString)
        ?.content
        ?.trim()
        ?.takeIf(String::isNotEmpty)
}

private fun extractHiveIdentifiers(transcript: String): Set<String> {
    val tokens = tokenize(transcript)
    return tokens.indices
        .asSequence()
        .filter { tokens[it] in HIVE_WORDS }
        .mapNotNull { readIdentifierAfterHive(tokens, it + 1) }
        .toSet()
}

private fun readIdentifierAfterHive(
    tokens: List<String>,
    startIndex: Int,
): String? {
    val identifierIndex = tokens
        .drop(startIndex)
        .indexOfFirst { it !in IDENTIFIER_LABELS }
        .takeIf { it in 0..MAX_SKIPPED_IDENTIFIER_LABELS }
        ?.plus(startIndex)
        ?: return null

    return parseDigitIdentifier(tokens[identifierIndex])
        ?: parseEnglishNumber(tokens, identifierIndex)?.toString()
        ?: parseGermanNumber(tokens[identifierIndex])?.toString()
        ?: parseSerbianNumber(tokens, identifierIndex)?.toString()
}

private fun tokenize(value: String): List<String> =
    value
        .lowercase()
        .map { character ->
            if (character.isLetterOrDigit()) character else ' '
        }
        .joinToString(separator = "")
        .split(' ')
        .filter(String::isNotBlank)

private fun parseDigitIdentifier(token: String): String? {
    if (token.isEmpty() || !token.all(Char::isDigit)) {
        return null
    }

    return token.map(Char::digitToInt).joinToString(separator = "")
}

private fun parseEnglishNumber(
    tokens: List<String>,
    index: Int,
): Int? = parseSpacedNumber(
    tokens = tokens,
    index = index,
    singleNumbers = ENGLISH_SINGLE_NUMBERS,
    tens = ENGLISH_TENS,
)

private fun parseGermanNumber(token: String): Int? = GERMAN_NUMBERS[token]

private fun parseSerbianNumber(
    tokens: List<String>,
    index: Int,
): Int? = parseSpacedNumber(
    tokens = tokens,
    index = index,
    singleNumbers = SERBIAN_SINGLE_NUMBERS,
    tens = SERBIAN_TENS,
)

private fun parseSpacedNumber(
    tokens: List<String>,
    index: Int,
    singleNumbers: Map<String, Int>,
    tens: Map<String, Int>,
): Int? {
    val token = tokens.getOrNull(index)
    val singleValue = token?.let(singleNumbers::get)
    val tensValue = token?.let(tens::get)
    val unitsValue = singleNumbers[tokens.getOrNull(index + 1)]
        ?.takeIf { it in 1..9 }
        ?: 0
    return singleValue ?: tensValue?.plus(unitsValue)
}

private const val MAX_SKIPPED_IDENTIFIER_LABELS = 2

private val HIVE_WORDS = setOf(
    "hive",
    "hives",
    "beehive",
    "beehives",
    "bienenstock",
    "bienenstöcke",
    "bienenstockes",
    "stock",
    "stöcke",
    "košnica",
    "košnicu",
    "košnice",
    "košnici",
    "kosnica",
    "kosnicu",
    "kosnice",
    "kosnici",
    "кошница",
    "кошницу",
    "кошнице",
    "кошници",
)

private val IDENTIFIER_LABELS = setOf(
    "number",
    "no",
    "id",
    "nummer",
    "nr",
    "broj",
    "број",
)

private val ENGLISH_SINGLE_NUMBERS = mapOf(
    "zero" to 0,
    "one" to 1,
    "two" to 2,
    "three" to 3,
    "four" to 4,
    "five" to 5,
    "six" to 6,
    "seven" to 7,
    "eight" to 8,
    "nine" to 9,
    "ten" to 10,
    "eleven" to 11,
    "twelve" to 12,
    "thirteen" to 13,
    "fourteen" to 14,
    "fifteen" to 15,
    "sixteen" to 16,
    "seventeen" to 17,
    "eighteen" to 18,
    "nineteen" to 19,
)

private val ENGLISH_TENS = mapOf(
    "twenty" to 20,
    "thirty" to 30,
    "forty" to 40,
    "fifty" to 50,
    "sixty" to 60,
    "seventy" to 70,
    "eighty" to 80,
    "ninety" to 90,
)

private val GERMAN_NUMBERS = buildMap {
    putAll(
        mapOf(
            "null" to 0,
            "eins" to 1,
            "zwei" to 2,
            "drei" to 3,
            "vier" to 4,
            "fünf" to 5,
            "fuenf" to 5,
            "sechs" to 6,
            "sieben" to 7,
            "acht" to 8,
            "neun" to 9,
            "zehn" to 10,
            "elf" to 11,
            "zwölf" to 12,
            "zwoelf" to 12,
            "dreizehn" to 13,
            "vierzehn" to 14,
            "fünfzehn" to 15,
            "fuenfzehn" to 15,
            "sechzehn" to 16,
            "siebzehn" to 17,
            "achtzehn" to 18,
            "neunzehn" to 19,
        ),
    )

    val units = mapOf(
        "ein" to 1,
        "zwei" to 2,
        "drei" to 3,
        "vier" to 4,
        "fünf" to 5,
        "fuenf" to 5,
        "sechs" to 6,
        "sieben" to 7,
        "acht" to 8,
        "neun" to 9,
    )
    val tens = mapOf(
        "zwanzig" to 20,
        "dreißig" to 30,
        "dreissig" to 30,
        "vierzig" to 40,
        "fünfzig" to 50,
        "fuenfzig" to 50,
        "sechzig" to 60,
        "siebzig" to 70,
        "achtzig" to 80,
        "neunzig" to 90,
    )

    putAll(tens)
    tens.forEach { (tensWord, tensValue) ->
        units.forEach { (unitWord, unitValue) ->
            put("${unitWord}und$tensWord", tensValue + unitValue)
        }
    }
}

private val SERBIAN_SINGLE_NUMBERS = mapOf(
    "nula" to 0,
    "jedan" to 1,
    "jedna" to 1,
    "dva" to 2,
    "dve" to 2,
    "tri" to 3,
    "četiri" to 4,
    "četri" to 4,
    "cetiri" to 4,
    "cetri" to 4,
    "pet" to 5,
    "šest" to 6,
    "sest" to 6,
    "sedam" to 7,
    "osam" to 8,
    "devet" to 9,
    "deset" to 10,
    "jedanaest" to 11,
    "dvanaest" to 12,
    "trinaest" to 13,
    "četrnaest" to 14,
    "cetrnaest" to 14,
    "petnaest" to 15,
    "šesnaest" to 16,
    "sesnaest" to 16,
    "sedamnaest" to 17,
    "osamnaest" to 18,
    "devetnaest" to 19,
    "нула" to 0,
    "један" to 1,
    "једна" to 1,
    "два" to 2,
    "две" to 2,
    "три" to 3,
    "четири" to 4,
    "четри" to 4,
    "пет" to 5,
    "шест" to 6,
    "седам" to 7,
    "осам" to 8,
    "девет" to 9,
    "десет" to 10,
    "једанаест" to 11,
    "дванаест" to 12,
    "тринаест" to 13,
    "четрнаест" to 14,
    "петнаест" to 15,
    "шеснаест" to 16,
    "седамнаест" to 17,
    "осамнаест" to 18,
    "деветнаест" to 19,
)

private val SERBIAN_TENS = mapOf(
    "dvadeset" to 20,
    "trideset" to 30,
    "četrdeset" to 40,
    "cetrdeset" to 40,
    "pedeset" to 50,
    "šezdeset" to 60,
    "sezdeset" to 60,
    "sedamdeset" to 70,
    "osamdeset" to 80,
    "devedeset" to 90,
    "двадесет" to 20,
    "тридесет" to 30,
    "четрдесет" to 40,
    "педесет" to 50,
    "шездесет" to 60,
    "седамдесет" to 70,
    "осамдесет" to 80,
    "деведесет" to 90,
)
