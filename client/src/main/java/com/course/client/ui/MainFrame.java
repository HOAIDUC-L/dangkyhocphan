package com.course.client.ui;

import com.course.client.network.ClientSocketManager;
import com.course.client.network.ServerPushListener;
import com.course.client.service.ClientService;
import com.course.client.ui.util.UiUtil;
import com.course.common.dto.AnnouncementDto;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.dto.LoginResponseDto;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Modern University Portal Main Frame for authenticated students.
 * Features Student Profile Card, Real-time Credit Progress Bar (12 - 24 TC),
 * and Campus Emergency Announcement Banner.
 */
public class MainFrame extends JFrame implements ServerPushListener {
    private final LoginResponseDto currentUser;

    private final CoursePanel coursePanel;
    private final MyEnrollmentsPanel myEnrollmentsPanel;
    private final ScheduleMatrixPanel scheduleMatrixPanel;
    private final JTabbedPane tabbedPane;

    // Credit progress bar components
    private JLabel lblCreditCount;
    private JLabel lblCreditHint;
    private JProgressBar progressBarCredits;

    // Announcement Banner components
    private JPanel bannerPanel;
    private JLabel lblBannerIcon;
    private JLabel lblBannerText;

    public MainFrame(LoginResponseDto user) {
        this.currentUser = user;

        setTitle("Cổng Đăng Ký Học Phần - " + (user != null ? user.getFullName() : "Sinh Viên") + " (" + (user != null ? user.getStudentId() : "") + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(1020, 640));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });

        JPanel contentPane = new JPanel(new BorderLayout(0, 0));
        contentPane.setBackground(UiUtil.COLOR_BG);
        setContentPane(contentPane);

        // Top Container: Header Bar + Emergency Announcement Banner
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(UiUtil.COLOR_BG);

        topContainer.add(createHeaderBar());
        topContainer.add(createAnnouncementBanner());
        contentPane.add(topContainer, BorderLayout.NORTH);

        // Center: Tabbed Panels
        Runnable refreshCallback = this::syncEnrollmentsAndSchedule;

        this.coursePanel = new CoursePanel(refreshCallback);
        this.myEnrollmentsPanel = new MyEnrollmentsPanel(refreshCallback);
        this.scheduleMatrixPanel = new ScheduleMatrixPanel();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiUtil.TITLE_FONT);
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.addTab("  📋 Đăng Ký Học Phần  ", coursePanel);
        tabbedPane.addTab("  📑 Học Phần Đã Đăng Ký  ", myEnrollmentsPanel);
        tabbedPane.addTab("  📅 Thời Khóa Biểu Tuần  ", scheduleMatrixPanel);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == scheduleMatrixPanel) {
                scheduleMatrixPanel.updateSchedule(myEnrollmentsPanel.getCurrentEnrollments());
            }
        });

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Register push listener for real-time events (Announcements, Force Logout)
        ClientSocketManager.getInstance().addPushListener(this);

        // Initial sync of enrollments, credit progress, and schedule
        syncEnrollmentsAndSchedule();
    }

    /**
     * University Portal Header: Profile Card + Credit Progress Bar + Actions
     */
    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(15, 10));
        header.setBorder(new CompoundBorder(
                new LineBorder(UiUtil.COLOR_CARD_BORDER, 1),
                new EmptyBorder(12, 18, 12, 18)
        ));
        header.setBackground(Color.WHITE);

        // 1. Left Profile Card
        JPanel profileCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        profileCard.setOpaque(false);

        // Student Avatar Badge
        JLabel lblAvatar = new JLabel("🎓", SwingConstants.CENTER);
        lblAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        lblAvatar.setPreferredSize(new Dimension(46, 46));
        lblAvatar.setOpaque(true);
        lblAvatar.setBackground(new Color(238, 242, 255));
        lblAvatar.setBorder(new LineBorder(new Color(199, 210, 254), 1, true));
        profileCard.add(lblAvatar);

        // Student Info Details
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel(currentUser != null ? currentUser.getFullName() : "Sinh viên");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblName.setForeground(UiUtil.COLOR_NAVY);

        String mssv = currentUser != null ? currentUser.getStudentId() : "N/A";
        String className = (currentUser != null && currentUser.getClassName() != null) ? currentUser.getClassName() : "N/A";
        JLabel lblDetails = new JLabel(String.format("MSSV: %s  •  Lớp: %s", mssv, className));
        lblDetails.setFont(UiUtil.SMALL_FONT);
        lblDetails.setForeground(new Color(100, 116, 139));

        JLabel lblOnline = new JLabel("● Trực tuyến");
        lblOnline.setFont(UiUtil.SMALL_FONT);
        lblOnline.setForeground(UiUtil.COLOR_SUCCESS_GREEN);

        JPanel subInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        subInfo.setOpaque(false);
        subInfo.add(lblDetails);
        subInfo.add(lblOnline);

        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(subInfo);

        profileCard.add(infoPanel);
        header.add(profileCard, BorderLayout.WEST);

        // 2. Center: Credit Progress Card
        JPanel creditCard = new JPanel();
        creditCard.setLayout(new BoxLayout(creditCard, BoxLayout.Y_AXIS));
        creditCard.setOpaque(false);
        creditCard.setBorder(new EmptyBorder(0, 15, 0, 15));

        JPanel creditTextRow = new JPanel(new BorderLayout());
        creditTextRow.setOpaque(false);

        lblCreditCount = new JLabel("Số tín chỉ đã đăng ký: 0 / 24 TC (Tối thiểu: 12 TC)");
        lblCreditCount.setFont(UiUtil.TITLE_FONT);
        lblCreditCount.setForeground(UiUtil.COLOR_NAVY);
        creditTextRow.add(lblCreditCount, BorderLayout.WEST);

        lblCreditHint = new JLabel("Chưa đạt mức tối thiểu (12 TC)");
        lblCreditHint.setFont(UiUtil.SMALL_FONT);
        lblCreditHint.setForeground(UiUtil.COLOR_WARNING_AMBER);
        creditTextRow.add(lblCreditHint, BorderLayout.EAST);

        progressBarCredits = new JProgressBar(0, 24);
        progressBarCredits.setValue(0);
        progressBarCredits.setStringPainted(true);
        progressBarCredits.setString("0 / 24 TC");
        progressBarCredits.setFont(UiUtil.SMALL_FONT);
        progressBarCredits.setPreferredSize(new Dimension(280, 18));
        progressBarCredits.setForeground(UiUtil.COLOR_WARNING_AMBER);

        creditCard.add(creditTextRow);
        creditCard.add(Box.createVerticalStrut(4));
        creditCard.add(progressBarCredits);

        header.add(creditCard, BorderLayout.CENTER);

        // 3. Right: Logout Button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        actionPanel.setOpaque(false);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(UiUtil.TITLE_FONT);
        btnLogout.setBackground(UiUtil.COLOR_DANGER_RED);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setPreferredSize(new Dimension(105, 34));
        btnLogout.addActionListener(e -> handleLogout());
        actionPanel.add(btnLogout);

        header.add(actionPanel, BorderLayout.EAST);
        return header;
    }

    /**
     * Emergency Broadcast Banner (University Portal Campus Alert)
     */
    private JPanel createAnnouncementBanner() {
        bannerPanel = new JPanel(new BorderLayout(12, 6));
        bannerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(251, 191, 36)),
                new EmptyBorder(8, 16, 8, 16)
        ));
        bannerPanel.setBackground(UiUtil.COLOR_WARNING_BG);
        bannerPanel.setVisible(false); // Initially hidden until an announcement arrives

        lblBannerIcon = new JLabel("📢");
        lblBannerIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        bannerPanel.add(lblBannerIcon, BorderLayout.WEST);

        lblBannerText = new JLabel("<html><b>[Ban Đào Tạo]</b> Thông báo khẩn cấp...</html>");
        lblBannerText.setFont(UiUtil.REGULAR_FONT);
        lblBannerText.setForeground(new Color(120, 53, 15));
        bannerPanel.add(lblBannerText, BorderLayout.CENTER);

        JButton btnDismiss = new JButton("✕");
        btnDismiss.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDismiss.setFocusPainted(false);
        btnDismiss.setBorderPainted(false);
        btnDismiss.setContentAreaFilled(false);
        btnDismiss.setForeground(new Color(120, 53, 15));
        btnDismiss.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDismiss.setToolTipText("Đóng thông báo này");
        btnDismiss.addActionListener(e -> bannerPanel.setVisible(false));
        bannerPanel.add(btnDismiss, BorderLayout.EAST);

        return bannerPanel;
    }

    /**
     * Updates the credit progress bar and dynamic colors based on current registered credits.
     * Thresholds:
     *   < 12 TC  : Amber (#F59E0B)  -> "Chưa đạt mức tối thiểu (12 TC)"
     *   12-24 TC : Green (#10B981)  -> "Số tín chỉ hợp lệ (Đủ điều kiện)"
     *   > 24 TC  : Red   (#EF4444)  -> "Vượt quá giới hạn tối đa (24 TC)"
     */
    public void updateCreditProgress(int totalCredits) {
        SwingUtilities.invokeLater(() -> {
            progressBarCredits.setValue(Math.min(24, totalCredits));
            progressBarCredits.setString(totalCredits + " / 24 TC");
            lblCreditCount.setText(String.format("Số tín chỉ đã đăng ký: %d / 24 TC (Tối thiểu: 12 TC)", totalCredits));

            if (totalCredits < 12) {
                progressBarCredits.setForeground(UiUtil.COLOR_WARNING_AMBER);
                lblCreditHint.setText("⚠️ Chưa đủ số tín chỉ tối thiểu (12 TC)");
                lblCreditHint.setForeground(UiUtil.COLOR_WARNING_AMBER);
            } else if (totalCredits <= 24) {
                progressBarCredits.setForeground(UiUtil.COLOR_SUCCESS_GREEN);
                lblCreditHint.setText("✅ Số tín chỉ hợp lệ (Đủ điều kiện)");
                lblCreditHint.setForeground(UiUtil.COLOR_SUCCESS_GREEN);
            } else {
                progressBarCredits.setForeground(UiUtil.COLOR_DANGER_RED);
                lblCreditHint.setText("❌ Vượt quá giới hạn tối đa 24 TC");
                lblCreditHint.setForeground(UiUtil.COLOR_DANGER_RED);
            }
        });
    }

    private void syncEnrollmentsAndSchedule() {
        SwingWorker<List<EnrollmentDetailDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EnrollmentDetailDto> doInBackground() {
                return ClientService.getInstance().getMyEnrollments();
            }

            @Override
            protected void done() {
                try {
                    List<EnrollmentDetailDto> list = get();
                    myEnrollmentsPanel.updateTableData(list);
                    scheduleMatrixPanel.updateSchedule(list);

                    int totalCredits = (list != null) ?
                            list.stream().mapToInt(EnrollmentDetailDto::getCredits).sum() : 0;
                    updateCreditProgress(totalCredits);
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private void cleanupListeners() {
        ClientSocketManager.getInstance().removePushListener(this);
        if (coursePanel != null) {
            ClientSocketManager.getInstance().removePushListener(coursePanel);
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?",
                "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            cleanupListeners();
            new Thread(() -> {
                try {
                    ClientService.getInstance().logout();
                } catch (Exception ignored) {
                } finally {
                    SwingUtilities.invokeLater(this::returnToLoginFrame);
                }
            }, "Client-LogoutThread").start();
        }
    }

    private void handleWindowClose() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn thoát khỏi ứng dụng?",
                "Xác nhận thoát", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            cleanupListeners();
            new Thread(() -> {
                try {
                    ClientService.getInstance().logout();
                } catch (Exception ignored) {
                } finally {
                    System.exit(0);
                }
            }, "Client-ExitThread").start();
        }
    }

    private void returnToLoginFrame() {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
        dispose();
    }

    // ==========================================
    // ServerPushListener Callbacks
    // ==========================================

    @Override
    public void onCourseUpdated(String courseId, int remainingSlots) {
        // Real-time slot update handled by coursePanel
    }

    /**
     * Triggered when an emergency announcement is received via server push.
     */
    @Override
    public void onAnnouncement(AnnouncementDto announcement) {
        if (announcement == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            String level = announcement.getLevel() != null ? announcement.getLevel().toUpperCase() : "INFO";
            String sender = announcement.getSender() != null ? announcement.getSender() : "Ban Đào Tạo";
            String title = announcement.getTitle() != null ? announcement.getTitle() : "";
            String content = announcement.getContent() != null ? announcement.getContent() : "";
            String time = announcement.getTimestamp() != null ? announcement.getTimestamp() : "";

            if ("URGENT".equals(level)) {
                bannerPanel.setBackground(new Color(254, 226, 226));
                bannerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(239, 68, 68)));
                lblBannerIcon.setText("🚨");
                lblBannerText.setForeground(new Color(153, 27, 27));
            } else if ("WARNING".equals(level)) {
                bannerPanel.setBackground(UiUtil.COLOR_WARNING_BG);
                bannerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(245, 158, 11)));
                lblBannerIcon.setText("⚠️");
                lblBannerText.setForeground(new Color(146, 64, 14));
            } else {
                bannerPanel.setBackground(new Color(224, 242, 254));
                bannerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(56, 189, 248)));
                lblBannerIcon.setText("📢");
                lblBannerText.setForeground(new Color(3, 105, 161));
            }

            String htmlText = String.format(
                    "<html><b>[%s - %s] %s</b>: %s <small style='color:#6B7280;'>(%s)</small></html>",
                    level, sender, title, content, time
            );
            lblBannerText.setText(htmlText);
            bannerPanel.setVisible(true);
            bannerPanel.revalidate();
            bannerPanel.repaint();

            // For URGENT announcements, also trigger a modal prompt so students immediately notice
            if ("URGENT".equals(level)) {
                JOptionPane.showMessageDialog(this,
                        String.format("THÔNG BÁO KHẨN CẤP TỪ %s:\n\n%s\n\n%s", sender.toUpperCase(), title, content),
                        "🚨 THÔNG BÁO KHẨN CẤP",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    @Override
    public void onForceLogout(String message) {
        SwingUtilities.invokeLater(() -> {
            cleanupListeners();
            JOptionPane.showMessageDialog(this,
                    message,
                    "Thông báo từ Quản Trị Viên",
                    JOptionPane.WARNING_MESSAGE);
            returnToLoginFrame();
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(this,
                    "Mất kết nối với máy chủ: " + reason + "\nBạn có muốn trở về màn hình đăng nhập không?",
                    "Mất kết nối", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (option == JOptionPane.YES_OPTION) {
                returnToLoginFrame();
            }
        });
    }
}
