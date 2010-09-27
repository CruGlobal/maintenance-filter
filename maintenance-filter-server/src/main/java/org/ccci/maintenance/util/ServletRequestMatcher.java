package org.ccci.maintenance.util;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * A tool to be used mostly by servlet filters that need to either ignore or include certain paths or types of requests.
 * 
 * @author Matt Drees
 */
//TODO: copy/pasted from Util project
public class ServletRequestMatcher
{
    private final Pattern urlPattern;
    private final boolean matchNonHttpRequests;

    private ServletRequestMatcher(Pattern urlPattern, boolean matchNonHttpRequests)
    {
        this.urlPattern = urlPattern;
        this.matchNonHttpRequests = matchNonHttpRequests;
    }

    public boolean matches(ServletRequest request)
    {
        Preconditions.checkNotNull(request, "request is null");
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String fullPath = HttpRequests.getFullPath(httpRequest);
            if (urlPattern.matcher(fullPath).matches())
                return true;
        }
        else
        {
            if (matchNonHttpRequests)
                return true;
        }
        return false;
    }
    
    public static class Builder
    {
        private boolean matchNonHttpRequests;
        private Iterable<String> urlPatterns = new ArrayList<String>();

        /**
         * by default, no paths are matched
         */
        public Builder matchUrlPatterns(Iterable<String> urlPatterns)
        {
            this.urlPatterns = Preconditions.checkNotNull(urlPatterns, "urlPatterns is null");
            return this;
        }
        
        /**
         * By default, don't match non-http requests 
         */
        public Builder matchNonHttpRequests()
        {
            matchNonHttpRequests = true;
            return this;
        }
        
        /** Create a the configured {@link ServletRequestMatcher} */
        public ServletRequestMatcher build()
        {
            String combinedPattern = joinPatterns(urlPatterns);
            return new ServletRequestMatcher(Pattern.compile(combinedPattern), matchNonHttpRequests);
        }

        private String joinPatterns(Iterable<String> urlPatterns)
        {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String pattern : urlPatterns)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    builder.append("|");
                }
                builder.append(pattern);
            }
            return builder.toString();
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    
}
