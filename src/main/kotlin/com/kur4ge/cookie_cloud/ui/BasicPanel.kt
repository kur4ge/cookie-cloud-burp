package com.kur4ge.cookie_cloud.ui

import burp.api.montoya.MontoyaApi
import com.kur4ge.cookie_cloud.utils.Config
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.border.TitledBorder

class BasicPanel(private val api: MontoyaApi) {
    private val panel = JPanel(GridBagLayout())
    private val enableToggle = JToggleButton("启用", false)
    private val endpointField = JTextField("", 100)
    private val config = Config.getInstance()
    
    init {
        setupUI()
        loadConfig()
    }
    
    private fun setupUI() {
        // 添加标题边框
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "基本设置",
            TitledBorder.LEFT,
            TitledBorder.TOP
        )
        
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL
        
        // 启用开关
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        panel.add(JLabel("Cookie Cloud 状态:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 0
        panel.add(enableToggle, gbc)
        
        // Endpoint 设置
        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(JLabel("Endpoint:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        panel.add(endpointField, gbc)
        
        gbc.gridx = 3
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 0.0  // 重置权重
        val saveEndpointButton = JButton("保存")
        panel.add(saveEndpointButton, gbc)
        
        // 事件监听
        enableToggle.addActionListener { 
            val enabled = enableToggle.isSelected
            enableToggle.text = if (enabled) "已启用" else "已禁用"
            config.setEnabled(enabled)
        }
        
        saveEndpointButton.addActionListener {
            val endpoint = endpointField.text
            config.setEndpoint(endpoint)
        }
    }
    
    private fun loadConfig() {
        // 加载基本设置
        enableToggle.isSelected = config.isEnabled()
        enableToggle.text = if (enableToggle.isSelected) "已启用" else "已禁用"
        endpointField.text = config.getEndpoint()
    }
    
    fun getPanel(): JPanel {
        return panel
    }
    
    fun isEnabled(): Boolean {
        return enableToggle.isSelected
    }
    
    fun getEndpoint(): String {
        return endpointField.text
    }
    
    fun setEnabled(enabled: Boolean) {
        enableToggle.isSelected = enabled
        enableToggle.text = if (enabled) "已启用" else "已禁用"
    }
    
    fun setEndpoint(endpoint: String) {
        endpointField.text = endpoint
    }
}