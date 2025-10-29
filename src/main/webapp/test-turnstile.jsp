<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.pcard.util.TurnstileVerifier" %>
<%@ page import="com.example.pcard.util.TurnstileGate" %>
<%
    // 强制触发 Turnstile（用于测试）
    TurnstileGate.requireForDuration(request, 10 * 60 * 1000L); // 10分钟
    String siteKey = TurnstileVerifier.getSiteKey();
    boolean enabled = TurnstileVerifier.isEnabled();
    boolean required = TurnstileGate.isRequired(request);
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Turnstile 测试页面</title>
    <link rel="stylesheet" href="css/style.css">
    <% if (enabled && siteKey != null && !siteKey.isEmpty()) { %>
        <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
    <% } %>
</head>
<body class="auth-page">
    <div class="auth-container">
        <div class="card-ui">
            <h1>🧪 Turnstile 测试页面</h1>
            
            <div style="background: #f5f5f5; padding: 20px; border-radius: 8px; margin: 20px 0;">
                <h3>配置状态：</h3>
                <ul style="list-style: none; padding: 0;">
                    <li>✅ Turnstile 是否启用: <strong><%= enabled ? "是" : "否" %></strong></li>
                    <li>✅ Site Key 是否配置: <strong><%= (siteKey != null && !siteKey.isEmpty()) ? "是" : "否" %></strong></li>
                    <li>✅ 是否需要验证: <strong><%= required ? "是" : "否" %></strong></li>
                    <% if (siteKey != null && !siteKey.isEmpty()) { %>
                        <li>🔑 Site Key: <code><%= siteKey %></code></li>
                    <% } %>
                    <li>🆔 Session ID: <code><%= request.getSession().getId() %></code></li>
                </ul>
            </div>

            <% if (!enabled) { %>
                <div style="background: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107;">
                    <strong>⚠️ Turnstile 未启用</strong>
                    <p>请确保设置了以下环境变量：</p>
                    <ul>
                        <li><code>CF_TURNSTILE_SECRET</code></li>
                        <li><code>CF_TURNSTILE_SITE_KEY</code></li>
                    </ul>
                </div>
            <% } else if (siteKey == null || siteKey.isEmpty()) { %>
                <div style="background: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107;">
                    <strong>⚠️ Site Key 未配置</strong>
                    <p>请检查 <code>CF_TURNSTILE_SITE_KEY</code> 环境变量</p>
                </div>
            <% } else { %>
                <div style="background: #d4edda; padding: 15px; border-radius: 8px; border-left: 4px solid #28a745; margin: 20px 0;">
                    <strong>✅ Turnstile 已正确配置</strong>
                    <p>下面应该显示验证框：</p>
                </div>
                
                <form action="#" method="post" style="margin-top: 20px;">
                    <div class="form-group">
                        <div class="cf-turnstile" data-sitekey="<%= siteKey %>"></div>
                    </div>
                    <button type="button" onclick="testValidation()" class="btn btn-primary btn-block">测试验证</button>
                </form>
            <% } %>

            <p style="text-align: center; margin-top: 24px;">
                <a href="${pageContext.request.contextPath}/" style="color: var(--text-secondary);">← 返回首页</a>
            </p>
        </div>
    </div>

    <script>
        function testValidation() {
            const token = document.querySelector('[name="cf-turnstile-response"]');
            if (token && token.value) {
                alert('✅ Turnstile Token 已获取:\n' + token.value.substring(0, 50) + '...');
            } else {
                alert('❌ 请先完成 Turnstile 验证');
            }
        }

        // 调试：监听错误
        window.addEventListener('error', function(e) {
            console.error('页面错误:', e);
        });

        // 调试：检查 Turnstile 是否加载
        window.addEventListener('load', function() {
            setTimeout(function() {
                const turnstileWidget = document.querySelector('.cf-turnstile');
                const turnstileFrame = document.querySelector('iframe[src*="cloudflare"]');
                console.log('Turnstile Widget:', turnstileWidget);
                console.log('Turnstile iFrame:', turnstileFrame);
                console.log('window.turnstile:', window.turnstile);
                
                if (!turnstileFrame && turnstileWidget) {
                    console.warn('⚠️ Turnstile 小部件存在但 iframe 未加载！可能是 CSP 或脚本加载问题。');
                }
            }, 2000);
        });
    </script>
</body>
</html>
