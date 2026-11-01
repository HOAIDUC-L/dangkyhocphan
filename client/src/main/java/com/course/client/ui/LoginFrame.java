package com.course.client.ui;

import com.course.client.service.ClientService;
import com.course.client.ui.util.UiUtil;
import com.course.common.dto.LoginResponseDto;
import com.course.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Swing login frame for student authentication.
 */
public class LoginFrame extends JFrame {
    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final JComboBox<String> cmbServerNode;
    private final JButton btnLogin;
    private final JLabel lblStatus;

    public LoginFrame() {
        UiUtil.setupLookAndFeel();

        setTitle("Hệ Thống Đăng Ký Học Phần - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headerPanel.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(UiUtil.HEADER_FONT);
        lblTitle.setForeground(UiUtil.PRIMARY_COLOR);

        JLabel lblSubtitle = new JLabel("Cổng thông tin Đăng ký học phần", SwingConstants.CENTER);
        lblSubtitle.setFont(UiUtil.REGULAR_FONT);
        lblSubtitle.setForeground(Color.GRAY);

        headerPanel.add(lblTitle);
        headerPanel.add(lblSubtitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Tên đăng nhập / MSSV:");
        lblUser.setFont(UiUtil.TITLE_FONT);
        formPanel.add(lblUser, gbc);

        gbc.gridy = 1;
        txtUsername = new JTextField("sv01", 20);
        txtUsername.setFont(UiUtil.REGULAR_FONT);
        txtUsername.setPreferredSize(new Dimension(280, 36));
        formPanel.add(txtUsername, gbc);

        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UiUtil.TITLE_FONT);
        formPanel.add(lblPass, gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField("123456", 20);
        txtPassword.setFont(UiUtil.REGULAR_FONT);
        txtPassword.setPreferredSize(new Dimension(280, 36));
        formPanel.add(txtPassword, gbc);

        gbc.gridy = 4;
        JLabel lblServerNode = new JLabel("Cụm Máy Chủ (Server Cluster):");
        lblServerNode.setFont(UiUtil.TITLE_FONT);
        formPanel.add(lblServerNode, gbc);

        gbc.gridy = 5;
        String[] serverOptions = {
                "Server Node 1 (Cổng 9991)",
                "Server Node 2 (Cổng 9992)",
                "Tự động (Auto Failover: 9991 -> 9992)",
                "Mặc định (Cổng 9999)"
        };
        cmbServerNode = new JComboBox<>(serverOptions);
        cmbServerNode.setFont(UiUtil.REGULAR_FONT);
        cmbServerNode.setPreferredSize(new Dimension(280, 36));
        cmbServerNode.setSelectedIndex(0); // Default to Node 1
        formPanel.add(cmbServerNode, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Footer & Actions
        JPanel footerPanel = new JPanel(new BorderLayout(10, 10));
        footerPanel.setBackground(Color.WHITE);

        btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setFont(UiUtil.TITLE_FONT);
        btnLogin.setBackground(UiUtil.PRIMARY_COLOR);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(280, 42));
        btnLogin.addActionListener(this::handleLogin);
        footerPanel.add(btnLogin, BorderLayout.NORTH);

        lblStatus = new JLabel("Sẵn sàng kết nối tới máy chủ", SwingConstants.CENTER);
        lblStatus.setFont(UiUtil.SMALL_FONT);
        lblStatus.setForeground(Color.DARK_GRAY);
        footerPanel.add(lblStatus, BorderLayout.SOUTH);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        int serverOptionIndex = cmbServerNode.getSelectedIndex();

        if (username.isEmpty() || password.isEmpty()) {
            UiUtil.showWarning(this, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", "Thiếu thông tin");
            return;
        }

        // Disable button during background network request
        btnLogin.setEnabled(false);
        lblStatus.setText("Đang kết nối và xác thực với máy chủ...");
        lblStatus.setForeground(UiUtil.PRIMARY_COLOR);

        // Execute network call on background thread (SwingWorker) to keep UI responsive
        SwingWorker<Response, String> worker = new SwingWorker<>() {
            @Override
            protected Response doInBackground() {
                com.course.client.config.ClientConfig config = com.course.client.config.ClientConfig.getInstance();

                if (serverOptionIndex == 0) {
                    config.setServerPort(9991);
                    return ClientService.getInstance().login(username, password);
                } else if (serverOptionIndex == 1) {
                    config.setServerPort(9992);
                    return ClientService.getInstance().login(username, password);
                } else if (serverOptionIndex == 3) {
                    config.setServerPort(9999);
                    return ClientService.getInstance().login(username, password);
                } else {
                    // Auto Failover mode: Try Node 1 (9991), if failed then switch to Node 2 (9992)
                    publish("Đang thử kết nối Server Node 1 (Cổng 9991)...");
                    config.setServerPort(9991);
                    Response resp = ClientService.getInstance().login(username, password);

                    if (!resp.isSuccess() && isConnectionFailure(resp)) {
                        publish("Node 1 không phản hồi. Tự động chuyển vùng sang Node 2 (Cổng 9992)...");
                        config.setServerPort(9992);
                        resp = ClientService.getInstance().login(username, password);
                    }
                    return resp;
                }
            }

            private boolean isConnectionFailure(Response resp) {
                if (resp == null) return true;
                String msg = resp.getMessage();
                return msg != null && (msg.contains("Không thể kết nối") || msg.contains("Connection refused") ||
                        msg.contains("timed out") || msg.contains("refused") || msg.contains("Lỗi kết nối"));
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    lblStatus.setText(chunks.get(chunks.size() - 1));
                    lblStatus.setForeground(UiUtil.PRIMARY_COLOR);
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        lblStatus.setText("Đăng nhập thành công! Đang chuyển hướng...");
                        lblStatus.setForeground(UiUtil.SUCCESS_COLOR);

                        LoginResponseDto user = response.getDataAs(LoginResponseDto.class);
                        openMainDashboard(user);
                    } else {
                        lblStatus.setText("Đăng nhập thất bại: " + response.getMessage());
                        lblStatus.setForeground(UiUtil.DANGER_COLOR);
                        UiUtil.showError(LoginFrame.this, response.getMessage(), "Đăng nhập thất bại");
                    }
                } catch (Exception e) {
                    lblStatus.setText("Lỗi kết nối máy chủ");
                    lblStatus.setForeground(UiUtil.DANGER_COLOR);
                    UiUtil.showError(LoginFrame.this, "Không thể kết nối đến máy chủ: " + e.getMessage(), "Lỗi mạng");
                }
            }
        };
        worker.execute();
    }

    private void openMainDashboard(LoginResponseDto user) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
            dispose();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
