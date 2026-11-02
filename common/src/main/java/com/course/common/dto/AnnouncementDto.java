package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO for emergency and administrative announcements broadcast across the cluster.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnouncementDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private String title;
    private String content;
    private String sender;    // e.g. "Ban Đào Tạo", "Quản Trị Viên Hệ Thống"
    private String level;     // "INFO", "WARNING", "URGENT"
    private String timestamp; // Formatted date time string

    public AnnouncementDto() {
    }

    public AnnouncementDto(String title, String content, String sender, String level) {
        this.title = title;
        this.content = content;
        this.sender = sender != null ? sender : "Ban Đào Tạo";
        this.level = level != null ? level : "INFO";
        this.timestamp = LocalDateTime.now().format(FORMATTER);
    }

    public AnnouncementDto(String title, String content, String sender, String level, String timestamp) {
        this.title = title;
        this.content = content;
        this.sender = sender;
        this.level = level;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AnnouncementDto{" +
                "title='" + title + '\'' +
                ", sender='" + sender + '\'' +
                ", level='" + level + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
