/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 主题管理器
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.service;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 主题管理器 - 简化版本，只支持亮色主题
 * 提供现代化的UI外观，移除主题切换功能
 */
public class ThemeManager {
    
    // 固定使用亮色主题
    private static final String THEME_NAME = "亮色主题";
    
    // 颜色配置
    private Color primaryColor = new Color(33, 150, 243);      // 主色调 - 蓝色
    private Color secondaryColor = new Color(76, 175, 80);     // 辅助色 - 绿色
    private Color dangerColor = new Color(244, 67, 54);        // 危险色 - 红色
    private Color warningColor = new Color(255, 152, 0);       // 警告色 - 橙色
    
    // 背景色
    private Color backgroundColor = new Color(250, 250, 250);  // 主背景
    private Color surfaceColor = Color.WHITE;                  // 表面背景
    private Color cardColor = Color.WHITE;                     // 卡片背景
    
    // 文字色
    private Color textPrimary = new Color(33, 33, 33);         // 主要文字
    private Color textSecondary = new Color(117, 117, 117);    // 次要文字
    private Color textHint = new Color(158, 158, 158);         // 提示文字
    
    // 边框色
    private Color borderColor = new Color(224, 224, 224);      // 边框颜色
    private Color dividerColor = new Color(224, 224, 224);     // 分割线
    
    // 字体
    private Font uiFont = new Font("思源黑体", Font.PLAIN, 14);
    private Font titleFont = new Font("思源黑体", Font.BOLD, 16);
    private Font smallFont = new Font("思源黑体", Font.PLAIN, 12);
    
    // 圆角半径
    private int borderRadius = 8;
    private int buttonRadius = 6;
    
    public ThemeManager() {
        // 固定使用亮色主题
        applyLightTheme();
        applyLookAndFeel();
    }
    
    /**
     * 应用亮色主题
     */
    private void applyLightTheme() {
        primaryColor = new Color(33, 150, 243);
        secondaryColor = new Color(76, 175, 80);
        dangerColor = new Color(244, 67, 54);
        warningColor = new Color(255, 152, 0);
        
        backgroundColor = new Color(250, 250, 250);
        surfaceColor = Color.WHITE;
        cardColor = Color.WHITE;
        
        textPrimary = new Color(33, 33, 33);
        textSecondary = new Color(117, 117, 117);
        textHint = new Color(158, 158, 158);
        
        borderColor = new Color(224, 224, 224);
        dividerColor = new Color(224, 224, 224);
    }
    
    // 亮色主题已定义，无需重复
    
    /**
     * 应用LookAndFeel
     */
    private void applyLookAndFeel() {
        try {
            // 设置系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // 自定义组件外观
            customizeUI();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 自定义UI组件
     */
    private void customizeUI() {
        // 按钮样式
        UIManager.put("Button.background", surfaceColor);
        UIManager.put("Button.foreground", textPrimary);
        UIManager.put("Button.border", createRoundedBorder(buttonRadius));
        UIManager.put("Button.font", uiFont);
        
        // 文本框样式
        UIManager.put("TextField.background", surfaceColor);
        UIManager.put("TextField.foreground", textPrimary);
        UIManager.put("TextField.border", createRoundedBorder(borderRadius));
        UIManager.put("TextField.font", uiFont);
        
        // 表格样式
        UIManager.put("Table.background", surfaceColor);
        UIManager.put("Table.foreground", textPrimary);
        UIManager.put("Table.selectionBackground", primaryColor);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.font", uiFont);
        UIManager.put("Table.gridColor", dividerColor);
        UIManager.put("TableHeader.background", surfaceColor);
        UIManager.put("TableHeader.foreground", textPrimary);
        
        // 树形控件样式
        UIManager.put("Tree.background", surfaceColor);
        UIManager.put("Tree.foreground", textPrimary);
        UIManager.put("Tree.selectionBackground", primaryColor);
        UIManager.put("Tree.selectionForeground", Color.WHITE);
        UIManager.put("Tree.font", uiFont);
        
        // 标签样式
        UIManager.put("Label.foreground", textPrimary);
        UIManager.put("Label.font", uiFont);
        
        // 面板样式
        UIManager.put("Panel.background", backgroundColor);
        
        // 滚动条样式
        UIManager.put("ScrollBar.background", surfaceColor);
        UIManager.put("ScrollBar.thumb", primaryColor);
        UIManager.put("ScrollBar.track", backgroundColor);
        
        // 复选框样式
        UIManager.put("CheckBox.background", surfaceColor);
        UIManager.put("CheckBox.foreground", textPrimary);
        UIManager.put("CheckBox.font", uiFont);
        
        // 下拉框样式
        UIManager.put("ComboBox.background", surfaceColor);
        UIManager.put("ComboBox.foreground", textPrimary);
        UIManager.put("ComboBox.font", uiFont);
        
        // 菜单样式
        UIManager.put("MenuBar.background", surfaceColor);
        UIManager.put("MenuBar.foreground", textPrimary);
        UIManager.put("Menu.background", surfaceColor);
        UIManager.put("Menu.foreground", textPrimary);
        UIManager.put("MenuItem.background", surfaceColor);
        UIManager.put("MenuItem.foreground", textPrimary);
        
        // 弹出菜单样式
        UIManager.put("PopupMenu.background", surfaceColor);
        UIManager.put("PopupMenu.foreground", textPrimary);
        
        // 分隔符颜色
        UIManager.put("Separator.foreground", dividerColor);
        
        // 标题边框颜色
        UIManager.put("TitledBorder.titleColor", textPrimary);
        
        // 工具提示样式
        UIManager.put("ToolTip.background", surfaceColor);
        UIManager.put("ToolTip.foreground", textPrimary);
        UIManager.put("ToolTip.font", uiFont);
    }
    
    /**
     * 创建圆角边框
     */
    private Border createRoundedBorder(int radius) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        );
    }
    
    /**
     * 应用主题到组件
     */
    public void applyTheme(Component component) {
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            jComponent.setBackground(getBackgroundColor());
            jComponent.setForeground(getTextColor());
            jComponent.setFont(uiFont);
        }
        
        // 递归应用到子组件
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                applyTheme(child);
            }
        }
    }
    
    /**
     * 获取悬停背景色（固定亮色主题）
     */
    public Color getHoverBackgroundColor() {
        return new Color(245, 245, 245); // 固定亮色主题悬停背景
    }
    
    // Getter方法 - 简化版本
    public String getThemeName() { return THEME_NAME; }
    public Color getPrimaryColor() { return primaryColor; }
    public Color getSecondaryColor() { return secondaryColor; }
    public Color getDangerColor() { return dangerColor; }
    public Color getWarningColor() { return warningColor; }
    public Color getBackgroundColor() { return backgroundColor; }
    public Color getSurfaceColor() { return surfaceColor; }
    public Color getCardColor() { return cardColor; }
    public Color getTextColor() { return textPrimary; }
    public Color getTextSecondaryColor() { return textSecondary; }
    public Color getTextHintColor() { return textHint; }
    public Color getBorderColor() { return borderColor; }
    public Color getDividerColor() { return dividerColor; }
    public Color getStatusBarColor() { return surfaceColor; }
    public Font getUIFont() { return uiFont; }
    public Font getTitleFont() { return titleFont; }
    public Font getSmallFont() { return smallFont; }
    public int getBorderRadius() { return borderRadius; }
    public int getButtonRadius() { return buttonRadius; }
}