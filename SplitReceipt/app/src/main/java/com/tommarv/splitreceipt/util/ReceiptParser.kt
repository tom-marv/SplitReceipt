package com.tommarv.splitreceipt.util

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

object ReceiptParser {
    
    fun parseText(visionText: Text): List<Pair<String, Double>> {
        val items = mutableListOf<Pair<String, Double>>()
        
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return emptyList()

        val sortedLines = allLines.sortedBy { it.boundingBox?.top ?: 0 }

        val rows = mutableListOf<MutableList<Text.Line>>()
        for (line in sortedLines) {
            val lineTop = line.boundingBox?.top ?: 0
            val lineBottom = line.boundingBox?.bottom ?: 0
            val lineHeight = lineBottom - lineTop
            
            val matchingRow = rows.find { row ->
                val rowTop = row.first().boundingBox?.top ?: 0
                val rowBottom = row.first().boundingBox?.bottom ?: 0
                val rowHeight = rowBottom - rowTop
                abs(lineTop - rowTop) < (rowHeight / 2)
            }
            
            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        val priceRegex = Regex("""(\d+[\.,]\d{2})""")
        val quantityRegex = Regex("""^\d+\s*x\s*""", RegexOption.IGNORE_CASE)
        val forbiddenWords = listOf("totale", "pagato", "resto", "euro", "p.iva", "documento", "gestionale")

        for (row in rows) {
            val sortedRow = row.sortedBy { it.boundingBox?.left ?: 0 }
            val fullLineText = sortedRow.joinToString(" ") { it.text }.trim()

            if (forbiddenWords.any { fullLineText.contains(it, ignoreCase = true) }) continue

            val match = priceRegex.find(fullLineText)
            if (match != null) {
                val priceString = match.groupValues[1].replace(",", ".")
                val price = priceString.toDoubleOrNull()
                
                if (price != null) {
                    var name = fullLineText.substring(0, match.range.first).trim()
                    if (name.isEmpty()) {
                        name = fullLineText.substring(match.range.last + 1).trim()
                    }
                    
                    name = quantityRegex.replace(name, "")
                    
                    if (name.isNotEmpty() && name.length > 1) {
                        items.add(normalizeName(name) to price)
                    }
                }
            }
        }
        
        return items
    }

    private fun normalizeName(name: String): String {
        val lowercaseWords = setOf(
            "di", "a", "da", "in", "con", "su", "per", "tra", "fra",
            "del", "dello", "della", "dei", "degli", "delle",
            "al", "allo", "alla", "ai", "agli", "alle",
            "dal", "dallo", "dalla", "dai", "dagli", "dalle",
            "nel", "nello", "nella", "nei", "negli", "nelle",
            "sul", "sullo", "sulla", "sui", "sugli", "sulle",
            "e", "o", "ma", "se", "che", "né", "x", "con"
        )

        return name.lowercase().split(" ")
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
