package com.kur4ge.cookie_cloud.handler

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.HttpHandler 
import burp.api.montoya.http.handler.HttpRequestToBeSent 
import burp.api.montoya.http.handler.RequestToBeSentAction
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.handler.ResponseReceivedAction
import burp.api.montoya.http.message.requests.HttpRequest
import com.kur4ge.cookie_cloud.model.DomainState
import com.kur4ge.cookie_cloud.utils.Config
import java.util.regex.Pattern

/**
 * Cookie Cloud HTTP 监听器
 * 用于处理 HTTP 请求和响应，实现 Cookie 的自动注入和提取
 */
class CookieCloudHttpHandler(private val api: MontoyaApi) : HttpHandler {

    // 定义模式匹配的正则表达式
    private val forceFetchPattern = Pattern.compile("!\\{([^}]+)}")
    private val cacheFetchPattern = Pattern.compile("\\$\\{([^}]+)}")
    // 定义Header模式匹配的正则表达式
    private val forceHeaderPattern = Pattern.compile("!\\{([^|]+)(?:\\|([^}]+))?}")
    private val cacheHeaderPattern = Pattern.compile("\\$\\{([^|]+)(?:\\|([^}]+))?}")
    private val domainState = DomainState.getInstance()
    private val config = Config.getInstance()

    /**
     * 处理Cookie模式匹配
     * 支持两种模式：
     * - !{name}: 强制从远程获取最新的cookie
     * - ${name}: 从缓存中获取cookie
     * 
     * @param cookieValue 原始Cookie值
     * @param domain 请求的域名
     * @param path 请求的路径
     * @return 处理后的Cookie值
     */
    private fun processCookiePatterns(cookieValue: String, domain: String, path: String = "/"): String {
        if (cookieValue.isEmpty()) return cookieValue
        
        var result = cookieValue
        
        // 处理强制获取模式 !{name}
        val forceMatcher = forceFetchPattern.matcher(result)
        while (forceMatcher.find()) {
            val name = forceMatcher.group(1)
            val value = domainState.getHttpCookie(name, domain, path, false)

            result = result.replace("!{$name}", value)
        }
        
        // 处理缓存获取模式 ${name}
        val cacheMatcher = cacheFetchPattern.matcher(result)
        while (cacheMatcher.find()) {
            val name = cacheMatcher.group(1)
            // 从缓存获取cookie值
            val value = domainState.getHttpCookie(name, domain, path)
            
            // 替换模式为实际值
            result = result.replace("\${$name}", value)
        }
        
        return result
    }

    /**
     * 处理Header模式匹配
     * 支持两种模式：
     * - !{peername|header-name}: 强制从远程获取最新的header
     * - ${peername|header-name}: 从缓存中获取header
     * peername可能为空，表示使用默认对端
     * 
     * @param headerValue 原始Header值
     * @param domain 请求的域名
     * @return 处理后的Header值
     */
    private fun processHeaderPatterns(headerName: String, headerValue: String, domain: String): String {
        if (headerValue.isEmpty()) return headerValue
        
        var result = headerValue
        
        // 处理强制获取模式 !{peername|header-name}
        val forceMatcher = forceHeaderPattern.matcher(result)
        while (forceMatcher.find()) {
            val peerName = forceMatcher.group(1)
            val headerKey = forceMatcher.group(2) ?: headerName
            val value = domainState.getHttpHeader(peerName, domain, headerKey, false) ?: ""
            
            // 根据是否有headerKey决定替换模式
            val pattern = if (forceMatcher.group(2) != null) "!{$peerName|$headerKey}" else "!{$peerName}"

            result = result.replace(pattern, value)
        }
        
        // 处理缓存获取模式 ${peername|header-name}
        val cacheMatcher = cacheHeaderPattern.matcher(result)
        while (cacheMatcher.find()) {
    
            val peerName = cacheMatcher.group(1)
            val headerKey = cacheMatcher.group(2) ?: headerName
            val value = domainState.getHttpHeader(peerName, domain, headerKey) ?: ""
            
            // 根据是否有headerKey决定替换模式
            val pattern = if (cacheMatcher.group(2) != null) "\${$peerName|$headerKey}" else "\${$peerName}"

            result = result.replace(pattern, value)
        }
        return result
    }

    override fun handleHttpRequestToBeSent(requestToBeSent: HttpRequestToBeSent): RequestToBeSentAction {
        // 检查全局开关是否启用
        if (!config.isEnabled(requestToBeSent.toolSource().toolType())) {
            return RequestToBeSentAction.continueWith(requestToBeSent)
        }
        
        // 获取请求的域名和路径
        val urlString = requestToBeSent.url()

        val url = java.net.URL(urlString)
        val domain = url.host
        val path = url.path.ifEmpty { "/" }

        var modifiedRequest: HttpRequest = requestToBeSent
        var isModified = false

        // 获取原始Cookie
        val originalCookie = requestToBeSent.headerValue("Cookie") ?: ""
        if (originalCookie.isNotEmpty()) {
            val processedCookie = processCookiePatterns(originalCookie, domain, path)
            if (processedCookie != originalCookie) {
                // 创建新的请求，替换Cookie头
                modifiedRequest = modifiedRequest.withUpdatedHeader("Cookie", processedCookie)
                isModified = true
            }
        }
        
        // 处理所有请求头
        for (headerName in requestToBeSent.headers().map { it.name() }) {
            if (headerName.equals("Cookie", ignoreCase = true)) continue // Cookie已单独处理
            val headerValue = requestToBeSent.headerValue(headerName) ?: ""
            if (headerValue.isNotEmpty()) {
                val processedHeader = processHeaderPatterns(headerName, headerValue, domain)
                if (processedHeader != headerValue) {
                    modifiedRequest = modifiedRequest.withUpdatedHeader(headerName, processedHeader)
                    api.logging().logToOutput("$urlString 修改后的请求头: `$headerName`: `$headerValue` -> `$processedHeader`")
                    isModified = true
                }
            }
        }
        if (isModified) {
            return RequestToBeSentAction.continueWith(modifiedRequest)
        }
        return RequestToBeSentAction.continueWith(requestToBeSent)
    }

    override fun handleHttpResponseReceived(responseReceived: HttpResponseReceived): ResponseReceivedAction {
        // 检查全局开关是否启用
        if (!config.isEnabled(responseReceived.toolSource().toolType())) {
            return ResponseReceivedAction.continueWith(responseReceived)
        }
        return ResponseReceivedAction.continueWith(responseReceived)
    }
}