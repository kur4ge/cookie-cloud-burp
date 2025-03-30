package com.kur4ge.cookie_cloud.ui

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import com.kur4ge.cookie_cloud.utils.Config
import com.kur4ge.cookie_cloud.utils.Version
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
    private val cacheTimeField = JTextField("10", 5)

    // 定义工具常量
    companion object {
        val TOOL_SUITE = 1 shl ToolType.SUITE.ordinal
        val TOOL_TARGET = 1 shl ToolType.TARGET.ordinal
        val TOOL_PROXY = 1 shl ToolType.PROXY.ordinal
        val TOOL_SCANNER = 1 shl ToolType.SCANNER.ordinal
        val TOOL_INTRUDER = 1 shl ToolType.INTRUDER.ordinal
        val TOOL_REPEATER = 1 shl ToolType.REPEATER.ordinal
        val TOOL_SEQUENCER =1 shl  ToolType.SEQUENCER.ordinal
        val TOOL_DECODER = 1 shl ToolType.DECODER.ordinal
        val TOOL_COMPARER = 1 shl ToolType.COMPARER.ordinal
        val TOOL_EXTENDER = 1 shl ToolType.EXTENSIONS.ordinal
        val TOOL_LOGGER = 1 shl ToolType.LOGGER.ordinal
        val TOOL_ORGANIZER = 1 shl ToolType.ORGANIZER.ordinal
        val TOOL_RECORDED_LOGIN_REPLAYER = 1 shl ToolType.RECORDED_LOGIN_REPLAYER.ordinal
    }
    
    // 工具选择复选框
    private val toolCheckboxes = mapOf(
        TOOL_PROXY to JCheckBox("Proxy", true),
        TOOL_REPEATER to JCheckBox("Repeater", true),
        TOOL_INTRUDER to JCheckBox("Intruder", true),
        TOOL_SCANNER to JCheckBox("Scanner", false),
        TOOL_TARGET to JCheckBox("Target", false),
        TOOL_SEQUENCER to JCheckBox("Sequencer", false),
        TOOL_DECODER to JCheckBox("Decoder", false),
        TOOL_COMPARER to JCheckBox("Comparer", false),
        TOOL_EXTENDER to JCheckBox("Extensions", false),
        TOOL_SUITE to JCheckBox("Suite", false),
        TOOL_LOGGER to JCheckBox("Logger", false),
        TOOL_ORGANIZER to JCheckBox("Organizer", false),
        TOOL_RECORDED_LOGIN_REPLAYER to JCheckBox("Recorded Login Replayer", false)
    )
    
    init {
        setupUI()
        loadConfig()
    }
    
    private fun setupUI() {
        // 添加标题边框
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            Version.getVersionDescription(),
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
        panel.add(JLabel("全局状态:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.weightx = 0.5
        panel.add(enableToggle, gbc)
        
        // 添加缓存时间设置
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("缓存时间(分钟):"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 0.5
        panel.add(cacheTimeField, gbc)
        
        // 创建工具选择面板
        val toolsPanel = JPanel()
        toolsPanel.border = BorderFactory.createTitledBorder("启用的工具")
        toolsPanel.layout = BoxLayout(toolsPanel, BoxLayout.Y_AXIS)
        
        // 创建行面板
        val row1 = JPanel()
        row1.layout = BoxLayout(row1, BoxLayout.X_AXIS)
        val row2 = JPanel()
        row2.layout = BoxLayout(row2, BoxLayout.X_AXIS)
        
        // 第一行添加7个工具
        row1.add(toolCheckboxes[TOOL_PROXY]!!)
        row1.add(Box.createHorizontalStrut(10))
        row1.add(toolCheckboxes[TOOL_REPEATER]!!)
        row1.add(Box.createHorizontalStrut(10))
        row1.add(toolCheckboxes[TOOL_INTRUDER]!!)
        row1.add(Box.createHorizontalStrut(10))
        row1.add(toolCheckboxes[TOOL_SCANNER]!!)
        row1.add(Box.createHorizontalStrut(10))
        row1.add(toolCheckboxes[TOOL_TARGET]!!)
        row1.add(Box.createHorizontalStrut(10))
        row1.add(toolCheckboxes[TOOL_SEQUENCER]!!)
        row1.add(Box.createHorizontalStrut(10))
        row1.add(toolCheckboxes[TOOL_DECODER]!!)
        
        // 第二行添加6个工具
        row2.add(toolCheckboxes[TOOL_COMPARER]!!)
        row2.add(Box.createHorizontalStrut(10))
        row2.add(toolCheckboxes[TOOL_EXTENDER]!!)
        row2.add(Box.createHorizontalStrut(10))
        row2.add(toolCheckboxes[TOOL_SUITE]!!)
        row2.add(Box.createHorizontalStrut(10))
        row2.add(toolCheckboxes[TOOL_LOGGER]!!)
        row2.add(Box.createHorizontalStrut(10))
        row2.add(toolCheckboxes[TOOL_ORGANIZER]!!)
        row2.add(Box.createHorizontalStrut(10))
        row2.add(toolCheckboxes[TOOL_RECORDED_LOGIN_REPLAYER]!!)
        row2.add(Box.createHorizontalStrut(10))
        
        toolsPanel.add(row1)
        toolsPanel.add(Box.createVerticalStrut(5))
        toolsPanel.add(row2)
        
        // 将工具选择面板放在启用按钮的右侧
        gbc.gridx = 2
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.weightx = 0.5
        panel.add(toolsPanel, gbc)
        
        // Endpoint 设置
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Endpoint:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 2
        gbc.gridwidth = 1
        gbc.weightx = 0.5
        panel.add(endpointField, gbc)
        
        gbc.gridx = 2
        gbc.gridy = 2
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        val saveEndpointButton = JButton("保存")
        panel.add(saveEndpointButton, gbc)
        
        // 事件监听
        enableToggle.addActionListener { 
            val enabled = enableToggle.isSelected
            enableToggle.text = if (enabled) "已启用" else "已禁用"
            config.setEnabled(enabled)
            
            // 更新复选框状态
            toolCheckboxes.forEach { (_, checkbox) ->
                checkbox.isEnabled = enabled
            }
        }
        
        // 为每个复选框添加事件监听
        toolCheckboxes.forEach { (_, checkbox) ->
            checkbox.addActionListener {
                updateEnabledTools()
            }
        }
        
        saveEndpointButton.addActionListener {
            val endpoint = endpointField.text
            config.setEndpoint(endpoint)
            
            // 保存缓存时间
            try {
                val cacheTime = cacheTimeField.text.toInt()
                config.setCacheTime(cacheTime)
            } catch (e: NumberFormatException) {
                JOptionPane.showMessageDialog(panel, "缓存时间必须是数字", "输入错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
    
    private fun updateEnabledTools() {
        var toolFlags = 0
        toolCheckboxes.forEach { (flag, checkbox) ->
            if (checkbox.isSelected) {
                toolFlags = toolFlags or flag
            }
        }
        config.setEnabledTools(toolFlags)
    }
    
    private fun loadConfig() {
        // 加载基本设置
        enableToggle.isSelected = config.isEnabled()
        enableToggle.text = if (enableToggle.isSelected) "已启用" else "已禁用"
        endpointField.text = config.getEndpoint()
        
        // 加载缓存时间
        val cacheTime = config.getCacheTime()
        if (cacheTime >= 0) {
            cacheTimeField.text = cacheTime.toString()
        }
        
        // 加载工具设置
        val enabledTools = config.getEnabledTools()
        toolCheckboxes.forEach { (flag, checkbox) ->
            checkbox.isSelected = (enabledTools and flag) != 0
            checkbox.isEnabled = enableToggle.isSelected
        }
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
        
        // 更新复选框状态
        toolCheckboxes.forEach { (_, checkbox) ->
            checkbox.isEnabled = enabled
        }
    }
    
    fun setEndpoint(endpoint: String) {
        endpointField.text = endpoint
    }
    
    fun getEnabledTools(): Int {
        var toolFlags = 0
        toolCheckboxes.forEach { (flag, checkbox) ->
            if (checkbox.isSelected) {
                toolFlags = toolFlags or flag
            }
        }
        return toolFlags
    }
}