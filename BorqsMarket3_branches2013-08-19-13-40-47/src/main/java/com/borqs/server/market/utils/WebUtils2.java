package com.borqs.server.market.utils;


import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebUtils2 {

    public static String getCookie(HttpServletRequest req, String name) {
        return getCookie(req, name, null);
    }

    public static String getCookie(HttpServletRequest req, String name, String def) {
        Cookie cookie = WebUtils.getCookie(req, name);
        return cookie != null ? cookie.getValue() : def;
    }

    public static void setCookie(HttpServletResponse resp, String name, String val) {
        if (val != null) {
            addCookie(resp, name, val);
        } else {
            deleteCookie(resp, name);
        }
    }

    public static void addCookie(HttpServletResponse resp, String name, String val) {
        Cookie cookie = new Cookie(name, val);
        cookie.setMaxAge(3600 * 24 * 60); // 60 days
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletResponse resp, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

    public static String getCookieWithDelete(HttpServletRequest req, HttpServletResponse resp, String name, String def) {
        String val = getCookie(req, name);
        deleteCookie(resp, name);
        return val != null ? val : def;
    }

    public static boolean isPostBack(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod());
    }
}
