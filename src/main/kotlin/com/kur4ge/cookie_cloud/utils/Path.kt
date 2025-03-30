package com.kur4ge.cookie_cloud.utils

import java.io.File
import java.nio.file.Paths

/**
 * 路径工具类
 * 提供获取配置文件路径等功能
 */
class Path {
    companion object {
        /**
         * 获取 Burp 配置文件路径
         * @param filename 配置文件名
         * @return 完整的配置文件路径
         */
        fun getConfig(filename: String): String {
            val homeDir = System.getProperty("user.home")
            
            // Windows 系统
            if (homeDir.startsWith("C:") && System.getenv("APPDATA") != null) {
                return Paths.get(System.getenv("APPDATA"), "BurpSuite", "ConfigLibrary", filename).toString()
            } 
            // 其他系统 (Linux/Mac)
            else {
                return Paths.get(homeDir, ".BurpSuite", "ConfigLibrary", filename).toString()
            }
        }
        
        /**
         * 确保配置文件目录存在
         * @param configPath 配置文件路径
         * @return 配置文件路径
         */
        fun ensureConfigDirExists(configPath: String): String {
            val configFile = File(configPath)
            val configDir = configFile.parentFile
            
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            
            return configPath
        }
    }
}