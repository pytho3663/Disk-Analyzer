/*
 * Copyright (c) 2026 Disk Analyzer. All rights reserved.
 * 磁盘空间分析器 - 最终优化版主程序
 * This software is released under the BSD 3-Clause License.
 */

package com.diskanalyzer;

import com.diskanalyzer.controller.MainUIController;
import com.diskanalyzer.service.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

/**
 * 磁盘空间分析器 v2.0 - 最终优化版
 * 现代化、高性能、用户友好的磁盘空间管理工具
 */
public class FinalDiskAnalyzer extends JFrame {
    
    private static final String APP_NAME = "磁盘空间分析器";
    private static final String APP_VERSION = "2.0";
    private static final String APP_TITLE = APP_NAME + " v" + APP_VERSION;
    
    private MainUIController uiController;
    
    public FinalDiskAnalyzer() {
        super(APP_TITLE);
        initializeApplication();
    }
    
    private void initializeApplication() {
        try {
            // 设置系统属性
            setupSystemProperties();
            
            // 设置窗口属性
            setupFrameProperties();
            
            // 创建UI控制器
            ThemeManager themeManager = new ThemeManager();
            uiController = new MainUIController(this, themeManager);
            
            // 显示启动信息
            showStartupInfo();
            
        } catch (Exception e) {
            handleInitializationError(e);
        }
    }
    
    private void setupSystemProperties() {
        // 设置系统属性
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "true");
        
        // 设置外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("使用默认外观");
        }
        
        // 设置全局字体
        Font chineseFont = new Font("微软雅黑", Font.PLAIN, 14);
        UIManager.put("Label.font", chineseFont);
        UIManager.put("Button.font", chineseFont);
        UIManager.put("ToggleButton.font", chineseFont);
        UIManager.put("RadioButton.font", chineseFont);
        UIManager.put("CheckBox.font", chineseFont);
        UIManager.put("ComboBox.font", chineseFont);
        UIManager.put("List.font", chineseFont);
        UIManager.put("MenuBar.font", chineseFont);
        UIManager.put("MenuItem.font", chineseFont);
        UIManager.put("RadioButtonMenuItem.font", chineseFont);
        UIManager.put("CheckBoxMenuItem.font", chineseFont);
        UIManager.put("Menu.font", chineseFont);
        UIManager.put("PopupMenu.font", chineseFont);
        UIManager.put("OptionPane.font", chineseFont);
        UIManager.put("Panel.font", chineseFont);
        UIManager.put("ProgressBar.font", chineseFont);
        UIManager.put("ScrollPane.font", chineseFont);
        UIManager.put("Viewport.font", chineseFont);
        UIManager.put("TabbedPane.font", chineseFont);
        UIManager.put("Table.font", chineseFont);
        UIManager.put("TableHeader.font", chineseFont);
        UIManager.put("TextField.font", chineseFont);
        UIManager.put("PasswordField.font", chineseFont);
        UIManager.put("TextArea.font", chineseFont);
        UIManager.put("TextPane.font", chineseFont);
        UIManager.put("EditorPane.font", chineseFont);
        UIManager.put("TitledBorder.font", chineseFont);
        UIManager.put("ToolBar.font", chineseFont);
        UIManager.put("ToolTip.font", chineseFont);
        UIManager.put("Tree.font", chineseFont);
        UIManager.put("InternalFrame.titleFont", chineseFont);
        UIManager.put("DesktopIcon.font", chineseFont);
        UIManager.put("Spinner.font", chineseFont);
        UIManager.put("FormattedTextField.font", chineseFont);
        UIManager.put("Tree.font", chineseFont);
    }
    
    private void setupFrameProperties() {
        // 设置窗口图标
        setIconImage(createAppIcon());
        
        // 设置窗口大小和位置
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1400, screenSize.width - 100);
        int height = Math.min(900, screenSize.height - 100);
        
        setSize(width, height);
        setLocationRelativeTo(null); // 居中显示
        
        // 设置关闭操作
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 设置窗口属性
        setResizable(true);
        setMinimumSize(new Dimension(800, 600));
        
        // 设置标题
        setTitle(APP_TITLE);
    }
    
    private Image createAppIcon() {
        try {
            int size = 64;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            
            // 开启抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 绘制背景圆
            g2d.setColor(new Color(33, 150, 243));
            g2d.fillOval(0, 0, size, size);
            
            // 绘制磁盘图标
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            
            // 外圆
            g2d.drawOval(8, 8, size - 16, size - 16);
            
            // 内部扇形
            g2d.drawArc(16, 16, size - 32, size - 32, 0, 120);
            g2d.drawArc(16, 16, size - 32, size - 32, 120, 120);
            g2d.drawArc(16, 16, size - 32, size - 32, 240, 120);
            
            g2d.dispose();
            return image;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void showStartupInfo() {
        System.out.println("=== " + APP_TITLE + " 启动成功 ===");
        System.out.println("Java版本: " + System.getProperty("java.version"));
        System.out.println("操作系统: " + System.getProperty("os.name"));
        System.out.println("屏幕分辨率: " + Toolkit.getDefaultToolkit().getScreenSize().width + "x" + 
                          Toolkit.getDefaultToolkit().getScreenSize().height);
        System.out.println("========================================");
    }
    
    private void handleInitializationError(Exception e) {
        System.err.println("应用程序初始化失败: " + e.getMessage());
        e.printStackTrace();
        
        JOptionPane.showMessageDialog(
            this,
            "应用程序初始化失败:\n" + e.getMessage() + "\n\n请检查错误日志获取详细信息。",
            "初始化错误",
            JOptionPane.ERROR_MESSAGE
        );
        System.exit(1);
    }
    
    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        // 检查管理员权限
        if (!isAdmin()) {
            showAdminWarning();
        }
        
        // 启动应用程序
        SwingUtilities.invokeLater(() -> {
            try {
                FinalDiskAnalyzer analyzer = new FinalDiskAnalyzer();
                analyzer.setVisible(true);
            } catch (Exception e) {
                System.err.println("应用程序启动失败: " + e.getMessage());
                e.printStackTrace();
                
                JOptionPane.showMessageDialog(
                    null,
                    "应用程序启动失败:\n" + e.getMessage(),
                    "启动错误",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
    
    private static boolean isAdmin() {
        try {
            // Windows系统检查管理员权限
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec("net session");
                process.waitFor();
                return process.exitValue() == 0;
            }
            return true; // 非Windows系统默认有权限
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void showAdminWarning() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("权限检查");
            frame.setAlwaysOnTop(true);
            
            Object[] options = {"继续运行", "以管理员身份运行"};
            int choice = JOptionPane.showOptionDialog(
                frame,
                "警告：当前程序未以管理员身份运行，可能无法访问某些系统目录。\n\n建议：以管理员身份运行以获得完整功能。",
                "权限检查",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == JOptionPane.NO_OPTION) {
                // 尝试以管理员身份重新运行
                try {
                    String javaPath = System.getProperty("java.home") + "\\bin\\java.exe";
                    String classPath = System.getProperty("java.class.path");
                    String mainClass = "com.diskanalyzer.FinalDiskAnalyzer";
                    String workingDir = System.getProperty("user.dir");
                    
                    String powerShellCommand = String.format(
                        "powershell -Command \"Start-Process '%s' -ArgumentList '-cp', '%s', '%s' -Verb RunAs -WorkingDirectory '%s'\"",
                        javaPath, classPath, mainClass, workingDir
                    );
                    
                    Runtime.getRuntime().exec("cmd /c " + powerShellCommand);
                    System.exit(0);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "无法以管理员身份启动程序。\n当前将以普通权限运行。",
                        "启动失败",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
    }
}