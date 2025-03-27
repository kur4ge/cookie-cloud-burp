package com.kur4ge.cookie_cloud.ui

import burp.api.montoya.MontoyaApi
import com.kur4ge.cookie_cloud.utils.Config
import com.kur4ge.cookie_cloud.utils.Crypto
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.border.TitledBorder

class LocalKeyPanel(private val api: MontoyaApi) {
    private val panel = JPanel(BorderLayout())
    private val localPublicKeyField = JTextField("", 30)
    private val localPrivateKeyField = JPasswordField("", 30)
    private var privateKeyVisible = false
    private val config = Config.getInstance()
    
    init {
        setupUI()
        loadExistingKeyPair()
    }
    
    private fun setupUI() {
        // 本端密钥管理面板
        val localKeyPanel = JPanel(GridBagLayout())
        localKeyPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "本端密钥管理",
            TitledBorder.LEFT,
            TitledBorder.TOP
        )
        
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL
        
        // 本端公钥 - 调整大小
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        localKeyPanel.add(JLabel("公钥:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.weightx = 1.0
        localPublicKeyField.isEditable = false
        localPublicKeyField.preferredSize = Dimension(300, localPublicKeyField.preferredSize.height)
        localKeyPanel.add(localPublicKeyField, gbc)
        
        // 生成新密钥对按钮
        gbc.gridx = 2
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        val generateKeyPairButton = JButton("生成新密钥对")
        localKeyPanel.add(generateKeyPairButton, gbc)
        
        // 本端私钥 - 调整大小并设为可编辑
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        localKeyPanel.add(JLabel("私钥:"), gbc)
        
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 1.0
        localPrivateKeyField.isEditable = true
        localPrivateKeyField.preferredSize = Dimension(300, localPrivateKeyField.preferredSize.height)
        localKeyPanel.add(localPrivateKeyField, gbc)
        
        // 显示/隐藏私钥按钮
        gbc.gridx = 2
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        val togglePrivateKeyButton = JButton("显示")
        localKeyPanel.add(togglePrivateKeyButton, gbc)
        
        // 添加更新私钥按钮
        gbc.gridx = 3
        gbc.gridy = 1
        val savePrivateKeyButton = JButton("更新私钥")
        localKeyPanel.add(savePrivateKeyButton, gbc)
        
        panel.add(localKeyPanel, BorderLayout.NORTH)
        
        // 显示/隐藏私钥按钮事件
        togglePrivateKeyButton.addActionListener {
            privateKeyVisible = !privateKeyVisible
            if (privateKeyVisible) {
                localPrivateKeyField.echoChar = 0.toChar() // 显示实际文本
                togglePrivateKeyButton.text = "隐藏"
            } else {
                localPrivateKeyField.echoChar = '•' // 使用点来隐藏文本
                togglePrivateKeyButton.text = "显示"
            }
        }
        
        // 更新私钥按钮事件
        savePrivateKeyButton.addActionListener {
            val privateKey = String(localPrivateKeyField.password)
            if (privateKey.isNotEmpty()) {
                try {
                    // 从私钥恢复密钥对，并更新公钥显示
                    val trimmedPrivateKey = if (privateKey.startsWith("0x")) privateKey.substring(2) else privateKey
                    val keyPair = Crypto.getKeyPairFromPrivateKey(trimmedPrivateKey)
                    val publicKey = "0x" + keyPair.publicKey
                    val formattedPrivateKey = if (privateKey.startsWith("0x")) privateKey else "0x"+ privateKey
                    
                    localPublicKeyField.text = publicKey
                    localPrivateKeyField.text = formattedPrivateKey
                    
                    // 保存到配置
                    config.setLocalPrivateKey(formattedPrivateKey)
                    
                    JOptionPane.showMessageDialog(panel, "私钥已保存，公钥已更新", "保存成功", JOptionPane.INFORMATION_MESSAGE)
                } catch (e: Exception) {
                    api.logging().logToError("更新私钥失败: ${e.message}")
                    JOptionPane.showMessageDialog(panel, "私钥格式无效: ${e.message}", "保存失败", JOptionPane.ERROR_MESSAGE)
                }
            } else {
                JOptionPane.showMessageDialog(panel, "私钥不能为空", "保存失败", JOptionPane.ERROR_MESSAGE)
            }
        }
        
        // 生成新密钥对按钮事件
        generateKeyPairButton.addActionListener {
            try {
                // 使用Crypto类生成真实的密钥对
                val keyPair = Crypto.generateKeyPair()
                val publicKey = "0x" + keyPair.publicKey
                val privateKey = "0x" + keyPair.privateKey
                
                localPublicKeyField.text = publicKey
                localPrivateKeyField.text = privateKey
                
                // 保存到配置
                config.setLocalPrivateKey(privateKey)
                
                JOptionPane.showMessageDialog(panel, "密钥对生成成功", "成功", JOptionPane.INFORMATION_MESSAGE)
            } catch (e: Exception) {
                api.logging().logToError("生成密钥对失败: ${e.message}")
                JOptionPane.showMessageDialog(panel, "生成密钥对失败: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
    
    // 加载已有的密钥对
    private fun loadExistingKeyPair() {
        try {
            // 从配置加载密钥对
            val privateKey = "0x" + config.getLocalPrivateKey()
            val publicKey = "0x" + config.getLocalPublicKey()
                
            if (privateKey.isNotEmpty() && publicKey.isNotEmpty()) {
                localPrivateKeyField.text = privateKey
                localPublicKeyField.text = publicKey
            } else {
                // 如果配置中没有密钥对，则生成一个新的
                val keyPair = Crypto.generateKeyPair()
                val newPublicKey = "0x" + keyPair.publicKey
                val newPrivateKey = "0x" + keyPair.privateKey
                
                localPublicKeyField.text = newPublicKey
                localPrivateKeyField.text = newPrivateKey
                
                // 保存到配置
                config.setLocalPrivateKey(newPrivateKey)
            }
        } catch (e: Exception) {
            api.logging().logToError("加载/生成初始密钥对失败: ${e.message}")
            // 使用空值作为后备
            localPublicKeyField.text = ""
            localPrivateKeyField.text = ""
        }
    }
    
    fun getPanel(): JPanel {
        return panel
    }
    
    fun getLocalPublicKey(): String {
        return localPublicKeyField.text
    }
    
    fun getLocalPrivateKey(): String {
        return String(localPrivateKeyField.password)
    }
    
    fun setLocalPublicKey(publicKey: String) {
        localPublicKeyField.text = publicKey
    }
    
    fun setLocalPrivateKey(privateKey: String) {
        localPrivateKeyField.text = privateKey
    }
}