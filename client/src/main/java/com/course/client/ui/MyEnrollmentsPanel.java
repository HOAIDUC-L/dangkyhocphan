package com.course.client.ui;

import com.course.client.service.ClientService;
import com.course.client.service.ScheduleExcelExporter;
import com.course.client.state.ClientState;
import com.course.client.ui.util.UiUtil;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel displaying the student's registered courses, with cancellation and Excel export buttons.
 */
public class MyEnrollmentsPanel extends JPanel {
    private final JTable tableEnrollments;
    private final DefaultTableModel tableModel;
    private final JButton btnCancel;
    private final JButton btnExportExcel;
    private final JButton btnRefresh;
    private final JLabel lblSummaryFooter;

    private final List<EnrollmentDetailDto> currentEnrollments = new ArrayList<>();
    private final Runnable onEnrollmentChanged;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public interface ExcelExportHandler {
        void export(List<EnrollmentDetailDto> enrollments, File targetFile);
    }
    private ExcelExportHandler excelExportHandler;

    public MyEnrollmentsPanel(Runnable onEnrollmentChanged) {
        this.onEnrollmentChanged = onEnrollmentChanged;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Danh sách học phần đã đăng ký thành công");
        lblTitle.setFont(UiUtil.TITLE_FONT);
        lblTitle.setForeground(UiUtil.PRIMARY_COLOR);
        topPanel.add(lblTitle, BorderLayout.WEST);

        btnRefresh = new JButton("Làm mới danh sách");
        btnRefresh.setPreferredSize(new Dimension(150, 32));
        btnRefresh.addActionListener(e -> loadEnrollments());
        topPanel.add(btnRefresh, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Table
        String[] columnNames = {
                "Mã lớp HP", "Mã môn", "Tên học phần", "Số TC", "Giảng viên",
                "Lịch học", "Phòng", "Thời gian đăng ký"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableEnrollments = new JTable(tableModel);
        tableEnrollments.setRowHeight(30);
        tableEnrollments.setFont(UiUtil.REGULAR_FONT);
        tableEnrollments.getTableHeader().setFont(UiUtil.TITLE_FONT);
        tableEnrollments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableEnrollments.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        tableEnrollments.getColumnModel().getColumn(0).setPreferredWidth(100);
        tableEnrollments.getColumnModel().getColumn(1).setPreferredWidth(85);
        tableEnrollments.getColumnModel().getColumn(2).setPreferredWidth(220);
        tableEnrollments.getColumnModel().getColumn(3).setPreferredWidth(65);
        tableEnrollments.getColumnModel().getColumn(7).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(tableEnrollments);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Bar: Summary Footer + Actions
        JPanel bottomContainer = new JPanel(new BorderLayout(15, 10));
        bottomContainer.setBorder(new EmptyBorder(8, 4, 4, 4));

        lblSummaryFooter = new JLabel("Tổng số môn: 0  |  Tổng số tín chỉ: 0 / 24 TC (Tối thiểu: 12 TC)");
        lblSummaryFooter.setFont(UiUtil.TITLE_FONT);
        lblSummaryFooter.setForeground(UiUtil.COLOR_NAVY);
        bottomContainer.add(lblSummaryFooter, BorderLayout.WEST);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));

        btnExportExcel = new JButton("Xuất Thời Khóa Biểu Ra Excel");
        btnExportExcel.setFont(UiUtil.TITLE_FONT);
        btnExportExcel.setBackground(new Color(30, 130, 76));
        btnExportExcel.setForeground(Color.WHITE);
        btnExportExcel.setPreferredSize(new Dimension(240, 36));
        btnExportExcel.addActionListener(this::handleExportExcel);
        actionBar.add(btnExportExcel);

        btnCancel = new JButton("HỦY ĐĂNG KÝ HỌC PHẦN ĐÃ CHỌN");
        btnCancel.setFont(UiUtil.TITLE_FONT);
        btnCancel.setBackground(UiUtil.COLOR_DANGER_RED);
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setPreferredSize(new Dimension(260, 36));
        btnCancel.addActionListener(this::handleCancel);
        actionBar.add(btnCancel);

        bottomContainer.add(actionBar, BorderLayout.EAST);
        add(bottomContainer, BorderLayout.SOUTH);

        // Note: Initial data load is handled by MainFrame.syncEnrollmentsAndSchedule()
    }

    public void setExcelExportHandler(ExcelExportHandler handler) {
        this.excelExportHandler = handler;
    }

    public void loadEnrollments() {
        btnRefresh.setEnabled(false);
        SwingWorker<List<EnrollmentDetailDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EnrollmentDetailDto> doInBackground() {
                return ClientService.getInstance().getMyEnrollments();
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    List<EnrollmentDetailDto> list = get();
                    updateTableData(list);
                } catch (Exception e) {
                    UiUtil.showError(MyEnrollmentsPanel.this, "Không thể tải danh sách đăng ký: " + e.getMessage(), "Lỗi");
                }
            }
        };
        worker.execute();
    }

    public synchronized void updateTableData(List<EnrollmentDetailDto> list) {
        currentEnrollments.clear();
        tableModel.setRowCount(0);

        int totalCredits = 0;
        if (list != null) {
            currentEnrollments.addAll(list);
            for (EnrollmentDetailDto item : list) {
                totalCredits += item.getCredits();
                String regTime = item.getRegisteredAt() != null ? item.getRegisteredAt().format(formatter) : "N/A";
                tableModel.addRow(new Object[]{
                        item.getCourseId(),
                        item.getSubjectCode(),
                        item.getCourseName(),
                        item.getCredits(),
                        item.getLecturerName(),
                        item.getScheduleDisplay(),
                        item.getRoom(),
                        regTime
                });
            }
        }

        int courseCount = currentEnrollments.size();
        if (totalCredits < 12) {
            lblSummaryFooter.setText(String.format("⚠️ Tổng số môn: %d  |  Tổng số tín chỉ: %d / 24 TC (Chưa đủ tối thiểu 12 TC)",
                    courseCount, totalCredits));
            lblSummaryFooter.setForeground(UiUtil.COLOR_WARNING_AMBER);
        } else if (totalCredits <= 24) {
            lblSummaryFooter.setText(String.format("✅ Tổng số môn: %d  |  Tổng số tín chỉ: %d / 24 TC (Hợp lệ)",
                    courseCount, totalCredits));
            lblSummaryFooter.setForeground(UiUtil.COLOR_SUCCESS_GREEN);
        } else {
            lblSummaryFooter.setText(String.format("❌ Tổng số môn: %d  |  Tổng số tín chỉ: %d / 24 TC (Vượt quá 24 TC)",
                    courseCount, totalCredits));
            lblSummaryFooter.setForeground(UiUtil.COLOR_DANGER_RED);
        }
    }

    private void handleCancel(ActionEvent event) {
        int selectedRow = tableEnrollments.getSelectedRow();
        if (selectedRow < 0) {
            UiUtil.showWarning(this, "Vui lòng chọn học phần cần hủy đăng ký trong bảng.", "Chưa chọn môn");
            return;
        }

        String courseId = (String) tableModel.getValueAt(selectedRow, 0);
        String courseName = (String) tableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn hủy đăng ký lớp học phần '" + courseName + "' (" + courseId + ") không?",
                "Xác nhận hủy đăng ký", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnCancel.setEnabled(false);
        SwingWorker<Response, Void> worker = new SwingWorker<>() {
            @Override
            protected Response doInBackground() {
                return ClientService.getInstance().cancelCourse(courseId);
            }

            @Override
            protected void done() {
                btnCancel.setEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        UiUtil.showInfo(MyEnrollmentsPanel.this,
                                "Đã hủy đăng ký thành công lớp học phần: " + courseName,
                                "Hủy thành công");
                        loadEnrollments();
                        if (onEnrollmentChanged != null) {
                            onEnrollmentChanged.run();
                        }
                    } else {
                        UiUtil.showError(MyEnrollmentsPanel.this,
                                "Hủy đăng ký thất bại: " + response.getMessage(),
                                "Lỗi hủy đăng ký");
                    }
                } catch (Exception e) {
                    UiUtil.showError(MyEnrollmentsPanel.this, "Lỗi kết nối máy chủ: " + e.getMessage(), "Lỗi mạng");
                }
            }
        };
        worker.execute();
    }

    private void handleExportExcel(ActionEvent event) {
        if (currentEnrollments.isEmpty()) {
            UiUtil.showWarning(this, "Bạn chưa đăng ký môn học nào để xuất thời khóa biểu.", "Danh sách trống");
            return;
        }

        int totalCredits = currentEnrollments.stream().mapToInt(EnrollmentDetailDto::getCredits).sum();
        if (totalCredits < 12) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("CẢNH BÁO TÍN CHỈ:\nBạn mới đăng ký %d / 12 tín chỉ tối thiểu theo quy chế đào tạo.\nBạn có chắc chắn muốn xuất thời khóa biểu này ra file Excel không?", totalCredits),
                    "Chưa đạt tín chỉ tối thiểu", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String studentId = ClientState.getInstance().getCurrentUser() != null ?
                ClientState.getInstance().getCurrentUser().getStudentId() : "STUDENT";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu file Excel Thời Khóa Biểu");
        fileChooser.setSelectedFile(new File("schedule_" + studentId + "_" + java.time.LocalDate.now() + ".xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            try {
                ScheduleExcelExporter.exportToFile(new ArrayList<>(currentEnrollments), fileToSave);
                UiUtil.showInfo(this, "Xuất file Excel thành công:\n" + fileToSave.getAbsolutePath(), "Xuất thành công");
            } catch (Exception e) {
                UiUtil.showError(this, "Lỗi khi xuất file Excel: " + e.getMessage(), "Lỗi xuất file");
            }
        }
    }

    public List<EnrollmentDetailDto> getCurrentEnrollments() {
        return currentEnrollments;
    }
}
