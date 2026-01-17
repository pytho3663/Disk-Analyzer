/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 现代化表格组件
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.ui.component;

import com.diskanalyzer.service.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化表格组件
 * 提供美观的外观和丰富的交互效果
 */
public class ModernTable extends JTable {
    
    private final ThemeManager themeManager;
    private Color hoverBackground;
    private Color selectionBackground;
    private Color gridColor;
    private int hoveredRow = -1;
    
    public ModernTable() {
        super();
        this.themeManager = new ThemeManager();
        initializeComponent();
        setupEventHandlers();
    }
    
    public ModernTable(ThemeManager externalThemeManager) {
        super();
        this.themeManager = externalThemeManager;
        initializeComponent();
        setupEventHandlers();
    }
    
    private void initializeComponent() {
        // 基本设置
        setFillsViewportHeight(true);
        setShowGrid(false);
        setRowHeight(32);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 延迟初始化UI以避免循环依赖
        SwingUtilities.invokeLater(() -> {
            updateColors();
            setupTableHeader();
        });
        
        // 设置选择模式
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 网格线设置
        setShowHorizontalLines(true);
        setShowVerticalLines(false);
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
            
            // 固定亮色主题网格颜色
            gridColor = new Color(240, 240, 240);
            
            // 亮色主题颜色设置
            setBackground(themeManager.getBackgroundColor());
            setForeground(themeManager.getTextColor());
            setFont(themeManager.getUIFont());
            setGridColor(gridColor);
        }
    }
    
    private void setupTableHeader() {
        JTableHeader header = getTableHeader();
        if (header != null && themeManager != null) {
            header.setFont(themeManager.getUIFont().deriveFont(Font.BOLD));
            // 增强表头文字对比度
            header.setForeground(themeManager.getTextColor());
            header.setBackground(themeManager.getSurfaceColor());
            header.setPreferredSize(new Dimension(header.getWidth(), 40));
            
            // 自定义表头渲染器
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, 
                        isSelected, hasFocus, row, column);
                    
                    // 设置样式 - 增强对比度
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setForeground(themeManager.getTextColor()); // 使用最亮的文字颜色
                    label.setBackground(themeManager.getSurfaceColor());
                    label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, gridColor),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));
                    
                    return label;
                }
            });
        }
    }
    
    private void setupEventHandlers() {
        // 鼠标悬停效果
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                int row = rowAtPoint(point);
                
                if (row != hoveredRow) {
                    hoveredRow = row;
                    repaint();
                }
            }
        });
        
        // 鼠标离开表格
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                repaint();
            }
        });
    }
    
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        
        // 设置行背景色和字体色
        if (!isRowSelected(row)) {
            if (row == hoveredRow) {
                component.setBackground(hoverBackground);
                component.setForeground(getContrastColor(hoverBackground));
            } else if (row % 2 == 0) {
                component.setBackground(getBackground());
                component.setForeground(getForeground());
            } else {
                // 固定亮色主题斑马纹效果
                Color zebraColor = new Color(250, 250, 250);
                component.setBackground(zebraColor);
                component.setForeground(themeManager.getTextColor());
            }
        } else {
            component.setBackground(selectionBackground);
            // 根据背景色亮度选择合适的字体颜色
            Color textColor = getContrastColor(selectionBackground);
            component.setForeground(textColor);
        }
        
        return component;
    }
    
    /**
     * 现代化表格单元格渲染器
     */
    public static class ModernTableCellRenderer extends DefaultTableCellRenderer {
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
            
            // 确保字体颜色正确（防止窗口切换后变白）
            if (table instanceof ModernTable) {
                ModernTable modernTable = (ModernTable) table;
                if (isSelected) {
                    Color bg = modernTable.getSelectionBackground();
                    Color fg = modernTable.getContrastColor(bg);
                    label.setForeground(fg);
                } else {
                    label.setForeground(modernTable.getForeground());
                }
            }
            
            // 设置图标（根据内容类型）
            if (value instanceof String) {
                String strValue = (String) value;
                if (column == 2) { // 类型列
                    if ("文件夹".equals(strValue)) {
                        label.setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    } else {
                        label.setIcon(UIManager.getIcon("FileView.fileIcon"));
                    }
                }
            }
            
            return label;
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
        super.setSelectionBackground(color);
        repaint();
    }
    
    /**
     * 设置网格线颜色
     */
    public void setGridColor(Color color) {
        this.gridColor = color;
        super.setGridColor(color);
        setupTableHeader(); // 重新设置表头
    }
    
    /**
     * 应用主题
     */
    public void applyTheme() {
        updateColors();
        if (themeManager != null) {
            setFont(themeManager.getUIFont());
            setupTableHeader();
        }
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
                Point point = e.getPoint();
                int row = rowAtPoint(point);
                
                if (row >= 0 && row < getRowCount()) {
                    // 如果右键的行未被选中，则选中该行
                    if (!isRowSelected(row)) {
                        setRowSelectionInterval(row, row);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    /**
     * 设置列宽自适应
     */
    public void autoResizeColumn(int column) {
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) getCellRenderer(0, column);
        Component comp = renderer.getTableCellRendererComponent(this, getValueAt(0, column), false, false, 0, column);
        
        int width = comp.getPreferredSize().width + getIntercellSpacing().width;
        getColumnModel().getColumn(column).setPreferredWidth(width);
    }
    
    /**
     * 设置所有列宽自适应
     */
    public void autoResizeAllColumns() {
        for (int i = 0; i < getColumnCount(); i++) {
            autoResizeColumn(i);
        }
    }
}