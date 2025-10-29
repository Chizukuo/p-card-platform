<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- 权限检查 --%>
<c:if test="${empty sessionScope.user or not sessionScope.user.isAdmin()}">
    <c:redirect url="/" />
</c:if>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>管理面板 - P-Card 平台</title>
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
        <h1 class="page-header-title">🛡️ 管理面板</h1>
        <nav class="header-nav">
            <a href="${pageContext.request.contextPath}/">返回首页</a>
            <a href="dashboard">我的后台</a>
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
            <a href="dashboard">我的后台</a>
            <a href="admin" class="active">管理面板</a>
            <a href="logout">退出登录</a>
        </nav>
    </aside>

    <c:if test="${not empty sessionScope.adminError}">
        <div class="alert alert-error">${sessionScope.adminError}</div>
        <c:remove var="adminError" scope="session"/>
    </c:if>

    <!-- 用户管理 -->
    <div class="card-ui">
        <h2>用户管理</h2>
        <form method="get" action="admin" class="filters-row">
            <input type="text" name="userQ" class="form-control" placeholder="搜索用户名" value="${fn:escapeXml(param.userQ)}" autocomplete="off" />
            <select name="role" class="form-control">
                <option value="all" ${empty param.role || param.role == 'all' ? 'selected' : ''}>全部角色</option>
                <option value="user" ${param.role == 'user' ? 'selected' : ''}>普通用户</option>
                <option value="admin" ${param.role == 'admin' ? 'selected' : ''}>管理员</option>
            </select>
            <select name="status" class="form-control">
                <option value="all" ${empty param.status || param.status == 'all' ? 'selected' : ''}>全部状态</option>
                <option value="active" ${param.status == 'active' ? 'selected' : ''}>active</option>
                <option value="banned" ${param.status == 'banned' ? 'selected' : ''}>banned</option>
            </select>
            <button type="submit" class="btn btn-secondary">筛选</button>
            <a href="admin" class="btn btn-secondary">重置</a>
        </form>
        <div class="admin-table-wrapper">
            <table class="admin-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>用户名</th>
                    <th>角色</th>
                    <th>状态</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="u" items="${allUsers}">
                    <tr>
                        <td>${u.id}</td>
                        <td><c:out value="${u.username}" /></td>
                        <td>
                            <span class="badge">${u.role}</span>
                            <form action="admin" method="post" style="display:inline-block; margin-left:8px;">
                                <input type="hidden" name="action" value="updateUserRole">
                                <input type="hidden" name="userId" value="${u.id}">
                                <select name="role" class="form-control" style="width:auto; display:inline-block;">
                                    <option value="user" ${u.role=='user' ? 'selected' : ''}>user</option>
                                    <option value="admin" ${u.role=='admin' ? 'selected' : ''}>admin</option>
                                </select>
                                <button type="submit" class="btn btn-secondary btn-sm">修改</button>
                            </form>
                        </td>
                        <td>
                             <span class="status-${u.status}">${u.status}</span>
                        </td>
                        <td>
                            <c:if test="${u.role ne 'admin'}">
                                <form action="admin" method="post" style="display: inline-block;">
                                    <input type="hidden" name="action" value="updateUserStatus">
                                    <input type="hidden" name="userId" value="${u.id}">
                                    <c:choose>
                                        <c:when test="${u.status eq 'active'}">
                                            <input type="hidden" name="status" value="banned">
                                            <button type="submit" class="btn btn-danger btn-sm">封禁</button>
                                        </c:when>
                                        <c:otherwise>
                                            <input type="hidden" name="status" value="active">
                                            <button type="submit" class="btn btn-secondary btn-sm">解封</button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                                <form action="admin" method="post" style="display: inline-block; margin-left:6px;">
                                    <input type="hidden" name="action" value="deleteUser">
                                    <input type="hidden" name="userId" value="${u.id}">
                                    <button type="submit" class="btn btn-danger btn-sm">删除</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        </div>

        <!-- 用户分页 -->
        <c:if test="${userTotalPages > 1}">
            <div class="pagination">
                <c:set var="prevU" value="${userPage - 1 > 0 ? userPage - 1 : 1}" />
                <c:set var="nextU" value="${userPage + 1 <= userTotalPages ? userPage + 1 : userTotalPages}" />
                <a class="page-btn" href="admin?uPage=${prevU}&role=${param.role}&status=${param.status}&userQ=${fn:escapeXml(param.userQ)}&cPage=${param.cPage}&visibility=${param.visibility}&cardQ=${fn:escapeXml(param.cardQ)}">上一页</a>
                <span class="page-info">第 ${userPage} / ${userTotalPages} 页，共 ${userTotal} 条</span>
                <a class="page-btn" href="admin?uPage=${nextU}&role=${param.role}&status=${param.status}&userQ=${fn:escapeXml(param.userQ)}&cPage=${param.cPage}&visibility=${param.visibility}&cardQ=${fn:escapeXml(param.cardQ)}">下一页</a>
            </div>
        </c:if>
    </div>

    <!-- 名片管理 -->
    <div class="card-ui">
        <h2>名片管理</h2>
        <form method="get" action="admin" class="filters-row">
            <input type="text" name="cardQ" class="form-control" placeholder="搜索P名/担当/地区/链接/用户" value="${fn:escapeXml(param.cardQ)}" autocomplete="off" />
            <select name="visibility" class="form-control">
                <option value="all" ${empty param.visibility || param.visibility=='all' ? 'selected' : ''}>全部可见性</option>
                <option value="PUBLIC" ${param.visibility=='PUBLIC' ? 'selected' : ''}>PUBLIC</option>
                <option value="LINK_ONLY" ${param.visibility=='LINK_ONLY' ? 'selected' : ''}>LINK_ONLY</option>
                <option value="PRIVATE" ${param.visibility=='PRIVATE' ? 'selected' : ''}>PRIVATE</option>
            </select>
            <button type="submit" class="btn btn-secondary">筛选</button>
            <a href="admin" class="btn btn-secondary">重置</a>
        </form>
        <div class="admin-table-wrapper">
            <table class="admin-table">
            <thead>
                <tr>
                    <th>名片ID</th>
                    <th>所有者</th>
                    <th>P名</th>
                    <th>担当</th>
                    <th>可见性</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="c" items="${allCards}">
                    <tr>
                        <td>${c.id}</td>
                        <td><c:out value="${c.ownerUsername}" /></td>
                        <td><c:out value="${c.producerName}" /></td>
                        <td><c:out value="${c.idolName}" /></td>
                        <td>
                            <form action="admin" method="post" class="table-inline-form">
                                <input type="hidden" name="action" value="setCardVisibility"/>
                                <input type="hidden" name="cardId" value="${c.id}"/>
                                <select name="visibility" class="form-control" style="width:auto; display:inline-block;">
                                    <option value="PUBLIC" ${c.visibility=='PUBLIC' ? 'selected' : ''}>PUBLIC</option>
                                    <option value="LINK_ONLY" ${c.visibility=='LINK_ONLY' ? 'selected' : ''}>LINK_ONLY</option>
                                    <option value="PRIVATE" ${c.visibility=='PRIVATE' ? 'selected' : ''}>PRIVATE</option>
                                </select>
                                <button type="submit" class="btn btn-secondary btn-sm">更新</button>
                            </form>
                        </td>
                        <td>
                             <a href="card/${c.uniqueLinkId}" class="btn btn-secondary btn-sm" target="_blank">查看</a>
                              <form action="cardAction" method="post" style="display: inline-block;">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="cardId" value="${c.id}">
                                <button type="submit" class="btn btn-danger btn-sm">删除</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        </div>

        <!-- 名片分页 -->
        <c:if test="${cardTotalPages > 1}">
            <div class="pagination">
                <c:set var="prevC" value="${cardPage - 1 > 0 ? cardPage - 1 : 1}" />
                <c:set var="nextC" value="${cardPage + 1 <= cardTotalPages ? cardPage + 1 : cardTotalPages}" />
                <a class="page-btn" href="admin?cPage=${prevC}&visibility=${param.visibility}&cardQ=${fn:escapeXml(param.cardQ)}&uPage=${param.uPage}&role=${param.role}&status=${param.status}&userQ=${fn:escapeXml(param.userQ)}">上一页</a>
                <span class="page-info">第 ${cardPage} / ${cardTotalPages} 页，共 ${cardTotal} 条</span>
                <a class="page-btn" href="admin?cPage=${nextC}&visibility=${param.visibility}&cardQ=${fn:escapeXml(param.cardQ)}&uPage=${param.uPage}&role=${param.role}&status=${param.status}&userQ=${fn:escapeXml(param.userQ)}">下一页</a>
            </div>
        </c:if>
    </div>
</div>
<script src="js/script.js"></script>
</body>
</html>