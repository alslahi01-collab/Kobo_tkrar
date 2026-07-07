package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiStatisticalService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun generateStatisticalInsights(
        result: AnalysisResult,
        crossTab: CrossTabulation? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY" || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "⚠️ لم يتم تكوين مفتاح Gemini API في إعدادات التطبيق. يرجى إضافته عبر لوحة الأسرار لتفعيل قراءة التحليلات الاستباقية الفائقة الذكاء."
        }

        val prompt = buildAnalysisPrompt(result, crossTab)
        
        try {
            // Build the JSON request body manually using org.json
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // System instruction
                val systemInstructionObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", "أنت خبير تحليل إحصائي وميداني متمكن جداً. لغتك عربية رسمية، بليغة وموضوعية. تقوم بصياغة استنتاجات وتقارير مباشرة تعتمد بدقة على الأرقام والبيانات المرسلة لك فقط دون تأليف أو افتراض أي أرقام أو معلومات خارجية.")
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)

                // Generation config
                val configObj = JSONObject().apply {
                    put("temperature", 0.2)
                }
                put("generationConfig", configObj)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "خطأ أثناء الاتصال بالذكاء الاصطناعي: رمز الاستجابة ${response.code}"
            }

            val responseBody = response.body?.string() ?: return@withContext "عذراً، لم نتمكن من الحصول على رد من خادم الذكاء الاصطناعي."
            val responseJson = JSONObject(responseBody)
            
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val text = firstPart?.optString("text")

            text ?: "عذراً، لم نتمكن من تحليل محتويات الرد المرسل."
        } catch (e: Exception) {
            e.printStackTrace()
            "خطأ أثناء الاتصال بالذكاء الاصطناعي: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun buildAnalysisPrompt(result: AnalysisResult, crossTab: CrossTabulation?): String {
        val sb = java.lang.StringBuilder()
        sb.append("يرجى إجراء تحليل إحصائي وصفي وتقاطعي عميق وشامل بناءً على البيانات والمقاييس التالية المستخرجة من ملف الدراسة الميدانية:\n\n")
        sb.append("### 1. معلومات الملف والمنهجية:\n")
        sb.append("- اسم الملف: ${result.fileName}\n")
        sb.append("- إجمالي السجلات: ${result.totalRecords}\n")
        sb.append("- السجلات الصالحة (المكتملة): ${result.validRecords}\n")
        val pct = (result.validRecords.toDouble() / result.totalRecords) * 100
        sb.append("- نسبة صلاحية المدخلات: ${String.format("%.2f", pct)}%\n")
        sb.append("- عدد المتغيرات الكلي: ${result.columns.size}\n\n")

        sb.append("### 2. التوزيع الإحصائي والمقاييس الوصفية للمتغيرات:\n")
        for (col in result.columns) {
            sb.append("- المتغير: [${col.name}] | النوع: ${col.type}\n")
            sb.append("  * القيم الصالحة: ${col.validCount} | المفقودة: ${col.missingCount}\n")
            if (col.type == ColumnType.NUMERIC) {
                sb.append("  * المتوسط الحسابي: ${String.format("%.2f", col.mean ?: 0.0)}\n")
                sb.append("  * الانحراف المعياري: ${String.format("%.2f", col.stdDev ?: 0.0)}\n")
                sb.append("  * الحد الأدنى: ${col.min} | الحد الأقصى: ${col.max}\n")
                sb.append("  * الوسيط: ${col.median}\n")
            } else {
                sb.append("  * التوزيعات الأكثر تكراراً (أعلى فئات):\n")
                col.frequencies.take(5).forEach { freq ->
                    sb.append("    > ${freq.category}: تكرار=${freq.count} (${String.format("%.2f", freq.percentage)}%)\n")
                }
                if (col.lowFrequencyAlerts.isNotEmpty()) {
                    sb.append("  * فئات منخفضة التكرار جداً: ${col.lowFrequencyAlerts.take(3).joinToString(" | ")}\n")
                }
            }
            sb.append("\n")
        }

        if (crossTab != null) {
            sb.append("### 3. دراسة الارتباط والتحليل التقاطعي (Cross Tabulation):\n")
            sb.append("- المتغير المستقل (X): ${crossTab.xHeader}\n")
            sb.append("- المتغير التابع (Y): ${crossTab.yHeader}\n")
            sb.append("- جدول التوزيع المشترك والنسب المئوية:\n")
            for (row in crossTab.rows) {
                sb.append("  * الفئة [${row.xValue}] (الإجمالي=${row.rowTotal}):\n")
                for (yCat in crossTab.yCategories) {
                    val count = row.yCounts[yCat] ?: 0
                    val ratio = row.yPercentages[yCat] ?: 0.0
                    sb.append("    > $yCat: $count تكرارات (${String.format("%.2f", ratio)}%)\n")
                }
            }
            sb.append("\n")
        }

        sb.append("### المطلوب:\n")
        sb.append("اكتب تقريراً إحصائياً متكاملاً باللغة العربية الفصحى وبشكل منسق للغاية بأسلوب النقاط والعناوين الفرعية، يتضمن:\n")
        sb.append("1. **ملخص تنفيذي**: ملخص شامل للنتائج والأرقام الإجمالية ونسبة جودة البيانات.\n")
        sb.append("2. **التحليل الكمي والوصفي**: تفسير للمتغيرات الأساسية ودلالات متوسطاتها الحسابية وانحرافاتها المعيارية.\n")
        if (crossTab != null) {
            sb.append("3. **تحليل العلاقات والارتباط**: تحليل طبيعة وقوة العلاقة بين المتغير المختار [${crossTab.xHeader}] والمتغير [${crossTab.yHeader}] (هل هي قوية، طردية، عكسية، دالة، الفروقات الرئيسية بين المجموعات).\n")
        }
        sb.append("4. **الاستنتاجات الإستراتيجية**: استخلاص الدلالات الكبرى، الفجوات المكتشفة، والاحتياجات الأكثر ظهوراً مع التوصيات المباشرة والعملية للتصرف الفوري بناءً على البيانات فقط.\n")
        sb.append("\nملاحظة هامة جداً: التزم باللغة الإحصائية والموضوعية الرسمية، ولا تختلق أي أرقام من عندك خارج ما هو مذكور أعلاه.")

        return sb.toString()
    }
}
