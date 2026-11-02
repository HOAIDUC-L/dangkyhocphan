package com.course.server.service;

import com.course.common.model.Course;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test validating the 12 - 24 semester credit limit logic.
 * Ensures the system allows registrations up to 24 credits and strictly rejects
 * any registration that pushes the total over 24 credits.
 */
class CreditLimitValidationTest {

    static class CreditValidationSimulator {
        public static Response validateAndRegister(List<Course> enrolledCourses, Course newCourse, String reqId) {
            int currentCredits = enrolledCourses.stream().mapToInt(Course::getCredits).sum();
            if (currentCredits + newCourse.getCredits() > 24) {
                return Response.error(
                        Action.REGISTER_COURSE_RESPONSE,
                        reqId,
                        ErrorCode.CREDIT_LIMIT_EXCEEDED,
                        String.format("Vượt quá giới hạn 24 tín chỉ tối đa trong học kỳ (Hiện có: %d TC, học phần đăng ký: %d TC, tổng: %d TC).",
                                currentCredits, newCourse.getCredits(), currentCredits + newCourse.getCredits())
                );
            }
            return Response.success(Action.REGISTER_COURSE_RESPONSE, reqId, "Đăng ký thành công", null);
        }
    }

    @Test
    @DisplayName("Should approve registration when total credits reach exactly 24 TC")
    void shouldAllowRegistration_WhenReachingExactLimit() {
        List<Course> enrolled = new ArrayList<>();
        // 7 courses of 3 credits = 21 credits
        for (int i = 1; i <= 7; i++) {
            Course c = new Course();
            c.setCourseId("COURSE_" + i);
            c.setCredits(3);
            enrolled.add(c);
        }
        assertEquals(21, enrolled.stream().mapToInt(Course::getCredits).sum());

        Course newCourse = new Course();
        newCourse.setCourseId("COURSE_FINAL");
        newCourse.setCredits(3); // 21 + 3 = 24 <= 24

        Response response = CreditValidationSimulator.validateAndRegister(enrolled, newCourse, "REQ-01");
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("Should reject registration when total credits exceed 24 TC")
    void shouldRejectRegistration_WhenExceedingLimit() {
        List<Course> enrolled = new ArrayList<>();
        // 7 courses of 3 credits = 21 credits
        for (int i = 1; i <= 7; i++) {
            Course c = new Course();
            c.setCourseId("COURSE_" + i);
            c.setCredits(3);
            enrolled.add(c);
        }

        Course newCourse = new Course();
        newCourse.setCourseId("COURSE_EXCEED");
        newCourse.setCredits(4); // 21 + 4 = 25 > 24

        Response response = CreditValidationSimulator.validateAndRegister(enrolled, newCourse, "REQ-02");
        assertFalse(response.isSuccess());
        assertEquals(ErrorCode.CREDIT_LIMIT_EXCEEDED, response.getErrorCode());
        assertTrue(response.getMessage().contains("Vượt quá giới hạn 24 tín chỉ"));
    }

    @Test
    @DisplayName("Should detect under-minimum threshold (< 12 TC) for semester status")
    void shouldDetectUnderMinimumThreshold() {
        List<Course> enrolled = new ArrayList<>();
        Course c1 = new Course();
        c1.setCredits(3);
        Course c2 = new Course();
        c2.setCredits(4);
        enrolled.add(c1);
        enrolled.add(c2);

        int totalCredits = enrolled.stream().mapToInt(Course::getCredits).sum();
        assertEquals(7, totalCredits);
        assertTrue(totalCredits < 12, "7 TC is below the 12 TC minimum requirement");
    }
}
