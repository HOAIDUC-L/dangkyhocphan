package com.course.client.ui;

import com.course.client.network.ClientSocketManager;
import com.course.client.network.ServerPushListener;
import com.course.client.service.ClientService;
import com.course.client.ui.util.UiUtil;
import com.course.common.constants.AppConstants;
import com.course.common.dto.SearchCourseRequestDto;
import com.course.common.model.Course;
import com.course.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Course catalog panel allowing search, viewing remaining slots,
 * and registering for courses with real-time slot update via server push broadcast.
 */
public class CoursePanel extends JPanel implements ServerPushListener {
    private final JTextField txtSearch;
    private final JComboBox<String> cbSearchType;
    private final JButton btnSearch;
    private final JButton btnRefresh;
    private final JButton btnRegister;

    private final JTable tableCourses;
    private final DefaultTableModel tableModel;

    private final List<Course> currentCourses = new ArrayList<>();
    private final Runnable onEnrollmentChanged;

    public CoursePanel(Runnable onEnrollmentChanged) {
        this.onEnrollmentChanged = onEnrollmentChanged;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Search Toolbar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.add(new JLabel("Tìm kiếm:"));

        txtSearch = new JTextField(18);
        txtSearch.setPreferredSize(new Dimension(200, 32));
        searchBar.add(txtSearch);

        cbSearchType = new JComboBox<>(new String[]{"Tất cả thông tin", "Tên học phần", "Mã môn học", "Giảng viên"});
        cbSearchType.setPreferredSize(new Dimension(150, 32));
        searchBar.add(cbSearchType);

        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setBackground(UiUtil.PRIMARY_COLOR);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setPreferredSize(new Dimension(100, 32));
        btnSearch.addActionListener(this::handleSearch);
        searchBar.add(btnSearch);

        btnRefresh = new JButton("Làm mới");
        btnRefresh.setPreferredSize(new Dimension(90, 32));
        btnRefresh.addActionListener(e -> loadCourses());
        searchBar.add(btnRefresh);

        add(searchBar, BorderLayout.NORTH);

        // 2. Center Table
        // 2. Center Table
        String[] columnNames = {
                "Mã lớp HP", "Mã môn", "Tên môn học", "Số TC", "Giảng viên",
                "Thứ", "Tiết BĐ", "Tiết KT", "Phòng", "Tổng số", "Slot còn lại"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableCourses = new JTable(tableModel);
        tableCourses.setRowHeight(30);
        tableCourses.setFont(UiUtil.REGULAR_FONT);
        tableCourses.getTableHeader().setFont(UiUtil.TITLE_FONT);
        tableCourses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom renderer for slot column (badges for full / low / available slots)
        DefaultTableCellRenderer slotRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value instanceof Number) {
                    int remaining = ((Number) value).intValue();
                    if (remaining <= 0) {
                        setText("● HẾT CHỖ (0)");
                        setFont(UiUtil.TITLE_FONT);
                        if (!isSelected) {
                            setForeground(UiUtil.COLOR_DANGER_RED);
                        }
                    } else if (remaining <= 5) {
                        setText("⚡ Chỉ còn " + remaining + " chỗ!");
                        setFont(UiUtil.TITLE_FONT);
                        if (!isSelected) {
                            setForeground(UiUtil.COLOR_WARNING_AMBER);
                        }
                    } else {
                        setText(remaining + " chỗ");
                        setFont(UiUtil.REGULAR_FONT);
                        if (!isSelected) {
                            setForeground(UiUtil.COLOR_SUCCESS_GREEN);
                        }
                    }
                }
                return c;
            }
        };
        tableCourses.getColumnModel().getColumn(10).setCellRenderer(slotRenderer);

        // Center renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableCourses.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);
        tableCourses.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);

        // Column widths
        tableCourses.getColumnModel().getColumn(0).setPreferredWidth(100);
        tableCourses.getColumnModel().getColumn(1).setPreferredWidth(85);
        tableCourses.getColumnModel().getColumn(2).setPreferredWidth(210);
        tableCourses.getColumnModel().getColumn(3).setPreferredWidth(65);
        tableCourses.getColumnModel().getColumn(10).setPreferredWidth(130);

        JScrollPane scrollPane = new JScrollPane(tableCourses);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        btnRegister = new JButton("ĐĂNG KÝ HỌC PHẦN ĐÃ CHỌN");
        btnRegister.setFont(UiUtil.TITLE_FONT);
        btnRegister.setBackground(UiUtil.COLOR_SUCCESS_GREEN);
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setPreferredSize(new Dimension(260, 38));
        btnRegister.addActionListener(this::handleRegister);
        actionBar.add(btnRegister);

        add(actionBar, BorderLayout.SOUTH);

        // Register push listener to receive real-time COURSE_UPDATED events
        ClientSocketManager.getInstance().addPushListener(this);

        // Initial load
        loadCourses();
    }

    public void loadCourses() {
        btnRefresh.setEnabled(false);
        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() {
                return ClientService.getInstance().getCourses();
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    List<Course> list = get();
                    updateTableData(list);
                } catch (Exception e) {
                    UiUtil.showError(CoursePanel.this, "Không thể tải danh sách học phần: " + e.getMessage(), "Lỗi");
                }
            }
        };
        worker.execute();
    }

    private void handleSearch(ActionEvent event) {
        String keyword = txtSearch.getText().trim();
        int searchIdx = cbSearchType.getSelectedIndex();
        SearchCourseRequestDto.SearchType type = SearchCourseRequestDto.SearchType.ALL;
        if (searchIdx == 1) type = SearchCourseRequestDto.SearchType.COURSE_NAME;
        else if (searchIdx == 2) type = SearchCourseRequestDto.SearchType.SUBJECT_CODE;
        else if (searchIdx == 3) type = SearchCourseRequestDto.SearchType.LECTURER;

        final SearchCourseRequestDto.SearchType finalType = type;
        btnSearch.setEnabled(false);

        SwingWorker<List<Course>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Course> doInBackground() {
                return ClientService.getInstance().searchCourses(keyword, finalType);
            }

            @Override
            protected void done() {
                btnSearch.setEnabled(true);
                try {
                    List<Course> list = get();
                    updateTableData(list);
                } catch (Exception e) {
                    UiUtil.showError(CoursePanel.this, "Lỗi tìm kiếm: " + e.getMessage(), "Lỗi");
                }
            }
        };
        worker.execute();
    }

    private synchronized void updateTableData(List<Course> courses) {
        currentCourses.clear();
        tableModel.setRowCount(0);

        if (courses != null) {
            currentCourses.addAll(courses);
            for (Course c : courses) {
                tableModel.addRow(new Object[]{
                        c.getCourseId(),
                        c.getSubjectCode(),
                        c.getCourseName(),
                        c.getCredits(),
                        c.getLecturerName(),
                        AppConstants.getDayName(c.getDayOfWeek()),
                        c.getStartPeriod(),
                        c.getEndPeriod(),
                        c.getRoom(),
                        c.getMaxSlots(),
                        c.getRemainingSlots()
                });
            }
        }
    }

    private void handleRegister(ActionEvent event) {
        int selectedRow = tableCourses.getSelectedRow();
        if (selectedRow < 0) {
            UiUtil.showWarning(this, "Vui lòng chọn một lớp học phần trong bảng để đăng ký.", "Chưa chọn lớp");
            return;
        }

        String courseId = (String) tableModel.getValueAt(selectedRow, 0);
        String courseName = (String) tableModel.getValueAt(selectedRow, 2);
        int credits = (int) tableModel.getValueAt(selectedRow, 3);
        int remaining = (int) tableModel.getValueAt(selectedRow, 10);

        // Pre-check 24 credit semester limit on Client-side
        int currentCredits = com.course.client.state.ClientState.getInstance().getTotalEnrolledCredits();
        if (currentCredits + credits > 24) {
            UiUtil.showError(this,
                    String.format("Không thể đăng ký: Tổng số tín chỉ sẽ là %d TC, vượt quá giới hạn tối đa 24 TC cho phép!\n(Hiện có: %d TC + Môn này: %d TC)",
                            currentCredits + credits, currentCredits, credits),
                    "Vượt quá giới hạn tín chỉ (24 TC)");
            return;
        }

        if (remaining <= 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Lớp học phần này hiện đã đầy (0 slot). Bạn vẫn muốn gửi yêu cầu đến máy chủ chứ?",
                    "Cảnh báo hết slot", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        btnRegister.setEnabled(false);
        SwingWorker<Response, Void> worker = new SwingWorker<>() {
            @Override
            protected Response doInBackground() {
                return ClientService.getInstance().registerCourse(courseId);
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        UiUtil.showInfo(CoursePanel.this,
                                "Đăng ký thành công lớp học phần: " + courseName + " (" + courseId + ") - " + credits + " TC",
                                "Đăng ký thành công");
                        if (onEnrollmentChanged != null) {
                            onEnrollmentChanged.run();
                        }
                    } else {
                        UiUtil.showError(CoursePanel.this,
                                "Đăng ký thất bại: " + response.getMessage(),
                                "Đăng ký không thành công");
                    }
                } catch (Exception e) {
                    UiUtil.showError(CoursePanel.this, "Lỗi đăng ký: " + e.getMessage(), "Lỗi mạng");
                }
            }
        };
        worker.execute();
    }

    /**
     * REAL-TIME BROADCAST LISTENER
     * Instantly updates the Remaining Slots cell in the table upon receiving COURSE_UPDATED from Server.
     */
    @Override
    public void onCourseUpdated(String courseId, int remainingSlots) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (courseId.equals(tableModel.getValueAt(i, 0))) {
                    tableModel.setValueAt(remainingSlots, i, 10);
                    break;
                }
            }
            for (Course c : currentCourses) {
                if (c.getCourseId().equals(courseId)) {
                    c.setRemainingSlots(remainingSlots);
                    break;
                }
            }
        });
    }

    @Override
    public void onForceLogout(String message) {
    }

    @Override
    public void onDisconnected(String reason) {
    }
}
