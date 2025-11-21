<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    
    <!-- SEO Meta Tags -->
    <title>P-Card 电子名片平台 - 制作人的专属名片墙</title>
    <meta name="description" content="P-Card电子名片平台，为制作人打造专属的数字名片墙，分享你的担当信息和制作人身份。">
    <meta name="keywords" content="P-Card,电子名片,名片墙,制作人,偶像大师,名片分享平台,名片交换平台,名片,ACG,二次元,虚拟偶像,lovelive,bangdream">
    <meta name="robots" content="index, follow">
    <meta name="googlebot" content="index, follow">
    
    <!-- Open Graph / Facebook -->
    <meta property="og:type" content="website">
    <meta property="og:url" content="${pageContext.request.scheme}://${pageContext.request.serverName}${pageContext.request.contextPath}/">
    <meta property="og:title" content="P-Card 电子名片平台 - 制作人的专属名片墙">
    <meta property="og:description" content="P-Card电子名片平台，为制作人打造专属的数字名片墙，分享你的担当信息和制作人身份。">
    
    <!-- Twitter -->
    <meta property="twitter:card" content="summary_large_image">
    <meta property="twitter:url" content="${pageContext.request.scheme}://${pageContext.request.serverName}${pageContext.request.contextPath}/">
    <meta property="twitter:title" content="P-Card 电子名片平台 - 制作人的专属名片墙">
    <meta property="twitter:description" content="P-Card电子名片平台，为制作人打造专属的数字名片墙，分享你的担当信息和制作人身份。">
    
    <!-- Canonical URL -->
    <link rel="canonical" href="${pageContext.request.scheme}://${pageContext.request.serverName}${pageContext.request.contextPath}/">
    
    <!-- 网站图标 favicon -->
    <link rel="icon" type="image/png" href="/favicon.png">
    <link rel="shortcut icon" href="/favicon.ico">
    <link rel="apple-touch-icon" href="/favicon.png">
    <link rel="stylesheet" href="css/style.css">
    <style>
        /* 首页专属样式 */
        body {
            position: relative;
        }
        
        .page-title {
            text-align: center; 
            font-size: 48px; 
            font-weight: 800; 
            margin: 50px 0 40px 0; 
            color: #1d1d1f;
            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            animation: titleFloat 3s ease-in-out infinite;
            position: relative;
            z-index: 1;
        }
        
        @keyframes titleFloat {
            0%, 100% {
                transform: translateY(0);
            }
            50% {
                transform: translateY(-5px);
            }
        }
        
        @media (max-width: 767px) {
            .page-title {
                font-size: 32px;
                margin: 30px 0 25px 0;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <header class="header">
            <div class="header-logo">P-Card Platform</div>
            <nav class="header-nav">
                <c:choose>
                    <c:when test="${not empty sessionScope.user}">
                        <a href="dashboard">我的后台</a>
                        <c:if test="${sessionScope.user.isAdmin()}">
                             <a href="admin">管理面板</a>
                        </c:if>
                        <a href="logout">退出登录</a>
                    </c:when>
                    <c:otherwise>
                        <a href="login">登录</a>
                        <a href="register">注册</a>
                    </c:otherwise>
                </c:choose>
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
                <c:choose>
                    <c:when test="${not empty sessionScope.user}">
                        <a href="${pageContext.request.contextPath}/">首页</a>
                        <a href="dashboard">我的后台</a>
                        <c:if test="${sessionScope.user.isAdmin()}">
                            <a href="admin">管理面板</a>
                        </c:if>
                        <a href="logout">退出登录</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/">首页</a>
                        <a href="login">登录</a>
                        <a href="register">注册</a>
                    </c:otherwise>
                </c:choose>
            </nav>
        </aside>
        
        <h1 class="page-title">✨ 制作人们的名片墙 ✨</h1>

        <!-- 搜索框 -->
        <form action="${pageContext.request.contextPath}/" method="get" class="search-bar">
            <div class="search-input-wrapper">
                <input type="text" name="q" id="searchInput" value="${fn:escapeXml(param.q)}" placeholder="搜索制作人/担当/地区/链接ID" class="form-control" autocomplete="off">
                <c:if test="${not empty param.q}">
                    <button type="button" class="search-clear-btn" onclick="clearSearch()">&times;</button>
                </c:if>
            </div>
            <button type="submit" class="btn btn-secondary">搜索</button>
        </form>
        
        <script>
        function clearSearch() {
            window.location.href = '${pageContext.request.contextPath}/';
        }
        </script>

        <c:if test="${not empty param.q}">
            <p>关于“<strong><c:out value='${param.q}'/></strong>”的搜索结果：</p>
        </c:if>
        <div class="card-grid" id="card-grid" data-offset="${fn:length(cards)}" data-limit="12">
            <c:forEach var="card" items="${cards}">
                <a href="card/${card.uniqueLinkId}" class="p-card-item-link">
                    <div class="p-card-item">
                        <%-- 构建图片 URL，区分绝对路径和相对路径 --%>
                        <c:set var="imgPath" value="${card.cardFrontPath != null ? card.cardFrontPath : 'https://placehold.co/600x400/FFC107/5D4037?text=P-CARD'}" />
                        <c:set var="imgUrl" value="${imgPath.startsWith('http://') || imgPath.startsWith('https://') ? imgPath : pageContext.request.contextPath.concat('/').concat(imgPath)}" />
                        
                        <div class="p-card-img-wrapper">
                            <%-- 模糊背景层 --%>
                            <div class="p-card-img-bg" style="background-image: url('${imgUrl}');"></div>
                            <%-- 清晰前景图片 --%>
                            <img 
                                data-src="${imgUrl}" 
                                alt="${card.producerName} 的名片"
                                class="lazy-load"
                                src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 600 400'%3E%3Crect fill='%23FFF8E1' width='600' height='400'/%3E%3C/svg%3E">
                        </div>
                        <div class="p-card-info">
                            <h3><c:out value="${card.producerName}"/></h3>
                            <p>担当: <c:out value="${card.idolName}"/></p>
                            <p>地区: <c:out value="${card.region}"/></p>
                        </div>
                    </div>
                </a>
            </c:forEach>
            <c:if test="${empty cards}">
                <c:choose>
                    <c:when test="${not empty param.q}">
                        <p>没有找到匹配 “<strong><c:out value='${param.q}'/></strong>” 的名片。</p>
                    </c:when>
                    <c:otherwise>
                        <p>还没有人创建名片，快去注册成为第一个吧！</p>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </div>
        <div id="load-sentinel" style="height: 1px;"></div>
    </div>
    <script>
        // 图片懒加载
        (function() {
            const lazyImages = document.querySelectorAll('img.lazy-load');
            
            if ('IntersectionObserver' in window) {
                const imageObserver = new IntersectionObserver((entries, observer) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            const img = entry.target;
                            img.src = img.dataset.src;
                            img.classList.remove('lazy-load');
                            imageObserver.unobserve(img);
                        }
                    });
                });
                
                lazyImages.forEach(img => imageObserver.observe(img));
            } else {
                // 降级方案：直接加载所有图片
                lazyImages.forEach(img => {
                    img.src = img.dataset.src;
                    img.classList.remove('lazy-load');
                });
            }
        })();
        
        // 无限滚动加载
        (function(){
            const grid = document.getElementById('card-grid');
            const sentinel = document.getElementById('load-sentinel');
            if (!grid || !sentinel) return;

            const ctx = '${pageContext.request.contextPath}';
            const q = '${fn:escapeXml(param.q)}';
            let loading = false;
            let ended = false;

            async function loadMore(){
                if (loading || ended) return;
                loading = true;
                const offset = parseInt(grid.getAttribute('data-offset')) || 0;
                const limit = parseInt(grid.getAttribute('data-limit')) || 12;
                try {
                    const url = new URL(window.location.origin + ctx + '/');
                    url.searchParams.set('format','1');
                    url.searchParams.set('offset', offset);
                    url.searchParams.set('limit', limit);
                    if (q) url.searchParams.set('q', q);
                    const res = await fetch(url.toString(), { headers: { 'X-Requested-With':'XMLHttpRequest' }});
                    const data = await res.json();
                    if (!Array.isArray(data) || data.length === 0) {
                        ended = true;
                        return;
                    }
                    const frag = document.createDocumentFragment();
                    data.forEach(c => {
                        const a = document.createElement('a');
                        a.href = ctx + '/card/' + c.uniqueLinkId;
                        a.className = 'p-card-item-link';
                        const item = document.createElement('div');
                        item.className = 'p-card-item';
                        
                        // 创建图片包装器
                        const imgWrapper = document.createElement('div');
                        imgWrapper.className = 'p-card-img-wrapper';
                        
                        // 创建模糊背景层
                        const imgBg = document.createElement('div');
                        imgBg.className = 'p-card-img-bg';
                        
                        // 构建图片 URL，判断是否为绝对路径
                        let imgSrc;
                        if (c.cardFrontPath) {
                            imgSrc = (c.cardFrontPath.startsWith('http://') || c.cardFrontPath.startsWith('https://')) 
                                ? c.cardFrontPath 
                                : ctx + '/' + c.cardFrontPath;
                        } else {
                            imgSrc = 'https://placehold.co/600x400/FFC107/5D4037?text=P-CARD';
                        }
                        
                        imgBg.style.backgroundImage = `url('${imgSrc}')`;
                        
                        // 创建前景图片
                        const img = document.createElement('img');
                        img.setAttribute('data-src', imgSrc);
                        img.className = 'lazy-load';
                        img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 600 400'%3E%3Crect fill='%23FFF8E1' width='600' height='400'/%3E%3C/svg%3E";
                        img.alt = (c.producerName || '') + ' 的名片';
                        
                        imgWrapper.appendChild(imgBg);
                        imgWrapper.appendChild(img);
                        
                        const info = document.createElement('div'); info.className='p-card-info';
                        info.innerHTML = '<h3>' + escapeHtml(c.producerName||'') + '</h3>' +
                                         '<p>担当: ' + escapeHtml(c.idolName||'') + '</p>' +
                                         '<p>地区: ' + escapeHtml(c.region||'') + '</p>';
                        item.appendChild(imgWrapper); item.appendChild(info); a.appendChild(item); frag.appendChild(a);
                    });
                    grid.appendChild(frag);
                    
                    // 为新添加的图片启用懒加载
                    const newImages = grid.querySelectorAll('img.lazy-load');
                    if ('IntersectionObserver' in window) {
                        const imageObserver = new IntersectionObserver((entries) => {
                            entries.forEach(entry => {
                                if (entry.isIntersecting) {
                                    const img = entry.target;
                                    img.src = img.dataset.src;
                                    img.classList.remove('lazy-load');
                                    imageObserver.unobserve(img);
                                }
                            });
                        });
                        newImages.forEach(img => imageObserver.observe(img));
                    } else {
                        newImages.forEach(img => {
                            img.src = img.dataset.src;
                            img.classList.remove('lazy-load');
                        });
                    }
                    
                    grid.setAttribute('data-offset', offset + data.length);
                } catch(e){
                    // 可选：toast 提示
                }
                loading = false;
            }

            function escapeHtml(s){
                return String(s).replace(/[&<>"]/g, function(c){
                    return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];
                });
            }

            // 若初始没有卡片，停止加载
            if ((parseInt(grid.getAttribute('data-offset')) || 0) === 0) {
                // 初次渲染已由服务端填充，若为空则无需观察
                if (!grid.querySelector('.p-card-item-link')) {
                    ended = true;
                    return;
                }
            }

            const io = new IntersectionObserver((entries)=>{
                entries.forEach(entry=>{
                    if (entry.isIntersecting) {
                        loadMore();
                    }
                });
            }, { rootMargin: '200px' });
            io.observe(sentinel);
        })();
        
        // 🧀 芝士碎屑点击特效 (优化版)
        document.addEventListener('click', function(e) {
            // 使用更深、对比度更高的颜色
            const colors = ['#F4D03F', '#E67E22', '#D4AC0D', '#FFF', '#8D6E63'];
            const particleCount = 24; // 增加粒子数量
            
            for (let i = 0; i < particleCount; i++) {
                const particle = document.createElement('div');
                particle.style.position = 'fixed';
                particle.style.left = e.clientX + 'px';
                particle.style.top = e.clientY + 'px';
                // 随机大小
                const size = Math.random() * 10 + 5;
                particle.style.width = size + 'px';
                particle.style.height = size + 'px';
                particle.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
                // 随机形状：圆形、方形、三角形(用clip-path模拟)
                const shapeType = Math.random();
                if (shapeType > 0.6) {
                    particle.style.borderRadius = '50%';
                } else if (shapeType > 0.3) {
                    particle.style.borderRadius = '2px';
                } else {
                    particle.style.clipPath = 'polygon(50% 0%, 0% 100%, 100% 100%)';
                }
                
                particle.style.pointerEvents = 'none';
                particle.style.zIndex = '2147483647'; // 最高层级
                
                // 物理参数
                const angle = Math.random() * Math.PI * 2;
                // 增加爆炸速度
                const velocity = Math.random() * 150 + 80; 
                const tx = Math.cos(angle) * velocity;
                // 增加重力影响 (y轴向下偏移)
                const ty = Math.sin(angle) * velocity + 100; 
                // 随机旋转
                const rot = Math.random() * 360;
                
                particle.animate([
                    { transform: 'translate(-50%, -50%) translate(0, 0) rotate(0deg) scale(1)', opacity: 1 },
                    { transform: `translate(-50%, -50%) translate(${tx}px, ${ty}px) rotate(${rot}deg) scale(0)`, opacity: 0 }
                ], {
                    duration: Math.random() * 800 + 500, // 持续时间稍长
                    easing: 'cubic-bezier(0.25, 1, 0.5, 1)' // 减速曲线
                }).onfinish = () => particle.remove();
                
                document.body.appendChild(particle);
            }
        });
    </script>
    <script src="js/script.js"></script>
</body>
</html>