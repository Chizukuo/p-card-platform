<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>403 权限不足</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="error-page">
<div class="container error-container">
    <h1 class="error-code">403</h1>
    <h2>权限不足</h2>
    <p>对不起，您没有权限访问此页面。如果您认为这是错误，请联系管理员或返回首页。</p>
    <div class="error-actions">
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/">返回首页</a>
    </div>
</div>
</body>
</html>
