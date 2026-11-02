package com.course.client.ui;

import com.course.client.ui.util.UiUtil;
import com.course.common.constants.AppConstants;
import com.course.common.dto.EnrollmentDetailDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Visual timetable matrix panel displaying a 10-period x 7-day schedule grid
 * with subject-specific pastel backgrounds, 3 centered lines of info, and rich tooltips.
 */
public class ScheduleMatrixPanel extends JPanel {
    private final JTable tableSchedule;
    private final DefaultTableModel tableModel;

    public static class ScheduleCellData {
        public final String courseId;
        public final String subjectCode;
        public final String courseName;
        public final int credits;
        public final String lecturerName;
        public final String room;
        public final String scheduleDisplay;

        public ScheduleCellData(EnrollmentDetailDto dto) {
            this.courseId = dto.getCourseId();
            this.subjectCode = dto.getSubjectCode();
            this.courseName = dto.getCourseName();
            this.credits = dto.getCredits();
            this.lecturerName = dto.getLecturerName();
            this.room = dto.getRoom() != null ? dto.getRoom() : "N/A";
            this.scheduleDisplay = dto.getScheduleDisplay();
        }
    }

    public ScheduleMatrixPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(UiUtil.COLOR_BG);

        // Top title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("📅 Thời Khóa Biểu Tuần (Ma trận 10 Tiết x 7 Ngày)");
        lblTitle.setFont(UiUtil.HEADER_FONT);
        lblTitle.setForeground(UiUtil.COLOR_NAVY);
        titlePanel.add(lblTitle);
        add(titlePanel, BorderLayout.NORTH);

        // Column headers: Tiết, Thứ 2, Thứ 3, Thứ 4, Thứ 5, Thứ 6, Thứ 7, Chủ Nhật
        String[] columns = {
                "Tiết học / Giờ",
                "Thứ 2 (Thứ Hai)",
                "Thứ 3 (Thứ Ba)",
                "Thứ 4 (Thứ Tư)",
                "Thứ 5 (Thứ Năm)",
                "Thứ 6 (Thứ Sáu)",
                "Thứ 7 (Thứ Bảy)",
                "Chủ Nhật"
        };

        tableModel = new DefaultTableModel(columns, AppConstants.MAX_PERIOD) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Populate row headers (Period 1 to 10 with time)
        for (int p = 1; p <= AppConstants.MAX_PERIOD; p++) {
            tableModel.setValueAt(
                    "<html><center><b style='color:#0F2B5C; font-size:12px;'>Tiết " + p + "</b><br><small style='color:#64748B;'>" + AppConstants.getPeriodTiming(p) + "</small></center></html>",
                    p - 1, 0
            );
        }

        tableSchedule = new JTable(tableModel);
        tableSchedule.setRowHeight(62);
        tableSchedule.getTableHeader().setFont(UiUtil.TITLE_FONT);
        tableSchedule.getTableHeader().setPreferredSize(new Dimension(100, 38));
        tableSchedule.getTableHeader().setBackground(new Color(241, 245, 249));
        tableSchedule.getTableHeader().setForeground(UiUtil.COLOR_NAVY);
        tableSchedule.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableSchedule.setCellSelectionEnabled(false);
        tableSchedule.setGridColor(UiUtil.COLOR_CARD_BORDER);

        // Column 0 renderer (Periods)
        DefaultTableCellRenderer periodRenderer = new DefaultTableCellRenderer() {
            private final Color periodBg = new Color(248, 250, 252);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(periodBg);
                }
                return c;
            }
        };
        tableSchedule.getColumnModel().getColumn(0).setCellRenderer(periodRenderer);
        tableSchedule.getColumnModel().getColumn(0).setPreferredWidth(130);

        // Course cell renderer for day columns
        ScheduleCellRenderer courseRenderer = new ScheduleCellRenderer();
        for (int c = 1; c <= 7; c++) {
            tableSchedule.getColumnModel().getColumn(c).setCellRenderer(courseRenderer);
            tableSchedule.getColumnModel().getColumn(c).setPreferredWidth(155);
        }

        JScrollPane scrollPane = new JScrollPane(tableSchedule);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiUtil.COLOR_CARD_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates the matrix using current enrolled courses.
     */
    public synchronized void updateSchedule(List<EnrollmentDetailDto> enrollments) {
        // Clear previous schedule cells (columns 1 to 7)
        for (int r = 0; r < AppConstants.MAX_PERIOD; r++) {
            for (int c = 1; c <= 7; c++) {
                tableModel.setValueAt(null, r, c);
            }
        }

        if (enrollments != null) {
            for (EnrollmentDetailDto item : enrollments) {
                int col = item.getDayOfWeek() - 1; // Day 2 (Mon) -> col 1, Day 8 (Sun) -> col 7
                if (col >= 1 && col <= 7) {
                    ScheduleCellData cellData = new ScheduleCellData(item);
                    for (int p = item.getStartPeriod(); p <= item.getEndPeriod(); p++) {
                        if (p >= 1 && p <= AppConstants.MAX_PERIOD) {
                            tableModel.setValueAt(cellData, p - 1, col);
                        }
                    }
                }
            }
        }
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 2) + ".." : text;
    }

    /**
     * Custom TableCellRenderer presenting pastel backgrounds per subject and 3 centered lines of course data.
     */
    private static class ScheduleCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (value instanceof ScheduleCellData cell) {
                if (!isSelected) {
                    c.setBackground(UiUtil.getPastelColor(cell.subjectCode));
                }
                String cellHtml = String.format(
                        "<html><center>" +
                        "<b style='color:#0F2B5C; font-size:11px;'>%s</b><br>" +
                        "<span style='color:#1E3A8A; font-size:11px;'>P: %s</span><br>" +
                        "<span style='color:#475569; font-size:10px;'>%s</span>" +
                        "</center></html>",
                        truncateText(cell.courseName, 20),
                        cell.room,
                        truncateText(cell.lecturerName, 18)
                );
                setText(cellHtml);

                String tooltipHtml = String.format(
                        "<html><div style='padding:4px; font-family:Segoe UI;'>" +
                        "<b style='font-size:12px; color:#0F2B5C;'>%s</b> (%s)<br>" +
                        "<b>Mã lớp học phần:</b> %s<br>" +
                        "<b>Số tín chỉ:</b> %d TC<br>" +
                        "<b>Giảng viên:</b> %s<br>" +
                        "<b>Phòng học:</b> %s<br>" +
                        "<b>Lịch học:</b> %s" +
                        "</div></html>",
                        cell.courseName, cell.subjectCode,
                        cell.courseId, cell.credits,
                        cell.lecturerName, cell.room, cell.scheduleDisplay
                );
                setToolTipText(tooltipHtml);
            } else {
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                setText("");
                setToolTipText(null);
            }

            return c;
        }
    }
}
