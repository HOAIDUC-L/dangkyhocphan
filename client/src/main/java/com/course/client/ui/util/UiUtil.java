package com.course.client.ui.util;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UI utility for setting modern Look and Feel, fonts, and dialogs.
 */
public final class UiUtil {
    private static final Logger LOGGER = Logger.getLogger(UiUtil.class.getName());

    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 12);

    // Campus Identity Palette
    public static final Color COLOR_NAVY = new Color(15, 43, 92);         // #0F2B5C
    public static final Color COLOR_NAVY_DARK = new Color(30, 58, 138);   // #1E3A8A
    public static final Color COLOR_ACCENT = new Color(37, 99, 235);      // #2563EB
    public static final Color COLOR_BG = new Color(248, 250, 252);        // #F8FAFC
    public static final Color COLOR_CARD_BORDER = new Color(226, 232, 240);// #E2E8F0
    public static final Color COLOR_WARNING_AMBER = new Color(217, 119, 6);// #D97706
    public static final Color COLOR_WARNING_BG = new Color(254, 243, 199);// #FEF3C7
    public static final Color COLOR_SUCCESS_GREEN = new Color(16, 185, 129);// #10B981
    public static final Color COLOR_DANGER_RED = new Color(239, 68, 68);  // #EF4444

    // Backwards-compatible aliases
    public static final Color PRIMARY_COLOR = COLOR_NAVY;
    public static final Color SUCCESS_COLOR = COLOR_SUCCESS_GREEN;
    public static final Color DANGER_COLOR = COLOR_DANGER_RED;
    public static final Color WARNING_COLOR = COLOR_WARNING_AMBER;

    // Harmonious Pastel Palette for Timetable Schedule Matrix
    private static final Color[] PASTEL_COLORS = new Color[]{
            new Color(224, 242, 254), // Pastel Sky Blue
            new Color(220, 252, 231), // Pastel Mint Green
            new Color(254, 237, 213), // Pastel Soft Peach
            new Color(243, 232, 255), // Pastel Lavender
            new Color(255, 228, 230), // Pastel Rose
            new Color(207, 250, 254), // Pastel Cyan
            new Color(254, 249, 195)  // Pastel Soft Yellow
    };

    public static Color getPastelColor(String seed) {
        if (seed == null || seed.isEmpty()) {
            return PASTEL_COLORS[0];
        }
        int hash = Math.abs(seed.hashCode());
        return PASTEL_COLORS[hash % PASTEL_COLORS.length];
    }

    private UiUtil() {
    }

    public static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set system look and feel", e);
            }
        }

        // Set default font globally
        UIManager.put("defaultFont", REGULAR_FONT);
    }

    public static void showError(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE)
        );
    }

    public static void showInfo(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE)
        );
    }

    public static void showWarning(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE)
        );
    }
}
