package com.hunterwb.anyascii.build

import com.ibm.icu.lang.UCharacter
import com.ibm.icu.text.Normalizer2
import java.nio.file.Files
import java.nio.file.Path
import java.util.IntSummaryStatistics
import java.util.TreeMap

typealias Table = TreeMap<Int, String>

fun Table.then(other: Table) = apply { putAllIfAbsent(other) }

fun Table.then(codePoint: Int, s: String) = apply { putIfAbsent(codePoint, s) }

fun Table.minus(codePoint: Int) = apply { remove(codePoint) }

fun Table.minus(other: Table) = apply { for (cp in other.keys) remove(cp) }

fun Table.write(path: String) = apply {
    Files.newBufferedWriter(Path.of(path)).use {
        for ((cp, r) in this) {
            if (r.isEmpty()) continue
            check(r.isPrintableAscii())
            it.append(toString(cp)).append('\t').append(r).append('\n')
        }
    }
}

fun Table(file: String) = Table().apply {
    forEachLine(Path.of("input/tables/$file.tsv")) { line ->
        if (line.startsWith('#')) return@forEachLine
        val cp = line.codePointAt(0)
        val i = Character.charCount(cp)
        check(line[i] == '\t')
        put(cp, line.substring(i + 1))
    }
}

fun Table.normalize(normalizer2: Normalizer2, replacement: String? = null) = apply {
    for (cp in 128..Character.MAX_CODE_POINT) {
        if (cp in this) continue
        val output = transliterate(normalizer2.normalize(toString(cp)), replacement)
        if (output != null && output.isNotEmpty()) {
            this[cp] = output
        }
    }
}

private fun Table.transliterate(s: String, default: String?): String? {
    val buf = StringBuilder()
    for (cp in s.codePoints()) {
        val d = this[cp] ?: default ?: return null
        buf.append(d)
    }
    return buf.toString()
}

inline fun Iterable<Int>.toTable(map: (Int) -> String): Table = associateWithTo(Table(), map)

fun Table.lengthStatistics() = IntSummaryStatistics().apply { values.forEach { accept(it.length) } }

fun Table.cased() = apply {
    for ((cp, s) in this.toMap()) {
        putIfAbsent(lower(cp), lower(s))
        putIfAbsent(upper(cp), s.capitalize())
    }
    for (cp in 0..Character.MAX_CODE_POINT) {
        if (cp in this) continue
        this[upper(cp)]?.let { putIfAbsent(cp, lower(it)) }
        this[lower(cp)]?.let { putIfAbsent(cp, it.capitalize()) }
    }
}

fun Table.aliasing(codePoints: Iterable<Int>, nameTransform: (String) -> String) = apply {
    for (cp in codePoints) {
        val cp2 = UCharacter.getCharFromName(nameTransform(name(cp)))
        check(cp2 != -1) { cp.toString(16) }
        putIfAbsent(cp, getValue(cp2))
    }
}

fun Table.retain(codePoints: Iterable<Int>) = apply { keys.retainAll(codePoints) }