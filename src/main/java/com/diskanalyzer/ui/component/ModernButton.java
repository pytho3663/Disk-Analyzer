/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 现代化按钮组件（Material Design 3）
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Material Design 3 风格按钮组件
 * 支持多种按钮类型和丰富的交互效果
 */
public class ModernButton extends JButton {
    
    /**
     * 按钮类型枚举
     */
    public enum Type {
        PRIMARY("主要按钮", new Color(33, 150, 243), Color.WHITE),
        SECONDARY("次要按钮", new Color(224, 224, 224), new Color(33, 33, 33)),
        SUCCESS("成功按钮", new Color(76, 175, 80), Color.WHITE),
        DANGER("危险按钮", new Color(244, 67, 54), Color.WHITE),
        WARNING("警告按钮", new Color(255, 152, 0), Color.WHITE),
        TEXT("文本按钮", new Color(0, 0, 0, 0), new Color(33, 150, 243));
        
        private final String displayName;
        private final Color backgroundColor;
        private final Color textColor;
        
        Type(String displayName, Color backgroundColor, Color textColor) {
            this.displayName = displayName;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
        
        public String getDisplayName() { return displayName; }
        public Color getBackgroundColor() { return backgroundColor; }
        public Color getTextColor() { return textColor; }
    }
    
    // 按钮状态
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isFocused = false;
    
    // 按钮属性
    private final Type buttonType;
    private int cornerRadius = 8;
    private int elevation = 2;
    private Color currentBackground;
    private Color currentTextColor;
    
    // 动画相关
    private float animationProgress = 0f;
    private Timer animationTimer;
    private static final int ANIMATION_DURATION = 200; // 毫秒
    
    public ModernButton(String text) {
        this(text, Type.PRIMARY);
    }
    
    public ModernButton(String text, Type type) {
        super(text);
        this.buttonType = type;
        this.currentBackground = type.getBackgroundColor();
        this.currentTextColor = type.getTextColor();
        
        initializeComponent();
        setupEventHandlers();
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }
    
    private void initializeComponent() {
        // 基本设置
        setFont(new Font("思源黑体", Font.PLAIN, 14));
        setForeground(currentTextColor);
        setBackground(currentBackground);
        
        // 设置默认尺寸
        setPreferredSize(new Dimension(120, 36));
        setMinimumSize(new Dimension(80, 32));
        
        // 设置内边距
        setMargin(new Insets(8, 16, 8, 16));
        
        // 设置光标
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    private void setupEventHandlers() {
        // 鼠标进入
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                startAnimation(1.0f);
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                startAnimation(0.0f);
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // 触发点击动画
                    triggerClickAnimation();
                }
            }
        });
        
        // 焦点监听
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                isFocused = true;
                repaint();
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }
    
    private void startAnimation(float targetProgress) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        float startProgress = animationProgress;
        long startTime = System.currentTimeMillis();
        
        animationTimer = new Timer(16, e -> {
            long currentTime = System.currentTimeMillis();
            float elapsed = (currentTime - startTime) / (float) ANIMATION_DURATION;
            
            if (elapsed >= 1.0f) {
                animationProgress = targetProgress;
                ((Timer) e.getSource()).stop();
            } else {
                // 使用缓动函数
                elapsed = easeInOutCubic(elapsed);
                animationProgress = startProgress + (targetProgress - startProgress) * elapsed;
            }
            
            updateColors();
            repaint();
        });
        
        animationTimer.start();
    }
    
    private void triggerClickAnimation() {
        // 点击动画：短暂的高亮效果
        Color originalBackground = currentBackground;
        Color highlightColor = getHighlightColor(currentBackground);
        
        Timer clickTimer = new Timer(50, new java.awt.event.ActionListener() {
            private int step = 0;
            
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (step == 0) {
                    currentBackground = highlightColor;
                } else if (step == 1) {
                    currentBackground = originalBackground;
                    ((Timer) e.getSource()).stop();
                }
                step++;
                repaint();
            }
        });
        clickTimer.start();
    }
    
    private void updateColors() {
        // 根据动画进度更新颜色
        Color baseColor = buttonType.getBackgroundColor();
        Color hoverColor = getHoverColor(baseColor);
        
        if (isHovered || animationProgress > 0) {
            currentBackground = interpolateColor(baseColor, hoverColor, animationProgress);
        } else {
            currentBackground = baseColor;
        }
        
        // 文本颜色保持不变或轻微调整
        currentTextColor = buttonType.getTextColor();
        
        setBackground(currentBackground);
        setForeground(currentTextColor);
    }
    
    private Color getHoverColor(Color baseColor) {
        // 计算悬停颜色（稍微变亮）
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.min(hsb[2] * 1.1f, 1.0f));
    }
    
    private Color getHighlightColor(Color baseColor) {
        // 计算高亮颜色（更亮）
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.min(hsb[2] * 1.3f, 1.0f));
    }
    
    private Color interpolateColor(Color color1, Color color2, float factor) {
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();
        int a1 = color1.getAlpha();
        
        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();
        int a2 = color2.getAlpha();
        
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        int a = (int) (a1 + (a2 - a1) * factor);
        
        return new Color(r, g, b, a);
    }
    
    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 计算当前状态的颜色
        Color paintBackground = currentBackground;
        Color paintForeground = currentTextColor;
        
        // 按下状态：颜色变暗
        if (isPressed) {
            paintBackground = getDarkerColor(paintBackground, 0.9f);
        }
        
        // 禁用状态：降低透明度
        if (!isEnabled()) {
            paintBackground = new Color(paintBackground.getRed(), paintBackground.getGreen(), 
                                      paintBackground.getBlue(), 128);
            paintForeground = new Color(paintForeground.getRed(), paintForeground.getGreen(), 
                                      paintForeground.getBlue(), 128);
        }
        
        // 绘制按钮背景（圆角矩形）
        RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float(
            0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        
        g2d.setColor(paintBackground);
        g2d.fill(roundRect);
        
        // 绘制边框（如果有焦点）
        if (isFocused && isEnabled()) {
            g2d.setColor(getFocusColor(paintBackground));
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(roundRect);
        }
        
        // 绘制阴影效果
        if (isEnabled() && elevation > 0) {
            drawShadow(g2d, roundRect);
        }
        
        // 绘制文本
        g2d.setColor(paintForeground);
        g2d.setFont(getFont());
        
        FontMetrics fm = g2d.getFontMetrics();
        String text = getText();
        int textX = (getWidth() - fm.stringWidth(text)) / 2;
        int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        
        g2d.drawString(text, textX, textY);
        
        // 绘制图标（如果有）
        Icon icon = getIcon();
        if (icon != null) {
            int iconX = textX - icon.getIconWidth() - 4;
            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2d, iconX, iconY);
        }
        
        g2d.dispose();
    }
    
    private void drawShadow(Graphics2D g2d, RoundRectangle2D.Float roundRect) {
        // 简单的阴影效果
        Color shadowColor = new Color(0, 0, 0, 30);
        g2d.setColor(shadowColor);
        
        for (int i = 0; i < elevation; i++) {
            RoundRectangle2D.Float shadowRect = new RoundRectangle2D.Float(
                i, i, getWidth() - 1 - i * 2, getHeight() - 1 - i * 2, 
                cornerRadius + i, cornerRadius + i);
            g2d.fill(shadowRect);
        }
    }
    
    private Color getDarkerColor(Color color, float factor) {
        return new Color(
            (int) (color.getRed() * factor),
            (int) (color.getGreen() * factor),
            (int) (color.getBlue() * factor),
            color.getAlpha()
        );
    }
    
    private Color getFocusColor(Color background) {
        // 计算焦点颜色（基于背景色的对比色）
        float[] hsb = Color.RGBtoHSB(background.getRed(), background.getGreen(), background.getBlue(), null);
        return Color.getHSBColor((hsb[0] + 0.5f) % 1.0f, hsb[1], hsb[2]);
    }
    
    // Getter和Setter方法
    public Type getButtonType() { return buttonType; }
    public int getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(int cornerRadius) { this.cornerRadius = cornerRadius; }
    public int getElevation() { return elevation; }
    public void setElevation(int elevation) { this.elevation = elevation; }
    
    /**
     * 创建图标按钮的便捷方法
     */
    public static ModernButton createIconButton(String text, String icon, Type type) {
        ModernButton button = new ModernButton(text, type);
        button.setIcon(createIcon(icon));
        return button;
    }
    
    private static Icon createIcon(String iconCode) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(c.getForeground());
                g.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
                g.drawString(iconCode, x, y + 12);
            }
            
            @Override
            public int getIconWidth() { return 16; }
            @Override
            public int getIconHeight() { return 16; }
        };
    }
}