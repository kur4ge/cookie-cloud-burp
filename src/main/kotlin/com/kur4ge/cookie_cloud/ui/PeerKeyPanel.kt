package com.kur4ge.cookie_cloud.ui

import burp.api.montoya.MontoyaApi
import com.kur4ge.cookie_cloud.utils.Config
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableModel

class PeerKeyPanel(private val api: MontoyaApi) {
    private val panel = JPanel(BorderLayout())
    private val peerTableModel = DefaultTableModel(arrayOf("名称", "对端名称", "公钥"), 0)
    private val peerTable = JTable(peerTableModel)
    private val config = Config.getInstance()
    
    init {
        setupUI()
        loadPeers()
    }
    
    private fun setupUI() {
        // 对端密钥管理面板
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "对端密钥管理", 
            TitledBorder.LEFT, 
            TitledBorder.TOP
        )
        
        // 表格设置
        peerTable.rowHeight = 30
        
        // 添加表格选择监听器，用于编辑功能
        val nameField = JTextField(15)
        val peerNameField = JTextField(15)
        val keyField = JTextField(30)
        val addButton = JButton("添加对端")
        val editButton = JButton("更新对端")
        val deleteButton = JButton("删除对端")  // 添加删除按钮
        
        peerTable.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting && peerTable.selectedRow != -1) {
                val selectedRow = peerTable.selectedRow
                nameField.text = peerTableModel.getValueAt(selectedRow, 0) as String
                peerNameField.text = peerTableModel.getValueAt(selectedRow, 1) as String
                keyField.text = peerTableModel.getValueAt(selectedRow, 2) as String
                
                // 切换按钮显示
                addButton.isVisible = false
                editButton.isVisible = true
                deleteButton.isVisible = true  // 显示删除按钮
            }
        }
        
        val scrollPane = JScrollPane(peerTable)
        scrollPane.preferredSize = Dimension(600, 200)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 添加新对端的面板
        val addPeerPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        addPeerPanel.add(JLabel("名称:"))
        addPeerPanel.add(nameField)
        addPeerPanel.add(JLabel("对端名称:"))
        addPeerPanel.add(peerNameField)
        addPeerPanel.add(JLabel("公钥:"))
        addPeerPanel.add(keyField)
        addPeerPanel.add(addButton)
        addPeerPanel.add(editButton)
        addPeerPanel.add(deleteButton)  // 添加删除按钮到面板
        
        // 初始状态下隐藏编辑和删除按钮
        editButton.isVisible = false
        deleteButton.isVisible = false  // 初始隐藏删除按钮
        
        // 添加重置和取消按钮
        val resetButton = JButton("重置")
        val cancelButton = JButton("取消")
        
        addPeerPanel.add(resetButton)
        addPeerPanel.add(cancelButton)
        
        // 初始状态下显示重置按钮，隐藏取消按钮
        resetButton.isVisible = true
        cancelButton.isVisible = false
        panel.add(addPeerPanel, BorderLayout.SOUTH)
        
        // 添加对端按钮事件
        addButton.addActionListener {
            val name = nameField.text
            val peerName = peerNameField.text
            val key = keyField.text
            if (name.isNotEmpty() && key.isNotEmpty() && peerName.isNotEmpty()) {
                peerTableModel.addRow(arrayOf(name, peerName, key))
                config.addPeer(name, peerName, key)
                nameField.text = ""
                peerNameField.text = ""
                keyField.text = ""
            } else {
                JOptionPane.showMessageDialog(panel, "名称、对端名称和公钥不能为空", "输入错误", JOptionPane.ERROR_MESSAGE)
            }
        }
        
        // 更新对端按钮事件
        editButton.addActionListener {
            val name = nameField.text
            val peerName = peerNameField.text
            val key = keyField.text
            val selectedRow = peerTable.selectedRow
            
            if (selectedRow != -1 && name.isNotEmpty() && key.isNotEmpty() && peerName.isNotEmpty()) {
                // 获取原始值，用于在配置中查找
                val originalName = peerTableModel.getValueAt(selectedRow, 0) as String
                val originalPeerName = peerTableModel.getValueAt(selectedRow, 1) as String
                val originalPublicKey = peerTableModel.getValueAt(selectedRow, 2) as String
                
                // 更新表格数据
                peerTableModel.setValueAt(name, selectedRow, 0)
                peerTableModel.setValueAt(peerName, selectedRow, 1)
                peerTableModel.setValueAt(key, selectedRow, 2)
                
                // 依次尝试使用不同的方法更新配置
                var updateSuccess = config.updatePeer(originalName, name, peerName, key)
                
                // 如果按名称更新失败，尝试按对端名称更新
                if (!updateSuccess) {
                    updateSuccess = config.updatePeerByPeerName(originalPeerName, name, peerName, key)
                }
                
                // 如果按对端名称更新也失败，尝试按公钥更新
                if (!updateSuccess) {
                    updateSuccess = config.updatePeerByPublicKey(originalPublicKey, name, peerName, key)
                }
                
                // 如果所有更新方法都失败，则添加为新对端
                if (!updateSuccess) {
                    config.addPeer(name, peerName, key)
                    JOptionPane.showMessageDialog(panel, "未找到原对端信息，已添加为新对端", "添加成功", JOptionPane.INFORMATION_MESSAGE)
                } else {
                    JOptionPane.showMessageDialog(panel, "对端信息已更新", "更新成功", JOptionPane.INFORMATION_MESSAGE)
                }
                
                // 清空输入框并重置按钮状态
                nameField.text = ""
                peerNameField.text = ""
                keyField.text = ""
                addButton.isVisible = true
                editButton.isVisible = false
                deleteButton.isVisible = false  // 隐藏删除按钮
                
                // 取消表格选择
                peerTable.clearSelection()
            } else {
                JOptionPane.showMessageDialog(panel, "名称、对端名称和公钥不能为空", "输入错误", JOptionPane.ERROR_MESSAGE)
            }
        }
        
        // 添加删除对端按钮事件
        deleteButton.addActionListener {
            val selectedRow = peerTable.selectedRow
            
            if (selectedRow != -1) {
                val name = peerTableModel.getValueAt(selectedRow, 0) as String
                
                // 确认删除
                val confirm = JOptionPane.showConfirmDialog(
                    panel,
                    "确定要删除对端 \"$name\" 吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
                )
                
                if (confirm == JOptionPane.YES_OPTION) {
                    // 从配置中删除对端
                    config.removePeerByName(name)
                    // 从表格中删除行
                    peerTableModel.removeRow(selectedRow)
                    
                    // 清空输入框并重置按钮状态
                    nameField.text = ""
                    peerNameField.text = ""
                    keyField.text = ""
                    addButton.isVisible = true
                    editButton.isVisible = false
                    deleteButton.isVisible = false
                    resetButton.isVisible = true  // 显示重置按钮
                    cancelButton.isVisible = false  // 隐藏取消按钮
                    
                    // 取消表格选择
                    peerTable.clearSelection()
                }
            }
        }
        
    
        
        // 重置按钮事件
        resetButton.addActionListener {
            // 清空输入框
            nameField.text = ""
            peerNameField.text = ""
            keyField.text = ""
        }
        
        // 取消按钮事件
        cancelButton.addActionListener {
            // 清空输入框并重置按钮状态
            nameField.text = ""
            peerNameField.text = ""
            keyField.text = ""
            addButton.isVisible = true
            editButton.isVisible = false
            deleteButton.isVisible = false
            resetButton.isVisible = true
            cancelButton.isVisible = false
            
            // 取消表格选择
            peerTable.clearSelection()
        }
        
        // 修改表格选择监听器处理方式
        peerTable.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting && peerTable.selectedRow != -1) {
                val selectedRow = peerTable.selectedRow
                nameField.text = peerTableModel.getValueAt(selectedRow, 0) as String
                peerNameField.text = peerTableModel.getValueAt(selectedRow, 1) as String
                keyField.text = peerTableModel.getValueAt(selectedRow, 2) as String
                
                // 切换按钮显示
                addButton.isVisible = false
                editButton.isVisible = true
                deleteButton.isVisible = true
                resetButton.isVisible = false
                cancelButton.isVisible = true
            }
        }
        
        // 在编辑按钮事件处理中也需要更新重置和取消按钮的可见性
        val originalEditAction = editButton.actionListeners[0]
        editButton.removeActionListener(originalEditAction)
        editButton.addActionListener {
            originalEditAction.actionPerformed(it)
            resetButton.isVisible = true
            cancelButton.isVisible = false
        }
    }
    
    // 加载对端列表
    fun loadPeers() {
        // 清空表格
        while (peerTableModel.rowCount > 0) {
            peerTableModel.removeRow(0)
        }
        
        // 从配置加载对端列表
        for (peer in config.getPeers()) {
            peerTableModel.addRow(arrayOf(peer.name, peer.peerName, "0x" + peer.publicKey))
        }
    }
    
    fun getPanel(): JPanel {
        return panel
    }
    
    // 表格中的按钮渲染器
    private class ButtonRenderer : JButton(), javax.swing.table.TableCellRenderer {
        init {
            isOpaque = true
        }
        
        override fun getTableCellRendererComponent(
            table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): java.awt.Component {
            text = value.toString()
            return this
        }
    }
    
    // 表格中的按钮编辑器
    private class ButtonEditor(
        checkbox: JCheckBox, 
        private val config: Config, 
        private val tableModel: DefaultTableModel
    ) : DefaultCellEditor(checkbox) {
        private val button = JButton()
        private var clicked = false
        private var row = 0
        private var column = 0
        private var table: JTable? = null
        
        init {
            button.isOpaque = true
            button.addActionListener { 
                fireEditingStopped()
                clicked = true
            }
        }
        
        override fun getTableCellEditorComponent(
            table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
        ): java.awt.Component {
            this.table = table
            this.row = row
            this.column = column
            button.text = value.toString()
            return button
        }
        
        override fun getCellEditorValue(): Any {
            if (clicked) {
                // 从配置中删除对端
                val name = tableModel.getValueAt(row, 0) as String
                config.removePeerByName(name)
                // 从表格中删除行
                tableModel.removeRow(row)
            }
            clicked = false
            return button.text
        }
        
        override fun stopCellEditing(): Boolean {
            clicked = false
            return super.stopCellEditing()
        }
    }
}