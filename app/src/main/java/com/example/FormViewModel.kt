package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class WebCommand {
    object RequestSaveValues : WebCommand()
    data class ApplyValues(val jsonValues: String) : WebCommand()
}

class FormViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("kobo_autofill_prefs", Context.MODE_PRIVATE)
    
    // Defaults
    private val defaultUrl = "https://ee-kobo.drc.ngo/x/y8MTPWup"
    
    // States
    private val _currentUrl = MutableStateFlow(sharedPrefs.getString("current_url", defaultUrl) ?: defaultUrl)
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    
    private val _isAutoFillEnabled = MutableStateFlow(sharedPrefs.getBoolean("autofill_enabled", true))
    val isAutoFillEnabled: StateFlow<Boolean> = _isAutoFillEnabled.asStateFlow()
    
    private val _templates = MutableStateFlow<List<FormTemplate>>(emptyList())
    val templates: StateFlow<List<FormTemplate>> = _templates.asStateFlow()
    
    private val _activeTemplateId = MutableStateFlow<String?>(sharedPrefs.getString("active_template_id", null))
    val activeTemplateId: StateFlow<String?> = _activeTemplateId.asStateFlow()
    
    // Events sent to WebView
    private val _webCommandFlow = MutableSharedFlow<WebCommand>(extraBufferCapacity = 1)
    val webCommandFlow: SharedFlow<WebCommand> = _webCommandFlow.asSharedFlow()
    
    // Message overlays
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        try {
            val json = sharedPrefs.getString("saved_templates", null)
            if (json != null) {
                val list = mutableListOf<FormTemplate>()
                val jsonArray = org.json.JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        FormTemplate(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            valuesJson = obj.getString("valuesJson"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _templates.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTemplatesToPrefs(list: List<FormTemplate>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (template in list) {
                val obj = org.json.JSONObject().apply {
                    put("id", template.id)
                    put("name", template.name)
                    put("valuesJson", template.valuesJson)
                    put("createdAt", template.createdAt)
                }
                jsonArray.put(obj)
            }
            val json = jsonArray.toString()
            sharedPrefs.edit().putString("saved_templates", json).apply()
            _templates.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateUrl(url: String) {
        val cleanUrl = if (url.trim().startsWith("http://") || url.trim().startsWith("https://")) {
            url.trim()
        } else {
            "https://" + url.trim()
        }
        _currentUrl.value = cleanUrl
        sharedPrefs.edit().putString("current_url", cleanUrl).apply()
    }

    fun resetUrlToDefault() {
        updateUrl(defaultUrl)
    }

    fun setAutoFillEnabled(enabled: Boolean) {
        _isAutoFillEnabled.value = enabled
        sharedPrefs.edit().putBoolean("autofill_enabled", enabled).apply()
    }

    fun setActiveTemplate(templateId: String?) {
        _activeTemplateId.value = templateId
        sharedPrefs.edit().putString("active_template_id", templateId).apply()
        
        // If a template is loaded and auto-fill is active, let's request to apply it!
        if (templateId != null) {
            getTemplateById(templateId)?.let {
                applyTemplateToWeb(it.valuesJson)
            }
        }
    }

    fun getTemplateById(id: String): FormTemplate? {
        return _templates.value.find { it.id == id }
    }

    fun requestSaveValues() {
        _webCommandFlow.tryEmit(WebCommand.RequestSaveValues)
    }

    fun applyTemplateToWeb(jsonValues: String) {
        _webCommandFlow.tryEmit(WebCommand.ApplyValues(jsonValues))
    }

    fun saveNewTemplate(name: String, valuesJson: String) {
        val newTemplate = FormTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            valuesJson = valuesJson
        )
        val updated = _templates.value.toMutableList()
        updated.add(newTemplate)
        saveTemplatesToPrefs(updated)
        
        // Auto-select the newly created template
        setActiveTemplate(newTemplate.id)
        showToast("✓ تم حفظ القالب الجديد: ${newTemplate.name}")
    }

    fun deleteTemplate(id: String) {
        val name = getTemplateById(id)?.name ?: ""
        val updated = _templates.value.filter { it.id != id }
        saveTemplatesToPrefs(updated)
        
        if (_activeTemplateId.value == id) {
            setActiveTemplate(null)
        }
        showToast("🗑 تم حذف القالب: $name")
    }

    fun showToast(message: String?) {
        _toastMessage.value = message
    }

    fun handleReceivedValues(valuesJson: String) {
        if (valuesJson == "{}" || valuesJson.isBlank()) {
            showToast("⚠️ لا توجد بيانات مكتوبة في الاستمارة لحفظها!")
            return
        }
        // This is called when Javascript has scraped current inputs in WebView and sent them back to Kotlin
        // We will notify the UI to trigger a save dialog name input
        _saveDialogTrigger.value = valuesJson
    }

    private val _saveDialogTrigger = MutableStateFlow<String?>(null)
    val saveDialogTrigger: StateFlow<String?> = _saveDialogTrigger.asStateFlow()

    fun clearSaveDialogTrigger() {
        _saveDialogTrigger.value = null
    }
}
