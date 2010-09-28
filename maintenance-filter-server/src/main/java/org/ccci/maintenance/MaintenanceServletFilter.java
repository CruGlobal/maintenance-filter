package org.ccci.maintenance;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ccci.maintenance.util.ServletRequestMatcher;

public class MaintenanceServletFilter implements Filter
{
    
    private final MaintenancePageRenderer renderer = new MaintenancePageRenderer();

    private ServletRequestMatcher ignoredRequestsMatcher;
    
    private MaintenanceService maintenanceService;
    
    public void destroy()
    {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException
    {
        MaintenanceWindow window = maintenanceService.getCurrentWindow();
        if (window == null)
        {
            chain.doFilter(request, response);
        }
        else if(shouldBypass(request))
        {
            if (isBypassRequestParameterPresent(request))
            {
                setSessionBypass(request);
            }
            chain.doFilter(request, response);
        }
        else
        {
            renderAppropriateMaintenancePage(request, response, window);
        }
    }

    private void renderAppropriateMaintenancePage(ServletRequest request, ServletResponse response,
                                                  MaintenanceWindow window)
    {
        if (isHtmlRequest(request))
        {
            renderer.renderMaintenancePage((HttpServletResponse) response, window);
        }
        else if (isOtherHttpRequest(request))
        {
            renderer.sendHttpUnavailable((HttpServletResponse) response, window);
        }
        else
        /* I'm not sure we deal with non-http requests.  But in this case I guess we can just send a straight text message.
         */
        {
            renderer.sendSimpleTextMaintenanceMessage(response, window);
        }
    }

    private boolean isHtmlRequest(ServletRequest request)
    {
        if (!(request instanceof HttpServletRequest))
            return false;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        //this is pretty rough, but I think it'll work ok
        String acceptHeader = httpRequest.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("html"))
            return true;
        else
            return false;
    }

    private boolean isOtherHttpRequest(ServletRequest request)
    {
        return request instanceof HttpServletRequest;
    }

    private boolean shouldBypass(ServletRequest request)
    {
        if (isBypassRequestParameterPresent(request))
            return true;;
        
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession(false);
            if (session != null)
            {
                Boolean sessionBypassFilterFlag = (Boolean) session.getAttribute(getSessionLocation());
                if (sessionBypassFilterFlag != null)
                    return true;
            }
        }
        
        if (ignoredRequestsMatcher.matches(request))
            return true;
        
        return false;
    }

    private boolean isBypassRequestParameterPresent(ServletRequest request)
    {
        String bypassMaintenanceFilter = request.getParameter(WindowControlApi.BYPASS_REQUEST_PARAMETER);
        if (bypassMaintenanceFilter != null && bypassMaintenanceFilter.equals("true"))
            return true;
        return false;
    }

    private String getSessionLocation()
    {
        return getClass().getName() + ".bypassMaintenanceFilter";
    }
    
    private void setSessionBypass(ServletRequest request)
    {
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();
            if (session != null)
            {
                session.setAttribute(getSessionLocation(), true);
            }
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException
    {
        ServletContext servletContext = filterConfig.getServletContext();
        Bootstrap bootstrap = Bootstrap.getInstance(servletContext);
        maintenanceService = bootstrap.getMaintenanceService();
        ignoredRequestsMatcher = buildIgnoredRequestsMatcher(filterConfig);
    }

    private ServletRequestMatcher buildIgnoredRequestsMatcher(FilterConfig filterConfig)
    {
        String ignorePaths = filterConfig.getInitParameter("bypassUrlPatterns");
        ServletRequestMatcher.Builder builder = ServletRequestMatcher.builder();
        if (ignorePaths != null)
        {
            String[] paths = ignorePaths.split("\\s*,\\s*");
            builder.matchUrlPatterns(Arrays.asList(paths));
        }
        return builder.build();
    }

}
