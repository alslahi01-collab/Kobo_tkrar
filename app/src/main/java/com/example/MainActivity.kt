package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: FormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    StatisticalMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticalMainScreen(viewModel: FormViewModel) {
    val context = LocalContext.current
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isGeneratingInsights by viewModel.isGeneratingInsights.collectAsState()
    val selectedXHeader by viewModel.selectedXHeader.collectAsState()
    val selectedYHeader by viewModel.selectedYHeader.collectAsState()
    val crossTabulation by viewModel.crossTabulation.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val historicalReports by viewModel.historicalReports.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Sync state for toasts
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.showToast(null)
        }
    }

    // Excel exporter callback contract
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            viewModel.exportExcelReport(uri) { success ->
                if (success) {
                    viewModel.showToast("✓ تم تصدير تقرير إكسل المتكامل بنجاح!")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F4C75))
                    .padding(top = 40.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Stats Logo",
                            tint = Color(0xFFFBE3B5),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المحلل الإحصائي الذكي",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = analysisResult?.fileName ?: "يرجى تحميل ملف استبيان أو مسح ميداني",
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "مساعدة",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = "الملفات") },
                    label = { Text("البيانات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F4C75),
                        selectedTextColor = Color(0xFF0F4C75),
                        indicatorColor = Color(0xFFEAF2F8)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    enabled = analysisResult != null,
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "الإحصاء الوصفي") },
                    label = { Text("إحصاء وصفي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F4C75),
                        selectedTextColor = Color(0xFF0F4C75),
                        indicatorColor = Color(0xFFEAF2F8)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    enabled = analysisResult != null,
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "العلاقات") },
                    label = { Text("دراسة العلاقات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F4C75),
                        selectedTextColor = Color(0xFF0F4C75),
                        indicatorColor = Color(0xFFEAF2F8)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    enabled = analysisResult != null,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "تحليل الذكاء الاصطناعي") },
                    label = { Text("تقرير ذكي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0F4C75),
                        selectedTextColor = Color(0xFF0F4C75),
                        indicatorColor = Color(0xFFEAF2F8)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F7FA))
        ) {
            when (activeTab) {
                0 -> UploadAndHistoryTab(
                    viewModel = viewModel,
                    historicalReports = historicalReports,
                    isAnalyzing = isAnalyzing,
                    analysisResult = analysisResult,
                    onStartAnalysis = { activeTab = 1 }
                )
                1 -> DescriptiveTab(
                    result = analysisResult,
                    onExportClick = {
                        val defaultName = "تقرير_إحصائي_${analysisResult?.fileName?.substringBeforeLast('.') ?: "دراسة"}.xls"
                        exportDocumentLauncher.launch(defaultName)
                    }
                )
                2 -> RelationshipsTab(
                    result = analysisResult,
                    selectedXHeader = selectedXHeader,
                    selectedYHeader = selectedYHeader,
                    crossTabulation = crossTabulation,
                    onHeadersChanged = { x, y -> viewModel.setCrossTabHeaders(x, y) }
                )
                3 -> AIReportTab(
                    result = analysisResult,
                    aiInsights = aiInsights,
                    isGenerating = isGeneratingInsights,
                    onGenerateClick = { viewModel.generateAISegments() },
                    onExportClick = {
                        val defaultName = "تقرير_إحصائي_متكامل_${analysisResult?.fileName?.substringBeforeLast('.') ?: "دراسة"}.xls"
                        exportDocumentLauncher.launch(defaultName)
                    }
                )
            }
        }
    }

    if (showInfoDialog) {
        SystemInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

// --- TAB 0: Upload & History Management ---

@Composable
fun UploadAndHistoryTab(
    viewModel: FormViewModel,
    historicalReports: List<ReportMeta>,
    isAnalyzing: Boolean,
    analysisResult: AnalysisResult?,
    onStartAnalysis: () -> Unit
) {
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeFile(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upload Button Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload icon",
                        tint = Color(0xFF0F4C75),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "رفع وتحليل استمارة أو مسح ميداني",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B262C)
                    )
                    Text(
                        text = "ادعم صيغ ملفات إكسل (XLSX) أو ملفات CSV لتشغيل محرك التحليل الإحصائي الإستراتيجي تلقائياً وبأقصى دقة.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color(0xFF0F4C75))
                        Text("جاري معالجة وفلترة السجلات وحساب الانحرافات...", fontSize = 12.sp, color = Color(0xFF0F4C75))
                    } else {
                        Button(
                            onClick = { fileChooserLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C75)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("upload_file_button")
                        ) {
                            Text("اختر ملف من جهازك 📂", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Active File summary card if present
        if (analysisResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3282B8))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الملف النشط حالياً: ${analysisResult.fileName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F4C75)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "إجمالي السجلات: ${analysisResult.totalRecords} | الصالحة: ${analysisResult.validRecords}",
                                fontSize = 12.sp,
                                color = Color(0xFF1B262C)
                            )
                        }
                        Button(
                            onClick = onStartAnalysis,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3282B8)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("عرض التحليلات 📊", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Historical Reports title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF1B262C))
                Text(
                    text = "سجل الدراسات المرفوعة سابقاً (${historicalReports.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B262C)
                )
            }
        }

        if (historicalReports.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "لا توجد تقارير سابقة حالياً",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(historicalReports) { report ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.clickable { viewModel.loadHistoricalReport(report) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.fileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B262C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سجلات صالحة: ${report.validRecords} | تاريخ: ${formatArabicDate(report.timestamp)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.deleteHistoricalReport(report.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFC0392B)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 1: Descriptive Statistics & Complete Distributions ---

@Composable
fun DescriptiveTab(
    result: AnalysisResult?,
    onExportClick: () -> Unit
) {
    if (result == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Descriptive Summary top card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "الإحصاء الوصفي والتوزيع التكراري",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C75)
                )
                Text(
                    text = "تحديد الانحرافات، المتوسطات، وتفاصيل الفئات تلقائياً",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير التقرير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(result.columns) { col ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header with Type Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = col.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B262C),
                                modifier = Modifier.weight(1f)
                            )
                            val (tagText, tagColor) = if (col.type == ColumnType.NUMERIC) {
                                Pair("عددي / كمي", Color(0xFF0F4C75))
                            } else {
                                Pair("وصفي / فئات", Color(0xFF2980B9))
                            }
                            Text(
                                text = tagText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(tagColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        if (col.type == ColumnType.NUMERIC) {
                            // Quantitative calculations block
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatBox(label = "المتوسط", value = String.format("%.2f", col.mean ?: 0.0), modifier = Modifier.weight(1f))
                                StatBox(label = "الإنحراف", value = String.format("%.2f", col.stdDev ?: 0.0), modifier = Modifier.weight(1f))
                                StatBox(label = "الوسيط", value = String.format("%.2f", col.median ?: 0.0), modifier = Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatBox(label = "الحد الأدنى", value = String.format("%.1f", col.min ?: 0.0), modifier = Modifier.weight(1f))
                                StatBox(label = "الحد الأقصى", value = String.format("%.1f", col.max ?: 0.0), modifier = Modifier.weight(1f))
                                StatBox(label = "القيم المفقودة", value = col.missingCount.toString(), modifier = Modifier.weight(1f))
                            }
                        } else {
                            // Categorical distributions and charts
                            Text(
                                text = "التوزيع التكراري للفئات الأبرز:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )

                            // Render Custom Native Bar Chart
                            BarChart(data = col.frequencies)

                            if (col.lowFrequencyAlerts.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF9E7), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFD35400),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "فئات نادرة أو مفقودة (تكرار أقل من 5%):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD35400)
                                        )
                                        col.lowFrequencyAlerts.take(2).forEach {
                                            Text(text = "• $it", fontSize = 10.sp, color = Color(0xFFD35400))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F4C75))
        }
    }
}

// --- TAB 2: Relationships & Cross-tabulation Matrix ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipsTab(
    result: AnalysisResult?,
    selectedXHeader: String?,
    selectedYHeader: String?,
    crossTabulation: CrossTabulation?,
    onHeadersChanged: (String, String) -> Unit
) {
    if (result == null) return

    var xExpanded by remember { mutableStateOf(false) }
    var yExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "تحليل العلاقات والجداول التقاطعية",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F4C75)
            )
            Text(
                text = "قارن وافهم الترابط المشترك والنسب بين أي متغيرين",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // Selected Variables Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dropdown for Column X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "المتغير المستقل (X):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B262C),
                        modifier = Modifier.width(130.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = xExpanded,
                        onExpandedChange = { xExpanded = !xExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedXHeader ?: "اختر عمود",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = xExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = xExpanded,
                            onDismissRequest = { xExpanded = false }
                        ) {
                            result.rawHeaders.forEach { header ->
                                DropdownMenuItem(
                                    text = { Text(header) },
                                    onClick = {
                                        onHeadersChanged(header, selectedYHeader ?: result.rawHeaders.getOrNull(1) ?: header)
                                        xExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Dropdown for Column Y
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "المتغير التابع (Y):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B262C),
                        modifier = Modifier.width(130.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = yExpanded,
                        onExpandedChange = { yExpanded = !yExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedYHeader ?: "اختر عمود",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = yExpanded,
                            onDismissRequest = { yExpanded = false }
                        ) {
                            result.rawHeaders.forEach { header ->
                                DropdownMenuItem(
                                    text = { Text(header) },
                                    onClick = {
                                        onHeadersChanged(selectedXHeader ?: result.rawHeaders.getOrNull(0) ?: header, header)
                                        yExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cross Tab Matrix Results
        if (crossTabulation != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "جدول التقاطع المشترك والنسب المئوية (X vs Y):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C75)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Render custom table
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            // Column headers row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF3282B8), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${selectedXHeader} \\ ${selectedYHeader}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.2f)
                                )
                                crossTabulation.yCategories.take(3).forEach { yCat ->
                                    Text(
                                        text = yCat,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "إجمالي",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }

                            // Data rows
                            crossTabulation.rows.forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = row.xValue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B262C),
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    crossTabulation.yCategories.take(3).forEach { yCat ->
                                        val count = row.yCounts[yCat] ?: 0
                                        val pct = row.yPercentages[yCat] ?: 0.0
                                        Text(
                                            text = "$count (${String.format(Locale.ENGLISH, "%.1f", pct)}%)",
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF0F4C75),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = row.rowTotal.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF1B262C),
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                            }
                        }
                    }

                    item {
                        Text(
                            text = "💡 يوضح الجدول أعلاه توزيع فئات المتغير المستقل ومساهمتها المئوية النسبية في المتغير التابع بالتناسب الكلي.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 3: AI-Powered Insights & Executive Reports ---

@Composable
fun AIReportTab(
    result: AnalysisResult?,
    aiInsights: String?,
    isGenerating: Boolean,
    onGenerateClick: () -> Unit,
    onExportClick: () -> Unit
) {
    if (result == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "التقرير والتحليل الإحصائي الذكي",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C75)
                )
                Text(
                    text = "توليد استنتاجات وتوصيات عملية معتمدة على البيانات",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (aiInsights == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFFF1C40F),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاهز لتوليد التقرير الذكي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B262C)
                        )
                        Text(
                            text = "سيقوم نظام الذكاء الاصطناعي بقراءة وتدقيق المتوسطات، العلاقات التقاطعية واستنتاج الفجوات والاحتياجات مباشرة بلغة رسمية وموضوعية تماماً.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isGenerating) {
                            CircularProgressIndicator(color = Color(0xFF0F4C75))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("جاري قراءة الأرقام وبناء هيكلية التقرير...", fontSize = 12.sp, color = Color(0xFF0F4C75))
                        } else {
                            Button(
                                onClick = onGenerateClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C75)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("generate_report_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("توليد التقرير الإستراتيجي الآن ⚡", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الاستنتاجات والتوصيات المولدة:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C75)
                        )
                        Button(
                            onClick = onExportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير لإكسل 5 أوراق", fontSize = 12.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = aiInsights,
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFF1B262C),
                                modifier = Modifier
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Dialogue Panels ---

@Composable
fun SystemInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ℹ️ نظام التحليل الإحصائي المتكامل والمرن",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C75)
                )
                Text(
                    text = "تم بناء هذا التطبيق ليعمل بكفاءة مطلقة ودون الارتباط بقطاع إنساني محدد، حيث يوفر:\n\n" +
                            "1️⃣ قراءة وتطهير فوري للبيانات من أي ملف إكسل أو CSV.\n\n" +
                            "2️⃣ حساب مباشر للإحصاء الوصفي (المتوسط، الانحراف المعياري، الوسيط، القيم الصغرى والعظمى) والتوزيع الفئوي للمسوح الميدانية.\n\n" +
                            "3️⃣ محرك جداول تقاطعية لدراسة العلاقات المشتركة بين المتغيرات وتوليد نسب مشتركة.\n\n" +
                            "4️⃣ مخرجات ذكية وصياغات إنسانية بليغة للتقارير والتوصيات مدعومة بذكاء اصطناعي آمن، مع إمكانية تصديرها بالكامل إلى ملف إكسل معتمد ومقسم إلى 5 أوراق عمل متناسقة الألوان.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF333333)
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C75)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("فهمت الطريقة 👍", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Helper Functions ---

@Composable
fun BarChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    val maxCount = data.maxOfOrNull { it.count } ?: 1
    val colors = listOf(Color(0xFF0F4C75), Color(0xFF3282B8))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.take(6).forEachIndexed { idx, item ->
            val fraction = item.count.toFloat() / maxCount
            val barColor = colors[idx % colors.size]
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.category.ifBlank { "(فارغ)" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B262C)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor)
                        )
                    }
                    Text(
                        text = "${item.count} (${String.format(Locale.ENGLISH, "%.1f", item.percentage)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F4C75),
                        modifier = Modifier.widthIn(min = 60.dp)
                    )
                }
            }
        }
    }
}

fun formatArabicDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "غير معروف"
    }
}
