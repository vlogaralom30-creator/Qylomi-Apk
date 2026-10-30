package com.example.adshield.ui

import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.adshield.engine.AdShieldEngine
import com.example.adshield.engine.ScriptHardening
import com.example.adshield.model.RequestDecision
import com.example.adshield.repository.AdShieldConfigRepository

open class AdShieldWebViewClient(
    private val activeProfileId: String? = null,
    private val onPageFinishedCallback: (() -> Unit)? = null,
    private val onErrorOccurred: (() -> Unit)? = null
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
        val decision = AdShieldEngine.evaluateRequest(url, activeProfileId)

        AdShieldConfigRepository.logInterceptedRequest(decision)

        return if (decision is RequestDecision.Blocked) {
            AdShieldEngine.createBlockResponse()
        } else {
            super.shouldInterceptRequest(view, request)
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        val decision = AdShieldEngine.evaluateRequest(url, activeProfileId)

        return if (decision is RequestDecision.Blocked) {
            true // Block main frame redirect
        } else {
            false // Allow
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        view?.evaluateJavascript(ScriptHardening.DOCUMENT_START_JS, null)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.evaluateJavascript(ScriptHardening.DOCUMENT_START_JS, null)
        onPageFinishedCallback?.invoke()
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        onErrorOccurred?.invoke()
    }

    override fun onRenderProcessGone(
        view: WebView?,
        detail: android.webkit.RenderProcessGoneDetail?
    ): Boolean {
        try {
            val parent = view?.parent as? android.view.ViewGroup
            parent?.removeView(view)
            view?.destroy()
        } catch (_: Throwable) {}
        onErrorOccurred?.invoke()
        return true
    }
}

open class AdShieldWebChromeClient : WebChromeClient() {

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        // Block all popups
        return false
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.confirm()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.cancel()
        return true
    }
}
