/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 主UI控制器
 * 采用Material Design 3设计规范
 */

package com.diskanalyzer.controller;

import com.diskanalyzer.model.EnhancedFileNode;
import com.diskanalyzer.service.EnhancedScanService;
import com.diskanalyzer.service.ThemeManager;
import com.diskanalyzer.service.EnhancedFileManager;
import com.diskanalyzer.ui.component.ModernButton;
import com.diskanalyzer.ui.component.ModernProgressBar;
import com.diskanalyzer.ui.component.ModernTable;
import com.diskanalyzer.ui.component.ModernTree;
import com.diskanalyzer.visualization.VisualizationEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * 主UI控制器 - 采用Material Design 3设计规范
 * 统一管理界面逻辑和用户交互
 */
public class MainUIController {
    private final JFrame mainFrame;
    private final ThemeManager themeManager;
    private final EnhancedScanService scanService;
    private final EnhancedFileManager fileManager;
    private final VisualizationEngine visualizationEngine;
    
    // UI组件
    private ModernButton scanButton;
    private ModernButton cancelButton;
    private ModernButton deleteButton;
    private ModernButton recycleButton;
    private ModernButton themeButton;
    private ModernButton exportButton;
    private ModernButton searchButton;
    private JTextField pathField;
    private JTextField searchField;
    private JLabel statusLabel;
    private JLabel totalSizeLabel;
    private ModernProgressBar progressBar;
    private JComboBox<String> sortComboBox;
    private JCheckBox showHiddenCheckBox;
    
    // 主要显示组件
    private ModernTree fileTree;
    private ModernTable fileTable;
    private JPanel visualizationPanel;
    private JScrollPane visualizationScrollPane;
    private JSplitPane mainSplitPane;
    private JSplitPane rightSplitPane;
    
    // 数据模型
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private EnhancedFileNode currentRoot;
    private EnhancedFileNode originalRoot; // 保存原始根节点用于搜索
    
    // 工具
    private final DecimalFormat sizeFormat = new DecimalFormat("#.##");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private List<File> scanHistory = new ArrayList<>();
    
    public MainUIController(JFrame frame, ThemeManager themeManager) {
        this.mainFrame = frame;
        this.themeManager = themeManager;
        this.scanService = new EnhancedScanService();
        this.fileManager = new EnhancedFileManager();
        this.visualizationEngine = new VisualizationEngine();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        applyTheme();
        loadScanHistory();
    }
    
    private void initializeComponents() {
        // 创建现代化按钮
        scanButton = new ModernButton("📁 选择目录", ModernButton.Type.PRIMARY);
        cancelButton = new ModernButton("⏹️ 取消扫描", ModernButton.Type.SECONDARY);
        cancelButton.setEnabled(false);
        
        deleteButton = new ModernButton("🗑️ 删除文件", ModernButton.Type.DANGER);
        deleteButton.setEnabled(false);
        recycleButton = new ModernButton("♻️ 移到回收站", ModernButton.Type.WARNING);
        recycleButton.setEnabled(false);
        
        // themeButton = new ModernButton("🎨 主题", ModernButton.Type.SECONDARY); // 移除主题切换
        exportButton = new ModernButton("📊 导出报告", ModernButton.Type.SECONDARY);
        searchButton = new ModernButton("🔍 搜索", ModernButton.Type.SECONDARY);
        
        // 输入组件
        pathField = new JTextField(30);
        pathField.setFont(new Font("思源黑体", Font.PLAIN, 14));
        
        searchField = new JTextField(15);
        searchField.setFont(new Font("思源黑体", Font.PLAIN, 14));
        searchField.setToolTipText("输入文件名进行搜索");
        
        // 状态组件
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font("思源黑体", Font.BOLD, 14));
        statusLabel.setForeground(themeManager.getTextColor());
        totalSizeLabel = new JLabel("总大小: 0 B");
        totalSizeLabel.setFont(new Font("思源黑体", Font.PLAIN, 12));
        totalSizeLabel.setForeground(themeManager.getTextSecondaryColor());
        
        progressBar = new ModernProgressBar();
        progressBar.setVisible(false);
        
        // 下拉框
        sortComboBox = new JComboBox<>(new String[]{"按大小排序", "按名称排序", "按类型排序", "按修改时间排序"});
        
        // 复选框
        showHiddenCheckBox = new JCheckBox("显示隐藏文件");
        showHiddenCheckBox.setFont(new Font("思源黑体", Font.PLAIN, 12));
        
        // 主要显示组件
        fileTree = new ModernTree(themeManager);
        fileTable = new ModernTable(themeManager);
        visualizationPanel = createVisualizationPanel();
        
        // 初始化数据模型
        rootNode = new DefaultMutableTreeNode("请选择目录进行扫描");
        treeModel = new DefaultTreeModel(rootNode);
        fileTree.setModel(treeModel);
        
        // 设置表格列
        setupTableColumns();
        
        // 设置组件样式
        styleComponents();
    }
    
    private void styleComponents() {
        // 统一按钮样式
        Dimension buttonSize = new Dimension(120, 36);
        scanButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        deleteButton.setPreferredSize(buttonSize);
        recycleButton.setPreferredSize(buttonSize);
        // themeButton.setPreferredSize(new Dimension(80, 36)); // 移除主题切换
        exportButton.setPreferredSize(new Dimension(100, 36));
        searchButton.setPreferredSize(new Dimension(80, 36));
        
        // 设置下拉框样式
        sortComboBox.setFont(new Font("思源黑体", Font.PLAIN, 12));
        
        // 设置进度条样式
        progressBar.setPreferredSize(new Dimension(200, 24));
        progressBar.setStringPainted(true);
        
        // 设置表格样式
        fileTable.setRowHeight(32);
        fileTable.setFont(new Font("思源黑体", Font.PLAIN, 13));
        fileTable.getTableHeader().setFont(new Font("思源黑体", Font.BOLD, 13));
        
        // 设置树形样式
        fileTree.setFont(new Font("思源黑体", Font.PLAIN, 13));
        fileTree.setRowHeight(24);
    }
    
    private void setupTableColumns() {
        String[] columnNames = {"名称", "大小", "类型", "修改时间", "占用比例"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1 || columnIndex == 4) {
                    return String.class; // 大小和比例列
                }
                return String.class;
            }
        };
        fileTable.setModel(tableModel);
        
        // 设置列宽
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        // 设置渲染器
        fileTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());
    }
    
    private JPanel createVisualizationPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintVisualization(g);
            }
        };
        
        panel.setBackground(themeManager.getSurfaceColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // 添加鼠标事件监听
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    EnhancedFileNode clickedNode = visualizationEngine.handleMouseClick(e.getX(), e.getY());
                    if (clickedNode != null && clickedNode.isDirectory()) {
                        navigateToDirectory(clickedNode);
                    }
                }
            }
        });
        
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                EnhancedFileNode hoveredNode = visualizationEngine.handleMouseMove(e.getX(), e.getY());
                if (hoveredNode != null) {
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    panel.setToolTipText(hoveredNode.getName() + " - " + hoveredNode.getFormattedSize());
                } else {
                    panel.setCursor(Cursor.getDefaultCursor());
                    panel.setToolTipText(null);
                }
                panel.repaint();
            }
        });
        
        return panel;
    }
    
    private void setupLayout() {
        mainFrame.setLayout(new BorderLayout());
        
        // 顶部控制面板
        JPanel topPanel = createTopPanel();
        mainFrame.add(topPanel, BorderLayout.NORTH);
        
        // 主内容区域
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(350);
        mainSplitPane.setBorder(new EmptyBorder(10, 0, 10, 0));
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setBackground(themeManager.getBackgroundColor());
        
        // 左侧树形面板
        JPanel leftPanel = createLeftPanel();
        mainSplitPane.setLeftComponent(leftPanel);
        
        // 右侧面板
        JPanel rightPanel = createRightPanel();
        mainSplitPane.setRightComponent(rightPanel);
        
        mainFrame.add(mainSplitPane, BorderLayout.CENTER);
        
        // 底部状态栏
        JPanel statusPanel = createStatusPanel();
        mainFrame.add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 10, 10, 10));
        panel.setBackground(themeManager.getBackgroundColor());
        
        // 左侧控制按钮
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        leftControls.setBackground(themeManager.getBackgroundColor());
        leftControls.add(new JLabel("路径:"));
        leftControls.add(pathField);
        leftControls.add(scanButton);
        leftControls.add(cancelButton);
        leftControls.add(searchButton);
        leftControls.add(searchField);
        
        // 中间选项
        JPanel centerControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        centerControls.setBackground(themeManager.getBackgroundColor());
        centerControls.add(new JLabel("排序:"));
        centerControls.add(sortComboBox);
        centerControls.add(showHiddenCheckBox);
        
        // 右侧状态和控制
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        rightControls.setBackground(themeManager.getBackgroundColor());
        rightControls.add(totalSizeLabel);
        rightControls.add(statusLabel);
        rightControls.add(progressBar);
        // rightControls.add(themeButton); // 移除主题切换
        rightControls.add(exportButton);
        
        // 创建分割面板
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplitPane.setLeftComponent(leftControls);
        topSplitPane.setRightComponent(rightControls);
        topSplitPane.setDividerLocation(600);
        topSplitPane.setResizeWeight(0.7);
        topSplitPane.setBackground(themeManager.getBackgroundColor());
        
        panel.add(centerControls, BorderLayout.CENTER);
        panel.add(topSplitPane, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("目录结构"),
            new EmptyBorder(5, 5, 5, 5)
        ));
        panel.setBackground(themeManager.getSurfaceColor());
        
        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        treeScrollPane.setBackground(themeManager.getSurfaceColor());
        treeScrollPane.getViewport().setBackground(themeManager.getSurfaceColor());
        panel.add(treeScrollPane, BorderLayout.CENTER);
        
        // 添加树形工具栏
        JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        treeToolbar.setBackground(themeManager.getSurfaceColor());
        ModernButton expandAllButton = new ModernButton("展开全部", ModernButton.Type.SECONDARY);
        ModernButton collapseAllButton = new ModernButton("收起全部", ModernButton.Type.SECONDARY);
        
        expandAllButton.addActionListener(e -> fileTree.expandAll());
        collapseAllButton.addActionListener(e -> fileTree.collapseAll());
        
        treeToolbar.add(expandAllButton);
        treeToolbar.add(collapseAllButton);
        panel.add(treeToolbar, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getSurfaceColor());
        
        // 上方可视化面板
        JPanel vizPanel = new JPanel(new BorderLayout());
        vizPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("磁盘占用可视化"),
            new EmptyBorder(5, 5, 5, 5)
        ));
        vizPanel.setBackground(themeManager.getSurfaceColor());
        
        visualizationScrollPane = new JScrollPane(visualizationPanel);
        visualizationScrollPane.setBackground(themeManager.getSurfaceColor());
        visualizationScrollPane.getViewport().setBackground(themeManager.getSurfaceColor());
        vizPanel.add(visualizationScrollPane, BorderLayout.CENTER);
        
        // 下方表格面板
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("文件详情 (双击目录可进入)"),
            new EmptyBorder(5, 5, 5, 5)
        ));
        tablePanel.setBackground(themeManager.getSurfaceColor());
        
        JScrollPane tableScrollPane = new JScrollPane(fileTable);
        tableScrollPane.setBackground(themeManager.getSurfaceColor());
        tableScrollPane.getViewport().setBackground(themeManager.getSurfaceColor());
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // 文件操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        buttonPanel.setBackground(themeManager.getSurfaceColor());
        buttonPanel.add(deleteButton);
        buttonPanel.add(recycleButton);
        
        // 添加右键菜单
        JPopupMenu tablePopupMenu = createTablePopupMenu();
        fileTable.addContextMenu(tablePopupMenu);
        
        tablePanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 分割面板
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setDividerLocation(350);
        rightSplitPane.setTopComponent(vizPanel);
        rightSplitPane.setBottomComponent(tablePanel);
        rightSplitPane.setOneTouchExpandable(true);
        rightSplitPane.setBackground(themeManager.getBackgroundColor());
        
        panel.add(rightSplitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPopupMenu createTablePopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem openItem = new JMenuItem("打开文件");
        JMenuItem deleteItem = new JMenuItem("删除文件");
        JMenuItem recycleItem = new JMenuItem("移到回收站");
        JMenuItem propertiesItem = new JMenuItem("属性");
        
        openItem.addActionListener(e -> openSelectedFile());
        deleteItem.addActionListener(e -> deleteSelectedFiles(false));
        recycleItem.addActionListener(e -> deleteSelectedFiles(true));
        propertiesItem.addActionListener(e -> showFileProperties());
        
        popupMenu.add(openItem);
        popupMenu.addSeparator();
        popupMenu.add(deleteItem);
        popupMenu.add(recycleItem);
        popupMenu.addSeparator();
        popupMenu.add(propertiesItem);
        
        return popupMenu;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.setBackground(themeManager.getStatusBarColor());
        
        // 左侧提示
        JLabel tipLabel = new JLabel("💡 提示: 选择文件后可进行删除或移动到回收站操作");
        tipLabel.setFont(new Font("思源黑体", Font.PLAIN, 12));
        tipLabel.setForeground(themeManager.getTextColor());
        
        // 右侧版权信息
        JLabel copyrightLabel = new JLabel("© 2026 Disk Analyzer v2.0");
        copyrightLabel.setFont(new Font("思源黑体", Font.PLAIN, 10));
        copyrightLabel.setForeground(themeManager.getTextSecondaryColor());
        
        panel.add(tipLabel, BorderLayout.WEST);
        panel.add(copyrightLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // 扫描按钮
        scanButton.addActionListener(e -> selectAndScanDirectory());
        
        // 取消按钮
        cancelButton.addActionListener(e -> cancelScan());
        
        // 主题切换 - 已移除
        // themeButton.addActionListener(e -> toggleTheme());
        
        // 导出按钮
        exportButton.addActionListener(e -> exportReport());
        
        // 搜索按钮
        searchButton.addActionListener(e -> performSearch());
        
        // 排序选择
        sortComboBox.addActionListener(e -> sortTable());
        
        // 显示隐藏文件
        showHiddenCheckBox.addActionListener(e -> rescanCurrentDirectory());
        
        // 路径输入
        pathField.addActionListener(e -> scanFromPath());
        
        // 搜索字段回车
        searchField.addActionListener(e -> performSearch());
        
        // 树节点选择
        fileTree.addTreeSelectionListener(e -> handleTreeSelection());
        
        // 表格双击
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleTableDoubleClick();
                }
            }
        });
        
        // 表格选择监听
        fileTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleTableSelection();
            }
        });
        
        // 文件操作按钮
        deleteButton.addActionListener(e -> deleteSelectedFiles(false));
        recycleButton.addActionListener(e -> deleteSelectedFiles(true));
        
        // 窗口焦点监听 - 修复窗口切换后字体变白问题
        mainFrame.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
                // 窗口获得焦点时重新应用主题
                SwingUtilities.invokeLater(() -> {
                    applyTheme();
                    if (fileTable != null) {
                        fileTable.repaint();
                    }
                    if (fileTree != null) {
                        fileTree.repaint();
                    }
                });
            }
            
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                // 窗口失去焦点时不做特殊处理
            }
        });
        
        // 扫描服务回调
        scanService.setProgressListener(new EnhancedScanService.ScanProgressListener() {
            @Override
            public void onProgressUpdate(String message) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(message);
                    progressBar.setString(message);
                });
            }
            
            @Override
            public void onScanComplete(com.diskanalyzer.model.EnhancedFileNode rootNode) {
                SwingUtilities.invokeLater(() -> handleScanComplete(rootNode));
            }
            
            @Override
            public void onScanFailed(String error) {
                SwingUtilities.invokeLater(() -> handleScanFailed(error));
            }
        });
    }
    
    private void applyTheme() {
        themeManager.applyTheme(mainFrame);
        updateVisualization();
        
        // 更新按钮颜色
        Color primaryColor = themeManager.getPrimaryColor();
        scanButton.setBackground(primaryColor);
        
        // 更新状态标签颜色
        statusLabel.setForeground(themeManager.getTextColor());
        totalSizeLabel.setForeground(themeManager.getTextSecondaryColor());
        
        // 更新复选框颜色
        showHiddenCheckBox.setForeground(themeManager.getTextColor());
        showHiddenCheckBox.setBackground(themeManager.getSurfaceColor());
        
        // 更新下拉框颜色
        sortComboBox.setForeground(themeManager.getTextColor());
        sortComboBox.setBackground(themeManager.getSurfaceColor());
        
        // 更新状态栏颜色
        Component statusPanel = ((BorderLayout)mainFrame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        if (statusPanel instanceof JPanel) {
            ((JPanel)statusPanel).setBackground(themeManager.getStatusBarColor());
        }
        
        // 更新所有面板背景色（修复暗色主题显示异常）
        updatePanelColors();
        
        // 强制刷新表格和树形控件的主题
        if (fileTable != null) {
            fileTable.applyTheme();
            // 重新设置选择颜色
            fileTable.setSelectionBackground(themeManager.getPrimaryColor());
        }
        
        if (fileTree != null) {
            fileTree.applyTheme();
        }
        
        // 强制重绘所有组件
        SwingUtilities.invokeLater(() -> {
            mainFrame.repaint();
            if (fileTable != null) fileTable.repaint();
            if (fileTree != null) fileTree.repaint();
        });
    }
    
    private void loadScanHistory() {
        // 从配置文件加载扫描历史
        // 这里简化实现，实际应该从配置文件读取
        System.out.println("扫描历史加载完成");
    }
    
    private void selectAndScanDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择要分析的目录");
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = chooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            pathField.setText(selectedDir.getAbsolutePath());
            
            // 添加到扫描历史
            addToScanHistory(selectedDir);
            
            startScan(selectedDir);
        }
    }
    
    private void addToScanHistory(File directory) {
        if (!scanHistory.contains(directory)) {
            scanHistory.add(directory);
            if (scanHistory.size() > 10) { // 限制历史记录数量
                scanHistory.remove(0);
            }
        }
    }
    
    private void startScan(File directory) {
        scanButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("正在扫描...");
        
        // 清空当前数据
        if (currentRoot != null) {
            currentRoot = null;
            updateTableView(null);
            updateVisualization();
        }
        
        scanService.startScan(directory);
    }
    
    private void cancelScan() {
        scanService.cancelScan();
        resetScanUI();
        statusLabel.setText("扫描已取消");
    }
    
    private void handleScanComplete(com.diskanalyzer.model.EnhancedFileNode rootNode) {
        // 直接使用增强版节点，无需转换
        currentRoot = rootNode;
        originalRoot = currentRoot; // 保存原始根节点
        
        updateTreeView(currentRoot);
        updateTableView(currentRoot);
        updateVisualization();
        updateTotalSize();
        resetScanUI();
        
        // 确保界面组件可见
        ensureComponentsVisible();
        
        statusLabel.setText("扫描完成 - 共扫描 " + scanService.getScannedFilesCount() + " 个文件");
        
        // 显示扫描摘要
        showScanSummary();
    }
    
    private void handleScanFailed(String error) {
        resetScanUI();
        statusLabel.setText("扫描失败: " + error);
        JOptionPane.showMessageDialog(mainFrame, "扫描失败: " + error, "错误", JOptionPane.ERROR_MESSAGE);
    }
    
    private void resetScanUI() {
        scanButton.setEnabled(true);
        cancelButton.setEnabled(false);
        progressBar.setVisible(false);
        deleteButton.setEnabled(currentRoot != null);
        recycleButton.setEnabled(currentRoot != null);
    }
    
    /**
     * 更新所有面板的背景色（修复暗色主题显示异常）
     */
    private void updatePanelColors() {
        // 更新主面板背景色
        if (mainFrame != null) {
            mainFrame.getContentPane().setBackground(themeManager.getBackgroundColor());
        }
        
        // 更新控制面板背景色
        Component[] components = mainFrame.getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setBackground(themeManager.getBackgroundColor());
                updateChildPanelColors((JPanel) comp);
            }
        }
        
        // 更新分割面板背景色
        if (mainSplitPane != null) {
            mainSplitPane.setBackground(themeManager.getBackgroundColor());
        }
        if (rightSplitPane != null) {
            rightSplitPane.setBackground(themeManager.getBackgroundColor());
        }
    }
    
    /**
     * 递归更新子面板背景色
     */
    private void updateChildPanelColors(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setBackground(themeManager.getSurfaceColor());
                updateChildPanelColors((JPanel) comp);
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                scrollPane.setBackground(themeManager.getSurfaceColor());
                scrollPane.getViewport().setBackground(themeManager.getSurfaceColor());
            }
        }
    }
    
    /**
     * 确保所有界面组件正确可见
     */
    private void ensureComponentsVisible() {
        // 确保主界面组件可见
        mainFrame.revalidate();
        mainFrame.repaint();
        
        // 更新面板颜色
        updatePanelColors();
        
        // 确保表格和树形控件可见
        if (fileTable != null) {
            fileTable.revalidate();
            fileTable.repaint();
            // 修复字体颜色
            fileTable.setForeground(themeManager.getTextColor());
        }
        
        if (fileTree != null) {
            fileTree.revalidate();
            fileTree.repaint();
            // 修复字体颜色
            fileTree.setForeground(themeManager.getTextColor());
        }
        
        // 确保可视化面板可见
        if (visualizationPanel != null) {
            visualizationPanel.revalidate();
            visualizationPanel.repaint();
        }
        
        // 确保主分割面板正确布局
        if (mainSplitPane != null) {
            mainSplitPane.revalidate();
            mainSplitPane.repaint();
        }
        
        // 强制刷新UI
        SwingUtilities.invokeLater(() -> {
            mainFrame.invalidate();
            mainFrame.validate();
            mainFrame.repaint();
            
            // 再次确保字体颜色正确
            if (fileTable != null) {
                fileTable.setForeground(themeManager.getTextColor());
                fileTable.repaint();
            }
            if (fileTree != null) {
                fileTree.setForeground(themeManager.getTextColor());
                fileTree.repaint();
            }
        });
    }
    
    private void updateTotalSize() {
        if (currentRoot != null) {
            totalSizeLabel.setText("总大小: " + formatSize(currentRoot.getTotalSize()));
        } else {
            totalSizeLabel.setText("总大小: 0 B");
        }
    }
    
    private void showScanSummary() {
        if (currentRoot == null) return;
        
        int fileCount = (int) scanService.getScannedFilesCount();
        int folderCount = countFolders(currentRoot);
        long totalSize = currentRoot.getTotalSize();
        
        String summary = String.format(
            "扫描完成！\n\n文件数量: %d\n文件夹数量: %d\n总大小: %s\n\n最大文件: %s (%s)",
            fileCount, folderCount, formatSize(totalSize),
            currentRoot.getChildren().isEmpty() ? "无" : currentRoot.getChildren().get(0).getName(),
            currentRoot.getChildren().isEmpty() ? "0 B" : currentRoot.getChildren().get(0).getFormattedSize()
        );
        
        // 可以选择显示或隐藏摘要
        // JOptionPane.showMessageDialog(mainFrame, summary, "扫描摘要", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private int countFolders(EnhancedFileNode node) {
        int count = node.isDirectory() ? 1 : 0;
        for (EnhancedFileNode child : node.getChildren()) {
            count += countFolders(child);
        }
        return count;
    }
    
    // 这个方法不再需要，因为EnhancedScanService直接返回EnhancedFileNode
    private EnhancedFileNode convertToEnhancedNode(com.diskanalyzer.model.EnhancedFileNode node) {
        return node; // 直接返回，无需转换
    }
    
    private void updateTreeView(EnhancedFileNode rootNode) {
        DefaultMutableTreeNode newRoot;
        if (rootNode != null) {
            newRoot = new DefaultMutableTreeNode(rootNode);
            addTreeNodes(newRoot, rootNode);
        } else {
            newRoot = new DefaultMutableTreeNode("请选择目录进行扫描");
        }
        
        treeModel.setRoot(newRoot);
        fileTree.expandRow(0);
    }
    
    private void addTreeNodes(DefaultMutableTreeNode parentNode, EnhancedFileNode fileNode) {
        for (EnhancedFileNode child : fileNode.getChildren()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            parentNode.add(childNode);
            
            if (child.isDirectory() && !child.getChildren().isEmpty()) {
                addTreeNodes(childNode, child);
            }
        }
    }
    
    private void updateTableView(EnhancedFileNode parentNode) {
        DefaultTableModel model = (DefaultTableModel) fileTable.getModel();
        model.setRowCount(0);
        
        if (parentNode == null) {
            deleteButton.setEnabled(false);
            recycleButton.setEnabled(false);
            return;
        }
        
        long parentSize = parentNode.getTotalSize();
        
        // 调试信息
        System.out.println("DEBUG: updateTableView - parentSize: " + parentSize + ", children count: " + parentNode.getChildren().size());
        System.out.println("DEBUG: parentNode class: " + parentNode.getClass().getName());
        System.out.println("DEBUG: parentNode file: " + parentNode.getFile());
        
        for (EnhancedFileNode child : parentNode.getChildren()) {
            long childSize = child.getTotalSize();
            double percentage = 0;
            
            if (parentSize > 0 && childSize > 0) {
                percentage = (double) childSize / parentSize * 100;
                // 确保百分比不超过100%
                percentage = Math.min(percentage, 100.0);
            }
            
            // 调试信息
            System.out.println("DEBUG: child " + child.getName() + " - size: " + childSize + ", percentage: " + percentage);
            System.out.println("DEBUG: child class: " + child.getClass().getName());
            
            Object[] row = {
                child.getName(),
                child.getFormattedSize(),
                child.getFileType().getDisplayName(),
                dateFormat.format(new Date(child.getLastModified())),
                String.format("%.1f%%", percentage)
            };
            model.addRow(row);
        }
        
        deleteButton.setEnabled(true);
        recycleButton.setEnabled(true);
        
        // 强制刷新表格视图
        model.fireTableDataChanged();
        fileTable.revalidate();
        fileTable.repaint();
    }
    
    private void updateVisualization() {
        visualizationPanel.repaint();
    }
    
    private void paintVisualization(Graphics g) {
        if (currentRoot == null) {
            paintEmptyVisualization(g);
            return;
        }
        
        visualizationEngine.paintVisualization(g, currentRoot, visualizationPanel.getWidth(), visualizationPanel.getHeight());
    }
    
    private void paintEmptyVisualization(Graphics g) {
        g.setColor(themeManager.getTextColor());
        g.setFont(new Font("思源黑体", Font.PLAIN, 16));
        String message = "请选择目录进行扫描";
        FontMetrics fm = g.getFontMetrics();
        int x = (visualizationPanel.getWidth() - fm.stringWidth(message)) / 2;
        int y = visualizationPanel.getHeight() / 2;
        g.drawString(message, x, y);
        
        // 绘制Material Design风格的图标
        g.setColor(new Color(224, 224, 224));
        int iconSize = 64;
        int iconX = (visualizationPanel.getWidth() - iconSize) / 2;
        int iconY = y - iconSize - 20;
        
        // 绘制文件夹图标
        g.fillRoundRect(iconX + 8, iconY + 8, iconSize - 16, iconSize - 16, 8, 8);
        g.setColor(Color.WHITE);
        g.fillRoundRect(iconX + 12, iconY + 12, iconSize - 24, iconSize - 24, 4, 4);
    }
    
    // 图表类型切换功能已移除 - 固定使用树状图
    
    private void handleTreeSelection() {
        TreePath path = fileTree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof EnhancedFileNode) {
                EnhancedFileNode fileNode = (EnhancedFileNode) node.getUserObject();
                currentRoot = fileNode;
                updateTableView(fileNode);
                pathField.setText(fileNode.getAbsolutePath());
                updateVisualization();
                updateTotalSize();
                
                // 确保选中项在视图中可见
                fileTree.scrollPathToVisible(path);
            }
        }
    }
    
    private void handleTableSelection() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            // 确保选中行在视图中可见
            Rectangle cellRect = fileTable.getCellRect(selectedRow, 0, true);
            fileTable.scrollRectToVisible(cellRect);
            
            // 更新按钮状态
            deleteButton.setEnabled(true);
            recycleButton.setEnabled(true);
            
            // 获取选中文件信息
            String fileName = (String) fileTable.getValueAt(selectedRow, 0);
            String fileType = (String) fileTable.getValueAt(selectedRow, 2);
            long fileSize = parseFileSize((String) fileTable.getValueAt(selectedRow, 1));
            
            statusLabel.setText(String.format("已选择: %s (%s, %s)", 
                fileName, fileType, formatSize(fileSize)));
        }
    }
    
    private void handleTableDoubleClick() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            String fileName = (String) fileTable.getValueAt(selectedRow, 0);
            String fileType = (String) fileTable.getValueAt(selectedRow, 2);
            
            if ("文件夹".equals(fileType)) {
                navigateToSubDirectory(fileName);
            } else {
                openSelectedFile();
            }
        }
    }
    
    private void navigateToSubDirectory(String dirName) {
        if (currentRoot == null) return;
        
        for (EnhancedFileNode child : currentRoot.getChildren()) {
            if (child.isDirectory() && child.getName().equals(dirName)) {
                navigateToDirectory(child);
                break;
            }
        }
    }
    
    private void navigateToDirectory(EnhancedFileNode directory) {
        currentRoot = directory;
        updateTableView(directory);
        pathField.setText(directory.getAbsolutePath());
        updateVisualization();
        updateTotalSize();
    }
    
    private void performSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty() || originalRoot == null) {
            currentRoot = originalRoot;
            updateTableView(currentRoot);
            return;
        }
        
        // 创建搜索结果节点
        EnhancedFileNode searchResult = searchInNode(originalRoot, searchText);
        if (searchResult != null && !searchResult.getChildren().isEmpty()) {
            currentRoot = searchResult;
            updateTableView(currentRoot);
            statusLabel.setText("搜索完成，找到 " + searchResult.getChildren().size() + " 个结果");
        } else {
            statusLabel.setText("未找到匹配的文件");
            currentRoot = new EnhancedFileNode(new File("搜索结果"));
            updateTableView(currentRoot);
        }
    }
    
    private EnhancedFileNode searchInNode(EnhancedFileNode node, String searchText) {
        EnhancedFileNode result = new EnhancedFileNode(new File("搜索结果"));
        
        for (EnhancedFileNode child : node.getChildren()) {
            if (child.getName().toLowerCase().contains(searchText)) {
                result.addChild(child);
            }
            
            // 递归搜索子目录
            if (child.isDirectory()) {
                EnhancedFileNode subResult = searchInNode(child, searchText);
                if (subResult != null && !subResult.getChildren().isEmpty()) {
                    for (EnhancedFileNode subChild : subResult.getChildren()) {
                        result.addChild(subChild);
                    }
                }
            }
        }
        
        return result.getChildren().isEmpty() ? null : result;
    }
    
    private void sortTable() {
        if (currentRoot == null) return;
        
        String sortOption = (String) sortComboBox.getSelectedItem();
        switch (sortOption) {
            case "按大小排序":
                currentRoot.sortChildrenBySize();
                break;
            case "按名称排序":
                currentRoot.sortChildrenByName();
                break;
            case "按类型排序":
                currentRoot.sortChildrenByType();
                break;
            case "按修改时间排序":
                currentRoot.sortChildrenByModifiedTime();
                break;
        }
        
        updateTableView(currentRoot);
        updateVisualization();
    }
    
    // 主题切换功能已移除
    /*
    private void toggleTheme() {
        themeManager.toggleTheme();
        applyTheme();
        statusLabel.setText("已切换到" + themeManager.getCurrentTheme().getDisplayName());
    }
    */
    
    private void scanFromPath() {
        String path = pathField.getText().trim();
        if (!path.isEmpty()) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                startScan(dir);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "路径不存在或不是目录", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void rescanCurrentDirectory() {
        if (currentRoot != null) {
            File dir = new File(currentRoot.getAbsolutePath());
            if (dir.exists()) {
                startScan(dir);
            }
        }
    }
    
    private void deleteSelectedFiles(boolean toRecycleBin) {
        int[] selectedRows = fileTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(mainFrame, "请先选择要删除的文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String action = toRecycleBin ? "移到回收站" : "删除";
        int result = JOptionPane.showConfirmDialog(
            mainFrame,
            String.format("确定要将选中的 %d 个文件%s吗？%s", selectedRows.length, action, 
                         toRecycleBin ? "" : "\n\n此操作不可恢复！"),
            "确认" + action,
            JOptionPane.YES_NO_OPTION,
            toRecycleBin ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            List<File> filesToDelete = new ArrayList<>();
            
            for (int row : selectedRows) {
                String fileName = (String) fileTable.getValueAt(row, 0);
                String fileType = (String) fileTable.getValueAt(row, 2);
                
                if (currentRoot != null) {
                    for (EnhancedFileNode child : currentRoot.getChildren()) {
                        if (child.getName().equals(fileName) && 
                            child.getFileType().getDisplayName().equals(fileType)) {
                            filesToDelete.add(new File(child.getAbsolutePath()));
                            break;
                        }
                    }
                }
            }
            
            // 设置操作监听器
            fileManager.setOperationListener(new EnhancedFileManager.FileOperationListener() {
                @Override
                public void onFileDeleted(File file) {
                    // 日志记录
                }
                
                @Override
                public void onFileMovedToRecycleBin(File file, EnhancedFileManager.RecycleBinEntry entry) {
                    // 日志记录
                }
                
                @Override
                public void onFileRestored(File file, EnhancedFileManager.RecycleBinEntry entry) {
                    // 不需要实现
                }
                
                @Override
                public void onFileOpened(File file) {
                    // 不需要实现
                }
                
                @Override
                public void onFileMoved(File source, File target) {
                    // 不需要实现
                }
                
                @Override
                public void onDirectoryCreated(File directory) {
                    // 不需要实现
                }
                
                @Override
                public void onOperationFailed(String message) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(mainFrame, message, "操作失败", JOptionPane.ERROR_MESSAGE);
                    });
                }
                
                @Override
                public void onBatchOperationFailed(String message, List<String> failedFiles) {
                    SwingUtilities.invokeLater(() -> {
                        StringBuilder sb = new StringBuilder(message);
                        if (!failedFiles.isEmpty()) {
                            sb.append("\n\n失败的文件:");
                            for (String file : failedFiles) {
                                sb.append("\n- ").append(file);
                            }
                        }
                        JOptionPane.showMessageDialog(mainFrame, sb.toString(), "批量操作失败", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            
            boolean success;
            if (toRecycleBin) {
                success = fileManager.moveFilesToRecycleBin(filesToDelete);
            } else {
                success = fileManager.deleteFiles(filesToDelete);
            }
            
            if (success) {
                statusLabel.setText("文件" + action + "完成");
                // 重新扫描当前目录
                rescanCurrentDirectory();
            }
            
            // 清除监听器
            fileManager.setOperationListener(null);
        }
    }
    
    private void openSelectedFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            String fileName = (String) fileTable.getValueAt(selectedRow, 0);
            
            if (currentRoot != null) {
                for (EnhancedFileNode child : currentRoot.getChildren()) {
                    if (child.getName().equals(fileName)) {
                        File file = new File(child.getAbsolutePath());
                        if (fileManager.openFile(file)) {
                            statusLabel.setText("已打开文件: " + fileName);
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, 
                                "无法打开文件: " + fileName, 
                                "打开失败", JOptionPane.ERROR_MESSAGE);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void showFileProperties() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0) {
            String fileName = (String) fileTable.getValueAt(selectedRow, 0);
            
            if (currentRoot != null) {
                for (EnhancedFileNode child : currentRoot.getChildren()) {
                    if (child.getName().equals(fileName)) {
                        EnhancedFileManager.FileInfo info = fileManager.getFileInfo(new File(child.getAbsolutePath()));
                        if (info != null) {
                            showFilePropertiesDialog(info);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void showFilePropertiesDialog(EnhancedFileManager.FileInfo info) {
        String properties = String.format(
            "文件属性\n\n" +
            "名称: %s\n" +
            "路径: %s\n" +
            "大小: %s\n" +
            "类型: %s\n" +
            "修改时间: %s\n" +
            "隐藏: %s\n" +
            "可读: %s\n" +
            "可写: %s\n" +
            "可执行: %s",
            info.name,
            info.path,
            formatSize(info.size),
            info.isDirectory ? "文件夹" : "文件",
            dateFormat.format(new Date(info.lastModified)),
            info.isHidden ? "是" : "否",
            info.canRead ? "是" : "否",
            info.canWrite ? "是" : "否",
            info.canExecute ? "是" : "否"
        );
        
        JOptionPane.showMessageDialog(mainFrame, properties, "文件属性", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportReport() {
        if (currentRoot == null) {
            JOptionPane.showMessageDialog(mainFrame, "请先扫描一个目录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出扫描报告");
        chooser.setSelectedFile(new File("磁盘空间分析报告_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));
        
        int result = chooser.showSaveDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (exportReportToFile(file)) {
                statusLabel.setText("报告已导出到: " + file.getName());
                JOptionPane.showMessageDialog(mainFrame, "报告导出成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "报告导出失败", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private boolean exportReportToFile(File file) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
            writer.println("磁盘空间分析报告");
            writer.println("生成时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("扫描路径: " + currentRoot.getAbsolutePath());
            writer.println("总大小: " + formatSize(currentRoot.getTotalSize()));
            writer.println("文件数量: " + scanService.getScannedFilesCount());
            
            // 获取磁盘空间信息
            EnhancedFileManager.DiskSpaceInfo diskInfo = fileManager.getDiskSpaceInfo(new File(currentRoot.getAbsolutePath()));
            if (diskInfo != null) {
                writer.println("磁盘总空间: " + diskInfo.getFormattedTotalSpace());
                writer.println("磁盘已用空间: " + diskInfo.getFormattedUsedSpace());
                writer.println("磁盘可用空间: " + diskInfo.getFormattedFreeSpace());
                writer.println("磁盘使用率: " + String.format("%.1f%%", diskInfo.getUsagePercentage()));
            }
            
            writer.println("=".repeat(50));
            writer.println();
            
            // 导出文件列表
            writer.println("文件详情:");
            exportNodeToReport(writer, currentRoot, 0);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void exportNodeToReport(java.io.PrintWriter writer, EnhancedFileNode node, int depth) {
        String indent = "  ".repeat(depth);
        writer.printf("%s%s [%s] %s\n", 
                     indent, 
                     node.getName(), 
                     node.getFileType().getDisplayName(), 
                     node.getFormattedSize());
        
        for (EnhancedFileNode child : node.getChildren()) {
            exportNodeToReport(writer, child, depth + 1);
        }
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return sizeFormat.format(size / 1024.0) + " KB";
        if (size < 1024 * 1024 * 1024) return sizeFormat.format(size / (1024.0 * 1024)) + " MB";
        return sizeFormat.format(size / (1024.0 * 1024 * 1024)) + " GB";
    }
    
    /**
     * 解析文件大小字符串为字节数
     */
    private long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 0;
        }
        
        sizeStr = sizeStr.trim();
        try {
            if (sizeStr.endsWith(" GB")) {
                double value = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 3));
                return (long)(value * 1024 * 1024 * 1024);
            } else if (sizeStr.endsWith(" MB")) {
                double value = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 3));
                return (long)(value * 1024 * 1024);
            } else if (sizeStr.endsWith(" KB")) {
                double value = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 3));
                return (long)(value * 1024);
            } else if (sizeStr.endsWith(" B")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2));
            } else {
                return Long.parseLong(sizeStr);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 现代化表格单元格渲染器
     */
    private static class ModernTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, 
                isSelected, hasFocus, row, column);
            
            // 设置对齐方式
            if (column == 1 || column == 4) { // 大小和比例列
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            
            // 设置边框
            label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            
            // 设置图标（根据内容类型）
            if (value instanceof String && column == 2) {
                String strValue = (String) value;
                if ("文件夹".equals(strValue)) {
                    label.setIcon(UIManager.getIcon("FileView.directoryIcon"));
                } else {
                    label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                }
            } else {
                label.setIcon(null);
            }
            
            return label;
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            if (scanService != null) {
                scanService.cancelScan();
            }
            System.out.println("UI控制器资源清理完成");
        } catch (Exception e) {
            System.err.println("资源清理失败: " + e.getMessage());
        }
    }
}