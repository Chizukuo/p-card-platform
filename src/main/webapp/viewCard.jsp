<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%
    // 获取卡片信息用于 OG 标签
    com.example.pcard.model.Card cardForMeta = (com.example.pcard.model.Card) request.getAttribute("card");

    // 基于代理/平台请求头推断对外可见的 base URL，避免暴露内部端口
    String forwardedProto = request.getHeader("X-Forwarded-Proto");
    String scheme = (forwardedProto != null && !forwardedProto.isEmpty()) ? forwardedProto : request.getScheme();
    String forwardedHost = request.getHeader("X-Forwarded-Host");
    String hostHeader = request.getHeader("Host");
    String host;
    if (forwardedHost != null && !forwardedHost.isEmpty()) {
        host = forwardedHost;
    } else if (hostHeader != null && !hostHeader.isEmpty()) {
        host = hostHeader; // Host 头已包含端口（若为非常规端口）
    } else {
        host = request.getServerName();
        int p = request.getServerPort();
        if (p != 80 && p != 443) host += ":" + p;
    }
    String baseUrl = scheme + "://" + host;

    String ogImageUrl = "";
    if (cardForMeta != null && cardForMeta.getCardFrontPath() != null) {
        String frontPath = cardForMeta.getCardFrontPath().trim();
        if (frontPath.startsWith("http://") || frontPath.startsWith("https://")) {
            ogImageUrl = frontPath;
        } else {
            ogImageUrl = baseUrl + request.getContextPath() + "/" + frontPath;
        }
    }
    String ogTitle = cardForMeta != null ? cardForMeta.getProducerName() + " 的P-Card" : "P-Card Platform";
    String ogDescription = cardForMeta != null 
        ? String.format("担当: %s | 地区: %s", 
            cardForMeta.getIdolName() != null ? cardForMeta.getIdolName() : "未知", 
            cardForMeta.getRegion() != null ? cardForMeta.getRegion() : "未知")
        : "查看电子名片";
    String query = request.getQueryString();
    String ogUrl = baseUrl + request.getRequestURI() + (query != null && !query.isEmpty() ? ("?" + query) : "");
%>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title><%= ogTitle %></title>
    
    <!-- Open Graph / Facebook / iMessage 预览 -->
    <meta property="og:type" content="website">
    <meta property="og:url" content="<%= ogUrl %>">
    <meta property="og:title" content="<%= ogTitle %>">
    <meta property="og:description" content="<%= ogDescription %>">
    <% if (!ogImageUrl.isEmpty()) { %>
    <meta property="og:image" content="<%= ogImageUrl %>">
    <meta property="og:image:width" content="1200">
    <meta property="og:image:height" content="630">
    <% } %>
    
    <!-- Twitter Card -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="<%= ogUrl %>">
    <meta name="twitter:title" content="<%= ogTitle %>">
    <meta name="twitter:description" content="<%= ogDescription %>">
    <% if (!ogImageUrl.isEmpty()) { %>
    <meta name="twitter:image" content="<%= ogImageUrl %>">
    <% } %>
    
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="container view-card-container">
         <header class="header">
            <a href="${pageContext.request.contextPath}/" class="header-logo">P-Card Platform</a>
            <nav class="header-nav">
                 <c:if test="${not empty sessionScope.user and sessionScope.user.id == card.userId}">
                    <a href="${pageContext.request.contextPath}/dashboard">我的后台</a>
                </c:if>
                <c:if test="${not empty sessionScope.user}">
                    <c:if test="${sessionScope.user.isAdmin()}"><a href="${pageContext.request.contextPath}/admin">管理面板</a></c:if>
                    <a href="${pageContext.request.contextPath}/logout">退出登录</a>
                </c:if>
                <c:if test="${empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/login?redirect=${pageContext.request.contextPath}/card/${card.uniqueLinkId}">登录</a>
                </c:if>
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
                <a href="${pageContext.request.contextPath}/">首页</a>
                <c:if test="${not empty sessionScope.user and sessionScope.user.id == card.userId}">
                    <a href="${pageContext.request.contextPath}/dashboard">我的后台</a>
                </c:if>
                <c:if test="${not empty sessionScope.user}">
                    <c:if test="${sessionScope.user.isAdmin()}">
                        <a href="${pageContext.request.contextPath}/admin">管理面板</a>
                    </c:if>
                    <a href="${pageContext.request.contextPath}/logout">退出登录</a>
                </c:if>
                <c:if test="${empty sessionScope.user}">
                    <a href="${pageContext.request.contextPath}/login?redirect=${pageContext.request.contextPath}/card/${card.uniqueLinkId}">登录</a>
                </c:if>
            </nav>
        </aside>

        <%-- 处理：支持未上传图片 / 只上传一面 图片为外部或内部路径 --%>
        <%
            com.example.pcard.model.Card cardObj = (com.example.pcard.model.Card) request.getAttribute("card");
            String frontPath = cardObj != null ? cardObj.getCardFrontPath() : null;
            String backPath = cardObj != null ? cardObj.getCardBackPath() : null;
            String imageOrientation = cardObj != null ? cardObj.getImageOrientation() : "HORIZONTAL";
            // 如果没有方向信息，默认为横版
            if (imageOrientation == null || imageOrientation.trim().isEmpty()) {
                imageOrientation = "HORIZONTAL";
            }
            String ctx = request.getContextPath();
            String placeholderFront = "https://placehold.co/800x450/0071e3/ffffff?text=P-CARD";
            String placeholderBack = "https://placehold.co/800x450/333333/ffffff?text=BACK";

            String frontUrl;
            if (frontPath == null || frontPath.trim().isEmpty()) {
                frontUrl = placeholderFront;
            } else {
                String p = frontPath.trim();
                if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("/")) {
                    frontUrl = p;
                } else {
                    frontUrl = ctx + "/" + p;
                }
            }

            String backUrl;
            if (backPath == null || backPath.trim().isEmpty()) {
                backUrl = placeholderBack;
            } else {
                String p2 = backPath.trim();
                if (p2.startsWith("http://") || p2.startsWith("https://") || p2.startsWith("/")) {
                    backUrl = p2;
                } else {
                    backUrl = ctx + "/" + p2;
                }
            }

            boolean hasFront = frontPath != null && !frontPath.trim().isEmpty();
            boolean hasBack = backPath != null && !backPath.trim().isEmpty();
            boolean noImages = !hasFront && !hasBack;
            boolean singleSide = (hasFront && !hasBack) || (!hasFront && hasBack);
            
            // 根据图片方向添加相应的CSS类
            String orientationClass = "VERTICAL".equalsIgnoreCase(imageOrientation) ? " vertical" : " horizontal";
            String containerClass = "flipper-container" + ((singleSide || noImages) ? " single" : "") + orientationClass;
            String onclickAttr = (singleSide || noImages) ? "" : "onclick=\"this.classList.toggle('flipped')\"";
            String flipperClass = "flipper" + ((singleSide || noImages) ? " single" : "");
        %>

        <!-- 名片和信息区域的容器 -->
        <div class="card-info-wrapper">
            <!-- 左侧：名片展示 -->
            <div class="card-display-section">
                <div class="card-details">
                    <div class="<%= containerClass %>" <%= onclickAttr %>>
                        <div class="<%= flipperClass %>">
                            <div class="flipper-front"><img src="<%= frontUrl %>" alt="名片正面"></div>
                            <div class="flipper-back"><img src="<%= backUrl %>" alt="名片背面"></div>
                        </div>
                    </div>
                    <p class="flip-instruction"><%= noImages ? "该用户未上传名片，显示占位图。" : (singleSide ? "仅上传一面名片，点击不可翻转" : "点击卡片可翻转查看正反面") %></p>
                </div>
            </div>

            <!-- 右侧:信息展示 -->
            <div class="card-info-section">
                <div class="card-ui info-box">
                     <h2>
                        <a href="${pageContext.request.contextPath}/?q=<c:out value='${card.producerName}'/>" 
                           class="producer-name-link" 
                           title="搜索该制作人的其他名片">
                            <c:out value="${card.producerName}"/>
                        </a>
                     </h2>
                     <div class="info-items-wrapper">
                         <p>
                            <strong>担当偶像:</strong> 
                            <a href="${pageContext.request.contextPath}/?q=<c:out value='${card.idolName}'/>" class="info-value">
                                <c:out value="${card.idolName}"/>
                            </a>
                         </p>
                         <p>
                            <strong>所在地区:</strong> 
                            <a href="${pageContext.request.contextPath}/?q=<c:out value='${card.region}'/>" class="info-value">
                                <c:out value="${card.region}"/>
                            </a>
                         </p>
                         <p>
                            <strong>名片所有者:</strong> 
                            <span class="info-value" style="cursor: default;">
                                <c:out value="${card.ownerUsername}"/>
                            </span>
                         </p>
                     </div>

                     <div class="sns-links" id="sns-links-container">
                        <%-- 社交图标将由JavaScript动态生成 --%>
                     </div>
                     <div id="sns-tooltip"></div>
                </div>
            </div>
        </div>

        <!-- Comment Section -->
        <div class="card-ui comment-section">
            <h2><i class="fas fa-comments"></i> 留言板</h2>
            <c:if test="${not empty sessionScope.user}">
                <form action="${pageContext.request.contextPath}/commentAction" method="post" class="comment-form">
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="cardId" value="${card.id}">
                    <input type="hidden" name="cardLink" value="${card.uniqueLinkId}">
                    <div class="form-group">
                        <textarea name="content" class="form-control comment-textarea" rows="3" placeholder="留下你的留言..." autocomplete="off" required></textarea>
                    </div>
                    <div class="comment-form-footer">
                        <span class="comment-tip"><i class="fas fa-info-circle"></i> 支持多行输入，礼貌留言</span>
                        <button type="submit" class="btn btn-primary"><i class="fas fa-paper-plane"></i> 发表留言</button>
                    </div>
                </form>
            </c:if>
            <c:if test="${empty sessionScope.user}">
                <div class="login-prompt">
                    <i class="fas fa-lock"></i> 请 <a href="${pageContext.request.contextPath}/login?redirect=${pageContext.request.contextPath}/card/${card.uniqueLinkId}">登录</a> 后发表留言
                </div>
            </c:if>

            <div class="comment-list">
                <c:forEach var="comment" items="${comments}">
                    <c:set var="comment" value="${comment}" scope="request"/>
                    <c:set var="card" value="${card}" scope="request"/>
                    <c:set var="level" value="0" scope="request"/>
                    <jsp:include page="/WEB-INF/comment-recursive.jsp"/>
                </c:forEach>
                <c:if test="${empty comments}">
                    <div class="empty-comments">
                        <i class="fas fa-comment-slash"></i>
                        <p>还没有留言，快来抢沙发吧！</p>
                    </div>
                </c:if>
            </div>
        </div>
    </div>
    <script>
        <!-- 使用 JSTL 输出 card.customSns 的 JSON 内容或空数组 -->
        <c:choose>
            <c:when test="${not empty card.customSns}">
                const snsData = ${card.customSns};
            </c:when>
            <c:otherwise>
                const snsData = [];
            </c:otherwise>
        </c:choose>
    </script>
    <script src="${pageContext.request.contextPath}/js/script.js"></script>
</body>
</html>