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
    <c:if test="${not empty turnstileSiteKey}">
        <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
    </c:if>
    <style>
        .input-error {
            border-color: #ef4444 !important;
        }
        .error-text {
            color: #ef4444;
            font-size: 12px;
            margin-top: 4px;
            display: none;
        }
        .error-text.show {
            display: block;
        }
    </style>
</head>
<body class="auth-page">
    <div class="auth-container">
        <div class="card-ui">
            <h1>欢迎回来</h1>
            <c:if test="${not empty errorMessage}">
                <p class="error-message">${errorMessage}</p>
            </c:if>
            <form action="login" method="post" id="loginForm" novalidate>
                <!-- 隐藏字段：保存 redirect 参数 -->
                <c:if test="${not empty redirect}">
                    <input type="hidden" name="redirect" value="${redirect}">
                </c:if>
                <div class="form-group">
                    <label for="username">用户名</label>
                    <input type="text" id="username" name="username" class="form-control" autocomplete="username" required>
                    <div class="error-text" id="usernameError"></div>
                </div>
                <div class="form-group">
                    <label for="password">密码</label>
                    <input type="password" id="password" name="password" class="form-control" autocomplete="current-password" required>
                    <div class="error-text" id="passwordError"></div>
                </div>
                <c:if test="${not empty turnstileSiteKey}">
                    <div class="form-group">
                        <div class="cf-turnstile" data-sitekey="${turnstileSiteKey}"></div>
                    </div>
                </c:if>
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
    <script>
        // 表单验证
        const loginForm = document.getElementById('loginForm');
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');

        // 验证用户名
        function validateUsername() {
            const username = usernameInput.value.trim();
            const errorElement = document.getElementById('usernameError');
            
            if (!username) {
                showError(usernameInput, errorElement, '用户名不能为空');
                return false;
            }
            
            if (username.length < 4 || username.length > 20) {
                showError(usernameInput, errorElement, '用户名长度必须在4-20位之间');
                return false;
            }
            
            if (!/^[a-zA-Z0-9_]+$/.test(username)) {
                showError(usernameInput, errorElement, '用户名格式不正确');
                return false;
            }
            
            clearError(usernameInput, errorElement);
            return true;
        }

        // 验证密码
        function validatePassword() {
            const password = passwordInput.value;
            const errorElement = document.getElementById('passwordError');
            
            if (!password) {
                showError(passwordInput, errorElement, '密码不能为空');
                return false;
            }
            
            clearError(passwordInput, errorElement);
            return true;
        }

        // 显示错误
        function showError(inputElement, errorElement, message) {
            inputElement.classList.add('input-error');
            errorElement.textContent = message;
            errorElement.classList.add('show');
        }

        // 清除错误
        function clearError(inputElement, errorElement) {
            inputElement.classList.remove('input-error');
            errorElement.classList.remove('show');
        }

        // 实时验证（失去焦点时）
        usernameInput.addEventListener('blur', validateUsername);
        passwordInput.addEventListener('blur', validatePassword);

        // 输入时清除错误提示
        usernameInput.addEventListener('input', function() {
            if (usernameInput.classList.contains('input-error')) {
                clearError(usernameInput, document.getElementById('usernameError'));
            }
        });

        passwordInput.addEventListener('input', function() {
            if (passwordInput.classList.contains('input-error')) {
                clearError(passwordInput, document.getElementById('passwordError'));
            }
        });

        // 表单提交验证
        loginForm.addEventListener('submit', function(e) {
            const isUsernameValid = validateUsername();
            const isPasswordValid = validatePassword();
            
            if (!isUsernameValid || !isPasswordValid) {
                e.preventDefault();
                return false;
            }
        });
    </script>
</body>
</html>