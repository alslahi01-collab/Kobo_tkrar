package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ReportMeta(
    val id: String,
    val fileName: String,
    val totalRecords: Int,
    val validRecords: Int,
    val timestamp: Long,
    val headersJson: String,
    val rowsJson: String
)

class FormViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("statistical_analyzer_prefs", Context.MODE_PRIVATE)

    // Primary analyzed dataset
    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    // Loading/Processing states
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Selected cross tabulation columns
    private val _selectedXHeader = MutableStateFlow<String?>(null)
    val selectedXHeader: StateFlow<String?> = _selectedXHeader.asStateFlow()

    private val _selectedYHeader = MutableStateFlow<String?>(null)
    val selectedYHeader: StateFlow<String?> = _selectedYHeader.asStateFlow()

    private val _crossTabulation = MutableStateFlow<CrossTabulation?>(null)
    val crossTabulation: StateFlow<CrossTabulation?> = _crossTabulation.asStateFlow()

    // AI Generated Insights and Strategic Recommendations
    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights.asStateFlow()

    private val _isGeneratingInsights = MutableStateFlow(false)
    val isGeneratingInsights: StateFlow<Boolean> = _isGeneratingInsights.asStateFlow()

    // UI Toast Messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Historical Reports list
    private val _historicalReports = MutableStateFlow<List<ReportMeta>>(emptyList())
    val historicalReports: StateFlow<List<ReportMeta>> = _historicalReports.asStateFlow()

    init {
        loadHistoricalReports()
    }

    private fun loadHistoricalReports() {
        try {
            val json = sharedPrefs.getString("saved_reports_history", null)
            if (json != null) {
                val list = mutableListOf<ReportMeta>()
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ReportMeta(
                            id = obj.getString("id"),
                            fileName = obj.getString("fileName"),
                            totalRecords = obj.getInt("totalRecords"),
                            validRecords = obj.getInt("validRecords"),
                            timestamp = obj.getLong("timestamp"),
                            headersJson = obj.getString("headersJson"),
                            rowsJson = obj.getString("rowsJson")
                        )
                    )
                }
                _historicalReports.value = list.sortedByDescending { it.timestamp }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistoryToPrefs(list: List<ReportMeta>) {
        try {
            val jsonArray = JSONArray()
            for (report in list) {
                val obj = JSONObject().apply {
                    put("id", report.id)
                    put("fileName", report.fileName)
                    put("totalRecords", report.totalRecords)
                    put("validRecords", report.validRecords)
                    put("timestamp", report.timestamp)
                    put("headersJson", report.headersJson)
                    put("rowsJson", report.rowsJson)
                }
                jsonArray.put(obj)
            }
            sharedPrefs.edit().putString("saved_reports_history", jsonArray.toString()).apply()
            _historicalReports.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun analyzeFile(uri: Uri) {
        _isAnalyzing.value = true
        _aiInsights.value = null
        _selectedXHeader.value = null
        _selectedYHeader.value = null
        _crossTabulation.value = null

        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val fileName = getFileNameFromUri(context, uri) ?: "data_file.csv"
                
                val parsed = DatasetParser.parse(context, uri, fileName)
                if (parsed == null || parsed.headers.isEmpty()) {
                    _toastMessage.value = "⚠️ فشل قراءة الملف. تأكد من أنه ملف إكسل أو CSV صالح ويحتوي على ترويسة صحيحة."
                    _isAnalyzing.value = false
                    return@launch
                }

                val analysis = StatisticalAnalyzer.analyze(parsed)
                _analysisResult.value = analysis

                // Save to historical reports
                val headersArray = JSONArray()
                analysis.rawHeaders.forEach { headersArray.put(it) }

                val rowsArray = JSONArray()
                analysis.rawRows.forEach { row ->
                    val rowArr = JSONArray()
                    row.forEach { rowArr.put(it) }
                    rowsArray.put(rowArr)
                }

                val newReport = ReportMeta(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    totalRecords = analysis.totalRecords,
                    validRecords = analysis.validRecords,
                    timestamp = System.currentTimeMillis(),
                    headersJson = headersArray.toString(),
                    rowsJson = rowsArray.toString()
                )

                val updatedHistory = _historicalReports.value.toMutableList()
                // Avoid duplicating reports with same name
                updatedHistory.removeAll { it.fileName == fileName }
                updatedHistory.add(newReport)
                saveHistoryToPrefs(updatedHistory)

                // Select default headers for CrossTabulation if available
                if (analysis.rawHeaders.size >= 2) {
                    val catCols = analysis.columns.filter { it.type == ColumnType.CATEGORICAL }
                    if (catCols.size >= 2) {
                        setCrossTabHeaders(catCols[0].name, catCols[1].name)
                    } else {
                        setCrossTabHeaders(analysis.rawHeaders[0], analysis.rawHeaders[1])
                    }
                }

                _toastMessage.value = "✓ تم تحميل وتحليل البيانات بنجاح: ${analysis.validRecords} سجلاً صالحاً."
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "❌ خطأ أثناء معالجة البيانات: ${e.localizedMessage ?: e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun loadHistoricalReport(report: ReportMeta) {
        try {
            val headersArray = JSONArray(report.headersJson)
            val headers = mutableListOf<String>()
            for (i in 0 until headersArray.length()) {
                headers.add(headersArray.getString(i))
            }

            val rowsArray = JSONArray(report.rowsJson)
            val rows = mutableListOf<List<String>>()
            for (i in 0 until rowsArray.length()) {
                val rowArr = rowsArray.getJSONArray(i)
                val row = mutableListOf<String>()
                for (j in 0 until rowArr.length()) {
                    row.add(rowArr.optString(j, ""))
                }
                rows.add(row)
            }

            val parsed = ParsedDataset(report.fileName, headers, rows)
            val analysis = StatisticalAnalyzer.analyze(parsed)
            
            _analysisResult.value = analysis
            _aiInsights.value = null
            
            if (analysis.rawHeaders.size >= 2) {
                val catCols = analysis.columns.filter { it.type == ColumnType.CATEGORICAL }
                if (catCols.size >= 2) {
                    setCrossTabHeaders(catCols[0].name, catCols[1].name)
                } else {
                    setCrossTabHeaders(analysis.rawHeaders[0], analysis.rawHeaders[1])
                }
            }
            
            _toastMessage.value = "📂 تم استرجاع تقرير: ${report.fileName}"
        } catch (e: Exception) {
            e.printStackTrace()
            _toastMessage.value = "⚠️ فشل في تحميل السجل التاريخي المتراكم."
        }
    }

    fun deleteHistoricalReport(reportId: String) {
        val updated = _historicalReports.value.filter { it.id != reportId }
        saveHistoryToPrefs(updated)
        _toastMessage.value = "🗑 تم حذف الملف من السجل التاريخي."
    }

    fun setCrossTabHeaders(xHeader: String?, yHeader: String?) {
        _selectedXHeader.value = xHeader
        _selectedYHeader.value = yHeader
        recomputeCrossTabulation()
    }

    private fun recomputeCrossTabulation() {
        val result = _analysisResult.value ?: return
        val x = _selectedXHeader.value ?: return
        val y = _selectedYHeader.value ?: return

        val table = StatisticalAnalyzer.computeCrossTabulation(result, x, y)
        _crossTabulation.value = table
    }

    fun generateAISegments() {
        val result = _analysisResult.value ?: return
        _isGeneratingInsights.value = true
        _aiInsights.value = null

        viewModelScope.launch {
            val insights = GeminiStatisticalService.generateStatisticalInsights(result, _crossTabulation.value)
            _aiInsights.value = insights
            _isGeneratingInsights.value = false
            _toastMessage.value = "💡 تم توليد التحليل الإحصائي الإستراتيجي بدعم الذكاء الاصطناعي."
        }
    }

    fun exportExcelReport(uri: Uri, onCompleted: (Boolean) -> Unit) {
        val result = _analysisResult.value
        if (result == null) {
            _toastMessage.value = "⚠️ لا توجد بيانات نشطة لتصديرها."
            onCompleted(false)
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val aiInsightsText = _aiInsights.value ?: "لم يتم توليد تحليل الذكاء الاصطناعي مسبقاً. تم إنشاء هذا الملف مباشرة بناءً على الإحصائيات الرياضية الوصفية."
            val conclusionsText = _aiInsights.value ?: "توصيات عامة: نوصي بتركيز التدخلات الإنسانية واللوجستية في المناطق والفئات الأكثر ضعفاً واحتياجاً بناءً على الإحصاء الوصفي وتوزيع النسب المئوية."

            val success = ExcelExporter.exportToExcelXml(
                context = context,
                uri = uri,
                result = result,
                crossTab = _crossTabulation.value,
                aiSummaryText = aiInsightsText,
                conclusionsText = conclusionsText
            )

            if (success) {
                _toastMessage.value = "💾 تم تصدير تقرير إكسل المتكامل بنجاح!"
            } else {
                _toastMessage.value = "❌ فشل في تصدير التقرير."
            }
            onCompleted(success)
        }
    }

    fun showToast(message: String?) {
        _toastMessage.value = message
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }
}
