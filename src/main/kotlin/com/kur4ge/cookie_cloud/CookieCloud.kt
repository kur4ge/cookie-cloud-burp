package com.kur4ge.cookie_cloud
import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import com.kur4ge.cookie_cloud.utils.Crypto
import com.kur4ge.cookie_cloud.utils.Path
import com.kur4ge.cookie_cloud.ui.CookieCloudUI
import com.kur4ge.cookie_cloud.model.Cookies
import com.kur4ge.cookie_cloud.model.DomainState
import com.kur4ge.cookie_cloud.api.CloudCookieClient
import com.kur4ge.cookie_cloud.handler.CookieCloudHttpHandler
import java.io.File
import java.net.URISyntaxException

@Suppress("unused")
class CookieCloud : BurpExtension {
    private lateinit var api: MontoyaApi
    private lateinit var ui: CookieCloudUI
    private var jarPath: String = ""
    
    override fun initialize(api: MontoyaApi?) {
        if (api == null) {
            return
        }
        this.api = api // 初始化 api

        api.extension().setName("Cookie Cloud")
        api.logging().logToOutput("Cookie Cloud 插件已加载")
        
        // 获取配置文件路径
        val configPath = Path.getConfig("cookie-cloud.json")
        api.logging().logToOutput("Cookie Cloud 配置文件路径: $configPath")

        initializeUI()
        registerHttpHandler()
    }
    
    private fun initializeUI() {
        // 创建 UI 实例
        ui = CookieCloudUI(api)
        
        // 将 UI 添加到 Burp 的 UI
        api.userInterface().registerSuiteTab("Cookie Cloud", ui.getUI())
        
        api.logging().logToOutput("Cookie Cloud UI 已加载")
    }
    
    private fun registerHttpHandler() {
        // 创建并注册 HTTP 拦截器
        val httpHandler = CookieCloudHttpHandler(api)
        api.http().registerHttpHandler(httpHandler)
        
        api.logging().logToOutput("Cookie Cloud HTTP 拦截器已注册")
    }

}
