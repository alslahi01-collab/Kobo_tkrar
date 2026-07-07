package com.example

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ParsedDataset(
    val fileName: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

object DatasetParser {

    fun parse(context: Context, uri: Uri, fileName: String): ParsedDataset? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            if (extension == "xlsx") {
                parseXlsx(inputStream, fileName)
            } else {
                parseCsv(inputStream, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseCsv(inputStream: InputStream, fileName: String): ParsedDataset {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val rows = mutableListOf<List<String>>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val parsedLine = parseCsvLine(line!!)
            if (parsedLine.any { it.isNotBlank() }) {
                rows.add(parsedLine)
            }
        }
        reader.close()

        if (rows.isEmpty()) {
            return ParsedDataset(fileName, emptyList(), emptyList())
        }

        val headers = rows.first()
        val dataRows = rows.drop(1)

        // Standardize column counts
        val maxCols = headers.size
        val standardizedRows = dataRows.map { row ->
            if (row.size < maxCols) {
                row + List(maxCols - row.size) { "" }
            } else {
                row.take(maxCols)
            }
        }

        return ParsedDataset(fileName, headers, standardizedRows)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseXlsx(inputStream: InputStream, fileName: String): ParsedDataset? {
        var sharedStrings = listOf<String>()
        val sheetInputStreams = mutableMapOf<String, ByteArray>()
        val zip = ZipInputStream(inputStream)
        
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            if (name == "xl/sharedStrings.xml") {
                sharedStrings = parseSharedStrings(zip)
            } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                sheetInputStreams[name] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        zip.close()

        // We take sheet1 as primary
        val sheetBytes = sheetInputStreams["xl/worksheets/sheet1.xml"] 
            ?: sheetInputStreams.values.firstOrNull() 
            ?: return null

        return parseSheetXml(sheetBytes.inputStream(), sharedStrings, fileName)
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        
        var eventType = parser.eventType
        var currentString = StringBuilder()
        var inT = false
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "t") {
                        inT = true
                        currentString.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) {
                        currentString.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "t") {
                        strings.add(currentString.toString())
                        inT = false
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>, fileName: String): ParsedDataset {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        val rawRows = mutableMapOf<Int, MutableMap<Int, String>>()
        var currentIdx = -1
        var colIdx = -1
        var isString = false
        var currentVal = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "row") {
                        val rAttr = parser.getAttributeValue(null, "r")
                        currentIdx = (rAttr?.toIntOrNull() ?: (currentIdx + 1)) - 1
                        rawRows[currentIdx] = mutableMapOf()
                    } else if (name == "c") {
                        val rRef = parser.getAttributeValue(null, "r") ?: ""
                        colIdx = getColIndexFromRef(rRef)
                        val tAttr = parser.getAttributeValue(null, "t")
                        isString = (tAttr == "s")
                        currentVal.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    currentVal.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (name == "v") {
                        val rawValue = currentVal.toString()
                        val finalVal = if (isString) {
                            val strIdx = rawValue.toIntOrNull()
                            if (strIdx != null && strIdx in sharedStrings.indices) {
                                sharedStrings[strIdx]
                            } else {
                                rawValue
                            }
                        } else {
                            rawValue
                        }
                        if (currentIdx >= 0 && colIdx >= 0) {
                            rawRows[currentIdx]?.put(colIdx, finalVal)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        inputStream.close()

        if (rawRows.isEmpty()) {
            return ParsedDataset(fileName, emptyList(), emptyList())
        }

        // Convert rawRows map to lists
        val sortedRowIndices = rawRows.keys.sorted()
        val maxColIdx = rawRows.values.flatMap { it.keys }.maxOrNull() ?: 0

        val parsedRowsList = mutableListOf<List<String>>()
        for (r in sortedRowIndices) {
            val rowMap = rawRows[r] ?: emptyMap()
            val rowList = ArrayList<String>(maxColIdx + 1)
            for (c in 0..maxColIdx) {
                rowList.add(rowMap[c] ?: "")
            }
            parsedRowsList.add(rowList)
        }

        if (parsedRowsList.isEmpty()) {
            return ParsedDataset(fileName, emptyList(), emptyList())
        }

        val headers = parsedRowsList.first()
        val dataRows = parsedRowsList.drop(1)

        return ParsedDataset(fileName, headers, dataRows)
    }

    private fun getColIndexFromRef(ref: String): Int {
        var letters = ""
        for (char in ref) {
            if (char in 'A'..'Z') {
                letters += char
            } else {
                break
            }
        }
        if (letters.isEmpty()) return -1
        var index = 0
        for (char in letters) {
            index = index * 26 + (char - 'A' + 1)
        }
        return index - 1
    }
}
