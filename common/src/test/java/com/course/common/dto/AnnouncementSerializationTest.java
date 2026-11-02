package com.course.common.dto;

import com.course.common.protocol.Action;
import com.course.common.protocol.Response;
import com.course.common.util.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnouncementSerializationTest {

    @Test
    @DisplayName("Should serialize and deserialize AnnouncementDto correctly")
    void testAnnouncementSerialization() {
        AnnouncementDto dto = new AnnouncementDto(
                "Gia hạn đăng ký học phần",
                "Hệ thống mở cổng đăng ký đến 24h00 ngày 02/11/2026.",
                "Ban Đào Tạo",
                "URGENT"
        );

        String json = JsonUtil.toJson(dto);
        assertNotNull(json);
        assertTrue(json.contains("Gia hạn đăng ký học phần"));
        assertTrue(json.contains("URGENT"));

        AnnouncementDto deserialized = JsonUtil.fromJson(json, AnnouncementDto.class);
        assertNotNull(deserialized);
        assertEquals("Gia hạn đăng ký học phần", deserialized.getTitle());
        assertEquals("Ban Đào Tạo", deserialized.getSender());
        assertEquals("URGENT", deserialized.getLevel());
        assertNotNull(deserialized.getTimestamp());
    }

    @Test
    @DisplayName("Should encapsulate AnnouncementDto inside Response push message")
    void testPushResponseWithAnnouncement() {
        AnnouncementDto dto = new AnnouncementDto(
                "Thông báo bảo trì",
                "Bảo trì định kỳ từ 01:00 đến 02:00 sáng.",
                "Quản Trị Viên",
                "WARNING"
        );

        Response pushResponse = Response.push(Action.ANNOUNCEMENT_PUSH, dto);
        String pushJson = JsonUtil.toJson(pushResponse);
        assertNotNull(pushJson);

        Response parsedResponse = JsonUtil.fromJson(pushJson, Response.class);
        assertNotNull(parsedResponse);
        assertEquals(Action.ANNOUNCEMENT_PUSH, parsedResponse.getAction());

        AnnouncementDto extractedDto = parsedResponse.getDataAs(AnnouncementDto.class);
        assertNotNull(extractedDto);
        assertEquals("Thông báo bảo trì", extractedDto.getTitle());
        assertEquals("WARNING", extractedDto.getLevel());
    }
}
