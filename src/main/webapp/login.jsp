<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>登录 - P-Card 平台</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body class="auth-page">
    <div class="auth-container">
        <div class="card-ui">
            <h1>欢迎回来</h1>
            <c:if test="${not empty errorMessage}">
                <p class="error-message">${errorMessage}</p>
            </c:if>
            <form action="login" method="post">
                <!-- 隐藏字段：保存 redirect 参数 -->
                <c:if test="${not empty redirect}">
                    <input type="hidden" name="redirect" value="${redirect}">
                </c:if>
                <div class="form-group">
                    <label for="username">用户名</label>
                    <input type="text" id="username" name="username" class="form-control" autocomplete="username" required>
                </div>
                <div class="form-group">
                    <label for="password">密码</label>
                    <input type="password" id="password" name="password" class="form-control" autocomplete="current-password" required>
                </div>
                <button type="submit" class="btn btn-primary btn-block">登录</button>
            </form>
            <p style="text-align: center; margin-top: 24px; color: var(--text-secondary);">
                还没有账户? <a href="register" style="font-weight: 600;">立即注册</a>
            </p>
            <p style="text-align: center; margin-top: 16px;">
                <a href="${pageContext.request.contextPath}/" style="color: var(--text-secondary); font-size: 14px;">← 返回首页</a>
            </p>
        </div>
    </div>
</body>
</html>