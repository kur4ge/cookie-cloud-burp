package com.kur4ge.cookie_cloud.handler

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.HttpHandler 
import burp.api.montoya.http.handler.HttpRequestToBeSent 
import burp.api.montoya.http.handler.RequestToBeSentAction
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.handler.ResponseReceivedAction
import com.kur4ge.cookie_cloud.model.DomainState
import com.kur4ge.cookie_cloud.utils.Config
import java.util.regex.Pattern

/**
 * Cookie Cloud HTTP 监听器
 * 用于处理 HTTP 请求和响应，实现 Cookie 的自动注入和提取
 */
class CookieCloudHttpHandler(private val api: MontoyaApi) : HttpHandler {

    // 定义模式匹配的正则表达式
    private val forceFetchPattern = Pattern.compile("!\\{([^}]+)\\}")
    private val cacheFetchPattern = Pattern.compile("\\$\\{([^}]+)\\}")
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

    override fun handleHttpRequestToBeSent(requestToBeSent: HttpRequestToBeSent): RequestToBeSentAction {
        // 检查全局开关是否启用
        if (!config.isEnabled(requestToBeSent.toolSource().toolType())) {
            return RequestToBeSentAction.continueWith(requestToBeSent)
        }
        
        // 获取请求的域名和路径
        val urlString = requestToBeSent.url()
        api.logging().logToOutput("url: $urlString, tool: ${requestToBeSent.toolSource().toolType().ordinal}")

        val url = java.net.URL(urlString)
        val domain = url.host
        val path = url.path.ifEmpty { "/" }

        // 获取原始Cookie
        val originalCookie = requestToBeSent.headerValue("Cookie") ?: ""
        if (originalCookie.isNotEmpty()) {
            val processedCookie = processCookiePatterns(originalCookie, domain, path)
            if (processedCookie != originalCookie) {
                // 创建新的请求，替换Cookie头
                val modifiedRequest = requestToBeSent.withUpdatedHeader("Cookie", processedCookie)
                return RequestToBeSentAction.continueWith(modifiedRequest)
            }
        }
        api.logging().logToOutput("拦截到 HTTP 请求: $originalCookie")
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