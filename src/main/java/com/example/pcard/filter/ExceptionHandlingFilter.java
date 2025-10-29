package com.example.pcard.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 全局异常处理过滤器
 * 捕获并记录所有未处理的异常
 */
@WebFilter("/*")
public class ExceptionHandlingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("ExceptionHandlingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("请求处理异常 - URI: {}, Method: {}", 
                httpRequest.getRequestURI(), 
                httpRequest.getMethod(), 
                e);
            
            // 返回友好的错误页面
            if (!httpResponse.isCommitted()) {
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "服务器内部错误，请稍后重试");
            }
        }
    }

    @Override
    public void destroy() {
        logger.info("ExceptionHandlingFilter destroyed");
    }
}
