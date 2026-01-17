/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 现代化进度条组件
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.ui.component;

import com.diskanalyzer.service.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 现代化进度条组件
 * 提供流畅的动画效果和美观的外观
 */
public class ModernProgressBar extends JProgressBar {
    
    private final ThemeManager themeManager;
    private Timer animationTimer;
    private float animationProgress = 0f;
    private boolean isIndeterminate = false;
    private Color progressColor;
    private Color backgroundColor;
    private Color textColor;
    
    // 动画相关
    private long lastUpdateTime = 0;
    private float speed = 0.02f;
    
    public ModernProgressBar() {
        this.themeManager = new ThemeManager();
        initializeComponent();
        setupAnimation();
    }
    
    private void initializeComponent() {
        // 基本设置
        setStringPainted(true);
        setBorderPainted(false);
        setOpaque(false);
        
        // 设置大小
        setPreferredSize(new Dimension(200, 24));
        setMinimumSize(new Dimension(100, 20));
        
        // 设置颜色
        updateColors();
        
        // 设置字体
        setFont(themeManager.getUIFont().deriveFont(Font.BOLD, 12f));
        
        // 初始值
        setMinimum(0);
        setMaximum(100);
        setValue(0);
    }
    
    private void setupAnimation() {
        animationTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isIndeterminate) {
                    updateIndeterminateAnimation();
                } else {
                    updateDeterminateAnimation();
                }
                repaint();
            }
        });
    }
    
    private void updateColors() {
        progressColor = themeManager.getPrimaryColor();
        backgroundColor = themeManager.getSurfaceColor();
        textColor = themeManager.getTextColor();
    }
    
    /**
     * 更新不确定动画
     */
    private void updateIndeterminateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return;
        }
        
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;
        
        animationProgress += speed * deltaTime * 60; // 60 FPS基准
        
        if (animationProgress > 1.0f) {
            animationProgress = 0.0f;
        }
    }
    
    /**
     * 更新确定动画
     */
    private void updateDeterminateAnimation() {
        int currentValue = getValue();
        int targetValue = getMaximum();
        
        if (currentValue < targetValue) {
            // 平滑过渡到目标值
            int newValue = currentValue + Math.max(1, (targetValue - currentValue) / 10);
            setValue(newValue);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 开启抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 绘制背景
        paintBackground(g2d);
        
        // 绘制进度
        paintProgress(g2d);
        
        // 绘制文字
        paintText(g2d);
        
        g2d.dispose();
    }
    
    /**
     * 绘制背景
     */
    private void paintBackground(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int arc = height / 2;
        
        // 绘制圆角矩形背景
        g2d.setColor(backgroundColor);
        g2d.fillRoundRect(0, 0, width, height, arc, arc);
        
        // 绘制边框
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
    }
    
    /**
     * 绘制进度
     */
    private void paintProgress(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        int arc = height / 2;
        
        if (isIndeterminate) {
            // 不确定模式 - 绘制移动的进度条
            paintIndeterminateProgress(g2d, width, height, arc);
        } else {
            // 确定模式 - 绘制实际进度
            paintDeterminateProgress(g2d, width, height, arc);
        }
    }
    
    /**
     * 绘制不确定进度
     */
    private void paintIndeterminateProgress(Graphics2D g2d, int width, int height, int arc) {
        int progressWidth = width / 3; // 移动块的宽度
        int progressX = (int) (animationProgress * (width + progressWidth)) - progressWidth;
        
        // 创建渐变效果
        GradientPaint gradient = new GradientPaint(
            progressX, 0, lightenColor(progressColor, 0.3f),
            progressX + progressWidth, 0, progressColor
        );
        
        g2d.setPaint(gradient);
        g2d.fillRoundRect(progressX, 2, progressWidth, height - 4, arc, arc);
    }
    
    /**
     * 绘制确定进度
     */
    private void paintDeterminateProgress(Graphics2D g2d, int width, int height, int arc) {
        int progressWidth = (int) ((width * getPercentComplete()));
        
        if (progressWidth > 0) {
            // 创建渐变效果
            GradientPaint gradient = new GradientPaint(
                0, 0, lightenColor(progressColor, 0.2f),
                progressWidth, 0, progressColor
            );
            
            g2d.setPaint(gradient);
            g2d.fillRoundRect(2, 2, progressWidth - 4, height - 4, arc, arc);
        }
    }
    
    /**
     * 绘制文字
     */
    private void paintText(Graphics2D g2d) {
        if (!isStringPainted()) {
            return;
        }
        
        String text = getString();
        if (text == null || text.isEmpty()) {
            return;
        }
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - textHeight) / 2 + fm.getAscent();
        
        // 根据背景选择文字颜色
        Color textColorToUse = isIndeterminate ? 
            getContrastColor(progressColor) : getContrastColor(backgroundColor);
        
        g2d.setColor(textColorToUse);
        g2d.setFont(getFont());
        g2d.drawString(text, x, y);
    }
    
    /**
     * 获取对比色
     */
    private Color getContrastColor(Color color) {
        // 计算颜色的亮度
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    /**
     * 颜色变浅函数
     */
    private Color lightenColor(Color color, float factor) {
        return new Color(
            Math.min(255, (int)(color.getRed() + (255 - color.getRed()) * factor)),
            Math.min(255, (int)(color.getGreen() + (255 - color.getGreen()) * factor)),
            Math.min(255, (int)(color.getBlue() + (255 - color.getBlue()) * factor))
        );
    }
    
    @Override
    public void setIndeterminate(boolean newValue) {
        boolean oldValue = isIndeterminate;
        isIndeterminate = newValue;
        
        if (newValue != oldValue) {
            if (newValue) {
                animationTimer.start();
            } else {
                animationTimer.stop();
                animationProgress = 0f;
            }
            repaint();
        }
        
        super.setIndeterminate(newValue);
    }
    
    @Override
    public void setValue(int n) {
        super.setValue(n);
        if (!isIndeterminate && isVisible()) {
            repaint();
        }
    }
    
    /**
     * 设置进度颜色
     */
    public void setProgressColor(Color color) {
        this.progressColor = color;
        repaint();
    }
    
    /**
     * 设置背景颜色
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }
    
    /**
     * 设置文字颜色
     */
    public void setTextColor(Color color) {
        this.textColor = color;
        repaint();
    }
    
    /**
     * 设置动画速度
     */
    public void setAnimationSpeed(float speed) {
        this.speed = Math.max(0.01f, Math.min(1.0f, speed));
    }
    
    /**
     * 开始动画
     */
    public void startAnimation() {
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }
    
    /**
     * 停止动画
     */
    public void stopAnimation() {
        animationTimer.stop();
        animationProgress = 0f;
        repaint();
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && isIndeterminate) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }
}