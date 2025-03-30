package com.kur4ge.cookie_cloud.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kur4ge.cookie_cloud.api.CloudCookieClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Cookie模型类
 * 专门处理Cookie相关功能
 */
class Cookies(private val domainState: DomainState) {
    companion object {
        private var instance: Cookies? = null
        private val gson = Gson()
        
        fun getInstance(domainState: DomainState): Cookies {
            if (instance == null) {
                instance = Cookies(domainState)
            }
            return instance!!
        }
    }
    
    /**
     * 获取指定域名的Cookie信息
     * @param name 对端名称
     * @param domain 域名
     * @return 域名的Cookie列表，如果获取失败则返回空列表
     */
    fun getCookies(name: String, domain: String, cache: Boolean=true): List<DomainState.CookieItem> {
        // 获取所有可能的Cookie域名形式
        val possibleDomains = getPossibleCookieDomains(domain)
        val allCookies = mutableListOf<DomainState.CookieItem>()

        if (cache) {
            // 收集所有可能域名的Cookie
            var needRemoteData = false
            
            // 遍历所有可能的域名形式
            for (possibleDomain in possibleDomains) {
                // 尝试从DomainState获取缓存的域名信息
                val domainInfo = domainState.getDomainState(name, possibleDomain)
                
                if (domainInfo != null) {
                    // 如果找到缓存的域名信息，添加到结果列表
                    allCookies.addAll(domainInfo.cookies)
                } else {
                    // 如果任何一个域名没有缓存，标记需要从远程获取
                    needRemoteData = true
                    break
                }
            }
            
            // 如果所有域名都有缓存，直接返回收集到的Cookie
            if (!needRemoteData) {
                return allCookies
            }
            allCookies.clear() // 清除 走后面逻辑

        }
        
        // 如果需要从远程获取数据
        val remoteDomainInfoMap = domainState.getRemoteDomainState(name, possibleDomains)
        
        // 清空之前收集的Cookie，因为可能有些域名已经过期或变更
        
        // 从远程获取的数据中提取Cookie
        for (possibleDomain in possibleDomains) {
            val remoteDomainInfo = remoteDomainInfoMap[possibleDomain]
            if (remoteDomainInfo != null) {
                allCookies.addAll(remoteDomainInfo.cookies)
            }
        }
        return allCookies
    }
        
    
    /**
     * 获取域名的所有可能的Cookie域名形式
     * 例如：www.qq.com 会返回 [www.qq.com, .www.qq.com, .qq.com]
     * @param domain 原始域名
     * @return 所有可能的Cookie域名形式列表
     */
    private fun getPossibleCookieDomains(domain: String): List<String> {
        val result = mutableListOf<String>()
        
        // 添加原始域名
        result.add(domain)
        
        // 添加带点前缀的原始域名
        result.add(".$domain")
        
        // 使用OkHttp的HttpUrl获取顶级私有域名
        val httpUrl = "https://$domain".toHttpUrlOrNull()
        if (httpUrl != null) {
            val topPrivateDomain = httpUrl.topPrivateDomain()
            if (topPrivateDomain != null && topPrivateDomain != domain) {
                result.add(".$topPrivateDomain")
                
                // 处理子域名
                val subDomainParts = domain.removeSuffix(topPrivateDomain).split(".")
                val subDomains = subDomainParts.filter { it.isNotEmpty() }
                
                if (subDomains.isNotEmpty()) {
                    // 逐级构建子域名
                    var currentSubDomain = ""
                    for (i in subDomains.indices.reversed()) {
                        if (currentSubDomain.isEmpty()) {
                            currentSubDomain = subDomains[i]
                        } else {
                            currentSubDomain = "${subDomains[i]}.$currentSubDomain"
                        }
                        
                        val fullDomain = "$currentSubDomain.$topPrivateDomain"
                        result.add(".$fullDomain")
                    }
                }
            }
        } else {
            // 如果无法解析URL，回退到简单的域名分割方法
            val parts = domain.split(".")
            
            // 如果域名有超过2个部分，则添加父域名
            if (parts.size > 2) {
                // 构建父域名（去掉最左边的子域名）
                val parentDomain = parts.drop(1).joinToString(".")
                
                // 添加父域名及其带点前缀形式
                result.add(".$parentDomain")
            }
        }
        
        return result.distinct()
    }
    
    /**
     * 使用topPrivateDomain的示例方法
     * @param domain 域名
     * @return 顶级私有域名，如果无法解析则返回null
     */
    fun getTopPrivateDomain(domain: String): String? {
        val httpUrl = "https://$domain".toHttpUrlOrNull() ?: return null
        return httpUrl.topPrivateDomain()
    }
}