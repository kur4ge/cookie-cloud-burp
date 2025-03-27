package com.kur4ge.cookie_cloud.utils

/**
 * 版本信息管理类
 * 用于存储和提供 Cookie Cloud 的版本信息
 */
object Version {
    // 主版本号
    private const val MAJOR = 1
    
    // 次版本号
    private const val MINOR = 0
    
    // 修订版本号
    private const val PATCH = 0
    
    // 构建版本号（可选）
    private const val BUILD = "beta"
    
    // 完整版本号
    private val FULL_VERSION = if (BUILD.isNotEmpty()) {
        "$MAJOR.$MINOR.$PATCH-$BUILD"
    } else {
        "$MAJOR.$MINOR.$PATCH"
    }
    
    /**
     * 获取主版本号
     * @return 主版本号
     */
    fun getMajor(): Int = MAJOR
    
    /**
     * 获取次版本号
     * @return 次版本号
     */
    fun getMinor(): Int = MINOR
    
    /**
     * 获取修订版本号
     * @return 修订版本号
     */
    fun getPatch(): Int = PATCH
    
    /**
     * 获取构建版本号
     * @return 构建版本号
     */
    fun getBuild(): String = BUILD
    
    /**
     * 获取完整版本号
     * @return 完整版本号字符串，格式为：主版本.次版本.修订版本[-构建版本]
     */
    fun getFullVersion(): String = FULL_VERSION
    
    /**
     * 获取版本号的简短描述
     * @return 版本号描述
     */
    fun getVersionDescription(): String {
        return "Cookie Cloud v$FULL_VERSION"
    }
    
    /**
     * 检查是否为测试版
     * @return 如果是测试版返回true，否则返回false
     */
    fun isBeta(): Boolean {
        return BUILD.contains("beta", ignoreCase = true)
    }
}