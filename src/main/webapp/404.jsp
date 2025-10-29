<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>404 未找到</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="error-page">
<div class="container error-container">
    <h1 class="error-code">404</h1>
    <h2>页面不存在</h2>
    <p>抱歉，我们无法找到您请求的页面。请检查链接是否正确或返回首页继续浏览。</p>
    <div class="error-actions">
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/">返回首页</a>
    </div>
</div>
</body>
</html>
