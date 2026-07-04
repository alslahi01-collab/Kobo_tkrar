package com.example

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: FormViewModel by viewModels()
    private var webViewRef: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Force RTL (Right to Left) for Arabic UI layout naturally
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FormMainScreen(viewModel = viewModel, onWebViewReady = { webViewRef = it })
                }
            }
        }

        // Listen for internal state event triggers to execute scripts directly inside active WebView
        lifecycleScope.launch {
            viewModel.webCommandFlow.collectLatest { command ->
                val webView = webViewRef
                if (webView != null) {
                    when (command) {
                        is WebCommand.RequestSaveValues -> {
                            requestScrapeFormValues(webView)
                        }
                        is WebCommand.ApplyValues -> {
                            injectAutoFillScripts(webView, command.jsonValues)
                        }
                    }
                } else {
                    viewModel.showToast("⚠️ الرجاء الانتظار حتى اكتمال تحميل الصفحة!")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormMainScreen(
    viewModel: FormViewModel,
    onWebViewReady: (WebView) -> Unit
) {
    val context = LocalContext.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isAutoFillEnabled by viewModel.isAutoFillEnabled.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val activeTemplateId by viewModel.activeTemplateId.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val saveDialogTrigger by viewModel.saveDialogTrigger.collectAsState()

    var webProgress by remember { mutableIntStateOf(0) }
    var showUrlConfig by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Toast and messages sync
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.showToast(null) // Reset
        }
    }

    // Active Template info
    val activeTemplate = templates.find { it.id == activeTemplateId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F4C75)) // Theme Deep Blue Header
                    .padding(top = 40.dp) // Offset for edge-to-edge statusbar
            ) {
                // Main Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sparkles / Magic Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Magic Icon",
                            tint = Color(0xFFFBE3B5),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مساعد استمارات KoBo",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeTemplate != null) "القالب المفّعل: ${activeTemplate.name}" else "لا يوجد قالب نشط حالياً",
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp
                        )
                    }

                    // Top Action Icons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.testTag("info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "معلومات",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showUrlConfig = !showUrlConfig },
                            modifier = Modifier.testTag("toggle_url_config_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "تغيير الرابط",
                                tint = if (showUrlConfig) Color(0xFF3282B8) else Color.White
                            )
                        }
                    }
                }

                // Smooth Linear Loading Progress Indicator
                AnimatedVisibility(
                    visible = webProgress < 100,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress = webProgress.toFloat() / 100f,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF3282B8),
                        trackColor = Color(0xFF0F4C75)
                    )
                }

                // Collapsible URL Input Row
                AnimatedVisibility(
                    visible = showUrlConfig,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    UrlConfigBar(
                        currentUrl = currentUrl,
                        onUrlSaved = { newUrl ->
                            viewModel.updateUrl(newUrl)
                            showUrlConfig = false
                        },
                        onResetDefault = {
                            viewModel.resetUrlToDefault()
                            showUrlConfig = false
                        }
                    )
                }
            }
        },
        bottomBar = {
            // Elegant persistence status control and actions bar at the bottom
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        // Safety padding to prevent gesture bottom bar overlap on newer Android versions
                        .padding(bottom = 16.dp)
                ) {
                    // Row 1: Configurations Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Auto Fill Icon",
                                tint = if (isAutoFillEnabled) Color(0xFF0F4C75) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "التعبئة التلقائية الذكية",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1E21)
                                )
                                Text(
                                    text = "تعبئة الخيارات فور ظهورها في النموذج",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Switch(
                            checked = isAutoFillEnabled,
                            onCheckedChange = { viewModel.setAutoFillEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0F4C75),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color(0x339E9E9E)
                            ),
                            modifier = Modifier.testTag("autofill_switch")
                        )
                    }

                    HorizontalDivider(color = Color(0x11000000), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Safe current input defaults
                        Button(
                            onClick = { viewModel.requestSaveValues() },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("save_defaults_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F4C75),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "حفظ كإفتراضي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Apply current template values manually
                        OutlinedButton(
                            onClick = {
                                if (activeTemplate != null) {
                                    viewModel.applyTemplateToWeb(activeTemplate.valuesJson)
                                    viewModel.showToast("⚡ تم تطبيق قيم القالب: ${activeTemplate.name}")
                                } else {
                                    viewModel.showToast("⚠️ يرجى تحديد قالب نشط أولاً!")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("apply_defaults_button"),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF0F4C75)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "تطبيق الآن", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Show Templates Manager Dialog Panel
                        Button(
                            onClick = { showTemplatesSheet = true },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("templates_manager_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF3F7FA), // Light premium card
                                contentColor = Color(0xFF0F4C75)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (activeTemplate != null) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF0F4C75)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إدارة القوالب (${templates.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF2F5F8)) // Subtle, non-intrusive slate background
        ) {
            WebViewContainer(
                url = currentUrl,
                viewModel = viewModel,
                onProgressChanged = { webProgress = it },
                onWebViewCreated = { onWebViewReady(it) }
            )
        }
    }

    // Modal Bottom Sheet: Templates Management Table
    if (showTemplatesSheet) {
        TemplatesManagerBottomSheet(
            viewModel = viewModel,
            templates = templates,
            activeTemplateId = activeTemplateId,
            onDismiss = { showTemplatesSheet = false }
        )
    }

    // Alert Dialog: Type Name for New Template Save Flow
    saveDialogTrigger?.let { valuesJson ->
        SaveTemplateNameDialog(
            onSave = { name ->
                viewModel.saveNewTemplate(name, valuesJson)
                viewModel.clearSaveDialogTrigger()
            },
            onDismiss = {
                viewModel.clearSaveDialogTrigger()
            }
        )
    }

    // Info Dialog Panel
    if (showInfoDialog) {
        InstructionalInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

@Composable
fun UrlConfigBar(
    currentUrl: String,
    onUrlSaved: (String) -> Unit,
    onResetDefault: () -> Unit
) {
    var rawInput by remember { mutableStateOf(currentUrl) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B262C)) // Contrast background for toolbar configs
            .padding(16.dp)
    ) {
        Text(
            text = "رابط استمارة KoBo النشط:",
            fontSize = 13.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("url_input_field"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3282B8),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x33FFFFFF)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onUrlSaved(rawInput) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3282B8),
                    contentColor = Color.White
                )
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💡 يمكنك كتابة أي رابط استمارة لمسحها وحفظ مدخلاتها.",
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = "استعادة الرابط الاصلي (DRC)",
                color = Color(0xFF3282B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onResetDefault() }
                    .padding(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesManagerBottomSheet(
    viewModel: FormViewModel,
    templates: List<FormTemplate>,
    activeTemplateId: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp) // Margin for bottom navigation bar
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "مدير القوالب التلقائية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F4C75)
                    )
                    Text(
                        text = "اختر أحد القوالب لتعبئة الاستمارة وتجهيزها",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "اغلاق")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0x11000000))
            Spacer(modifier = Modifier.height(16.dp))

            if (templates.isEmpty()) {
                // Empty State illustration description
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد قوالب مخزنة بعد",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "املأ الاستمارة ثم اضغط 'حفظ كإفتراضي' لإنشاء أول قالب لك.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    items(templates) { template ->
                        val isActive = template.id == activeTemplateId
                        val fieldCount = remember(template.valuesJson) {
                            try {
                                // Dynamic count of items in the JSON map
                                val trimmed = template.valuesJson.trim()
                                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                                    val contentList = trimmed.substring(1, trimmed.length - 1).split(",")
                                    if (contentList.firstOrNull()?.isBlank() == true) 0 else contentList.size
                                } else 0
                            } catch (e: Exception) {
                                0
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActiveTemplate(template.id)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) Color(0xFFE3F2FD) else Color(0xFFF5F9FC)
                            ),
                            border = if (isActive) {
                                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0F4C75))
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isActive) Color(0xFF0F4C75) else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = template.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) Color(0xFF0F4C75) else Color(0xFF1C1E21)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "تم الحفظ بـ: ${formatArabicDate(template.createdAt)} | عدد الحقول: $fieldCount",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isActive) {
                                        Text(
                                            text = "نشط",
                                            color = Color(0xFF0F4C75),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color(0xFFBBDEFB), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteTemplate(template.id) },
                                        modifier = Modifier.testTag("delete_template_btn_${template.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف قالب",
                                            tint = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.setActiveTemplate(null)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_selection_button"),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء تفعيل القوالب النشطة (تعبئة يدوية)", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SaveTemplateNameDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "💾 حفظ البيانات الافتراضية",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C75)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "تم التقاط وقراءة المدخلات والخيارات التي ملأتها في الاستمارة بنجاح! الرجاء إدخال اسم لهذا القالب لتتمكن من تعبئته في المرات القادمة بضغطة زر.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = templateName,
                    onValueChange = {
                        templateName = it
                        hasError = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("template_name_input"),
                    label = { Text("اسم القالب (مثال: بيانات الضالع - فريق A)") },
                    shape = RoundedCornerShape(8.dp),
                    isError = hasError,
                    supportingText = {
                        if (hasError) {
                            Text("الرجاء كتابة اسم لحفظ القالب!", color = Color.Red)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("إلغاء", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (templateName.trim().isNotBlank()) {
                                onSave(templateName.trim())
                            } else {
                                hasError = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C75)),
                        modifier = Modifier.testTag("confirm_save_template_btn")
                    ) {
                        Text("حفظ القالب", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionalInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "⚙️ طريقة عمل مساعد الاستمارة الذكي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F4C75)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "صُمم هذا التطبيق ومُهيأ لتسهيل وتوفير وقت الباحثين الميدانيين في ملء الاستمارات الطويلة والمتشابهة بشكل يومي عبر الخطوات التالية:\n\n" +
                            "1️⃣ افتح النموذج الميداني ونظم خياراتك الأساسية والأجوبة المتكررة (مثل: اسم الباحث، اسم المديرية، الموقع، جهة الاتصال والخيارات المكررة).\n\n" +
                            "2️⃣ اضغط على زر 'حفظ كإفتراضي' بالأسفل. سيقرأ مساعد الاستمارة كافة خياراتك الملية ويطلب منك إدخال اسم لتخزينها كمسودة على التطبيق!\n\n" +
                            "3️⃣ عند فتح استمارة جديدة فارغة لاحقًا، وبمجرد تشغيل زر 'التعرّف الذكي على الحقول' الفعّال تلقائياً، ستتم تعبئة جميع الأجوبة المحفوظة مسبقًا بدقة فائقة وفورياً! مما يسمح لك فقط باستكمال البيانات المتغيرة (مثل: الرقم التعريفي الجديد، اسم المستفيد المتغير) ثم الإرسال بشكل طبيعي تماماً 🚀.",
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(20.dp))
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

fun formatArabicDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "غير معروف"
    }
}
