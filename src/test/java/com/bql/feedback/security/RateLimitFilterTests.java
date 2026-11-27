package com.bql.feedback.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RateLimitFilterTests {

    private int call(RateLimitFilter f, String method, String uri, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void loginIsBlockedAfterLimitWithRetryAfter() throws Exception {
        RateLimitFilter f = new RateLimitFilter(3, 20, false);
        for (int i = 0; i < 3; i++) assertEquals(200, call(f, "POST", "/api/auth/login", "1.1.1.1"));

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        assertEquals(429, res.getStatus());
        assertNotNull(res.getHeader("Retry-After"));
    }

    @Test
    void otherIpAndGetRequestsAreNotAffected() throws Exception {
        RateLimitFilter f = new RateLimitFilter(1, 1, false);
        assertEquals(200, call(f, "POST", "/api/auth/login", "1.1.1.1"));
        assertEquals(429, call(f, "POST", "/api/auth/login", "1.1.1.1"));
        assertEquals(200, call(f, "POST", "/api/auth/login", "2.2.2.2"));
        assertEquals(200, call(f, "GET", "/api/feedbacks", "1.1.1.1"));
    }

    @Test
    void feedbackSubmitAndAttachmentsShareFeedbackGroup() throws Exception {
        RateLimitFilter f = new RateLimitFilter(10, 1, false);
        assertEquals(200, call(f, "POST", "/api/feedbacks", "3.3.3.3"));
        assertEquals(429, call(f, "POST", "/api/feedbacks/5/attachments", "3.3.3.3"));
    }
}