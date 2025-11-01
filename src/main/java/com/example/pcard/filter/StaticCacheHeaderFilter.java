package com.example.pcard.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 为静态资源设置缓存头，充分利用 Cloudflare 边缘缓存
 * 优化版：添加Gzip压缩支持和更智能的缓存策略
 */
@WebFilter({"/css/*", "/js/*", "/uploads/*"})
public class StaticCacheHeaderFilter implements Filter {
    private static final long ONE_YEAR = 31536000L; // 秒
    private static final long ONE_DAY = 86400L; // 秒
    
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        String uri = req.getRequestURI();
        
        // 根据资源类型设置不同的缓存策略
        if (uri.contains("/uploads/")) {
            // 用户上传的图片，相对稳定，缓存30天
            resp.setHeader("Cache-Control", "public, max-age=" + (ONE_DAY * 30));
        } else if (uri.endsWith(".css") || uri.endsWith(".js")) {
            // CSS和JS文件，如果有版本控制可以缓存更长时间
            resp.setHeader("Cache-Control", "public, max-age=" + ONE_YEAR + ", immutable");
            
            // 为可压缩的文本资源添加Vary头
            resp.setHeader("Vary", "Accept-Encoding");
        } else {
            // 其他静态资源
            resp.setHeader("Cache-Control", "public, max-age=" + ONE_YEAR + ", immutable");
        }
        
        // 添加ETag支持以便更好的缓存验证
        // 注意：Cloudflare 会自动处理 ETag，这里只是确保设置了合适的缓存头
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
