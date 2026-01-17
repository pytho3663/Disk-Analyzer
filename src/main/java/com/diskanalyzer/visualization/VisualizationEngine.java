/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 可视化引擎
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.visualization;

import com.diskanalyzer.model.EnhancedFileNode;
import com.diskanalyzer.service.ThemeManager;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * 可视化引擎 - 负责磁盘空间数据的可视化展示
 * 支持多种图表类型和交互效果
 */
public class VisualizationEngine {
    
    /**
     * 图表类型枚举（简化版，只保留树状图）
     */
    public enum ChartType {
        TREE_MAP("树状图");
        
        private final String displayName;
        
        ChartType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final ThemeManager themeManager;
    private ChartType currentChartType = ChartType.TREE_MAP; // 固定使用树状图
    
    // 颜色方案
    private final Color[] colorPalette = {
        new Color(66, 133, 244),   // Google Blue
        new Color(234, 67, 53),    // Google Red
        new Color(251, 188, 5),    // Google Yellow
        new Color(52, 168, 83),    // Google Green
        new Color(171, 71, 188),   // Purple
        new Color(255, 87, 34),    // Deep Orange
        new Color(3, 169, 244),   // Light Blue
        new Color(139, 195, 74),  // Light Green
        new Color(255, 193, 7),   // Amber
        new Color(121, 85, 72)     // Brown
    };
    
    // 交互状态
    private Map<Rectangle, EnhancedFileNode> clickableAreas;
    private EnhancedFileNode hoveredNode;
    private Rectangle hoveredArea;
    
    public VisualizationEngine() {
        this.themeManager = new ThemeManager();
        this.clickableAreas = new HashMap<>();
    }
    
    /**
     * 绘制可视化图表
     */
    public void paintVisualization(Graphics g, EnhancedFileNode rootNode, int width, int height) {
        if (rootNode == null || rootNode.getChildren().isEmpty()) {
            paintEmptyState(g, width, height);
            return;
        }
        
        // 清空可点击区域
        clickableAreas.clear();
        
        // 只绘制树状图（简化版）
        paintTreeMap(g, rootNode, width, height);
        
        // 绘制悬停效果
        if (hoveredArea != null && hoveredNode != null) {
            paintHoverEffect(g, hoveredArea, hoveredNode);
        }
    }
    
    /**
     * 绘制空状态
     */
    private void paintEmptyState(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制背景圆圈 - 根据主题调整颜色
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 6;
        
        // 固定亮色主题背景圆圈
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        
        // 绘制文件夹图标
        g2d.setColor(themeManager.getTextHintColor());
        g2d.setStroke(new BasicStroke(2));
        
        // 文件夹主体
        int folderWidth = radius;
        int folderHeight = radius * 3 / 4;
        int folderX = centerX - folderWidth / 2;
        int folderY = centerY - folderHeight / 2;
        
        g2d.drawRect(folderX, folderY, folderWidth, folderHeight);
        
        // 文件夹标签
        g2d.drawLine(folderX + folderWidth / 4, folderY - 5, folderX + folderWidth * 3 / 4, folderY - 5);
        
        // 绘制文字 - 使用最亮的文字颜色确保可见性
        g2d.setFont(themeManager.getUIFont().deriveFont(16f));
        FontMetrics fm = g2d.getFontMetrics();
        String message = "请选择目录进行扫描";
        int textWidth = fm.stringWidth(message);
        int textY = centerY + radius + 30;
        
        g2d.setColor(themeManager.getTextColor());
        g2d.drawString(message, centerX - textWidth / 2, textY);
        
        // 副标题
        g2d.setFont(themeManager.getUIFont().deriveFont(12f));
        fm = g2d.getFontMetrics();
        String subtitle = "扫描完成后将显示磁盘空间分析";
        int subtitleWidth = fm.stringWidth(subtitle);
        
        g2d.setColor(themeManager.getTextHintColor());
        g2d.drawString(subtitle, centerX - subtitleWidth / 2, textY + 25);
        
        g2d.dispose();
    }
    
    // 矩形图功能已移除
    
    // 饼图功能已移除
    
    /**
     * 绘制树状图
     */
    private void paintTreeMap(Graphics g, EnhancedFileNode rootNode, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制标题（简化版）
        paintChartTitle(g2d, "磁盘空间分析 - " + rootNode.getName(), width, 20);
        
        // 递归绘制树状图
        Rectangle bounds = new Rectangle(40, 60, width - 80, height - 100);
        paintTreeMapRecursive(g2d, rootNode, bounds, 0);
        
        g2d.dispose();
    }
    
    /**
     * 递归绘制树状图
     */
    private void paintTreeMapRecursive(Graphics2D g2d, EnhancedFileNode node, Rectangle bounds, int depth) {
        if (node == null || node.getChildren().isEmpty()) {
            return;
        }
        
        List<EnhancedFileNode> children = getSortedChildren(node);
        if (children.isEmpty()) return;
        
        // 计算布局
        long totalSize = node.getTotalSize();
        if (totalSize == 0) return;
        
        // 使用更真实的树状图布局算法
        paintSquarifiedTreeMap(g2d, children, bounds, depth);
    }
    
    /**
     * 使用平方化算法绘制树状图
     */
    private void paintSquarifiedTreeMap(Graphics2D g2d, List<EnhancedFileNode> items, Rectangle bounds, int depth) {
        if (items.isEmpty()) return;
        
        // 按大小排序
        items.sort((a, b) -> Long.compare(b.getTotalSize(), a.getTotalSize()));
        
        // 计算每个项目的比例
        long totalSize = items.stream().mapToLong(EnhancedFileNode::getTotalSize).sum();
        if (totalSize == 0) return;
        
        // 使用平方化布局算法
        paintSquarifiedLayout(g2d, items, bounds, 0, items.size(), totalSize, depth);
    }
    
    /**
     * 平方化布局算法
     */
    private void paintSquarifiedLayout(Graphics2D g2d, List<EnhancedFileNode> items, Rectangle bounds, 
                                     int start, int end, long totalSize, int depth) {
        if (start >= end) return;
        
        // 如果只剩余一个项目，直接绘制
        if (end - start == 1) {
            EnhancedFileNode item = items.get(start);
            Color color = getItemColor(start, item);
            paintTreeMapCell(g2d, bounds, item, color, depth);
            clickableAreas.put(bounds, item);
            return;
        }
        
        // 计算长宽比，决定分割方向
        double aspectRatio = (double) bounds.width / bounds.height;
        boolean horizontalSplit = aspectRatio > 1;
        
        // 找到最佳分割点
        int splitPoint = findSplitPoint(items, start, end, totalSize, bounds, horizontalSplit);
        
        if (splitPoint <= start || splitPoint >= end) {
            splitPoint = start + (end - start) / 2;
        }
        
        // 计算两个区域的尺寸
        Rectangle firstBounds, secondBounds;
        if (horizontalSplit) {
            long firstSize = items.subList(start, splitPoint).stream().mapToLong(EnhancedFileNode::getTotalSize).sum();
            int firstWidth = (int) (bounds.width * (double) firstSize / totalSize);
            
            firstBounds = new Rectangle(bounds.x, bounds.y, firstWidth, bounds.height);
            secondBounds = new Rectangle(bounds.x + firstWidth, bounds.y, bounds.width - firstWidth, bounds.height);
        } else {
            long firstSize = items.subList(start, splitPoint).stream().mapToLong(EnhancedFileNode::getTotalSize).sum();
            int firstHeight = (int) (bounds.height * (double) firstSize / totalSize);
            
            firstBounds = new Rectangle(bounds.x, bounds.y, bounds.width, firstHeight);
            secondBounds = new Rectangle(bounds.x, bounds.y + firstHeight, bounds.width, bounds.height - firstHeight);
        }
        
        // 递归绘制两个区域
        long firstTotalSize = items.subList(start, splitPoint).stream().mapToLong(EnhancedFileNode::getTotalSize).sum();
        paintSquarifiedLayout(g2d, items, firstBounds, start, splitPoint, firstTotalSize, depth);
        
        long secondTotalSize = items.subList(splitPoint, end).stream().mapToLong(EnhancedFileNode::getTotalSize).sum();
        paintSquarifiedLayout(g2d, items, secondBounds, splitPoint, end, secondTotalSize, depth);
    }
    
    /**
     * 找到最佳分割点
     */
    private int findSplitPoint(List<EnhancedFileNode> items, int start, int end, long totalSize, 
                             Rectangle bounds, boolean horizontalSplit) {
        double bestRatio = Double.MAX_VALUE;
        int bestSplit = start + 1;
        
        for (int i = start + 1; i < end; i++) {
            long firstSize = items.subList(start, i).stream().mapToLong(EnhancedFileNode::getTotalSize).sum();
            long secondSize = totalSize - firstSize;
            
            if (firstSize == 0 || secondSize == 0) continue;
            
            double firstRatio = horizontalSplit ? 
                (double) firstSize / bounds.width : (double) firstSize / bounds.height;
            double secondRatio = horizontalSplit ? 
                (double) secondSize / bounds.width : (double) secondSize / bounds.height;
            
            double maxRatio = Math.max(firstRatio, secondRatio);
            double minRatio = Math.min(firstRatio, secondRatio);
            double ratio = maxRatio / minRatio;
            
            if (ratio < bestRatio) {
                bestRatio = ratio;
                bestSplit = i;
            }
        }
        
        return bestSplit;
    }
    
    // 柱状图功能已移除
    
    /**
     * 绘制图表标题
     */
    private void paintChartTitle(Graphics2D g2d, String title, int width, int y) {
        g2d.setFont(themeManager.getTitleFont());
        g2d.setColor(themeManager.getTextColor());
        
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        
        g2d.drawString(title, (width - titleWidth) / 2, y + 20);
    }
    
    // 矩形块绘制功能已移除
    
    /**
     * 绘制树状图单元格
     */
    private void paintTreeMapCell(Graphics2D g2d, Rectangle rect, EnhancedFileNode item, Color color, int depth) {
        // 绘制背景
        g2d.setColor(color);
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        
        // 绘制边框
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        
        // 只在足够大的单元格中绘制内容
        if (rect.width > 60 && rect.height > 40) {
            // 固定使用黑色文字（亮色主题）
            g2d.setColor(Color.BLACK);
            paintItemContent(g2d, rect, item);
        }
    }
    
    // 柱形绘制功能已移除
    
    /**
     * 绘制项目内容（图标、名称、大小）
     */
    private void paintItemContent(Graphics2D g2d, Rectangle rect, EnhancedFileNode item) {
        // 固定使用黑色文字（亮色主题）
        g2d.setColor(Color.BLACK);
        
        // 绘制图标
        g2d.setFont(themeManager.getUIFont().deriveFont(16f));
        
        FontMetrics fm = g2d.getFontMetrics();
        String icon = item.getFileType().getIcon();
        int iconWidth = fm.stringWidth(icon);
        
        int iconX = rect.x + (rect.width - iconWidth) / 2;
        int iconY = rect.y + rect.height / 3;
        
        g2d.drawString(icon, iconX, iconY);
        
        // 绘制文件名
        g2d.setFont(themeManager.getUIFont().deriveFont(12f));
        fm = g2d.getFontMetrics();
        
        String name = truncateText(item.getName(), fm, rect.width - 10);
        int nameWidth = fm.stringWidth(name);
        int nameX = rect.x + (rect.width - nameWidth) / 2;
        int nameY = iconY + 20;
        
        g2d.drawString(name, nameX, nameY);
        
        // 绘制大小
        g2d.setFont(themeManager.getSmallFont());
        fm = g2d.getFontMetrics();
        
        String size = item.getFormattedSize();
        int sizeWidth = fm.stringWidth(size);
        int sizeX = rect.x + (rect.width - sizeWidth) / 2;
        int sizeY = nameY + 15;
        
        g2d.drawString(size, sizeX, sizeY);
    }
    
    // 柱状图标签功能已移除
    
    // 饼图图例功能已移除
    
    /**
     * 绘制统计信息
     */
    private void paintStatistics(Graphics2D g2d, EnhancedFileNode rootNode, int width, int y) {
        g2d.setFont(themeManager.getSmallFont());
        g2d.setColor(themeManager.getTextSecondaryColor());
        
        String stats = String.format("总计: %d 个项目 | %s | 最后修改: %s", 
            rootNode.getFileCount() + rootNode.getDirectoryCount(),
            rootNode.getFormattedSize(),
            rootNode.getFormattedLastModified());
        
        FontMetrics fm = g2d.getFontMetrics();
        int statsWidth = fm.stringWidth(stats);
        
        g2d.drawString(stats, (width - statsWidth) / 2, y - 10);
    }
    
    /**
     * 绘制悬停效果
     */
    private void paintHoverEffect(Graphics g, Rectangle rect, EnhancedFileNode item) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制高亮边框
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(rect.x - 2, rect.y - 2, rect.width + 4, rect.height + 4);
        
        // 绘制阴影效果
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRect(rect.x + 2, rect.y + 2, rect.width, rect.height);
        
        // 绘制工具提示信息
        paintTooltip(g2d, item, rect);
        
        g2d.dispose();
    }
    
    /**
     * 绘制工具提示
     */
    private void paintTooltip(Graphics2D g2d, EnhancedFileNode item, Rectangle rect) {
        // 简化的工具提示实现 - 显示文件信息
        String tooltip = String.format("%s: %s", 
            item.getName(), item.getFormattedSize());
        
        FontMetrics fm = g2d.getFontMetrics();
        int tooltipWidth = fm.stringWidth(tooltip) + 20;
        int tooltipHeight = 24;
        
        int x = rect.x + rect.width / 2 - tooltipWidth / 2;
        int y = rect.y - tooltipHeight - 5;
        
        // 确保工具提示在可视区域内
        if (x < 0) x = 0;
        if (x + tooltipWidth > g2d.getClipBounds().width) {
            x = g2d.getClipBounds().width - tooltipWidth;
        }
        
        // 绘制背景
        g2d.setColor(new Color(50, 50, 50, 240));
        g2d.fillRoundRect(x, y, tooltipWidth, tooltipHeight, 6, 6);
        
        // 绘制边框
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, tooltipWidth, tooltipHeight);
        
        // 绘制文字
        g2d.setColor(Color.WHITE);
        g2d.drawString(tooltip, x + 10, y + 16);
    }
    
    /**
     * 获取排序后的子节点
     */
    private List<EnhancedFileNode> getSortedChildren(EnhancedFileNode rootNode) {
        List<EnhancedFileNode> children = new ArrayList<>(rootNode.getChildren());
        children.sort((a, b) -> Long.compare(b.getTotalSize(), a.getTotalSize()));
        return children;
    }
    
    // 计算最佳列数功能已移除
    
    /**
     * 获取项目颜色
     */
    private Color getItemColor(int index, EnhancedFileNode item) {
        // 根据文件类型选择颜色
        switch (item.getFileType()) {
            case DIRECTORY:
                return colorPalette[0];
            case DOCUMENT:
                return colorPalette[1];
            case IMAGE:
                return colorPalette[2];
            case VIDEO:
                return colorPalette[3];
            case AUDIO:
                return colorPalette[4];
            case ARCHIVE:
                return colorPalette[5];
            case EXECUTABLE:
                return colorPalette[6];
            case CODE:
                return colorPalette[7];
            default:
                return colorPalette[index % colorPalette.length];
        }
    }
    
    /**
     * 颜色加深函数
     */
    private Color darkenColor(Color color, float factor) {
        return new Color(
            Math.max(0, (int)(color.getRed() * (1 - factor))),
            Math.max(0, (int)(color.getGreen() * (1 - factor))),
            Math.max(0, (int)(color.getBlue() * (1 - factor)))
        );
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
     * 文本截断函数
     */
    private String truncateText(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        
        for (int i = text.length() - 1; i >= 0; i--) {
            String truncated = text.substring(0, i) + ellipsis;
            if (fm.stringWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        
        return ellipsis;
    }
    
    /**
     * 获取总大小（用于计算百分比）
     */
    private long getTotalSize(EnhancedFileNode item) {
        // 这里应该返回父节点的总大小，简化实现
        return item.getTotalSize() * 10; // 临时解决方案
    }
    
    /**
     * 处理鼠标移动事件
     */
    public EnhancedFileNode handleMouseMove(int x, int y) {
        hoveredNode = null;
        hoveredArea = null;
        
        for (Map.Entry<Rectangle, EnhancedFileNode> entry : clickableAreas.entrySet()) {
            Rectangle rect = entry.getKey();
            if (rect.contains(x, y)) {
                hoveredNode = entry.getValue();
                hoveredArea = rect;
                return hoveredNode;
            }
        }
        
        return null;
    }
    
    /**
     * 处理鼠标点击事件
     */
    public EnhancedFileNode handleMouseClick(int x, int y) {
        for (Map.Entry<Rectangle, EnhancedFileNode> entry : clickableAreas.entrySet()) {
            Rectangle rect = entry.getKey();
            if (rect.contains(x, y)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * 清除悬停状态
     */
    public void clearHover() {
        hoveredNode = null;
        hoveredArea = null;
    }
    
    /**
     * 设置图表类型（已简化，固定为树状图）
     */
    public void setChartType(ChartType type) {
        // 只支持树状图，忽略其他类型
        if (type == ChartType.TREE_MAP) {
            this.currentChartType = type;
            clearHover();
        }
    }
    
    /**
     * 获取当前图表类型（固定为树状图）
     */
    public ChartType getChartType() {
        return ChartType.TREE_MAP;
    }
}