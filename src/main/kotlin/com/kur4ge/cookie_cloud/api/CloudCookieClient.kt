package com.kur4ge.cookie_cloud.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.kur4ge.cookie_cloud.utils.Config
import com.kur4ge.cookie_cloud.utils.Crypto
import com.kur4ge.cookie_cloud.model.DomainState
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * API交互类
 * 负责与Cookie Cloud服务端进行HTTP通信
 */
class CloudCookieClient {
    companion object {
        private var instance: CloudCookieClient? = null
        
        /**
         * 获取ApiClient实例（单例模式）
         * @return ApiClient 实例
         */
        fun getInstance(): CloudCookieClient {
            if (instance == null) {
                instance = CloudCookieClient()
            }
            return instance!!
        }
    }
    
    private val config = Config.getInstance()
    private val gson = Gson()
    
    /**
     * 请求体数据类
     */
    data class GetRequest(val id: List<String> = emptyList())
    
    /**
     * 响应数据结构
     */
    data class GetResponse(
        @SerializedName("code")
        val code: Int,
        
        @SerializedName("message")
        val message: String,
        
        @SerializedName("data")
        val data: Map<String, EncryptedData>
    )
    
    /**
     * 解密后的数据结构
     */
    data class DomainStateItem(
        @SerializedName("cookies")
        val cookies: List<DomainState.CookieItem> = emptyList(),
        
        @SerializedName("headers")
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * Get 加密的数据结构
     */
    data class EncryptedData(
        @SerializedName("data")
        val data: String,
        
        @SerializedName("update")
        val update: Long
    )
    
   /**
     * 加密数据内部结构
     */
    private data class EncryptedDataContent(
        @SerializedName("ephPubKey")
        val ephPubKey: String,
        
        @SerializedName("data")
        val data: String,
        
        @SerializedName("shareKeys")
        val shareKeys: Map<String, String>,
        
        @SerializedName("signature")
        val signature: String
    )
    
    /**
     * 解密EncryptedData
     * @param encryptedData 加密的数据
     * @param peerPublicKey 对端公钥
     * @return 解密后的数据，如果解密失败则返回null
     */
    private fun decryptData(encryptedData: EncryptedData, peerPublicKey: String): DomainStateItem? {
        try {
            // 获取本地私钥
            val localPrivateKey = config.getLocalPrivateKey()
            if (localPrivateKey.isEmpty()) {
                throw IllegalStateException("本地私钥未配置")
            }
            
            // 使用Crypto工具类解密数据
            val decryptedJson = Crypto.verifyAndDecryptForMultipleRecipients(
                recipientPrivateKey = localPrivateKey,
                senderPublicKey = peerPublicKey,
                encryptedData = encryptedData.data
            )
            // 使用Gson解析解密后的数据
            val decryptedData = gson.fromJson(decryptedJson, DomainStateItem::class.java)
            return decryptedData
        } catch (e: Exception) {
            // throw RuntimeException("解密出错，错误码: ${e}")
            return null
        }
    }

    /**
     * 计算远端ID
     * @param publicKey 公钥
     * @param domain 域名
     * @param name 对端名称
     * @return 计算得到的远端ID（SHA-256哈希值）
     */
    fun calculateId(publicKey: String, domain: String, name: String): String {
        val data = "$publicKey:$domain:$name"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(StandardCharsets.UTF_8))
        
        // 将字节数组转换为十六进制字符串
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    /**
     * 发送POST请求到/get接口
     * @param name 对端名称
     * @param domains 要获取的域名列表
     * @return 解密后的域名状态映射，域名为键，DomainStateItem为值
     * @throws Exception 当请求失败时抛出异常
     */
    fun get(name: String, domains: List<String> = emptyList()): Map<String, DomainStateItem> {
        val peer = config.getPeer(name) ?: throw IllegalArgumentException("未找到名称为 $name 的对端")
        val publicKey = peer.publicKey
        val peerName = peer.peerName

        // 将域名列表转换为ID列表，并保存ID到域名的映射
        val idToDomainMap = mutableMapOf<String, String>()
        val ids = domains.map { domain -> 
            val id = calculateId(publicKey, domain, peerName)
            idToDomainMap[id] = domain
            id
        }
        // 发送POST请求
        val endpoint = config.getEndpoint()
        if (endpoint.isEmpty()) {
            throw IllegalStateException("API端点未配置")
        }
        
        val url = URL("${endpoint.trimEnd('/')}/get")
        val connection = url.openConnection() as HttpURLConnection

        
        try {
            // 设置为POST请求
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            
            // 创建请求对象并序列化为JSON
            val requestBody = GetRequest(ids)
            val jsonRequestBody = gson.toJson(requestBody)
            
            // 写入请求体
            val outputStream = connection.outputStream
            outputStream.write(jsonRequestBody.toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8))
                val response = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                // 使用Gson解析响应为GetResponse对象
                val responseObj = gson.fromJson(response.toString(), GetResponse::class.java)
                
                // 解密数据并构建域名到DomainStateItem的映射
                val domainStateMap = mutableMapOf<String, DomainStateItem>()
                
                if (responseObj.code == 0) {
                    for ((id, encryptedData) in responseObj.data) {
                        val domain = idToDomainMap[id] ?: continue
                        val decryptedData = decryptData(encryptedData, publicKey)
                        if (decryptedData != null) {
                            domainStateMap[domain] = decryptedData
                        }
                    }
                } else {
                    throw RuntimeException("API请求失败，错误码: ${responseObj.code}, 消息: ${responseObj.message}")
                }
                return domainStateMap
            } else {
                throw RuntimeException("API请求失败，响应码: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
}