package com.example

import kotlin.math.sqrt

enum class ColumnType {
    NUMERIC,
    CATEGORICAL
}

data class CategoryStat(
    val category: String,
    val count: Int,
    val percentage: Double
)

data class ColumnAnalysis(
    val name: String,
    val type: ColumnType,
    val totalCount: Int,
    val validCount: Int,
    val missingCount: Int,
    val uniqueCount: Int,
    // Descriptive Statistics
    val min: Double? = null,
    val max: Double? = null,
    val mean: Double? = null,
    val median: Double? = null,
    val stdDev: Double? = null,
    // Categorical frequencies (sorted descending)
    val frequencies: List<CategoryStat> = emptyList(),
    val lowFrequencyAlerts: List<String> = emptyList()
)

data class CrossTabRow(
    val xValue: String,
    val yCounts: Map<String, Int>,
    val rowTotal: Int,
    val yPercentages: Map<String, Double> // out of rowTotal
)

data class CrossTabulation(
    val xHeader: String,
    val yHeader: String,
    val yCategories: List<String>,
    val rows: List<CrossTabRow>,
    val columnTotals: Map<String, Int>,
    val grandTotal: Int
)

data class AnalysisResult(
    val fileName: String,
    val totalRecords: Int,
    val validRecords: Int,
    val columns: List<ColumnAnalysis>,
    val rawHeaders: List<String>,
    val rawRows: List<List<String>>
)

object StatisticalAnalyzer {

    fun analyze(dataset: ParsedDataset): AnalysisResult {
        val headers = dataset.headers
        val rawRows = dataset.rows
        val totalRecords = rawRows.size
        
        // Count valid rows (at least one non-empty cell)
        val validRows = rawRows.filter { row -> row.any { it.isNotBlank() } }
        val validCount = validRows.size

        val columnAnalyses = headers.mapIndexed { colIdx, headerName ->
            val colValues = validRows.map { row -> if (colIdx < row.size) row[colIdx].trim() else "" }
            val nonBlankValues = colValues.filter { it.isNotBlank() }
            val missingCount = totalRecords - nonBlankValues.size
            
            // Auto-detect column type
            // If at least 80% of non-blank values can be parsed as Double, we treat as NUMERIC
            val doubleValues = nonBlankValues.mapNotNull { it.toDoubleOrNull() }
            val isNumeric = nonBlankValues.isNotEmpty() && (doubleValues.size.toDouble() / nonBlankValues.size >= 0.8)

            val uniqueValues = nonBlankValues.toSet()
            val uniqueCount = uniqueValues.size

            if (isNumeric) {
                val minVal = doubleValues.minOrNull() ?: 0.0
                val maxVal = doubleValues.maxOrNull() ?: 0.0
                val sumVal = doubleValues.sum()
                val meanVal = if (doubleValues.isNotEmpty()) sumVal / doubleValues.size else 0.0
                
                // Median
                val sortedDoubles = doubleValues.sorted()
                val medianVal = if (sortedDoubles.isNotEmpty()) {
                    if (sortedDoubles.size % 2 == 1) {
                        sortedDoubles[sortedDoubles.size / 2]
                    } else {
                        (sortedDoubles[sortedDoubles.size / 2 - 1] + sortedDoubles[sortedDoubles.size / 2]) / 2.0
                    }
                } else 0.0

                // Standard Deviation
                val stdDevVal = if (doubleValues.size > 1) {
                    val sumOfSquares = doubleValues.sumOf { (it - meanVal) * (it - meanVal) }
                    sqrt(sumOfSquares / (doubleValues.size - 1))
                } else 0.0

                // Also generate frequency distribution as categorical fallback
                val frequencies = nonBlankValues.groupingBy { it }
                    .eachCount()
                    .map { (cat, count) ->
                        CategoryStat(cat, count, (count.toDouble() / nonBlankValues.size) * 100.0)
                    }
                    .sortedByDescending { it.count }

                ColumnAnalysis(
                    name = headerName,
                    type = ColumnType.NUMERIC,
                    totalCount = totalRecords,
                    validCount = nonBlankValues.size,
                    missingCount = missingCount,
                    uniqueCount = uniqueCount,
                    min = minVal,
                    max = maxVal,
                    mean = meanVal,
                    median = medianVal,
                    stdDev = stdDevVal,
                    frequencies = frequencies
                )
            } else {
                // Categorical analysis
                val frequenciesMap = nonBlankValues.groupingBy { it }.eachCount()
                val frequencies = frequenciesMap.map { (cat, count) ->
                    CategoryStat(cat, count, (count.toDouble() / nonBlankValues.size) * 100.0)
                }.sortedByDescending { it.count }

                // Low frequency alert (less than 5% or count < 2)
                val lowFrequencyAlerts = frequencies.filter { it.count < 2 || it.percentage < 5.0 }
                    .map { "${it.category} (${it.count} تكرار، ${String.format("%.2f", it.percentage)}%)" }

                ColumnAnalysis(
                    name = headerName,
                    type = ColumnType.CATEGORICAL,
                    totalCount = totalRecords,
                    validCount = nonBlankValues.size,
                    missingCount = missingCount,
                    uniqueCount = uniqueCount,
                    frequencies = frequencies,
                    lowFrequencyAlerts = lowFrequencyAlerts
                )
            }
        }

        return AnalysisResult(
            fileName = dataset.fileName,
            totalRecords = totalRecords,
            validRecords = validCount,
            columns = columnAnalyses,
            rawHeaders = headers,
            rawRows = rawRows
        )
    }

    fun computeCrossTabulation(
        result: AnalysisResult,
        xHeader: String,
        yHeader: String
    ): CrossTabulation? {
        val xIdx = result.rawHeaders.indexOf(xHeader)
        val yIdx = result.rawHeaders.indexOf(yHeader)
        if (xIdx == -1 || yIdx == -1) return null

        val validRows = result.rawRows.filter { row ->
            xIdx < row.size && yIdx < row.size && row[xIdx].trim().isNotBlank() && row[yIdx].trim().isNotBlank()
        }

        val xCategories = validRows.map { it[xIdx].trim() }.toSet().sorted()
        val yCategories = validRows.map { it[yIdx].trim() }.toSet().sorted()

        val counts = mutableMapOf<String, MutableMap<String, Int>>()
        for (x in xCategories) {
            counts[x] = yCategories.associateWith { 0 }.toMutableMap()
        }

        for (row in validRows) {
            val xVal = row[xIdx].trim()
            val yVal = row[yIdx].trim()
            counts[xVal]?.put(yVal, (counts[xVal]?.get(yVal) ?: 0) + 1)
        }

        val columnTotals = yCategories.associateWith { y ->
            validRows.count { it[yIdx].trim() == y }
        }
        val grandTotal = validRows.size

        val crossTabRows = xCategories.map { x ->
            val yCounts = counts[x] ?: emptyMap()
            val rowTotal = yCounts.values.sum()
            val yPercentages = yCounts.mapValues { (_, count) ->
                if (rowTotal > 0) (count.toDouble() / rowTotal) * 100.0 else 0.0
            }
            CrossTabRow(
                xValue = x,
                yCounts = yCounts,
                rowTotal = rowTotal,
                yPercentages = yPercentages
            )
        }

        return CrossTabulation(
            xHeader = xHeader,
            yHeader = yHeader,
            yCategories = yCategories,
            rows = crossTabRows,
            columnTotals = columnTotals,
            grandTotal = grandTotal
        )
    }
}
