package com.example

import android.content.Context
import android.net.Uri
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    fun exportToExcelXml(
        context: Context,
        uri: Uri,
        result: AnalysisResult,
        crossTab: CrossTabulation?,
        aiSummaryText: String,
        conclusionsText: String
    ): Boolean {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return false
            val writer = OutputStreamWriter(outputStream, "UTF-8")

            // XML Header
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
            writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" +
                    " xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n" +
                    " xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n" +
                    " xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n" +
                    " xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")

            // Styles
            writer.write(" <Styles>\n" +
                    "  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n" +
                    "   <Alignment ss:Vertical=\"Center\" ss:Horizontal=\"Right\"/>\n" +
                    "   <Borders/>\n" +
                    "   <Font ss:FontName=\"Segoe UI\" ss:Size=\"11\" ss:Color=\"#333333\"/>\n" +
                    "   <Interior/>\n" +
                    "   <NumberFormat/>\n" +
                    "   <Protection/>\n" +
                    "  </Style>\n" +
                    "  <Style ss:ID=\"TitleStyle\">\n" +
                    "   <Font ss:FontName=\"Segoe UI\" ss:Size=\"16\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n" +
                    "   <Interior ss:Color=\"#0F4C75\" ss:Pattern=\"Solid\"/>\n" +
                    "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n" +
                    "  </Style>\n" +
                    "  <Style ss:ID=\"HeaderStyle\">\n" +
                    "   <Font ss:FontName=\"Segoe UI\" ss:Size=\"12\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n" +
                    "   <Interior ss:Color=\"#3282B8\" ss:Pattern=\"Solid\"/>\n" +
                    "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n" +
                    "  </Style>\n" +
                    "  <Style ss:ID=\"MetaLabel\">\n" +
                    "   <Font ss:FontName=\"Segoe UI\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#1B262C\"/>\n" +
                    "   <Interior ss:Color=\"#EAF2F8\" ss:Pattern=\"Solid\"/>\n" +
                    "   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n" +
                    "  </Style>\n" +
                    "  <Style ss:ID=\"CardHeader\">\n" +
                    "   <Font ss:FontName=\"Segoe UI\" ss:Size=\"12\" ss:Bold=\"1\" ss:Color=\"#0F4C75\"/>\n" +
                    "   <Interior ss:Color=\"#D4E6F1\" ss:Pattern=\"Solid\"/>\n" +
                    "   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n" +
                    "  </Style>\n" +
                    "  <Style ss:ID=\"CellTextCenter\">\n" +
                    "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n" +
                    "  </Style>\n" +
                    "  <Style ss:ID=\"CellBold\">\n" +
                    "   <Font ss:FontName=\"Segoe UI\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#0F4C75\"/>\n" +
                    "   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n" +
                    "  </Style>\n" +
                    " </Styles>\n")

            // SHEET 1: ملخص عام والمنهجية
            writeSummarySheet(writer, result, aiSummaryText)

            // SHEET 2: إحصاءات وصفية وتوزيعات كاملة
            writeDescriptiveSheet(writer, result)

            // SHEET 3: علاقات المتغيرات والتحليل المقارن
            writeRelationshipsSheet(writer, result, crossTab)

            // SHEET 4: الرسوم والبيانات
            writeRawDataSheet(writer, result)

            // SHEET 5: الاستنتاجات والتوصيات المباشرة
            writeConclusionsSheet(writer, conclusionsText)

            // XML Footer
            writer.write("</Workbook>\n")
            writer.flush()
            writer.close()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeSummarySheet(writer: OutputStreamWriter, result: AnalysisResult, aiSummaryText: String) {
        writer.write(" <Worksheet ss:Name=\"الملخص العام والمنهجية\">\n" +
                "  <Table ss:ExpandedColumnCount=\"6\">\n" +
                "   <Column ss:Width=\"150\"/>\n" +
                "   <Column ss:Width=\"180\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n")

        // Title
        writer.write("   <Row ss:Height=\"40\">\n" +
                "    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"TitleStyle\"><Data ss:Type=\"String\">تقرير التحليل الإحصائي المتكامل</Data></Cell>\n" +
                "   </Row>\n")

        // Subtitle / Date
        val currentDate = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())
        writer.write("   <Row ss:Height=\"20\">\n" +
                "    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">تم إنشاء هذا التقرير تلقائياً بتاريخ: $currentDate</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row ss:Height=\"10\"><Cell ss:MergeAcross=\"5\"/></Row>\n")

        // Meta Info Card
        writer.write("   <Row ss:Height=\"25\">\n" +
                "    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"CardHeader\"><Data ss:Type=\"String\">1. منهجية التحقق من جودة وصلاحية البيانات المرفوعة</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row>\n" +
                "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">اسم الملف الأصلي</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"4\"><Data ss:Type=\"String\">${result.fileName}</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row>\n" +
                "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">العدد الإجمالي للسجلات</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"4\" ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"Number\">${result.totalRecords}</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row>\n" +
                "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">عدد السجلات الصالحة (المكتملة)</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"4\" ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"Number\">${result.validRecords}</Data></Cell>\n" +
                "   </Row>\n")

        val percentageValid = if (result.totalRecords > 0) (result.validRecords.toDouble() / result.totalRecords) * 100.0 else 0.0
        writer.write("   <Row>\n" +
                "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">نسبة البيانات الصالحة للتحليل</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"4\" ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">${String.format(Locale.ENGLISH, "%.2f", percentageValid)}%</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row>\n" +
                "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">إجمالي المتغيرات (الأعمدة)</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"4\" ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"Number\">${result.columns.size}</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row ss:Height=\"15\"><Cell ss:MergeAcross=\"5\"/></Row>\n")

        // Executive summary
        writer.write("   <Row ss:Height=\"25\">\n" +
                "    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"CardHeader\"><Data ss:Type=\"String\">2. الملخص التنفيذي والإطار العام للدراسة (AI Insights)</Data></Cell>\n" +
                "   </Row>\n")

        // Write executive summary paragraphs
        val paragraphs = aiSummaryText.split("\n").filter { it.isNotBlank() }
        for (para in paragraphs) {
            writer.write("   <Row ss:Height=\"30\">\n" +
                    "    <Cell ss:MergeAcross=\"5\"><Data ss:Type=\"String\">$para</Data></Cell>\n" +
                    "   </Row>\n")
        }

        writer.write("  </Table>\n" +
                " </Worksheet>\n")
    }

    private fun writeDescriptiveSheet(writer: OutputStreamWriter, result: AnalysisResult) {
        writer.write(" <Worksheet ss:Name=\"الإحصاء الوصفي والتوزيعات\">\n" +
                "  <Table ss:ExpandedColumnCount=\"7\">\n" +
                "   <Column ss:Width=\"180\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n" +
                "   <Column ss:Width=\"80\"/>\n" +
                "   <Column ss:Width=\"80\"/>\n" +
                "   <Column ss:Width=\"80\"/>\n" +
                "   <Column ss:Width=\"80\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n")

        // Headers
        writer.write("   <Row ss:Height=\"30\">\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">المتغير (العمود)</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">نوع البيانات</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">المتوسط</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">الانحراف المعياري</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">الحد الأدنى</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">الحد الأقصى</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">القيم المفقودة</Data></Cell>\n" +
                "   </Row>\n")

        for (col in result.columns) {
            val typeStr = if (col.type == ColumnType.NUMERIC) "كمي / عددي" else "فئات / وصفي"
            val meanStr = col.mean?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "-"
            val stdDevStr = col.stdDev?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "-"
            val minStr = col.min?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "-"
            val maxStr = col.max?.let { String.format(Locale.ENGLISH, "%.2f", it) } ?: "-"

            writer.write("   <Row>\n" +
                    "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">${col.name}</Data></Cell>\n" +
                    "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">$typeStr</Data></Cell>\n" +
                    "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">$meanStr</Data></Cell>\n" +
                    "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">$stdDevStr</Data></Cell>\n" +
                    "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">$minStr</Data></Cell>\n" +
                    "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">$maxStr</Data></Cell>\n" +
                    "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"Number\">${col.missingCount}</Data></Cell>\n" +
                    "   </Row>\n")
        }

        writer.write("   <Row ss:Height=\"20\"><Cell ss:MergeAcross=\"6\"/></Row>\n")

        // Detailed Frequencies Category breakdown
        writer.write("   <Row ss:Height=\"25\">\n" +
                "    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"CardHeader\"><Data ss:Type=\"String\">توزيع تكرارات ونسب المتغيرات الفئوية</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row ss:Height=\"25\">\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">المتغير الأساسي</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"2\" ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">الفئة / الاختيار</Data></Cell>\n" +
                "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">التكرار (العدد)</Data></Cell>\n" +
                "    <Cell ss:MergeAcross=\"1\" ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">النسبة المئوية</Data></Cell>\n" +
                "   </Row>\n")

        for (col in result.columns) {
            if (col.type == ColumnType.CATEGORICAL && col.frequencies.isNotEmpty()) {
                var isFirst = true
                for (freq in col.frequencies) {
                    val nameCell = if (isFirst) {
                        "<Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">${col.name}</Data></Cell>"
                    } else {
                        "<Cell/>"
                    }
                    isFirst = false

                    writer.write("   <Row>\n" +
                            "    $nameCell\n" +
                            "    <Cell ss:MergeAcross=\"2\"><Data ss:Type=\"String\">${freq.category}</Data></Cell>\n" +
                            "    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"Number\">${freq.count}</Data></Cell>\n" +
                            "    <Cell ss:MergeAcross=\"1\" ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">${String.format(Locale.ENGLISH, "%.2f", freq.percentage)}%</Data></Cell>\n" +
                            "   </Row>\n")
                }
                writer.write("   <Row ss:Height=\"5\"><Cell ss:MergeAcross=\"6\"/></Row>\n")
            }
        }

        writer.write("  </Table>\n" +
                " </Worksheet>\n")
    }

    private fun writeRelationshipsSheet(writer: OutputStreamWriter, result: AnalysisResult, crossTab: CrossTabulation?) {
        writer.write(" <Worksheet ss:Name=\"علاقات المتغيرات والتحليل\">\n" +
                "  <Table ss:ExpandedColumnCount=\"7\">\n" +
                "   <Column ss:Width=\"150\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n" +
                "   <Column ss:Width=\"100\"/>\n")

        if (crossTab == null) {
            writer.write("   <Row ss:Height=\"40\">\n" +
                    "    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"CardHeader\"><Data ss:Type=\"String\">لم يتم تحديد أو تحليل علاقة بين متغيرين في نموذج التقاطع بعد.</Data></Cell>\n" +
                    "   </Row>\n")
        } else {
            // Title card
            writer.write("   <Row ss:Height=\"25\">\n" +
                    "    <Cell ss:MergeAcross=\"6\" ss:StyleID=\"CardHeader\"><Data ss:Type=\"String\">جدول التقاطع (Cross Tabulation) لدراسة الارتباط</Data></Cell>\n" +
                    "   </Row>\n")

            writer.write("   <Row>\n" +
                    "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">المتغير المستقل (X)</Data></Cell>\n" +
                    "    <Cell ss:MergeAcross=\"5\"><Data ss:Type=\"String\">${crossTab.xHeader}</Data></Cell>\n" +
                    "   </Row>\n")

            writer.write("   <Row>\n" +
                    "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">المتغير التابع (Y)</Data></Cell>\n" +
                    "    <Cell ss:MergeAcross=\"5\"><Data ss:Type=\"String\">${crossTab.yHeader}</Data></Cell>\n" +
                    "   </Row>\n")

            writer.write("   <Row ss:Height=\"15\"><Cell ss:MergeAcross=\"6\"/></Row>\n")

            // Headers for CrossTab Table
            writer.write("   <Row ss:Height=\"25\">\n" +
                    "    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">${crossTab.xHeader} (X) \\ ${crossTab.yHeader} (Y)</Data></Cell>\n")

            for (yCat in crossTab.yCategories) {
                writer.write("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">$yCat</Data></Cell>\n")
            }
            writer.write("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">الإجمالي</Data></Cell>\n")
            writer.write("   </Row>\n")

            // Rows
            for (row in crossTab.rows) {
                writer.write("   <Row>\n" +
                        "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">${row.xValue}</Data></Cell>\n")

                for (yCat in crossTab.yCategories) {
                    val count = row.yCounts[yCat] ?: 0
                    val pct = row.yPercentages[yCat] ?: 0.0
                    val cellText = "$count (${String.format(Locale.ENGLISH, "%.1f", pct)}%)"
                    writer.write("    <Cell ss:StyleID=\"CellTextCenter\"><Data ss:Type=\"String\">$cellText</Data></Cell>\n")
                }
                writer.write("    <Cell ss:StyleID=\"CellBold\"><Data ss:Type=\"Number\">${row.rowTotal}</Data></Cell>\n")
                writer.write("   </Row>\n")
            }

            // Totals Row
            writer.write("   <Row>\n" +
                    "    <Cell ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">الإجمالي الكلي</Data></Cell>\n")
            for (yCat in crossTab.yCategories) {
                val total = crossTab.columnTotals[yCat] ?: 0
                writer.write("    <Cell ss:StyleID=\"CellBold\"><Data ss:Type=\"Number\">$total</Data></Cell>\n")
            }
            writer.write("    <Cell ss:StyleID=\"TitleStyle\"><Data ss:Type=\"Number\">${crossTab.grandTotal}</Data></Cell>\n")
            writer.write("   </Row>\n")
        }

        writer.write("  </Table>\n" +
                " </Worksheet>\n")
    }

    private fun writeRawDataSheet(writer: OutputStreamWriter, result: AnalysisResult) {
        val maxCols = result.rawHeaders.size
        writer.write(" <Worksheet ss:Name=\"جداول البيانات والرسوم\">\n" +
                "  <Table>\n")

        for (col in result.rawHeaders) {
            writer.write("   <Column ss:Width=\"120\"/>\n")
        }

        // Headers
        writer.write("   <Row ss:Height=\"25\">\n")
        for (header in result.rawHeaders) {
            writer.write("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">$header</Data></Cell>\n")
        }
        writer.write("   </Row>\n")

        // Rows (limit to first 200 for clean preview size inside spreadsheet)
        val exportRows = result.rawRows.take(500)
        for (row in exportRows) {
            writer.write("   <Row>\n")
            for (cellVal in row) {
                // Check if numeric
                val numVal = cellVal.toDoubleOrNull()
                if (numVal != null) {
                    writer.write("    <Cell><Data ss:Type=\"Number\">$numVal</Data></Cell>\n")
                } else {
                    writer.write("    <Cell><Data ss:Type=\"String\">$cellVal</Data></Cell>\n")
                }
            }
            writer.write("   </Row>\n")
        }

        writer.write("  </Table>\n" +
                " </Worksheet>\n")
    }

    private fun writeConclusionsSheet(writer: OutputStreamWriter, conclusionsText: String) {
        writer.write(" <Worksheet ss:Name=\"الاستنتاجات والتوصيات\">\n" +
                "  <Table ss:ExpandedColumnCount=\"6\">\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n" +
                "   <Column ss:Width=\"120\"/>\n")

        writer.write("   <Row ss:Height=\"30\">\n" +
                "    <Cell ss:MergeAcross=\"5\" ss:StyleID=\"TitleStyle\"><Data ss:Type=\"String\">الاستنتاجات الإستراتيجية والتوصيات المباشرة</Data></Cell>\n" +
                "   </Row>\n")

        writer.write("   <Row ss:Height=\"15\"><Cell ss:MergeAcross=\"5\"/></Row>\n")

        val sections = conclusionsText.split("\n").filter { it.isNotBlank() }
        for (section in sections) {
            writer.write("   <Row ss:Height=\"35\">\n" +
                    "    <Cell ss:MergeAcross=\"5\"><Data ss:Type=\"String\">$section</Data></Cell>\n" +
                    "   </Row>\n")
        }

        writer.write("  </Table>\n" +
                " </Worksheet>\n")
    }
}
