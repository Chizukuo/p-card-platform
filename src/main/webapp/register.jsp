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
            <h1>创建新账户</h1>
            <c:if test="${not empty errorMessage}">
                <p class="error-message">${errorMessage}</p>
            </c:if>
            <form action="register" method="post" id="registerForm" novalidate>
                <div class="form-group">
                    <label for="username">用户名</label>
                    <input type="text" id="username" name="username" class="form-control" autocomplete="username" required>
                    <small style="color: var(--text-secondary); font-size: 12px;">4-20位字符，只能包含字母、数字和下划线</small>
                    <div class="error-text" id="usernameError"></div>
                </div>
                <div class="form-group">
                    <label for="nickname">昵称</label>
                    <input type="text" id="nickname" name="nickname" class="form-control" autocomplete="nickname" maxlength="50" required>
                    <small style="color: var(--text-secondary); font-size: 12px;">昵称将显示在留言板和名片信息中</small>
                    <div class="error-text" id="nicknameError"></div>
                </div>
                <div class="form-group">
                    <label for="password">密码</label>
                    <input type="password" id="password" name="password" class="form-control" autocomplete="new-password" required>
                    <small style="color: var(--text-secondary); font-size: 12px;">至少8位字符，必须包含字母和数字</small>
                    <div class="error-text" id="passwordError"></div>
                </div>
                <div class="form-group">
                    <label for="confirmPassword">确认密码</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" autocomplete="new-password" required>
                    <div class="error-text" id="confirmPasswordError"></div>
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
    <script>
        // 表单验证
        const registerForm = document.getElementById('registerForm');
        const usernameInput = document.getElementById('username');
        const nicknameInput = document.getElementById('nickname');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');
        const agreePolicyInput = document.getElementById('agreePolicy');

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
                showError(usernameInput, errorElement, '用户名只能包含字母、数字和下划线');
                return false;
            }
            
            clearError(usernameInput, errorElement);
            return true;
        }

        // 验证昵称
        function validateNickname() {
            const nickname = nicknameInput.value.trim();
            const errorElement = document.getElementById('nicknameError');
            
            if (!nickname) {
                showError(nicknameInput, errorElement, '昵称不能为空');
                return false;
            }
            
            if (nickname.length > 50) {
                showError(nicknameInput, errorElement, '昵称长度不能超过50位');
                return false;
            }
            
            clearError(nicknameInput, errorElement);
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
            
            if (password.length < 8) {
                showError(passwordInput, errorElement, '密码长度至少8位');
                return false;
            }
            
            if (!/[a-zA-Z]/.test(password)) {
                showError(passwordInput, errorElement, '密码必须包含字母');
                return false;
            }
            
            if (!/[0-9]/.test(password)) {
                showError(passwordInput, errorElement, '密码必须包含数字');
                return false;
            }
            
            clearError(passwordInput, errorElement);
            return true;
        }

        // 验证确认密码
        function validateConfirmPassword() {
            const password = passwordInput.value;
            const confirmPassword = confirmPasswordInput.value;
            const errorElement = document.getElementById('confirmPasswordError');
            
            if (!confirmPassword) {
                showError(confirmPasswordInput, errorElement, '请确认密码');
                return false;
            }
            
            if (password !== confirmPassword) {
                showError(confirmPasswordInput, errorElement, '两次输入的密码不一致');
                return false;
            }
            
            clearError(confirmPasswordInput, errorElement);
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

        // 实时验证
        usernameInput.addEventListener('blur', validateUsername);
        nicknameInput.addEventListener('blur', validateNickname);
        passwordInput.addEventListener('blur', validatePassword);
        confirmPasswordInput.addEventListener('blur', validateConfirmPassword);

        // 密码输入时也验证确认密码（如果已填写）
        passwordInput.addEventListener('input', function() {
            if (confirmPasswordInput.value) {
                validateConfirmPassword();
            }
        });

        // 表单提交验证
        registerForm.addEventListener('submit', function(e) {
            const isUsernameValid = validateUsername();
            const isNicknameValid = validateNickname();
            const isPasswordValid = validatePassword();
            const isConfirmPasswordValid = validateConfirmPassword();
            
            if (!agreePolicyInput.checked) {
                alert('请先阅读并同意隐私政策');
                e.preventDefault();
                return false;
            }
            
            if (!isUsernameValid || !isNicknameValid || !isPasswordValid || !isConfirmPasswordValid) {
                e.preventDefault();
                return false;
            }
        });
    </script>
</body>
</html>