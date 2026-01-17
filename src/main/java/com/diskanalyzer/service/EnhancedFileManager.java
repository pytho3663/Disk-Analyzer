/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 增强文件管理器
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.service;

import com.diskanalyzer.model.EnhancedFileNode;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 增强文件管理器 - 提供完整的文件操作功能
 * 包括安全删除、系统回收站集成、批量操作等
 */
public class EnhancedFileManager {
    
    private static final Logger logger = Logger.getLogger(EnhancedFileManager.class.getName());
    
    // 系统保护文件列表
    private static final String[] PROTECTED_FILES = {
        "ntldr", "boot.ini", "bootmgr", "pagefile.sys", "hiberfil.sys",
        "swapfile.sys", "autoexec.bat", "config.sys", "io.sys", "msdos.sys",
        "command.com", "kernel32.dll", "ntoskrnl.exe", "hal.dll",
        "smss.exe", "csrss.exe", "winlogon.exe", "lsass.exe", "services.exe",
        "svchost.exe", "explorer.exe", "taskhost.exe", "dwm.exe",
        "vmlinuz", "initrd.img", "grub.cfg", "fstab", "passwd", "shadow",
        "group", "sudoers", "hosts", "resolv.conf", "hostname", "locale.conf",
        "bashrc", "profile", "bash_profile", "init", "systemd", "mach_kernel",
        "kernel", "SystemVersion.plist", "com.apple.boot.plist", "launchd", "launchctl"
    };
    
    // 系统保护目录列表
    private static final String[] PROTECTED_DIRECTORIES = {
        "\\windows\\system32", "\\windows\\syswow64", "\\windows\\winsxs",
        "\\program files", "\\program files (x86)", "\\boot", "\\recovery",
        "/bin", "/sbin", "/usr/bin", "/usr/sbin", "/etc", "/lib", "/lib64",
        "/usr/lib", "/usr/lib64", "/boot", "/dev", "/proc", "/sys", "/root",
        "/system", "/library", "/cores", "/private", "/applications", "/utilities"
    };
    
    // 回收站管理
    private final File recycleBinDir;
    private final ConcurrentHashMap<String, RecycleBinEntry> recycleBinIndex;
    private final AtomicLong recycleBinCounter;
    
    // 操作监听器
    private FileOperationListener operationListener;
    
    public EnhancedFileManager() {
        this.recycleBinDir = new File(System.getProperty("user.home"), ".diskanalyzer_recycle");
        this.recycleBinIndex = new ConcurrentHashMap<>();
        this.recycleBinCounter = new AtomicLong(System.currentTimeMillis());
        
        initializeRecycleBin();
        loadRecycleBinIndex();
    }
    
    /**
     * 初始化回收站目录
     */
    private void initializeRecycleBin() {
        if (!recycleBinDir.exists()) {
            if (!recycleBinDir.mkdirs()) {
                logger.log(Level.WARNING, "无法创建回收站目录: " + recycleBinDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 加载回收站索引
     */
    private void loadRecycleBinIndex() {
        File indexFile = new File(recycleBinDir, "index.dat");
        if (indexFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile))) {
                @SuppressWarnings("unchecked")
                ConcurrentHashMap<String, RecycleBinEntry> loadedIndex = 
                    (ConcurrentHashMap<String, RecycleBinEntry>) ois.readObject();
                recycleBinIndex.putAll(loadedIndex);
            } catch (Exception e) {
                logger.log(Level.WARNING, "无法加载回收站索引", e);
            }
        }
    }
    
    /**
     * 保存回收站索引
     */
    private void saveRecycleBinIndex() {
        File indexFile = new File(recycleBinDir, "index.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexFile))) {
            oos.writeObject(recycleBinIndex);
        } catch (Exception e) {
            logger.log(Level.WARNING, "无法保存回收站索引", e);
        }
    }
    
    /**
     * 检查文件是否为系统保护文件
     */
    public boolean isSystemProtectedFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        String filePath = file.getAbsolutePath().toLowerCase();
        
        // 检查文件名
        for (String protectedFile : PROTECTED_FILES) {
            if (fileName.equals(protectedFile.toLowerCase())) {
                return true;
            }
        }
        
        // 检查文件路径
        for (String protectedDir : PROTECTED_DIRECTORIES) {
            if (filePath.contains(protectedDir.toLowerCase())) {
                return true;
            }
        }
        
        // 检查隐藏文件（通常是配置文件）
        if (fileName.startsWith(".") && fileName.length() > 1) {
            return true;
        }
        
        // 检查根目录下的文件
        if (file.getParentFile() != null && file.getParentFile().getAbsolutePath().equals("/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 安全删除文件或目录
     */
    public boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        // 检查是否为系统保护文件
        if (isSystemProtectedFile(file)) {
            logger.log(Level.WARNING, "尝试删除系统保护文件: " + file.getAbsolutePath());
            if (operationListener != null) {
                operationListener.onOperationFailed("无法删除系统保护文件: " + file.getName());
            }
            return false;
        }
        
        try {
            boolean success;
            if (file.isDirectory()) {
                success = deleteDirectory(file);
            } else {
                success = file.delete();
            }
            
            if (success && operationListener != null) {
                operationListener.onFileDeleted(file);
            }
            
            return success;
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "删除文件时权限不足: " + file.getAbsolutePath(), e);
            if (operationListener != null) {
                operationListener.onOperationFailed("权限不足: " + file.getName());
            }
            return false;
        }
    }
    
    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return false;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                } else {
                    if (!deleteFile(file)) {
                        return false;
                    }
                }
            }
        }
        
        return directory.delete();
    }
    
    /**
     * 移动文件到回收站（增强版）
     */
    public boolean moveToRecycleBin(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        // 检查是否为系统保护文件
        if (isSystemProtectedFile(file)) {
            logger.log(Level.WARNING, "尝试移动系统保护文件到回收站: " + file.getAbsolutePath());
            if (operationListener != null) {
                operationListener.onOperationFailed("无法移动系统保护文件: " + file.getName());
            }
            return false;
        }
        
        try {
            // 生成唯一的回收文件名
            String timestamp = String.valueOf(recycleBinCounter.incrementAndGet());
            String recycleName = timestamp + "_" + file.getName();
            File recycleFile = new File(recycleBinDir, recycleName);
            
            // 创建回收站条目
            RecycleBinEntry entry = new RecycleBinEntry(
                file.getAbsolutePath(),
                recycleFile.getAbsolutePath(),
                file.getName(),
                file.length(),
                System.currentTimeMillis()
            );
            
            // 移动文件
            boolean success;
            if (file.isDirectory()) {
                success = moveDirectory(file, recycleFile);
            } else {
                success = file.renameTo(recycleFile);
            }
            
            if (success) {
                // 添加到索引
                recycleBinIndex.put(timestamp, entry);
                saveRecycleBinIndex();
                
                if (operationListener != null) {
                    operationListener.onFileMovedToRecycleBin(file, entry);
                }
            }
            
            return success;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "移动文件到回收站失败: " + file.getAbsolutePath(), e);
            if (operationListener != null) {
                operationListener.onOperationFailed("移动到回收站失败: " + file.getName());
            }
            return false;
        }
    }
    
    /**
     * 批量移动文件到回收站
     */
    public boolean moveFilesToRecycleBin(List<File> files) {
        boolean allSuccess = true;
        List<String> failedFiles = new ArrayList<>();
        
        for (File file : files) {
            if (!moveToRecycleBin(file)) {
                allSuccess = false;
                failedFiles.add(file.getName());
            }
        }
        
        if (!allSuccess && operationListener != null) {
            operationListener.onBatchOperationFailed("部分文件移动失败", failedFiles);
        }
        
        return allSuccess;
    }
    
    /**
     * 移动目录
     */
    private boolean moveDirectory(File source, File target) {
        if (!source.exists() || !source.isDirectory()) {
            return false;
        }
        
        if (!target.mkdirs()) {
            return false;
        }
        
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    if (!moveDirectory(file, targetFile)) {
                        return false;
                    }
                } else {
                    if (!file.renameTo(targetFile)) {
                        return false;
                    }
                }
            }
        }
        
        return source.delete();
    }
    
    /**
     * 清空回收站
     */
    public boolean emptyRecycleBin() {
        try {
            boolean allDeleted = true;
            List<String> failedFiles = new ArrayList<>();
            
            for (RecycleBinEntry entry : recycleBinIndex.values()) {
                File recycleFile = new File(entry.getRecyclePath());
                if (recycleFile.exists()) {
                    if (recycleFile.isDirectory()) {
                        if (!deleteDirectory(recycleFile)) {
                            allDeleted = false;
                            failedFiles.add(entry.getOriginalName());
                        }
                    } else {
                        if (!recycleFile.delete()) {
                            allDeleted = false;
                            failedFiles.add(entry.getOriginalName());
                        }
                    }
                }
            }
            
            recycleBinIndex.clear();
            saveRecycleBinIndex();
            
            if (!allDeleted && operationListener != null) {
                operationListener.onBatchOperationFailed("部分文件删除失败", failedFiles);
            }
            
            return allDeleted;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "清空回收站失败", e);
            if (operationListener != null) {
                operationListener.onOperationFailed("清空回收站失败");
            }
            return false;
        }
    }
    
    /**
     * 获取回收站中的文件列表
     */
    public List<RecycleBinEntry> getRecycleBinFiles() {
        List<RecycleBinEntry> files = new ArrayList<>();
        files.addAll(recycleBinIndex.values());
        return files;
    }
    
    /**
     * 从回收站恢复文件
     */
    public boolean restoreFromRecycleBin(RecycleBinEntry entry) {
        if (entry == null) {
            return false;
        }
        
        try {
            File recycleFile = new File(entry.getRecyclePath());
            if (!recycleFile.exists()) {
                if (operationListener != null) {
                    operationListener.onOperationFailed("回收站文件不存在: " + entry.getOriginalName());
                }
                return false;
            }
            
            File originalFile = new File(entry.getOriginalPath());
            
            // 确保原始目录存在
            File parentDir = originalFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    if (operationListener != null) {
                        operationListener.onOperationFailed("无法创建原始目录");
                    }
                    return false;
                }
            }
            
            // 移动文件
            boolean success;
            if (recycleFile.isDirectory()) {
                success = moveDirectory(recycleFile, originalFile);
            } else {
                success = recycleFile.renameTo(originalFile);
            }
            
            if (success) {
                // 从索引中移除
                recycleBinIndex.remove(entry.getId());
                saveRecycleBinIndex();
                
                if (operationListener != null) {
                    operationListener.onFileRestored(originalFile, entry);
                }
            }
            
            return success;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "从回收站恢复文件失败", e);
            if (operationListener != null) {
                operationListener.onOperationFailed("恢复文件失败: " + entry.getOriginalName());
            }
            return false;
        }
    }
    
    /**
     * 打开文件或目录
     */
    public boolean openFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        
        try {
            // 使用系统命令打开文件
            String osName = System.getProperty("os.name").toLowerCase();
            String[] command;
            
            if (osName.contains("win")) {
                // Windows系统
                command = new String[]{"cmd", "/c", "start", file.getAbsolutePath()};
            } else if (osName.contains("mac")) {
                // macOS系统
                command = new String[]{"open", file.getAbsolutePath()};
            } else {
                // Linux系统
                command = new String[]{"xdg-open", file.getAbsolutePath()};
            }
            
            Process process = Runtime.getRuntime().exec(command);
            
            // 等待进程完成
            boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (completed && process.exitValue() == 0) {
                if (operationListener != null) {
                    operationListener.onFileOpened(file);
                }
                return true;
            } else {
                logger.log(Level.WARNING, "打开文件进程异常退出");
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "打开文件失败: " + file.getAbsolutePath(), e);
            if (operationListener != null) {
                operationListener.onOperationFailed("无法打开文件: " + file.getName());
            }
            return false;
        }
    }
    
    /**
     * 获取文件详细信息
     */
    public FileInfo getFileInfo(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        
        try {
            FileInfo info = new FileInfo();
            info.name = file.getName();
            info.path = file.getAbsolutePath();
            info.size = file.length();
            info.lastModified = file.lastModified();
            info.isDirectory = file.isDirectory();
            info.isHidden = file.isHidden();
            info.canRead = file.canRead();
            info.canWrite = file.canWrite();
            info.canExecute = file.canExecute();
            
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                info.fileCount = files != null ? files.length : 0;
                info.totalSize = calculateTotalSize(file);
            }
            
            // 获取文件类型
            info.fileType = determineFileType(file);
            
            return info;
        } catch (Exception e) {
            logger.log(Level.WARNING, "获取文件信息失败: " + file.getAbsolutePath(), e);
            return null;
        }
    }
    
    /**
     * 计算目录总大小（优化版）
     */
    private long calculateTotalSize(File directory) {
        if (!directory.isDirectory()) {
            return directory.length();
        }
        
        final AtomicLong totalSize = new AtomicLong(0);
        
        try {
            Files.walk(directory.toPath())
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        totalSize.addAndGet(Files.size(path));
                    } catch (IOException e) {
                        // 忽略无法访问的文件
                    }
                });
        } catch (IOException e) {
            logger.log(Level.WARNING, "计算目录大小失败: " + directory.getAbsolutePath(), e);
        }
        
        return totalSize.get();
    }
    
    /**
     * 确定文件类型
     */
    private String determineFileType(File file) {
        if (file.isDirectory()) {
            return "文件夹";
        }
        
        String name = file.getName().toLowerCase();
        
        // 常见文件类型
        if (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".md")) {
            return "文本文件";
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || 
                   name.endsWith(".gif") || name.endsWith(".bmp")) {
            return "图片文件";
        } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") || 
                   name.endsWith(".mov") || name.endsWith(".wmv")) {
            return "视频文件";
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") || 
                   name.endsWith(".aac")) {
            return "音频文件";
        } else if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") || 
                   name.endsWith(".tar") || name.endsWith(".gz")) {
            return "压缩文件";
        } else if (name.endsWith(".exe") || name.endsWith(".msi") || name.endsWith(".deb") || 
                   name.endsWith(".rpm") || name.endsWith(".dmg")) {
            return "可执行文件";
        } else if (name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".pdf") || 
                   name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") || 
                   name.endsWith(".pptx")) {
            return "文档文件";
        } else {
            return "其他文件";
        }
    }
    
    /**
     * 批量删除文件
     */
    public boolean deleteFiles(List<File> files) {
        boolean allSuccess = true;
        List<String> failedFiles = new ArrayList<>();
        
        for (File file : files) {
            if (!deleteFile(file)) {
                allSuccess = false;
                failedFiles.add(file.getName());
            }
        }
        
        if (!allSuccess && operationListener != null) {
            operationListener.onBatchOperationFailed("部分文件删除失败", failedFiles);
        }
        
        return allSuccess;
    }
    
    /**
     * 复制文件
     */
    public boolean copyFile(File source, File target) {
        if (source == null || !source.exists() || target == null) {
            return false;
        }
        
        try {
            if (source.isDirectory()) {
                return copyDirectory(source, target);
            } else {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "复制文件失败", e);
            if (operationListener != null) {
                operationListener.onOperationFailed("复制文件失败: " + source.getName());
            }
            return false;
        }
    }
    
    /**
     * 复制目录
     */
    private boolean copyDirectory(File source, File target) {
        if (!source.exists() || !source.isDirectory()) {
            return false;
        }
        
        if (!target.mkdirs()) {
            return false;
        }
        
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    if (!copyDirectory(file, targetFile)) {
                        return false;
                    }
                } else {
                    try {
                        Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "复制文件失败", e);
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 移动文件
     */
    public boolean moveFile(File source, File target) {
        if (source == null || !source.exists() || target == null) {
            return false;
        }
        
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            if (operationListener != null) {
                operationListener.onFileMoved(source, target);
            }
            
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "移动文件失败", e);
            if (operationListener != null) {
                operationListener.onOperationFailed("移动文件失败: " + source.getName());
            }
            return false;
        }
    }
    
    /**
     * 重命名文件
     */
    public boolean renameFile(File file, String newName) {
        if (file == null || !file.exists() || newName == null || newName.trim().isEmpty()) {
            return false;
        }
        
        try {
            File newFile = new File(file.getParentFile(), newName.trim());
            return moveFile(file, newFile);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "重命名文件失败", e);
            if (operationListener != null) {
                operationListener.onOperationFailed("重命名文件失败: " + file.getName());
            }
            return false;
        }
    }
    
    /**
     * 创建新目录
     */
    public boolean createDirectory(File parentDir, String dirName) {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory() || 
            dirName == null || dirName.trim().isEmpty()) {
            return false;
        }
        
        try {
            File newDir = new File(parentDir, dirName.trim());
            boolean created = newDir.mkdirs();
            
            if (created && operationListener != null) {
                operationListener.onDirectoryCreated(newDir);
            }
            
            return created;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "创建目录失败", e);
            if (operationListener != null) {
                operationListener.onOperationFailed("创建目录失败: " + dirName);
            }
            return false;
        }
    }
    
    /**
     * 获取磁盘空间信息
     */
    public DiskSpaceInfo getDiskSpaceInfo(File path) {
        if (path == null) {
            return null;
        }
        
        try {
            FileStore store = Files.getFileStore(path.toPath());
            DiskSpaceInfo info = new DiskSpaceInfo();
            info.totalSpace = store.getTotalSpace();
            info.freeSpace = store.getUsableSpace();
            info.usedSpace = info.totalSpace - info.freeSpace;
            info.path = path.getAbsolutePath();
            
            return info;
        } catch (IOException e) {
            logger.log(Level.WARNING, "获取磁盘空间信息失败", e);
            return null;
        }
    }
    
    /**
     * 设置操作监听器
     */
    public void setOperationListener(FileOperationListener listener) {
        this.operationListener = listener;
    }
    
    /**
     * 文件操作监听器接口
     */
    public interface FileOperationListener {
        void onFileDeleted(File file);
        void onFileMovedToRecycleBin(File file, RecycleBinEntry entry);
        void onFileRestored(File file, RecycleBinEntry entry);
        void onFileOpened(File file);
        void onFileMoved(File source, File target);
        void onDirectoryCreated(File directory);
        void onOperationFailed(String message);
        void onBatchOperationFailed(String message, List<String> failedFiles);
    }
    
    /**
     * 文件信息类
     */
    public static class FileInfo {
        public String name;
        public String path;
        public long size;
        public long lastModified;
        public boolean isDirectory;
        public boolean isHidden;
        public boolean canRead;
        public boolean canWrite;
        public boolean canExecute;
        public int fileCount;
        public long totalSize;
        public String fileType;
        
        @Override
        public String toString() {
            return String.format("FileInfo{name='%s', path='%s', size=%d, isDirectory=%s, type='%s'}", 
                name, path, size, isDirectory, fileType);
        }
    }
    
    /**
     * 磁盘空间信息类
     */
    public static class DiskSpaceInfo {
        public long totalSpace;
        public long freeSpace;
        public long usedSpace;
        public String path;
        
        public double getUsagePercentage() {
            if (totalSpace == 0) return 0;
            return (double) usedSpace / totalSpace * 100;
        }
        
        public String getFormattedTotalSpace() {
            return formatSize(totalSpace);
        }
        
        public String getFormattedFreeSpace() {
            return formatSize(freeSpace);
        }
        
        public String getFormattedUsedSpace() {
            return formatSize(usedSpace);
        }
        
        private String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 回收站条目类
     */
    public static class RecycleBinEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String id;
        private final String originalPath;
        private final String recyclePath;
        private final String originalName;
        private final long size;
        private final long deletionTime;
        
        public RecycleBinEntry(String originalPath, String recyclePath, String originalName, 
                             long size, long deletionTime) {
            this.id = String.valueOf(deletionTime);
            this.originalPath = originalPath;
            this.recyclePath = recyclePath;
            this.originalName = originalName;
            this.size = size;
            this.deletionTime = deletionTime;
        }
        
        // Getter方法
        public String getId() { return id; }
        public String getOriginalPath() { return originalPath; }
        public String getRecyclePath() { return recyclePath; }
        public String getOriginalName() { return originalName; }
        public long getSize() { return size; }
        public long getDeletionTime() { return deletionTime; }
        
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
        
        public String getFormattedDeletionTime() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(deletionTime));
        }
        
        @Override
        public String toString() {
            return String.format("RecycleBinEntry{name='%s', size=%s, deleted=%s}", 
                originalName, getFormattedSize(), getFormattedDeletionTime());
        }
    }
}