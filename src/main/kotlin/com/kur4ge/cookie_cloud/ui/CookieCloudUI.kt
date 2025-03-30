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
        topPanel.add(localKeyPanel.getPanel(), BorderLayout.CENTER)

        // 设置顶部面板的首选高度，使其更紧凑
        basicPanel.getPanel().preferredSize = Dimension(basicPanel.getPanel().preferredSize.width, basicPanel.getPanel().preferredSize.height)
        localKeyPanel.getPanel().preferredSize = Dimension(localKeyPanel.getPanel().preferredSize.width, localKeyPanel.getPanel().preferredSize.height)
        
        // 创建主布局面板
        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.add(topPanel, BorderLayout.NORTH)
        mainContentPanel.add(peerKeyPanel.getPanel(), BorderLayout.CENTER)
        
        // 添加一些垂直间距
        topPanel.border = EmptyBorder(0, 0, 5, 0)
        
        // 将内容面板添加到主面板
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