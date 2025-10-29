package com.example.pcard.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 为静态资源设置缓存头，充分利用 Cloudflare 边缘缓存
 */
@WebFilter({"/css/*", "/js/*", "/uploads/*"})
public class StaticCacheHeaderFilter implements Filter {
    private static final long ONE_YEAR = 31536000L; // 秒

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse resp = (HttpServletResponse) response;

        // 针对指纹化/版本化的静态资源可设置为 immutable + 长缓存
        // 这里统一设置较长的 Cache-Control，若有需要可在具体 Servlet 覆盖
        resp.setHeader("Cache-Control", "public, max-age=" + ONE_YEAR + ", immutable");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
