package com.kur4ge.cookie_cloud.ui

import burp.api.montoya.MontoyaApi
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.EmptyBorder
import com.kur4ge.cookie_cloud.utils.Config

class CookieCloudUI(private val api: MontoyaApi) {
    private val mainPanel = JPanel(BorderLayout())
    private val config = Config.getInstance()
    private val basicPanel = BasicPanel(api)
    private val localKeyPanel = LocalKeyPanel(api)
    private val peerKeyPanel = PeerKeyPanel(api)
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        // 主面板设置
        mainPanel.border = EmptyBorder(10, 10, 10, 10)

        // 创建顶部面板，包含基本设置和本端密钥
        val topPanel = JPanel(BorderLayout())
        topPanel.add(basicPanel.getPanel(), BorderLayout.NORTH)
        // 设置顶部面板的首选高度，使其更紧凑
        basicPanel.getPanel().preferredSize = Dimension(basicPanel.getPanel().preferredSize.width, basicPanel.getPanel().preferredSize.height)
        // 增加个分割线
        topPanel.add(JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH)

        // 创建标签页面板
        val tabbedPane = JTabbedPane()
        // 创建密钥管理面板（合并本地密钥和对端密钥）
        val keyManagementPanel = JPanel(BorderLayout())
        keyManagementPanel.border = EmptyBorder(5, 5, 5, 5)
        
        // 添加本地密钥面板到密钥管理面板顶部
        keyManagementPanel.add(localKeyPanel.getPanel(), BorderLayout.NORTH)
        // 添加对端密钥面板到密钥管理面板中央
        keyManagementPanel.add(peerKeyPanel.getPanel(), BorderLayout.CENTER)
        
        // 创建缓存管理面板
        val cacheManagementPanel = JPanel(BorderLayout())
        cacheManagementPanel.border = EmptyBorder(5, 5, 5, 5)
        
        // 添加缓存管理相关的组件
        cacheManagementPanel.add(JLabel("缓存管理功能正在开发中..."), BorderLayout.CENTER)
        
        // 将面板添加到标签页
        tabbedPane.addTab("密钥管理", keyManagementPanel)
        tabbedPane.addTab("缓存管理", cacheManagementPanel)
        

        // 创建主布局面板
        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.add(topPanel, BorderLayout.NORTH)
        // 添加一些垂直间距
        topPanel.border = EmptyBorder(0, 0, 5, 0)
        // 将内容面板添加到主面板
        mainContentPanel.add(tabbedPane, BorderLayout.CENTER)
        mainPanel.add(mainContentPanel, BorderLayout.CENTER)
    }

    /**
     * 获取UI面板
     * @return 主面板组件
     */
    fun getUI(): JPanel {
        return mainPanel
    }
}