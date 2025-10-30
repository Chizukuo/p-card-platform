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
    <title>评论管理 - P-Card 平台</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .page-header-title {
            color: var(--text-color);
            margin: 0;
            font-size: 32px;
            font-weight: 700;
        }
        .admin-tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            border-bottom: 2px solid var(--border-color);
            flex-wrap: wrap;
        }
        .admin-tab {
            padding: 10px 20px;
            background: none;
            border: none;
            border-bottom: 3px solid transparent;
            cursor: pointer;
            font-size: 16px;
            color: var(--text-color);
            text-decoration: none;
            transition: all 0.3s;
        }
        .admin-tab:hover {
            color: var(--primary-color);
        }
        .admin-tab.active {
            border-bottom-color: var(--primary-color);
            color: var(--primary-color);
            font-weight: bold;
        }
        .comment-content {
            max-width: 400px;
            word-wrap: break-word;
        }
        .alert-success {
            background-color: #d4edda;
            color: #155724;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            border: 1px solid #c3e6cb;
        }
    </style>
</head>
<body>
<div class="container">
    <header class="header">
        <h1 class="page-header-title">💬 评论管理</h1>
        <nav class="header-nav">
            <a href="${pageContext.request.contextPath}/">返回首页</a>
            <a href="dashboard">我的后台</a>
            <a href="logout">退出登录</a>
        </nav>
        <button id="mobile-menu-btn" class="mobile-menu-btn" aria-label="打开菜单">☰</button>
    </header>
    
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
    
    <c:if test="${not empty sessionScope.adminSuccess}">
        <div class="alert-success">${sessionScope.adminSuccess}</div>
        <c:remove var="adminSuccess" scope="session"/>
    </c:if>

    <!-- 导航标签 -->
    <div class="admin-tabs">
        <a href="admin" class="admin-tab">用户 & 名片</a>
        <a href="admin?view=comments" class="admin-tab active">评论管理</a>
    </div>

    <!-- 评论管理 -->
    <div class="card-ui">
        <h2>💬 评论管理</h2>
        <p style="color: var(--text-muted); margin-bottom: 20px;">共 ${totalComments} 条评论</p>
        
        <form method="get" action="admin" class="filters-row">
            <input type="hidden" name="view" value="comments">
            <input type="text" name="commentQ" class="form-control" placeholder="搜索评论内容/用户/名片" value="${fn:escapeXml(param.commentQ)}" autocomplete="off" />
            <button type="submit" class="btn btn-secondary">搜索</button>
            <a href="admin?view=comments" class="btn btn-secondary">重置</a>
        </form>
        
        <div class="admin-table-wrapper">
            <table class="admin-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>评论内容</th>
                    <th>评论者</th>
                    <th>所属名片</th>
                    <th>名片所有者</th>
                    <th>时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty allComments}">
                        <c:forEach var="comment" items="${allComments}">
                            <tr>
                                <td>${comment.id}</td>
                                <td class="comment-content"><c:out value="${comment.content}" /></td>
                                <td>
                                    <c:out value="${comment.nickname}" />
                                    <br><small style="color: var(--text-muted);">@<c:out value="${comment.username}" /></small>
                                </td>
                                <td><c:out value="${comment.cardTitle}" /></td>
                                <td><c:out value="${comment.ownerUsername}" /></td>
                                <td>${comment.formattedTime}</td>
                                <td>
                                    <form action="admin" method="post" style="display: inline-block;">
                                        <input type="hidden" name="action" value="deleteComment">
                                        <input type="hidden" name="commentId" value="${comment.id}">
                                        <input type="hidden" name="view" value="comments">
                                        <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('确定删除此评论吗？')">删除</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td colspan="7" style="text-align: center; padding: 40px; color: var(--text-muted);">
                                暂无评论
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
        </div>

        <!-- 分页 -->
        <c:if test="${totalPages > 1}">
            <div class="pagination">
                <c:set var="prevPage" value="${page - 1 > 0 ? page - 1 : 1}" />
                <c:set var="nextPage" value="${page + 1 <= totalPages ? page + 1 : totalPages}" />
                <a class="page-btn" href="admin?view=comments&page=${prevPage}&commentQ=${fn:escapeXml(param.commentQ)}">上一页</a>
                <span class="page-info">第 ${page} / ${totalPages} 页，共 ${totalComments} 条</span>
                <a class="page-btn" href="admin?view=comments&page=${nextPage}&commentQ=${fn:escapeXml(param.commentQ)}">下一页</a>
            </div>
        </c:if>
    </div>
</div>

<script src="js/script.js"></script>
</body>
</html>
