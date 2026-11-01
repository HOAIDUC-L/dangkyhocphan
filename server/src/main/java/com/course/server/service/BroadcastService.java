package com.course.server.service;

import com.course.common.dto.CourseUpdatedPushDto;
import com.course.common.protocol.Action;
import com.course.common.protocol.Response;
import com.course.server.session.SessionManager;

import java.util.logging.Logger;

/**
 * Service responsible for broadcasting real-time events to connected clients.
 */
public class BroadcastService {
    private static final Logger LOGGER = Logger.getLogger(BroadcastService.class.getName());
    private static final BroadcastService INSTANCE = new BroadcastService();

    private final SessionManager sessionManager;

    public BroadcastService() {
        this.sessionManager = SessionManager.getInstance();
    }

    public static BroadcastService getInstance() {
        return INSTANCE;
    }

    /**
     * Broadcasts updated remaining slots of a course to ALL local clients connected to this node.
     */
    public void notifyCourseUpdatedLocal(String courseId, int remainingSlots) {
        LOGGER.info(String.format("[%s] Broadcasting COURSE_UPDATED: Course %s -> %d remaining slots",
                com.course.server.config.ServerConfig.getInstance().getNodeName(), courseId, remainingSlots));
        CourseUpdatedPushDto dto = new CourseUpdatedPushDto(courseId, remainingSlots);
        Response pushResponse = Response.push(Action.COURSE_UPDATED, dto);
        sessionManager.broadcast(pushResponse);
    }

    /**
     * Legacy / Direct local broadcast wrapper.
     */
    public void notifyCourseUpdated(String courseId, int remainingSlots) {
        notifyCourseUpdatedLocal(courseId, remainingSlots);
    }
}
