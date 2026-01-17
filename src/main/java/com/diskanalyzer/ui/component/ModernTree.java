/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 现代化树形组件
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.ui.component;

import com.diskanalyzer.service.ThemeManager;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化树形组件
 * 提供美观的文件树显示和丰富的交互效果
 */
public class ModernTree extends JTree {
    
    private final ThemeManager themeManager;
    private Color hoverBackground;
    private Color selectionBackground;
    private int hoveredRow = -1;
    
    public ModernTree() {
        super();
        this.themeManager = new ThemeManager();
        initializeComponent();
        setupEventHandlers();
    }
    
    public ModernTree(ThemeManager externalThemeManager) {
        super();
        this.themeManager = externalThemeManager;
        initializeComponent();
        setupEventHandlers();
    }
    
    private void initializeComponent() {
        // 基本设置
        setRootVisible(true);
        setShowsRootHandles(true);
        setRowHeight(28);
        putClientProperty("JTree.lineStyle", "None");
        
        // 延迟初始化UI以避免循环依赖
        SwingUtilities.invokeLater(() -> {
            updateColors();
            setupRenderer();
        });
        
        // 选择模式
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }
    
    private void updateColors() {
        if (themeManager != null) {
            // 固定使用亮色主题
            Color primaryColor = themeManager.getPrimaryColor();
            
            // 亮色主题下使用稍暗的背景色
            hoverBackground = new Color(
                Math.max(0, primaryColor.getRed() - 40),
                Math.max(0, primaryColor.getGreen() - 40),
                Math.max(0, primaryColor.getBlue() - 40)
            );
            
            selectionBackground = primaryColor;
            
            // 亮色主题颜色设置
            setBackground(themeManager.getBackgroundColor());
            setForeground(themeManager.getTextColor());
            setFont(themeManager.getUIFont());
        }
    }
    
    private void setupRenderer() {
        setCellRenderer(new ModernTreeCellRenderer());
    }
    
    private void setupEventHandlers() {
        // 鼠标悬停效果
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                int row = getRowForLocation(point.x, point.y);
                
                if (row != hoveredRow) {
                    hoveredRow = row;
                    repaint();
                }
            }
        });
        
        // 鼠标离开
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                repaint();
            }
        });
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 绘制悬停效果 - 增强对比度
        if (hoveredRow >= 0 && hoveredRow < getRowCount()) {
            Rectangle bounds = getRowBounds(hoveredRow);
            if (bounds != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(hoverBackground);
                g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                
                // 添加边框增强视觉效果
                g2d.setColor(getContrastColor(hoverBackground));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                
                g2d.dispose();
            }
        }
    }
    
    /**
     * 现代化树形单元格渲染器
     */
    private class ModernTreeCellRenderer extends DefaultTreeCellRenderer {
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            // 设置字体
            if (themeManager != null) {
                setFont(themeManager.getUIFont());
            }
            
            // 设置选中状态颜色
            if (selected) {
                setBackground(selectionBackground);
                // 根据背景色亮度选择合适的字体颜色
                Color textColor = getContrastColor(selectionBackground);
                setForeground(textColor);
            } else {
                if (themeManager != null) {
                    setBackground(themeManager.getBackgroundColor()); // 使用纯黑色背景
                    setForeground(themeManager.getTextColor()); // 使用最亮的文字颜色
                }
            }
            
            // 设置边框
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            
            return this;
        }
    }
    
    /**
     * 设置悬停背景色
     */
    public void setHoverBackground(Color color) {
        this.hoverBackground = color;
        repaint();
    }
    
    /**
     * 设置选中背景色
     */
    public void setSelectionBackground(Color color) {
        this.selectionBackground = color;
        repaint();
    }
    
    /**
     * 应用主题
     */
    public void applyTheme() {
        updateColors();
        setupRenderer();
        repaint();
    }
    
    /**
     * 获取当前悬停的行
     */
    public int getHoveredRow() {
        return hoveredRow;
    }
    
    /**
     * 清除悬停状态
     */
    public void clearHover() {
        hoveredRow = -1;
        repaint();
    }
    
    /**
     * 计算与背景色对比度合适的文字颜色
     */
    private Color getContrastColor(Color backgroundColor) {
        if (backgroundColor == null) {
            return Color.BLACK;
        }
        
        // 计算背景色的亮度
        double luminance = (0.299 * backgroundColor.getRed() + 
                           0.587 * backgroundColor.getGreen() + 
                           0.114 * backgroundColor.getBlue()) / 255;
        
        // 亮度大于0.5使用黑色文字，否则使用白色文字
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    /**
     * 添加右键菜单支持
     */
    public void addContextMenu(JPopupMenu popupMenu) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e, popupMenu);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e, popupMenu);
                }
            }
            
            private void showPopupMenu(MouseEvent e, JPopupMenu popup) {
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    setSelectionPath(path);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    /**
     * 展开所有节点
     */
    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }
    
    /**
     * 收起所有节点
     */
    public void collapseAll() {
        for (int i = getRowCount() - 1; i >= 0; i--) {
            collapseRow(i);
        }
    }
}