package org.ccci.maintenance.util;

import javax.servlet.http.HttpServletRequest;

//TODO: copy/pasted from util project
public class HttpRequests
{

    public static String getFullPath(HttpServletRequest httpRequest)
    {
        String pathInfo = httpRequest.getPathInfo();
        pathInfo = pathInfo == null ? "" : pathInfo;
        String servletPath = httpRequest.getServletPath();
        servletPath = servletPath == null ? "" : servletPath;
        String fullPath = servletPath + pathInfo;
        return fullPath;
    }
}
