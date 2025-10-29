<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${empty sessionScope.user}"><c:redirect url="login" /></c:if>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>管理后台 - P-Card 平台</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="css/style.css">
    <style>
        .page-header-title {
            color: var(--text-color);
            margin: 0;
            font-size: 32px;
            font-weight: 700;
        }
    </style>
</head>
<body>

<div class="container">
    <header class="header">
        <h1 class="page-header-title">欢迎, <c:out value="${sessionScope.user.username}" />! 👋</h1>
        <nav class="header-nav">
            <a href="${pageContext.request.contextPath}/">返回首页</a>
            <c:if test="${sessionScope.user.isAdmin()}"><a href="admin">管理面板</a></c:if>
            <a href="logout">退出登录</a>
        </nav>
        <!-- 移动端菜单按钮 -->
        <button id="mobile-menu-btn" class="mobile-menu-btn" aria-label="打开菜单">☰</button>
    </header>
    
    <!-- 移动端侧栏 -->
    <div id="sidebar-overlay" class="sidebar-overlay"></div>
    <aside id="sidebar" class="sidebar">
        <div class="sidebar-header">
            <h2>菜单</h2>
            <button id="sidebar-close" class="sidebar-close" aria-label="关闭菜单">&times;</button>
        </div>
        <nav class="sidebar-nav">
            <a href="${pageContext.request.contextPath}/">返回首页</a>
            <a href="dashboard" class="active">我的后台</a>
            <c:if test="${sessionScope.user.isAdmin()}">
                <a href="admin">管理面板</a>
            </c:if>
            <a href="logout">退出登录</a>
        </nav>
    </aside>

    <%
        // 计算对外可见的 baseUrl，尽量识别 HTTPS（短链需展示 https）
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String scheme = null;
        if (forwardedProto != null && !forwardedProto.isEmpty()) {
            int comma = forwardedProto.indexOf(',');
            scheme = (comma > -1 ? forwardedProto.substring(0, comma) : forwardedProto).trim();
        }
        if (scheme == null || scheme.isEmpty()) {
            String cfVisitor = request.getHeader("CF-Visitor");
            if (cfVisitor != null) {
                if (cfVisitor.contains("\"scheme\":\"https\"")) scheme = "https";
                else if (cfVisitor.contains("\"scheme\":\"http\"")) scheme = "http";
            }
        }
        if (scheme == null || scheme.isEmpty()) {
            String xUrlScheme = request.getHeader("X-Url-Scheme");
            if (xUrlScheme != null && !xUrlScheme.isEmpty()) scheme = xUrlScheme;
        }
        if (scheme == null || scheme.isEmpty()) {
            String xfPort = request.getHeader("X-Forwarded-Port");
            if ("443".equals(xfPort)) scheme = "https";
            else if ("80".equals(xfPort)) scheme = "http";
        }
        if (scheme == null || scheme.isEmpty()) {
            scheme = request.isSecure() ? "https" : request.getScheme();
        }

        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String hostHeader = request.getHeader("Host");
        String host;
        if (forwardedHost != null && !forwardedHost.isEmpty()) {
            int comma = forwardedHost.indexOf(',');
            host = (comma > -1 ? forwardedHost.substring(0, comma) : forwardedHost).trim();
        } else if (hostHeader != null && !hostHeader.isEmpty()) {
            host = hostHeader; // Host 可能已包含端口
        } else {
            host = request.getServerName();
            int p = request.getServerPort();
            if (p != 80 && p != 443) host += ":" + p;
        }
        String baseUrl = scheme + "://" + host;
        request.setAttribute("baseUrl", baseUrl);
    %>

    <div class="card-ui">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;">
            <h2 style="margin:0;">我的名片</h2>
            <div>
                <a href="dashboard?create=1" class="btn btn-primary" style="white-space:nowrap;text-shadow: 0 1px 2px rgba(0,0,0,0.2);">+ 创建新名片</a>
            </div>
        </div>
        <c:if test="${not empty myCards}">
            <div class="my-cards-list">
                <c:forEach var="citem" items="${myCards}">
                    <div class="my-card-item small">
                        <%-- 判断是否为绝对 URL（GCS 路径），如果是则直接使用，否则添加 contextPath --%>
                        <c:set var="imagePath" value="${citem.cardFrontPath != null ? citem.cardFrontPath : 'https://placehold.co/200x120/0071e3/ffffff?text=P-CARD'}" />
                        <c:set var="imageUrl" value="${imagePath.startsWith('http://') || imagePath.startsWith('https://') ? imagePath : pageContext.request.contextPath.concat('/').concat(imagePath)}" />
                        <img src="${imageUrl}" alt="${citem.producerName}"/>
                        <div class="my-card-meta">
                            <strong><c:out value="${citem.producerName}"/></strong>
                            <div style="margin-top:6px;">
                                <a href="dashboard?cardId=${citem.id}" class="btn btn-secondary btn-sm">编辑</a>
                                <a href="card/${citem.uniqueLinkId}" target="_blank" class="btn btn-sm" style="margin-left:6px;">查看</a>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <c:if test="${empty myCards}">
            <p>您还没有名片，点击“创建新名片”开始创建。</p>
        </c:if>

        <c:if test="${createMode}">
            <h3>创建新的电子名片</h3>
        </c:if>
        <c:if test="${not empty myCard}">
            <h3>编辑所选名片</h3>
        </c:if>
    <c:if test="${createMode or not empty myCard}">
    <form action="cardAction" method="post" enctype="multipart/form-data">
            <c:choose>
                <c:when test="${empty myCard}"><input type="hidden" name="action" value="create"></c:when>
                <c:otherwise>
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="cardId" value="${myCard.id}">
                </c:otherwise>
            </c:choose>

            <div class="form-group"><label for="producerName">制作人昵称 (P名)</label><input type="text" id="producerName" name="producerName" value="${myCard.producerName}" class="form-control" autocomplete="nickname" required></div>
            <div class="form-group"><label for="region">所在地区</label><input type="text" id="region" name="region" value="${myCard.region}" class="form-control" autocomplete="address-level2"></div>
            <div class="form-group"><label for="idolName">担当偶像</label><input type="text" id="idolName" name="idolName" value="${myCard.idolName}" class="form-control" autocomplete="off"></div>
            <div class="form-group"><label for="cardFront">名片正面 (不更改则无需上传)</label><input type="file" id="cardFront" name="cardFront" class="form-control" accept="image/*"></div>
            <div class="form-group"><label for="cardBack">名片背面 (不更改则无需上传)</label><input type="file" id="cardBack" name="cardBack" class="form-control" accept="image/*"></div>

            <h3>社交平台 / 个人链接</h3>
            <p class="form-text">请填写平台名称和对应的主页链接或号码。系统将根据平台名称自动匹配图标。</p>
            <div class="form-group">
                <label for="visibility">可见性</label>
                <select id="visibility" name="visibility" class="form-control">
                    <option value="PUBLIC" ${empty myCard or myCard.visibility eq 'PUBLIC' ? 'selected' : ''}>公开 (任何人可见)</option>
                    <option value="LINK_ONLY" ${not empty myCard and myCard.visibility eq 'LINK_ONLY' ? 'selected' : ''}>仅通过分享链接访问</option>
                    <option value="PRIVATE" ${not empty myCard and myCard.visibility eq 'PRIVATE' ? 'selected' : ''}>私密 (仅自己或管理员可见)</option>
                </select>
            </div>
            <div id="custom-sns-container">
                 <c:if test="${not empty myCard}">
                     <c:forEach var="link" items="${myCard.getSnsLinks()}">
                         <div class="custom-sns-item">
                             <input type="text" name="customSnsName" placeholder="平台 (e.g. 微博, Twitter, QQ)" value="${link.name}" class="form-control" autocomplete="off">
                             <input type="text" name="customSnsValue" placeholder="主页链接或号码" value="${link.value}" class="form-control" autocomplete="off">
                             <button type="button" class="btn btn-danger btn-sm" onclick="removeCustomSns(this)">移除</button>
                         </div>
                     </c:forEach>
                 </c:if>
            </div>
            <button type="button" class="btn btn-secondary" onclick="addCustomSns()">+ 添加一个</button>

            <br><br>
            <button type="submit" class="btn btn-primary btn-block">
                <c:if test="${empty myCard}">创建名片</c:if>
                <c:if test="${not empty myCard}">保存更改</c:if>
            </button>
        </form>
        </c:if>
    </div>

    <c:if test="${not empty myCard}">
        <div class="card-ui">
            <h2>分享当前名片</h2>
            <div class="form-group">
                <label>当前名片链接</label>
                <div class="link-section">
                    <c:choose>
                        <c:when test="${not empty myCard.shortCode}">
                            <div class="link-box">${baseUrl}${pageContext.request.contextPath}/s/${myCard.shortCode}</div>
                        </c:when>
                        <c:when test="${myCard.visibility eq 'LINK_ONLY'}">
                            <div class="link-box">${baseUrl}${pageContext.request.contextPath}/card/${myCard.uniqueLinkId}?token=${myCard.shareToken}</div>
                        </c:when>
                        <c:otherwise>
                            <div class="link-box">${baseUrl}${pageContext.request.contextPath}/card/${myCard.uniqueLinkId}</div>
                        </c:otherwise>
                    </c:choose>
                    <button type="button" class="btn btn-secondary btn-copy">复制</button>
                </div>
            </div>
        </div>
    </c:if>

    <!-- 修改密码 -->
    <div class="card-ui">
        <h2>修改密码</h2>
        <c:if test="${not empty sessionScope.passwordError}">
            <p class="error-message">${sessionScope.passwordError}</p>
            <c:remove var="passwordError" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.passwordSuccess}">
            <p class="success-message">${sessionScope.passwordSuccess}</p>
            <c:remove var="passwordSuccess" scope="session"/>
        </c:if>
        <form action="userAction" method="post">
            <input type="hidden" name="action" value="changePassword">
            <div class="form-group"><label for="oldPassword">旧密码</label><input type="password" id="oldPassword" name="oldPassword" class="form-control" autocomplete="current-password" required></div>
            <div class="form-group"><label for="newPassword">新密码</label><input type="password" id="newPassword" name="newPassword" class="form-control" autocomplete="new-password" required></div>
            <div class="form-group"><label for="confirmPassword">确认新密码</label><input type="password" id="confirmPassword" name="confirmPassword" class="form-control" autocomplete="new-password" required></div>
            <button type="submit" class="btn btn-primary">确认修改</button>
        </form>
    </div>

    <c:if test="${not empty myCard}">
        <div class="card-ui">
            <h2>危险区域</h2>
            <form action="cardAction" method="post" onsubmit="return confirm('您确定要删除您的名片吗？此操作不可恢复。');">
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="cardId" value="${myCard.id}">
                <button type="submit" class="btn btn-danger">删除我的名片</button>
            </form>
        </div>
    </c:if>
</div>
<script src="js/script.js"></script>
<script>
    // 当创建新名片时，自动填充昵称到制作人昵称字段
    document.addEventListener('DOMContentLoaded', function() {
        var producerNameInput = document.getElementById('producerName');
        var isCreateMode = <c:out value="${createMode}" default="false"/>;
        
        // 如果是创建模式且制作人昵称字段为空，自动填充用户昵称
        if (isCreateMode && producerNameInput && !producerNameInput.value) {
            var userNickname = '<c:out value="${sessionScope.user.nickname}"/>';
            if (userNickname) {
                producerNameInput.value = userNickname;
            }
        }
    });
</script>
</body>
</html>