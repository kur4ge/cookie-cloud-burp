package com.kur4ge.cookie_cloud.utils

import burp.api.montoya.core.ToolType
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 配置管理类
 * 负责读取、保存和管理 Cookie Cloud 的配置
 */
class Config {
    companion object {
        private const val CONFIG_FILENAME = "cookie-cloud.json"
        private var instance: Config? = null
        
        /**
         * 获取配置实例（单例模式）
         * @return Config 实例
         */
        fun getInstance(): Config {
            if (instance == null) {
                instance = Config()
            }
            return instance!!
        }
    }
    
    // 配置数据
    private var enabled: Boolean = false
    private var endpoint: String = ""
    private var localPrivateKey: String = ""
    private val peers = mutableListOf<Peer>()
    private var enabledTools: Int = 0  // 新增：启用的工具标识
    private var cacheTime: Int = 10    // 新增：缓存时间，默认30分钟
    
    // Gson 实例
    private val gson = Gson()
    
    /**
     * 对端信息数据类
     */
    data class Peer(
        val name: String,
        val peerName: String,
        val publicKey: String
    )
    
    init {
        // 初始化时加载配置
        loadConfig()
    }
    
    /**
     * 加载配置
     */
    fun loadConfig() {
        val configPath = Path.ensureConfigDirExists(Path.getConfig(CONFIG_FILENAME))
        val configFile = File(configPath)
        
        if (configFile.exists()) {
            try {
                val jsonContent = configFile.readText(StandardCharsets.UTF_8)
                val jsonObject = JsonParser.parseString(jsonContent).asJsonObject
                
                // 读取基本配置
                enabled = jsonObject.get("enabled")?.asBoolean ?: enabled
                endpoint = jsonObject.get("endpoint")?.asString ?: endpoint
                localPrivateKey = jsonObject.get("localPrivateKey")?.asString ?: localPrivateKey
                enabledTools = jsonObject.get("enabledTools")?.asInt ?: enabledTools
                cacheTime = jsonObject.get("cacheTime")?.asInt ?: cacheTime
                
                // 读取对端列表
                peers.clear()
                val peersArray = jsonObject.get("peers")?.asJsonArray ?: JsonArray()
                for (i in 0 until peersArray.size()) {
                    val peerObj = peersArray.get(i).asJsonObject
                    peers.add(
                        Peer(
                            name = peerObj.get("name").asString,
                            peerName = peerObj.get("peerName").asString,
                            publicKey = peerObj.get("publicKey").asString
                        )
                    )
                }
            } catch (e: Exception) {
                // 配置文件读取失败，使用默认值
                resetToDefaults()
            }
        } else {
            // 配置文件不存在，使用默认值
            resetToDefaults()
        }
    }
    
    /**
     * 保存配置
     */
    fun saveConfig() {
        val configPath = Path.ensureConfigDirExists(Path.getConfig(CONFIG_FILENAME))
        val configFile = File(configPath)
        
        try {
            val jsonObject = JsonObject()
            
            // 保存基本配置
            jsonObject.addProperty("enabled", enabled)
            jsonObject.addProperty("endpoint", endpoint)
            jsonObject.addProperty("enabledTools", enabledTools)
            jsonObject.addProperty("cacheTime", cacheTime)
            jsonObject.addProperty("localPrivateKey", if (localPrivateKey.startsWith("0x", ignoreCase = true)) {
                localPrivateKey.substring(2)
            } else {
                localPrivateKey
            })
            
            // 保存对端列表
            val peersArray = JsonArray()
            for (peer in peers) {
                val peerObj = JsonObject()
                peerObj.addProperty("name", peer.name)
                peerObj.addProperty("peerName", peer.peerName)
                peerObj.addProperty("publicKey", if (peer.publicKey.startsWith("0x", ignoreCase = true)) {
                    peer.publicKey.substring(2)
                } else {
                    peer.publicKey
                })
                peersArray.add(peerObj)
            }
            jsonObject.add("peers", peersArray)
            
            // 写入文件，使用Gson的pretty printing
            configFile.writeText(gson.toJson(jsonObject), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // 处理保存失败的情况
            throw RuntimeException("保存配置文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 重置为默认配置
     */
    private fun resetToDefaults() {
        enabled = false
        endpoint = ""
        localPrivateKey = ""
        enabledTools = 0
        cacheTime = 10
        peers.clear()
    }
    
    // Getter 和 Setter 方法
    
    /**
     * 检查是否启用
     * @param toolType 可选的工具类型标识，如果提供则检查特定工具是否启用
     * @return 如果未提供工具类型，则返回全局启用状态；否则返回特定工具是否启用
     */
    fun isEnabled(toolType: ToolType? = null): Boolean {
        if (toolType == null) {
            return enabled
        }
        // 首先检查全局开关是否启用
        if (!enabled) {
            return false
        }
        // 然后检查特定工具是否启用
        return (enabledTools and (1 shl toolType.ordinal )) != 0
    }
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        saveConfig()
    }
    
    fun getEndpoint(): String = endpoint
    
    fun setEndpoint(endpoint: String) {
        this.endpoint = endpoint
        saveConfig()
    }
    
    fun getLocalPrivateKey(): String = localPrivateKey
    
    fun setLocalPrivateKey(privateKey: String) {
        this.localPrivateKey = privateKey
        saveConfig()
    }
    
    fun getLocalPublicKey(): String {
        return Crypto.getKeyPairFromPrivateKey(this.localPrivateKey).publicKey
    }

    
    fun getPeers(): List<Peer> = peers.toList()

    /**
     * 通过名称获取对端信息
     * @param name 对端名称
     * @return 对端信息，如果未找到则返回null
     */
    fun getPeer(name: String): Peer? {
        return peers.find { it.name == name }
    }

    fun addPeer(name: String, peerName: String, publicKey: String) {
        peers.add(Peer(name, peerName, publicKey))
        saveConfig()
    }
    
    fun removePeer(index: Int) {
        if (index >= 0 && index < peers.size) {
            peers.removeAt(index)
            saveConfig()
        }
    }
    
    fun removePeerByName(name: String) {
        peers.removeIf { it.name == name }
        saveConfig()
    }
    
    /**
     * 更新对端信息（按名称查找）
     * @param originalName 原始名称，用于查找要更新的对端
     * @param newName 新名称
     * @param newPeerName 新对端名称
     * @param newPublicKey 新公钥
     * @return 是否更新成功
     */
    fun updatePeer(originalName: String, newName: String, newPeerName: String, newPublicKey: String): Boolean {
        val index = peers.indexOfFirst { it.name == originalName }
        if (index != -1) {
            peers[index] = Peer(newName, newPeerName, newPublicKey)
            saveConfig()
            return true
        }
        return false
    }
        
    /**
     * 更新对端信息（按对端名称查找）
     * @param originalPeerName 原始对端名称，用于查找要更新的对端
     * @param newName 新名称
     * @param newPeerName 新对端名称
     * @param newPublicKey 新公钥
     * @return 是否更新成功
     */
    fun updatePeerByPeerName(originalPeerName: String, newName: String, newPeerName: String, newPublicKey: String): Boolean {
        val index = peers.indexOfFirst { it.peerName == originalPeerName }
        if (index != -1) {
            peers[index] = Peer(newName, newPeerName, newPublicKey)
            saveConfig()
            return true
        }
        return false
    }
    
    /**
     * 更新对端信息（按公钥查找）
     * @param originalPublicKey 原始公钥，用于查找要更新的对端
     * @param newName 新名称
     * @param newPeerName 新对端名称
     * @param newPublicKey 新公钥（如果不需要更新公钥，可以传入原始公钥）
     * @return 是否更新成功
     */
    fun updatePeerByPublicKey(originalPublicKey: String, newName: String, newPeerName: String, newPublicKey: String): Boolean {
        val index = peers.indexOfFirst { it.publicKey == originalPublicKey }
        if (index != -1) {
            peers[index] = Peer(newName, newPeerName, newPublicKey)
            saveConfig()
            return true
        }
        return false
    }

    fun clearPeers() {
        peers.clear()
        saveConfig()
    }
    
    /**
     * 获取启用的工具标识
     * @return 启用的工具标识（整数值，每个位代表一个工具）
     */
    fun getEnabledTools(): Int = enabledTools
    
    /**
     * 设置启用的工具标识
     * @param toolFlags 工具标识（整数值，每个位代表一个工具）
     */
    fun setEnabledTools(toolFlags: Int) {
        this.enabledTools = toolFlags
        saveConfig()
    }
    
    /**
     * 获取缓存时间（分钟）
     * @return 缓存时间，单位为分钟
     */
    fun getCacheTime(): Int = cacheTime
    
    /**
     * 设置缓存时间（分钟）
     * @param minutes 缓存时间，单位为分钟
     */
    fun setCacheTime(minutes: Int) {
        this.cacheTime = if (minutes < 0) 0 else minutes
        saveConfig()
    }
}