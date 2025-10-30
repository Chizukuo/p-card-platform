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
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .stat-card {
            background: var(--card-bg);
            padding: 20px;
            border-radius: 12px;
            text-align: center;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .stat-value {
            font-size: 32px;
            font-weight: bold;
            color: var(--primary-color);
            margin: 10px 0;
        }
        .stat-label {
            font-size: 14px;
            color: var(--text-muted);
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
        .batch-actions {
            display: flex;
            gap: 10px;
            margin-bottom: 15px;
            flex-wrap: wrap;
        }
        .checkbox-col {
            width: 40px;
            text-align: center;
        }
        .alert-success {
            background-color: #d4edda;
            color: #155724;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            border: 1px solid #c3e6cb;
        }
        .export-buttons {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            flex-wrap: wrap;
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
    
    <c:if test="${not empty sessionScope.adminSuccess}">
        <div class="alert-success">${sessionScope.adminSuccess}</div>
        <c:remove var="adminSuccess" scope="session"/>
    </c:if>

    <!-- 统计仪表板 -->
    <div class="card-ui">
        <h2>📊 系统统计</h2>
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-label">总用户数</div>
                <div class="stat-value">${totalUsers}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">活跃用户</div>
                <div class="stat-value">${totalActiveUsers}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">管理员</div>
                <div class="stat-value">${totalAdmins}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">封禁用户</div>
                <div class="stat-value" style="color: #dc3545;">${totalBannedUsers}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">总名片数</div>
                <div class="stat-value">${totalCards}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">公开名片</div>
                <div class="stat-value">${totalPublicCards}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">私密名片</div>
                <div class="stat-value">${totalPrivateCards}</div>
            </div>
        </div>
    </div>

    <!-- 导航标签 -->
    <div class="admin-tabs">
        <a href="admin" class="admin-tab active">用户 & 名片</a>
        <a href="admin?view=comments" class="admin-tab">评论管理</a>
    </div>

    <!-- 用户管理 -->
    <div class="card-ui">
        <h2>👥 用户管理</h2>
        
        <!-- 导出按钮 -->
        <div class="export-buttons">
            <a href="admin?view=exportUsers" class="btn btn-secondary">📥 导出用户CSV</a>
        </div>
        
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
        
        <!-- 批量操作 -->
        <form method="post" action="admin" id="batchUserForm">
            <div class="batch-actions">
                <button type="button" onclick="batchAction('batchBanUsers')" class="btn btn-danger btn-sm">批量封禁</button>
                <button type="button" onclick="batchAction('batchDeleteUsers')" class="btn btn-danger btn-sm">批量删除</button>
                <button type="button" onclick="selectAllUsers()" class="btn btn-secondary btn-sm">全选/取消</button>
            </div>
            
            <div class="admin-table-wrapper">
                <table class="admin-table">
                <thead>
                    <tr>
                        <th class="checkbox-col"><input type="checkbox" id="selectAllUsersCheck" onclick="selectAllUsers()"></th>
                        <th>ID</th>
                        <th>用户名</th>
                        <th>昵称</th>
                        <th>角色</th>
                        <th>状态</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="u" items="${allUsers}">
                        <tr>
                            <td class="checkbox-col">
                                <c:if test="${u.role ne 'admin'}">
                                    <input type="checkbox" name="userIds" value="${u.id}" class="user-checkbox">
                                </c:if>
                            </td>
                            <td>${u.id}</td>
                            <td><c:out value="${u.username}" /></td>
                            <td><c:out value="${u.nickname}" /></td>
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
                                        <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('确定删除该用户吗？')">删除</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            </div>
        </form>

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
        <h2>🎴 名片管理</h2>
        
        <!-- 导出按钮 -->
        <div class="export-buttons">
            <a href="admin?view=exportCards" class="btn btn-secondary">📥 导出名片CSV</a>
        </div>
        
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
        
        <!-- 批量操作 -->
        <form method="post" action="admin" id="batchCardForm">
            <div class="batch-actions">
                <select name="batchVisibility" class="form-control" style="width: auto;">
                    <option value="">选择可见性</option>
                    <option value="PUBLIC">PUBLIC</option>
                    <option value="LINK_ONLY">LINK_ONLY</option>
                    <option value="PRIVATE">PRIVATE</option>
                </select>
                <button type="button" onclick="batchAction('batchSetCardVisibility')" class="btn btn-secondary btn-sm">批量设置可见性</button>
                <button type="button" onclick="batchAction('batchDeleteCards')" class="btn btn-danger btn-sm">批量删除</button>
                <button type="button" onclick="selectAllCards()" class="btn btn-secondary btn-sm">全选/取消</button>
            </div>
            
            <div class="admin-table-wrapper">
                <table class="admin-table">
                <thead>
                    <tr>
                        <th class="checkbox-col"><input type="checkbox" id="selectAllCardsCheck" onclick="selectAllCards()"></th>
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
                            <td class="checkbox-col">
                                <input type="checkbox" name="cardIds" value="${c.id}" class="card-checkbox">
                            </td>
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
                                    <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('确定删除该名片吗？')">删除</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            </div>
        </form>

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
<script>
function selectAllUsers() {
    var checkboxes = document.querySelectorAll('.user-checkbox');
    var mainCheckbox = document.getElementById('selectAllUsersCheck');
    checkboxes.forEach(function(checkbox) {
        checkbox.checked = mainCheckbox.checked;
    });
}

function selectAllCards() {
    var checkboxes = document.querySelectorAll('.card-checkbox');
    var mainCheckbox = document.getElementById('selectAllCardsCheck');
    checkboxes.forEach(function(checkbox) {
        checkbox.checked = mainCheckbox.checked;
    });
}

function batchAction(action) {
    var form;
    if (action.includes('User')) {
        form = document.getElementById('batchUserForm');
    } else {
        form = document.getElementById('batchCardForm');
    }
    
    var checkboxes = form.querySelectorAll('input[type="checkbox"]:checked:not(#selectAllUsersCheck):not(#selectAllCardsCheck)');
    if (checkboxes.length === 0) {
        alert('请至少选择一项');
        return;
    }
    
    if (action === 'batchSetCardVisibility') {
        var visibility = form.querySelector('select[name="batchVisibility"]').value;
        if (!visibility) {
            alert('请选择可见性选项');
            return;
        }
    }
    
    if (confirm('确定要执行此批量操作吗？')) {
        var actionInput = document.createElement('input');
        actionInput.type = 'hidden';
        actionInput.name = 'action';
        actionInput.value = action;
        form.appendChild(actionInput);
        form.submit();
    }
}
</script>
</body>
</html>