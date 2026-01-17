/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 增强扫描服务
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer.service;

import com.diskanalyzer.model.EnhancedFileNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 增强扫描服务 - 高性能多线程文件扫描
 * 采用生产者-消费者模式，支持实时进度更新和内存优化
 */
public class EnhancedScanService {
    
    private static final Logger logger = Logger.getLogger(EnhancedScanService.class.getName());
    
    // 线程池配置
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY = 1000;
    private static final long KEEP_ALIVE_TIME = 60L;
    
    // 内存优化配置
    private static final int MAX_QUEUE_SIZE = 5000;
    private static final int BATCH_SIZE = 100;
    private static final long MEMORY_CHECK_INTERVAL = 1000; // 毫秒
    
    // 扫描状态
    private volatile boolean isScanning = false;
    private volatile boolean isCancelled = false;
    private final AtomicLong scannedFilesCount = new AtomicLong(0);
    private final AtomicLong totalFilesCount = new AtomicLong(0);
    private final AtomicLong totalSize = new AtomicLong(0);
    
    // 线程池
    private ExecutorService executorService;
    private CompletionService<EnhancedFileNode> completionService;
    private ScheduledExecutorService scheduledExecutor;
    
    // 队列和缓存
    private final BlockingQueue<ScanTask> taskQueue;
    private final ConcurrentLinkedQueue<EnhancedFileNode> resultBuffer;
    private final ConcurrentHashMap<String, Long> processedFiles;
    
    // 进度监听器
    private ScanProgressListener progressListener;
    
    // 内存监控
    private volatile long lastMemoryCheck = 0;
    private volatile boolean isMemoryLow = false;
    
    // 性能统计
    private long scanStartTime;
    private final ConcurrentHashMap<String, Long> scanStats;
    
    public EnhancedScanService() {
        this.taskQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.resultBuffer = new ConcurrentLinkedQueue<>();
        this.processedFiles = new ConcurrentHashMap<>();
        this.scanStats = new ConcurrentHashMap<>();
        
        initializeThreadPool();
    }
    
    private void initializeThreadPool() {
        // 创建工作线程池
        executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private final AtomicLong threadCounter = new AtomicLong(0);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "ScanWorker-" + threadCounter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 创建调度线程池用于进度更新
        scheduledExecutor = Executors.newScheduledThreadPool(2);
        
        // 初始化完成服务
        completionService = new ExecutorCompletionService<>(executorService);
    }
    
    /**
     * 开始扫描目录
     */
    public void startScan(File directory) {
        if (isScanning) {
            logger.warning("扫描已在进行中");
            return;
        }
        
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            if (progressListener != null) {
                progressListener.onScanFailed("无效的扫描目录");
            }
            return;
        }
        
        // 重新初始化线程池（如果已被清理）
        if (scheduledExecutor == null || scheduledExecutor.isShutdown()) {
            initializeThreadPool();
        }
        
        resetScanState();
        isScanning = true;
        isCancelled = false;
        scanStartTime = System.currentTimeMillis();
        
        logger.info("开始扫描目录: " + directory.getAbsolutePath());
        
        // 启动扫描任务
        CompletableFuture<EnhancedFileNode> scanFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return performScan(directory);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "扫描过程中发生错误", e);
                throw new RuntimeException("扫描失败", e);
            }
        }, executorService);
        
        // 启动进度更新任务
        startProgressUpdateTask();
        startMemoryMonitorTask();
        
        // 处理扫描结果
        scanFuture.whenComplete((result, throwable) -> {
            isScanning = false;
            
            if (throwable != null) {
                handleScanError(throwable);
            } else if (isCancelled) {
                handleScanCancelled();
            } else {
                handleScanComplete(result);
            }
            
            cleanup();
        });
    }
    
    /**
     * 执行实际的扫描逻辑（直接扫描，无预扫描阶段）
     */
    private EnhancedFileNode performScan(File directory) throws Exception {
        EnhancedFileNode rootNode = new EnhancedFileNode(directory);
        
        // 直接开始主扫描阶段，无预扫描统计
        scanDirectory(directory, rootNode, 0);
        
        return rootNode;
    }
    
    // 预扫描阶段已移除 - 直接扫描模式
    
    /**
     * 扫描目录
     */
    private void scanDirectory(File directory, EnhancedFileNode parentNode, int depth) throws Exception {
        if (isCancelled) {
            throw new CancellationException("扫描被取消");
        }
        
        // 检查内存使用情况
        checkMemoryUsage();
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        // 批量处理文件
        List<File> batch = new ArrayList<>(BATCH_SIZE);
        
        for (File file : files) {
            if (isCancelled) {
                throw new CancellationException("扫描被取消");
            }
            
            // 跳过已处理的文件（防止符号链接循环）
            // 但确保在预扫描阶段已经统计过这些文件
            if (processedFiles.containsKey(file.getAbsolutePath())) {
                continue;
            }
            
            // 只处理在预扫描中统计过的文件类型
            if (!file.isDirectory() && !file.isFile()) {
                continue;
            }
            
            batch.add(file);
            
            // 当批次满时，提交扫描任务
            if (batch.size() >= BATCH_SIZE) {
                processBatch(batch, parentNode, depth);
                batch.clear();
                
                // 短暂休眠，避免CPU过载
                Thread.sleep(1);
            }
        }
        
        // 处理剩余的文件
        if (!batch.isEmpty()) {
            processBatch(batch, parentNode, depth);
        }
    }
    
    /**
     * 处理一批文件
     */
    private void processBatch(List<File> batch, EnhancedFileNode parentNode, int depth) throws Exception {
        List<CompletableFuture<EnhancedFileNode>> futures = new ArrayList<>();
        
        for (File file : batch) {
            CompletableFuture<EnhancedFileNode> future = CompletableFuture.supplyAsync(() -> {
                return scanFile(file, depth);
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        for (CompletableFuture<EnhancedFileNode> future : futures) {
            try {
                EnhancedFileNode node = future.get(30, TimeUnit.SECONDS);
                if (node != null) {
                    parentNode.addChild(node);
                    
                    // 如果是目录，递归扫描
                    if (node.isDirectory()) {
                        scanDirectory(new File(node.getAbsolutePath()), node, depth + 1);
                    }
                }
            } catch (TimeoutException e) {
                logger.warning("扫描任务超时");
            } catch (Exception e) {
                logger.log(Level.WARNING, "扫描文件失败", e);
            }
        }
    }
    
    /**
     * 扫描单个文件
     */
    private EnhancedFileNode scanFile(File file, int depth) {
        try {
            // 记录已处理的文件
            processedFiles.put(file.getAbsolutePath(), file.length());
            
            EnhancedFileNode node = new EnhancedFileNode(file);
            
            // 更新统计信息
            updateScanStats(file, depth);
            
            // 增加已扫描文件计数
            scannedFilesCount.incrementAndGet();
            totalSize.addAndGet(file.length());
            
            return node;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "扫描文件失败: " + file.getAbsolutePath(), e);
            return null;
        }
    }
    
    /**
     * 更新扫描统计
     */
    private void updateScanStats(File file, int depth) {
        String extension = getFileExtension(file);
        String key = "ext_" + extension;
        scanStats.merge(key, 1L, Long::sum);
        
        String sizeKey = "size_" + getFileSizeCategory(file.length());
        scanStats.merge(sizeKey, 1L, Long::sum);
        
        scanStats.merge("depth_" + depth, 1L, Long::sum);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex > 0 ? name.substring(lastDotIndex + 1).toLowerCase() : "no_extension";
    }
    
    /**
     * 获取文件大小分类
     */
    private String getFileSizeCategory(long size) {
        if (size < 1024) return "tiny";
        if (size < 1024 * 1024) return "small";
        if (size < 1024 * 1024 * 1024) return "medium";
        return "large";
    }
    
    /**
     * 启动进度更新任务
     */
    private void startProgressUpdateTask() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (!isScanning || isCancelled) {
                return;
            }
            
            try {
                long currentCount = scannedFilesCount.get();
                long total = totalFilesCount.get();
                
                long elapsedTime = System.currentTimeMillis() - scanStartTime;
                double scanRate = currentCount / (elapsedTime / 1000.0); // 文件/秒
                
                // 直接扫描模式：显示已扫描文件数和速度，不显示百分比
                String message;
                if (total > 0) {
                    // 如果有预估总数，显示百分比
                    double progress = Math.min((double) currentCount / total * 100, 100.0);
                    long remainingFiles = Math.max(0, total - currentCount);
                    long estimatedTimeRemaining = scanRate > 0 ? (long) (remainingFiles / scanRate * 1000) : 0;
                    
                    message = String.format(
                        "扫描进度: %.1f%% (%d/%d 文件) | 速度: %.1f 文件/秒 | 预计剩余: %s",
                        progress, currentCount, total, scanRate,
                        formatTime(estimatedTimeRemaining)
                    );
                } else {
                    // 直接扫描模式：只显示已扫描文件数和速度
                    message = String.format(
                        "正在扫描: %d 文件 | 速度: %.1f 文件/秒 | 已用时间: %s",
                        currentCount, scanRate, formatTime(elapsedTime)
                    );
                }
                
                if (progressListener != null) {
                    progressListener.onProgressUpdate(message);
                }
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "更新进度时发生错误", e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 启动内存监控任务
     */
    private void startMemoryMonitorTask() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (!isScanning) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL) {
                return;
            }
            
            lastMemoryCheck = currentTime;
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // 如果内存使用超过80%，触发垃圾回收
            if (usedMemory > totalMemory * 0.8) {
                logger.info("内存使用率超过80%，触发垃圾回收");
                System.gc();
                isMemoryLow = true;
                
                if (progressListener != null) {
                    progressListener.onProgressUpdate("内存使用率高，正在优化...");
                }
            } else {
                isMemoryLow = false;
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * 检查内存使用情况
     */
    private void checkMemoryUsage() throws Exception {
        if (isMemoryLow) {
            // 如果内存不足，暂停扫描
            Thread.sleep(100);
            
            // 强制垃圾回收
            System.gc();
            
            // 再次检查内存
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            if (usedMemory > totalMemory * 0.9) {
                throw new OutOfMemoryError("内存不足，无法继续扫描");
            }
        }
    }
    
    /**
     * 处理扫描完成（直接扫描模式）
     */
    private void handleScanComplete(EnhancedFileNode result) {
        // 重置扫描状态
        isScanning = false;
        
        long elapsedTime = System.currentTimeMillis() - scanStartTime;
        double scanRate = scannedFilesCount.get() / (elapsedTime / 1000.0);
        
        logger.info(String.format(
            "扫描完成！文件数: %d, 总大小: %d bytes, 耗时: %s, 扫描速度: %.1f 文件/秒",
            scannedFilesCount.get(), totalSize.get(),
            formatTime(elapsedTime), scanRate
        ));
        
        // 打印扫描统计
        printScanStats();
        
        if (progressListener != null) {
            // 发送最终完成消息
            progressListener.onProgressUpdate(String.format(
                "扫描完成！共扫描 %d 个文件，耗时 %s，平均速度 %.1f 文件/秒",
                scannedFilesCount.get(), formatTime(elapsedTime), scanRate
            ));
            progressListener.onScanComplete(result);
        }
    }
    
    /**
     * 处理扫描错误
     */
    private void handleScanError(Throwable throwable) {
        // 重置扫描状态
        isScanning = false;
        
        logger.severe("扫描失败: " + throwable.getMessage());
        
        if (progressListener != null) {
            progressListener.onScanFailed("扫描失败: " + throwable.getMessage());
        }
    }
    
    /**
     * 处理扫描取消
     */
    private void handleScanCancelled() {
        // 重置扫描状态
        isScanning = false;
        
        logger.info("扫描已取消");
        
        if (progressListener != null) {
            progressListener.onScanFailed("扫描已取消");
        }
    }
    
    /**
     * 打印扫描统计
     */
    private void printScanStats() {
        logger.info("=== 扫描统计 ===");
        
        // 文件类型统计
        scanStats.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("ext_"))
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
            .limit(10)
            .forEach(entry -> {
                String ext = entry.getKey().substring(4);
                logger.info(String.format("扩展名 %s: %d 个文件", ext, entry.getValue()));
            });
        
        // 大小分类统计
        scanStats.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("size_"))
            .forEach(entry -> {
                String sizeCat = entry.getKey().substring(5);
                logger.info(String.format("大小分类 %s: %d 个文件", sizeCat, entry.getValue()));
            });
    }
    
    /**
     * 取消扫描
     */
    public void cancelScan() {
        if (isScanning) {
            isCancelled = true;
            logger.info("正在取消扫描...");
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            // 不再关闭线程池，只清理数据
            // 清空队列和缓存数据
            taskQueue.clear();
            resultBuffer.clear();
            
            logger.info("扫描服务数据清理完成");
        } catch (Exception e) {
            logger.log(Level.WARNING, "清理资源时发生错误", e);
        }
    }
    
    /**
     * 重置扫描状态
     */
    private void resetScanState() {
        isScanning = false;
        isCancelled = false;
        scannedFilesCount.set(0);
        totalFilesCount.set(0);
        totalSize.set(0);
        processedFiles.clear();
        scanStats.clear();
        taskQueue.clear();
        resultBuffer.clear();
    }
    
    /**
     * 格式化时间
     */
    private String formatTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "毫秒";
        } else if (milliseconds < 60000) {
            return String.format("%.1f秒", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%d分%d秒", minutes, seconds);
        }
    }
    
    /**
     * 获取已扫描文件数量
     */
    public long getScannedFilesCount() {
        return scannedFilesCount.get();
    }
    
    /**
     * 获取总文件数量
     */
    public long getTotalFilesCount() {
        return totalFilesCount.get();
    }
    
    /**
     * 获取总大小
     */
    public long getTotalSize() {
        return totalSize.get();
    }
    
    /**
     * 设置进度监听器
     */
    public void setProgressListener(ScanProgressListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * 关闭扫描服务，释放所有资源
     */
    public void shutdown() {
        try {
            isScanning = false;
            isCancelled = true;
            
            if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
                scheduledExecutor.shutdown();
                scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);
            }
            
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            }
            
            // 清空所有数据
            taskQueue.clear();
            resultBuffer.clear();
            processedFiles.clear();
            scanStats.clear();
            
            logger.info("扫描服务已完全关闭");
        } catch (Exception e) {
            logger.log(Level.WARNING, "关闭扫描服务时发生错误", e);
        }
    }
    
    /**
     * 扫描进度监听器接口
     */
    public interface ScanProgressListener {
        void onProgressUpdate(String message);
        void onScanComplete(EnhancedFileNode rootNode);
        void onScanFailed(String error);
    }
    
    /**
     * 扫描任务类
     */
    private static class ScanTask {
        private final File file;
        private final int depth;
        
        public ScanTask(File file, int depth) {
            this.file = file;
            this.depth = depth;
        }
        
        public File getFile() { return file; }
        public int getDepth() { return depth; }
    }
}