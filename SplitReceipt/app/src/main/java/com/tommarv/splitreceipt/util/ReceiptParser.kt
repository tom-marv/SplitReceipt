package com.tommarv.splitreceipt.util

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

object ReceiptParser {
    
    fun parseText(visionText: Text): List<Pair<String, Double>> {
        val items = mutableListOf<Pair<String, Double>>()
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return emptyList()

        // 1. Group lines into rows using a more robust overlap algorithm
        val rows = groupLinesIntoRows(allLines)

        // 2. Constants for parsing
        val priceRegex = Regex("""(\d+[\.,]\d{2})\b""")
        val quantityRegex = Regex("""^(\d+\s*x\s*)|(\d+[\.,]\d{3}\s*)""", RegexOption.IGNORE_CASE)
        val forbiddenWords = listOf(
            "totale", "pagato", "resto", "euro", "p.iva", "documento", 
            "gestionale", "contante", "subtotale", "fiscal", "tesser", "punti"
        )

        // 3. Process each row
        for (row in rows) {
            val sortedRow = row.sortedBy { it.boundingBox?.left ?: 0 }
            val fullLineText = sortedRow.joinToString(" ") { it.text }.trim()

            // Skip lines that clearly aren't items
            if (fullLineText.isEmpty() || forbiddenWords.any { fullLineText.contains(it, ignoreCase = true) }) continue

            // Look for prices
            val matches = priceRegex.findAll(fullLineText).toList()
            if (matches.isNotEmpty()) {
                // Usually the last price on the line is the actual item price (avoiding quantities/codes)
                val lastMatch = matches.last()
                val priceString = lastMatch.groupValues[1].replace(",", ".")
                val price = priceString.toDoubleOrNull()
                
                if (price != null && price > 0) {
                    // Extract name: everything before the price, or the longest segment
                    var name = fullLineText.substring(0, lastMatch.range.first).trim()
                    
                    // Cleanup name
                    name = quantityRegex.replace(name, "").trim()
                    name = name.trim { !it.isLetterOrDigit() }

                    if (name.length >= 2) {
                        items.add(normalizeName(name) to price)
                    }
                }
            }
        }
        
        return items
    }

    /**
     * Groups lines that are on the same horizontal plane.
     */
    private fun groupLinesIntoRows(lines: List<Text.Line>): List<List<Text.Line>> {
        val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }
        val rows = mutableListOf<MutableList<Text.Line>>()

        for (line in sortedLines) {
            val lineBox = line.boundingBox ?: continue
            val lineCenterY = (lineBox.top + lineBox.bottom) / 2

            val matchingRow = rows.find { row ->
                val rowBox = row.first().boundingBox ?: return@find false
                val rowHeight = rowBox.bottom - rowBox.top
                // If the center of the new line is within the vertical bounds of the row
                // with a small tolerance, they belong together.
                val tolerance = rowHeight / 3
                lineCenterY in (rowBox.top - tolerance)..(rowBox.bottom + tolerance)
            }

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }
        return rows
    }

    private fun normalizeName(name: String): String {
        val lowercaseWords = setOf(
            "di", "a", "da", "in", "con", "su", "per", "tra", "fra",
            "del", "dello", "della", "dei", "degli", "delle",
            "al", "allo", "alla", "ai", "agli", "alle",
            "dal", "dallo", "dalla", "dai", "dagli", "dalle",
            "nel", "nello", "nella", "nei", "negli", "nelle",
            "sul", "sullo", "sulla", "sui", "sugli", "sulle",
            "e", "o", "ma", "se", "che", "né", "x"
        )

        return name.lowercase().split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .mapIndexed { index, word ->
                if (index == 0 || !lowercaseWords.contains(word)) {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    word
                }
            }.joinToString(" ")
    }
}
