<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- 递归显示评论和回复的组件，最大嵌套3层 --%>
<%-- 参数：comment, card, level (嵌套层级) --%>

<c:set var="currentLevel" value="${empty level ? 0 : level}" />
<c:set var="isReply" value="${currentLevel > 0}" />
<c:set var="maxNestLevel" value="3" />
<c:set var="canNest" value="${currentLevel < maxNestLevel}" />

<div class="comment-item ${isReply ? 'reply-item' : ''}" data-comment-id="${comment.id}" data-level="${currentLevel}">
    <div class="${isReply ? 'reply-avatar' : 'comment-avatar'}">
        ${fn:substring(comment.nickname != null ? comment.nickname : comment.username, 0, 1)}
    </div>
    <div class="${isReply ? 'reply-content-wrapper' : 'comment-content-wrapper'}">
        <div class="${isReply ? 'reply-header' : 'comment-header'}">
            <span class="${isReply ? 'reply-username' : 'comment-username'}">
                <c:out value="${comment.nickname != null ? comment.nickname : comment.username}"/>
            </span>
            <c:if test="${not empty comment.replyToUsername}">
                <span class="reply-to">
                    <i class="fas fa-reply" style="font-size: 11px;"></i>
                    <span class="reply-to-username">@<c:out value="${comment.replyToNickname != null ? comment.replyToNickname : comment.replyToUsername}"/></span>
                </span>
            </c:if>
            <span class="${isReply ? 'reply-time' : 'comment-time'}" data-timestamp="${comment.timestamp}">
                ${comment.formattedTime}
            </span>
        </div>
        <div class="${isReply ? 'reply-body' : 'comment-body'}">
            <c:out value="${comment.content}"/>
        </div>
        <div class="${isReply ? 'reply-actions' : 'comment-actions'}">
            <c:if test="${not empty sessionScope.user}">
                <button class="${isReply ? 'btn-reply-small' : 'btn-reply'}" 
                        data-comment-id="${comment.id}" 
                        data-username="<c:out value='${comment.username}'/>"
                        data-nickname="<c:out value='${comment.nickname != null ? comment.nickname : comment.username}'/>"
                        onclick="showReplyForm(this.dataset.commentId, this.dataset.username, this.dataset.nickname)">
                    <i class="fas fa-reply"></i> 回复
                </button>
            </c:if>
            <c:if test="${not empty sessionScope.user && (sessionScope.user.isAdmin() || sessionScope.user.id == card.userId || sessionScope.user.id == comment.userId)}">
                <c:set var="deleteMsg" value="${isReply ? '回复' : '评论'}" />
                <c:set var="extraMsg" value="${not empty comment.replies ? '及其所有回复' : ''}" />
                <form action="${pageContext.request.contextPath}/commentAction" method="post" style="display: inline;" 
                      data-delete-msg="确定删除这条${deleteMsg}${extraMsg}吗？"
                      onsubmit="return confirm(this.dataset.deleteMsg)">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="commentId" value="${comment.id}">
                    <input type="hidden" name="cardId" value="${card.id}">
                    <input type="hidden" name="cardLink" value="${card.uniqueLinkId}">
                    <button type="submit" class="${isReply ? 'btn-delete-small' : 'btn-delete'}">
                        <i class="fas fa-trash-alt"></i> 删除
                    </button>
                </form>
            </c:if>
        </div>
        
        <!-- 回复表单（默认隐藏） -->
        <div class="reply-form-container" id="reply-form-${comment.id}" style="display: none;">
            <form action="${pageContext.request.contextPath}/commentAction" method="post" class="reply-form">
                <input type="hidden" name="action" value="reply">
                <input type="hidden" name="cardId" value="${card.id}">
                <input type="hidden" name="cardLink" value="${card.uniqueLinkId}">
                <input type="hidden" name="parentId" value="${comment.id}">
                <input type="hidden" name="replyToUsername" value="${comment.username}">
                <input type="hidden" name="replyToNickname" value="${comment.nickname != null ? comment.nickname : comment.username}">
                <div class="reply-input-group">
                    <textarea name="content" class="form-control reply-textarea" rows="2" 
                              placeholder="回复 @${comment.nickname != null ? comment.nickname : comment.username}..." required></textarea>
                    <div class="reply-actions-form">
                        <button type="button" class="btn-cancel" data-comment-id="${comment.id}" onclick="hideReplyForm(this.dataset.commentId)">取消</button>
                        <button type="submit" class="btn-submit">
                            <i class="fas fa-paper-plane"></i> 回复
                        </button>
                    </div>
                </div>
            </form>
        </div>
        
        <!-- 递归显示子回复（最多3层嵌套） -->
        <c:if test="${not empty comment.replies}">
            <c:choose>
                <%-- 如果还可以嵌套，则继续嵌套显示 --%>
                <c:when test="${canNest}">
                    <%-- 保存当前评论的回复列表和数量 --%>
                    <c:set var="currentReplies" value="${comment.replies}" />
                    <c:set var="repliesCount" value="${fn:length(currentReplies)}" />
                    
                    <div class="replies-container" data-level="${currentLevel + 1}" data-total-replies="${repliesCount}">
                        <c:forEach var="reply" items="${currentReplies}" varStatus="status">
                            <c:choose>
                                <c:when test="${status.index >= 2}">
                                    <div class="reply-wrapper reply-hidden">
                                </c:when>
                                <c:otherwise>
                                    <div class="reply-wrapper">
                                </c:otherwise>
                            </c:choose>
                                <c:set var="comment" value="${reply}" scope="request"/>
                                <c:set var="level" value="${currentLevel + 1}" scope="request"/>
                                <c:set var="card" value="${card}" scope="request"/>
                                <jsp:include page="/WEB-INF/comment-recursive.jsp"/>
                            </div>
                        </c:forEach>
                        <c:if test="${repliesCount > 2}">
                            <div class="show-more-replies">
                                <button class="btn-show-more" onclick="toggleReplies(this)">
                                    <i class="fas fa-chevron-down"></i>
                                    <span class="show-text">展开 ${repliesCount - 2} 条回复</span>
                                    <span class="hide-text" style="display: none;">收起回复</span>
                                </button>
                            </div>
                        </c:if>
                    </div>
                </c:when>
                <%-- 如果已达到最大嵌套层级，则平铺显示所有子孙回复（DFS遍历） --%>
                <c:otherwise>
                    <%-- 使用栈模拟DFS，将树结构展平为列表 --%>
                    <jsp:useBean id="flatList" class="java.util.ArrayList" scope="page"/>
                    <jsp:useBean id="stack" class="java.util.Stack" scope="page"/>
                    <%
                        // 将当前评论的所有子回复压入栈（逆序，使得先进后出）
                        java.util.List replies = ((com.example.pcard.model.Comment)request.getAttribute("comment")).getReplies();
                        for (int i = replies.size() - 1; i >= 0; i--) {
                            stack.push(replies.get(i));
                        }
                        
                        // DFS遍历整个子树
                        while (!stack.isEmpty()) {
                            com.example.pcard.model.Comment current = (com.example.pcard.model.Comment)stack.pop();
                            flatList.add(current);
                            
                            // 将当前节点的子回复压入栈
                            java.util.List currentReplies = current.getReplies();
                            if (currentReplies != null && !currentReplies.isEmpty()) {
                                for (int i = currentReplies.size() - 1; i >= 0; i--) {
                                    stack.push(currentReplies.get(i));
                                }
                            }
                        }
                    %>
                    
                    <div class="replies-container flat" data-level="${currentLevel}">
                        <c:forEach var="flatReply" items="${flatList}">
                            <div class="comment-item reply-item" data-comment-id="${flatReply.id}" data-level="${currentLevel}">
                                <div class="reply-avatar">
                                    ${fn:substring(flatReply.nickname != null ? flatReply.nickname : flatReply.username, 0, 1)}
                                </div>
                                <div class="reply-content-wrapper">
                                    <div class="reply-header">
                                        <span class="reply-username">
                                            <c:out value="${flatReply.nickname != null ? flatReply.nickname : flatReply.username}"/>
                                        </span>
                                        <c:if test="${not empty flatReply.replyToUsername}">
                                            <span class="reply-to">
                                                <i class="fas fa-reply" style="font-size: 11px;"></i>
                                                <span class="reply-to-username">@<c:out value="${flatReply.replyToNickname != null ? flatReply.replyToNickname : flatReply.replyToUsername}"/></span>
                                            </span>
                                        </c:if>
                                        <span class="reply-time" data-timestamp="${flatReply.timestamp}">
                                            ${flatReply.formattedTime}
                                        </span>
                                    </div>
                                    <div class="reply-body">
                                        <c:out value="${flatReply.content}"/>
                                    </div>
                                    <div class="reply-actions">
                                        <c:if test="${not empty sessionScope.user}">
                                            <button class="btn-reply-small" 
                                                    data-comment-id="${flatReply.id}" 
                                                    data-username="<c:out value='${flatReply.username}'/>"
                                                    data-nickname="<c:out value='${flatReply.nickname != null ? flatReply.nickname : flatReply.username}'/>"
                                                    onclick="showReplyForm(this.dataset.commentId, this.dataset.username, this.dataset.nickname)">
                                                <i class="fas fa-reply"></i> 回复
                                            </button>
                                        </c:if>
                                        <c:if test="${not empty sessionScope.user && (sessionScope.user.isAdmin() || sessionScope.user.id == card.userId || sessionScope.user.id == flatReply.userId)}">
                                            <form action="${pageContext.request.contextPath}/commentAction" method="post" style="display: inline;" 
                                                  data-delete-msg="确定删除这条回复吗？"
                                                  onsubmit="return confirm(this.dataset.deleteMsg)">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="commentId" value="${flatReply.id}">
                                                <input type="hidden" name="cardId" value="${card.id}">
                                                <input type="hidden" name="cardLink" value="${card.uniqueLinkId}">
                                                <button type="submit" class="btn-delete-small">
                                                    <i class="fas fa-trash-alt"></i> 删除
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>
                                    
                                    <!-- 回复表单（默认隐藏） -->
                                    <div class="reply-form-container" id="reply-form-${flatReply.id}" style="display: none;">
                                        <form action="${pageContext.request.contextPath}/commentAction" method="post" class="reply-form">
                                            <input type="hidden" name="action" value="reply">
                                            <input type="hidden" name="cardId" value="${card.id}">
                                            <input type="hidden" name="cardLink" value="${card.uniqueLinkId}">
                                            <input type="hidden" name="parentId" value="${flatReply.id}">
                                            <input type="hidden" name="replyToUsername" value="${flatReply.username}">
                                            <input type="hidden" name="replyToNickname" value="${flatReply.nickname != null ? flatReply.nickname : flatReply.username}">
                                            <div class="reply-input-group">
                                                <textarea name="content" class="form-control reply-textarea" rows="2" 
                                                          placeholder="回复 @${flatReply.nickname != null ? flatReply.nickname : flatReply.username}..." required></textarea>
                                                <div class="reply-actions-form">
                                                    <button type="button" class="btn-cancel" data-comment-id="${flatReply.id}" onclick="hideReplyForm(this.dataset.commentId)">取消</button>
                                                    <button type="submit" class="btn-submit">
                                                        <i class="fas fa-paper-plane"></i> 回复
                                                    </button>
                                                </div>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>
