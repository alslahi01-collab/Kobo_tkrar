package com.example

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class WebAppInterface(private val onValuesReceived: (String) -> Unit) {
    @JavascriptInterface
    fun sendValues(jsonValues: String) {
        onValuesReceived(jsonValues)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    url: String,
    viewModel: FormViewModel,
    onProgressChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    var filePathCallback: ValueCallback<Array<Uri>>? = remember { null }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        val results = if (result.resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        } else {
            null
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    allowFileAccess = true
                    allowContentAccess = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    
                    // Allow mixed content for HTTP inside HTTPS if necessary (handled safely)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                }

                // Add JS interface for custom data transfer from page
                addJavascriptInterface(WebAppInterface { valuesJson ->
                    post {
                        viewModel.handleReceivedValues(valuesJson)
                    }
                }, "AndroidApp")

                // Client to track loading and inject observers
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                            return false // Loaded inside this WebView
                        }
                        return try {
                            // Non-web links (like tel:, mailto:, map links) opened externally
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                            context.startActivity(intent)
                            true
                        } catch (e: Exception) {
                            true
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Trigger autofill on load completed if enabled
                        if (viewModel.isAutoFillEnabled.value) {
                            val activeId = viewModel.activeTemplateId.value
                            if (activeId != null) {
                                viewModel.getTemplateById(activeId)?.let {
                                    injectAutoFillScripts(this@apply, it.valuesJson)
                                }
                            }
                        }
                    }
                }

                // WebChromeClient for files, progress, and logs
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        android.util.Log.d("WebViewConsole", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}")
                        return true
                    }

                    // Handle form <input type="file"> triggers
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallbackIn: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = filePathCallbackIn

                        try {
                            val intent = fileChooserParams?.createIntent()
                            if (intent != null) {
                                fileChooserLauncher.launch(intent)
                            } else {
                                val backupIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                                fileChooserLauncher.launch(backupIntent)
                            }
                        } catch (e: Exception) {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = null
                            return false
                        }
                        return true
                    }
                }

                onWebViewCreated(this)
            }
        },
        update = { webView ->
            // Trigger load when URL shifts
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

/**
 * JS injector that fills input fields and configures a MutationObserver
 * to capture dynamic UI responses of the Single Page App (Enketo)
 */
fun injectAutoFillScripts(webView: WebView, jsonValues: String) {
    val escapedJson = jsonValues.replace("\\", "\\\\").replace("'", "\\'")
    val jsScript = """
        (function() {
            try {
                var values = JSON.parse('$escapedJson');
                
                function applyToElement(el) {
                    var name = el.getAttribute('name');
                    if (!name || !(name in values)) return;
                    var val = values[name];
                    var tag = el.tagName.toLowerCase();
                    
                    if (tag === 'input') {
                        var type = el.getAttribute('type');
                        if (type === 'radio') {
                            if (el.value === val) {
                                if (!el.checked) {
                                    el.checked = true;
                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                    el.dispatchEvent(new Event('input', { bubbles: true }));
                                }
                            }
                        } else if (type === 'checkbox') {
                            var shouldBeChecked = false;
                            if (Array.isArray(val)) {
                                shouldBeChecked = val.indexOf(el.value) !== -1;
                            } else {
                                shouldBeChecked = (el.value === val);
                            }
                            if (el.checked !== shouldBeChecked) {
                                el.checked = shouldBeChecked;
                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                            }
                        } else {
                            if (el.value !== val) {
                                el.value = val;
                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                            }
                        }
                    } else if (tag === 'select' || tag === 'textarea') {
                        if (el.value !== val) {
                            el.value = val;
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                    }
                }
                
                // 1) Apply to already rendered inputs immediately
                document.querySelectorAll('input, select, textarea').forEach(applyToElement);
                
                // 2) Set up a MutationObserver to apply values to dynamically generated inputs
                if (window.koboMutationObserver) {
                    window.koboMutationObserver.disconnect();
                }
                
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) { // ELEMENT_NODE
                                if (node.matches('input, select, textarea')) {
                                    applyToElement(node);
                                }
                                node.querySelectorAll('input, select, textarea').forEach(applyToElement);
                            }
                        });
                    });
                });
                
                observer.observe(document.body, { childList: true, subtree: true });
                window.koboMutationObserver = observer;
                
                // Trigger global model recomputes if Enketo uses standard triggering
                if (window.$ && typeof window.$.prototype.trigger === "function") {
                    $('form.or').trigger('change');
                }
                
                console.log("AutoFill applied successfully with " + Object.keys(values).length + " fields.");
            } catch(e) {
                console.error("AutoFill error: " + e.message);
            }
        })()
    """.trimIndent()

    webView.evaluateJavascript(jsScript, null)
}

/**
 * JS script to scrape current visual selections in the WebView
 * and transmit them back to Android via WebAppInterface
 */
fun requestScrapeFormValues(webView: WebView) {
    val jsScript = """
        (function() {
            try {
                var values = {};
                var elements = document.querySelectorAll('input, select, textarea');
                elements.forEach(function(el) {
                    var name = el.getAttribute('name');
                    if (!name) return;
                    
                    // Filter out elements that don't belong to the XML data submission
                    // Enketo form values are mapped directly to XML schemas which always require a '/' root or start with data
                    if (!name.startsWith('/') && !name.includes('/') && !name.startsWith('data')) return;
                    
                    var tag = el.tagName.toLowerCase();
                    if (tag === 'input') {
                        var type = el.getAttribute('type');
                        if (type === 'radio') {
                            if (el.checked) {
                                values[name] = el.value;
                            }
                        } else if (type === 'checkbox') {
                            if (!values[name]) values[name] = [];
                            if (el.checked) {
                                values[name].push(el.value);
                            }
                        } else {
                            if (el.value && el.value.trim() !== "") {
                                values[name] = el.value;
                            }
                        }
                    } else if (tag === 'select' || tag === 'textarea') {
                        if (el.value && el.value.trim() !== "") {
                            values[name] = el.value;
                        }
                    }
                });
                AndroidApp.sendValues(JSON.stringify(values));
            } catch (e) {
                AndroidApp.sendValues(JSON.stringify({error: e.message}));
            }
        })()
    """.trimIndent()

    webView.evaluateJavascript(jsScript, null)
}
