package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

/**
 * Exception thrown when student attempts to register beyond the 24-credit semester limit.
 */
public class CreditLimitExceededException extends CourseRegistrationException {
    public CreditLimitExceededException(String message) {
        super(ErrorCode.CREDIT_LIMIT_EXCEEDED, message);
    }
}
