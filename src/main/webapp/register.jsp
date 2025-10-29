<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>注册 - P-Card 平台</title>
    <link rel="stylesheet" href="css/style.css">
    <c:if test="${not empty turnstileSiteKey}">
        <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
    </c:if>
</head>
<body class="auth-page">
    <div class="auth-container">
        <div class="card-ui">
            <h1>创建新账户</h1>
            <c:if test="${not empty errorMessage}">
                <p class="error-message">${errorMessage}</p>
            </c:if>
            <form action="register" method="post">
                <div class="form-group">
                    <label for="username">用户名</label>
                    <input type="text" id="username" name="username" class="form-control" autocomplete="username" required>
                    <small style="color: var(--text-secondary); font-size: 12px;">用户名用于登录</small>
                </div>
                <div class="form-group">
                    <label for="nickname">昵称</label>
                    <input type="text" id="nickname" name="nickname" class="form-control" autocomplete="nickname" required>
                    <small style="color: var(--text-secondary); font-size: 12px;">昵称将显示在留言板和名片信息中</small>
                </div>
                <div class="form-group">
                    <label for="password">密码</label>
                    <input type="password" id="password" name="password" class="form-control" autocomplete="new-password" required>
                </div>
                <div class="form-group">
                    <label for="confirmPassword">确认密码</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" autocomplete="new-password" required>
                </div>
                <div class="form-group">
                    <label style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                        <input type="checkbox" id="agreePolicy" name="agreePolicy" required>
                        <span>我已阅读并同意 <a href="${pageContext.request.contextPath}/privacy.jsp" target="_blank" rel="noopener noreferrer">隐私政策</a></span>
                    </label>
                </div>
                <c:if test="${not empty turnstileSiteKey}">
                    <div class="form-group">
                        <div class="cf-turnstile" data-sitekey="${turnstileSiteKey}"></div>
                    </div>
                </c:if>
                <button type="submit" class="btn btn-primary btn-block">注册</button>
            </form>
             <p style="text-align: center; margin-top: 24px; color: var(--text-secondary);">
                已有账户? <a href="login" style="font-weight: 600;">返回登录</a>
            </p>
            <p style="text-align: center; margin-top: 16px;">
                <a href="${pageContext.request.contextPath}/" style="color: var(--text-secondary); font-size: 14px;">← 返回首页</a>
            </p>
        </div>
    </div>
</body>
</html>