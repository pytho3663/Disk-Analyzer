/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 增强文件节点模型
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.model;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版文件节点模型
 * 提供更丰富的文件信息和更好的性能
 */
public class EnhancedFileNode {
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        DIRECTORY("文件夹", "📁"),
        DOCUMENT("文档", "📄"),
        IMAGE("图片", "🖼️"),
        VIDEO("视频", "🎬"),
        AUDIO("音频", "🎵"),
        ARCHIVE("压缩包", "📦"),
        EXECUTABLE("可执行文件", "⚙️"),
        CODE("代码", "💻"),
        OTHER("其他", "📎");
        
        private final String displayName;
        private final String icon;
        
        FileType(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
    }
    
    private final File file;
    private final List<EnhancedFileNode> children;
    private final Map<String, Object> metadata;
    
    // 文件属性
    private long size;
    private long lastModified;
    private boolean isDirectory;
    private String extension;
    private FileType fileType;
    
    // 统计信息
    private int fileCount;
    private int directoryCount;
    private long totalSize;
    
    // 缓存
    private String formattedSize;
    private String formattedLastModified;
    private boolean isCalculated;
    
    // 文件类型映射
    private static final Map<String, FileType> EXTENSION_TYPE_MAP = new HashMap<>();
    
    static {
        // 初始化文件类型映射
        EXTENSION_TYPE_MAP.put("txt", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("doc", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("docx", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("pdf", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("xls", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("xlsx", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("ppt", FileType.DOCUMENT);
        EXTENSION_TYPE_MAP.put("pptx", FileType.DOCUMENT);
        
        EXTENSION_TYPE_MAP.put("jpg", FileType.IMAGE);
        EXTENSION_TYPE_MAP.put("jpeg", FileType.IMAGE);
        EXTENSION_TYPE_MAP.put("png", FileType.IMAGE);
        EXTENSION_TYPE_MAP.put("gif", FileType.IMAGE);
        EXTENSION_TYPE_MAP.put("bmp", FileType.IMAGE);
        EXTENSION_TYPE_MAP.put("svg", FileType.IMAGE);
        
        EXTENSION_TYPE_MAP.put("mp4", FileType.VIDEO);
        EXTENSION_TYPE_MAP.put("avi", FileType.VIDEO);
        EXTENSION_TYPE_MAP.put("mkv", FileType.VIDEO);
        EXTENSION_TYPE_MAP.put("mov", FileType.VIDEO);
        EXTENSION_TYPE_MAP.put("wmv", FileType.VIDEO);
        
        EXTENSION_TYPE_MAP.put("mp3", FileType.AUDIO);
        EXTENSION_TYPE_MAP.put("wav", FileType.AUDIO);
        EXTENSION_TYPE_MAP.put("flac", FileType.AUDIO);
        EXTENSION_TYPE_MAP.put("aac", FileType.AUDIO);
        EXTENSION_TYPE_MAP.put("m4a", FileType.AUDIO);
        
        EXTENSION_TYPE_MAP.put("zip", FileType.ARCHIVE);
        EXTENSION_TYPE_MAP.put("rar", FileType.ARCHIVE);
        EXTENSION_TYPE_MAP.put("7z", FileType.ARCHIVE);
        EXTENSION_TYPE_MAP.put("tar", FileType.ARCHIVE);
        EXTENSION_TYPE_MAP.put("gz", FileType.ARCHIVE);
        
        EXTENSION_TYPE_MAP.put("exe", FileType.EXECUTABLE);
        EXTENSION_TYPE_MAP.put("msi", FileType.EXECUTABLE);
        EXTENSION_TYPE_MAP.put("bat", FileType.EXECUTABLE);
        EXTENSION_TYPE_MAP.put("sh", FileType.EXECUTABLE);
        EXTENSION_TYPE_MAP.put("jar", FileType.EXECUTABLE);
        
        EXTENSION_TYPE_MAP.put("java", FileType.CODE);
        EXTENSION_TYPE_MAP.put("py", FileType.CODE);
        EXTENSION_TYPE_MAP.put("js", FileType.CODE);
        EXTENSION_TYPE_MAP.put("html", FileType.CODE);
        EXTENSION_TYPE_MAP.put("css", FileType.CODE);
        EXTENSION_TYPE_MAP.put("cpp", FileType.CODE);
        EXTENSION_TYPE_MAP.put("c", FileType.CODE);
        EXTENSION_TYPE_MAP.put("h", FileType.CODE);
    }
    
    public EnhancedFileNode(File file) {
        this.file = file;
        this.children = new ArrayList<>();
        this.metadata = new ConcurrentHashMap<>();
        
        // 基本属性
        this.isDirectory = file.isDirectory();
        this.lastModified = file.lastModified();
        
        if (!isDirectory) {
            this.size = file.length();
            this.extension = getFileExtension(file.getName());
            this.fileType = determineFileType();
        } else {
            this.size = 0;
            this.extension = "";
            this.fileType = FileType.DIRECTORY;
        }
        
        // 初始化统计信息
        this.fileCount = isDirectory ? 0 : 1;
        this.directoryCount = isDirectory ? 1 : 0;
        this.totalSize = size;
        
        // 初始化缓存
        this.formattedSize = formatSize(size);
        this.formattedLastModified = formatDate(lastModified);
        this.isCalculated = false;
    }
    
    /**
     * 添加子节点
     */
    public void addChild(EnhancedFileNode child) {
        children.add(child);
        updateStatistics();
    }
    
    /**
     * 批量添加子节点
     */
    public void addChildren(Collection<EnhancedFileNode> newChildren) {
        children.addAll(newChildren);
        updateStatistics();
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        fileCount = 0;
        directoryCount = 0;
        totalSize = size;
        
        for (EnhancedFileNode child : children) {
            fileCount += child.fileCount;
            directoryCount += child.directoryCount;
            totalSize += child.totalSize;
        }
        
        isCalculated = true;
    }
    
    /**
     * 计算目录大小（递归）
     */
    public void calculateDirectorySize() {
        if (!isDirectory) {
            return;
        }
        
        long totalSize = 0;
        for (EnhancedFileNode child : children) {
            child.calculateDirectorySize();
            totalSize += child.getSize();
        }
        
        this.size = totalSize;
        this.formattedSize = formatSize(size);
        updateStatistics();
    }
    
    /**
     * 确定文件类型
     */
    private FileType determineFileType() {
        if (extension.isEmpty()) {
            return FileType.OTHER;
        }
        
        return EXTENSION_TYPE_MAP.getOrDefault(extension.toLowerCase(), FileType.OTHER);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 格式化文件大小
     */
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
    
    /**
     * 格式化日期
     */
    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }
    
    /**
     * 按大小排序子节点
     */
    public void sortChildrenBySize() {
        children.sort((a, b) -> Long.compare(b.totalSize, a.totalSize));
        for (EnhancedFileNode child : children) {
            if (child.isDirectory()) {
                child.sortChildrenBySize();
            }
        }
    }
    
    /**
     * 按名称排序子节点
     */
    public void sortChildrenByName() {
        children.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (EnhancedFileNode child : children) {
            if (child.isDirectory()) {
                child.sortChildrenByName();
            }
        }
    }
    
    /**
     * 按类型排序子节点
     */
    public void sortChildrenByType() {
        children.sort((a, b) -> {
            int typeCompare = a.fileType.compareTo(b.fileType);
            if (typeCompare != 0) return typeCompare;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (EnhancedFileNode child : children) {
            if (child.isDirectory()) {
                child.sortChildrenByType();
            }
        }
    }
    
    /**
     * 按修改时间排序子节点
     */
    public void sortChildrenByModifiedTime() {
        children.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
        for (EnhancedFileNode child : children) {
            if (child.isDirectory()) {
                child.sortChildrenByModifiedTime();
            }
        }
    }
    
    /**
     * 根据名称查找子节点
     */
    public EnhancedFileNode findChildByName(String name) {
        for (EnhancedFileNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }
    
    /**
     * 获取指定类型的文件统计
     */
    public Map<FileType, Integer> getFileTypeStatistics() {
        Map<FileType, Integer> statistics = new HashMap<>();
        
        if (isDirectory) {
            for (EnhancedFileNode child : children) {
                if (!child.isDirectory) {
                    statistics.merge(child.fileType, 1, Integer::sum);
                }
            }
        }
        
        return statistics;
    }
    
    /**
     * 获取大文件列表（大于指定大小）
     */
    public List<EnhancedFileNode> getLargeFiles(long minSize) {
        List<EnhancedFileNode> largeFiles = new ArrayList<>();
        
        if (!isDirectory && size >= minSize) {
            largeFiles.add(this);
        } else if (isDirectory) {
            for (EnhancedFileNode child : children) {
                largeFiles.addAll(child.getLargeFiles(minSize));
            }
        }
        
        return largeFiles;
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 获取所有元数据
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    // Getter方法
    public File getFile() { return file; }
    public String getName() { return file.getName(); }
    public String getAbsolutePath() { return file.getAbsolutePath(); }
    public long getSize() { return size; }
    public long getLastModified() { return lastModified; }
    public boolean isDirectory() { return isDirectory; }
    public String getExtension() { return extension; }
    public FileType getFileType() { return fileType; }
    public List<EnhancedFileNode> getChildren() { return new ArrayList<>(children); }
    
    public int getFileCount() { return fileCount; }
    public int getDirectoryCount() { return directoryCount; }
    public long getTotalSize() { return totalSize; }
    
    public String getFormattedSize() { 
        if (formattedSize == null) {
            formattedSize = formatSize(size);
        }
        return formattedSize; 
    }
    
    public String getFormattedLastModified() { 
        if (formattedLastModified == null) {
            formattedLastModified = formatDate(lastModified);
        }
        return formattedLastModified; 
    }
    
    public boolean isCalculated() { return isCalculated; }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EnhancedFileNode that = (EnhancedFileNode) obj;
        return file.equals(that.file);
    }
    
    @Override
    public int hashCode() {
        return file.hashCode();
    }
}