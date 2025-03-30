package com.kur4ge.cookie_cloud.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.kur4ge.cookie_cloud.api.CloudCookieClient
import com.kur4ge.cookie_cloud.utils.Config
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 域名状态模型
 * 用于管理域名的Cookie和Header状态，并提供缓存功能
 */
class DomainState {
    companion object {
        private val instance = DomainState()
        private val gson = Gson()
        
        // 缓存过期时间（毫秒）
        private const val CACHE_EXPIRATION_TIME = 5 * 60 * 1000L // 5分钟
        
        fun getInstance(): DomainState {
            return instance
        }
    }
    
    // 初始化Cookies实例
    private val cookies = Cookies(this)
    
    // 域名状态缓存，使用ConcurrentHashMap保证线程安全
    private val domainCache = ConcurrentHashMap<String, CachedDomainInfo>()
    private val config = Config.getInstance()
    private val apiClient = CloudCookieClient.getInstance()
    
    /**
     * 缓存的域名信息，包含数据和过期时间
     */
    private data class CachedDomainInfo(
        val domainInfo: DomainInfo,
        val expirationTime: Long
    )
    
    /**
     * 域名信息数据类
     */
    data class DomainInfo(
        @SerializedName("domain")
        val domain: String,
        
        @SerializedName("cookies")
        val cookies: List<CookieItem> = emptyList(),
        
        @SerializedName("headers")
        val headers: Map<String, String> = emptyMap(),
        
        @SerializedName("last_updated")
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    /**
     * Cookie数据类
     */
    data class CookieItem(
        @SerializedName("name")
        val name: String,
        
        @SerializedName("value")
        val value: String,
        
        @SerializedName("domain")
        val domain: String,
        
        @SerializedName("path")
        val path: String = "/",
        
        @SerializedName("expirationDate")
        val expires: Double = -1.0,
        
        @SerializedName("secure")
        val secure: Boolean = false,
        
        @SerializedName("httpOnly")
        val httpOnly: Boolean = false,

        @SerializedName("hostOnly")
        val hostOnly: Boolean = false,

        @SerializedName("sameSite")
        val sameSite: String,

        @SerializedName("session")
        val session: Boolean = false,
        
        @SerializedName("storeId")
        val storeId: String
    )

    /**
     * 获取指定域名和路径的所有Cookie，用于HTTP请求
     * @param domain 域名
     * @param path 路径，默认为"/"
     * @return 所有匹配的Cookie，用分号和空格连接，如果没有找到则返回空字符串
     */
    fun getHttpCookie(name: String, domain: String, path: String = "/"): String {
        val cookieItems = cookies.getCookies(name, domain)
        val matchedCookies = mutableListOf<String>()
        
        // 遍历所有匹配域名的Cookie项
        for (cookie in cookieItems) {
            // 检查路径是否匹配
            // 路径匹配规则：Cookie的路径是请求路径的前缀，或者Cookie的路径是"/"
            if (path.startsWith(cookie.path) || cookie.path == "/") {
                // 添加到匹配列表
                matchedCookies.add("${cookie.name}=${cookie.value}")
            }
        }
        
        // 用分号和空格连接所有匹配的Cookie
        return matchedCookies.joinToString("; ")
    }

    /**
     * 从缓存获取域名状态
     * @param name 对端名称
     * @param domain 域名
     * @return 域名信息
     */
    fun getDomainState(name: String, domain: String): DomainInfo? {
        // 生成缓存键，如果有对端名称则包含在键中
        val cacheKey = "$domain:$name"
        
        // 检查缓存
        val cachedInfo = domainCache[cacheKey]
        if (cachedInfo != null && System.currentTimeMillis() < cachedInfo.expirationTime) {
            return cachedInfo.domainInfo
        }
        
        return null
    }
    
    /**
     * 从远程获取域名状态并保存到缓存
     * @param name 对端名称
     * @param domains 要获取的域名列表
     * @return 获取到的域名信息映射，域名为键，DomainInfo为值
     */
    fun getRemoteDomainState(name: String, domains: List<String>): Map<String, DomainInfo> {
        try {
            // 从API获取域名状态
            val domainStateItems = apiClient.get(name, domains)
            val result = mutableMapOf<String, DomainInfo>()
            
            // 将获取到的数据转换为DomainInfo并保存到缓存
            for ((domain, stateItem) in domainStateItems) {
                val domainInfo = DomainInfo(
                    domain = domain,
                    cookies = stateItem.cookies,
                    headers = stateItem.headers,
                    lastUpdated = System.currentTimeMillis()
                )
                
                // 更新缓存
                val cacheKey = "$domain:$name"
                domainCache[cacheKey] = CachedDomainInfo(
                    domainInfo = domainInfo,
                    expirationTime = System.currentTimeMillis() + CACHE_EXPIRATION_TIME
                )
                
                result[domain] = domainInfo
            }
            
            return result
        } catch (e: Exception) {
            // 处理异常，记录错误信息
            println("获取远程域名状态失败: ${e.message}")
            return emptyMap()
        }
    }

    /**
     * 清除指定域名的缓存
     * @param domain 域名
     */
    fun clearCache(domain: String) {
        domainCache.remove(domain)
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        domainCache.clear()
    }
    
    /**
     * 获取所有缓存的域名
     * @return 域名列表
     */
    fun getCachedDomains(): List<String> {
        return domainCache.keys.toList()
    }
    
    /**
     * 手动更新域名状态缓存
     * @param domainInfo 域名信息
     */
    fun updateCache(domainInfo: DomainInfo) {
        domainCache[domainInfo.domain] = CachedDomainInfo(
            domainInfo = domainInfo,
            expirationTime = System.currentTimeMillis() + CACHE_EXPIRATION_TIME
        )
    }
    
    /**
     * 设置缓存过期时间
     * @param domain 域名
     * @param duration 持续时间
     * @param timeUnit 时间单位
     */
    fun setCacheExpiration(domain: String, duration: Long, timeUnit: TimeUnit) {
        val cachedInfo = domainCache[domain] ?: return
        domainCache[domain] = CachedDomainInfo(
            domainInfo = cachedInfo.domainInfo,
            expirationTime = System.currentTimeMillis() + timeUnit.toMillis(duration)
        )
    }
}