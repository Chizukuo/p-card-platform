function copyToClipboard(text, element) {
    // 返回一个 promise，以便调用方能控制 UI 反馈
    
    // 检测iOS设备
    const isIOS = /ipad|iphone|ipod/i.test(navigator.userAgent);
    
    // iOS设备直接使用降级方案，因为Clipboard API在iOS上不可靠
    if (isIOS) {
        console.log('iOS设备，使用降级复制方案');
        return fallbackCopyTextToClipboard(text);
    }
    
    // 非iOS设备：优先使用 navigator.clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
        return navigator.clipboard.writeText(text).catch(err => {
            console.warn('Clipboard API 失败，尝试降级方案: ', err);
            // 如果失败，降级使用传统方法
            return fallbackCopyTextToClipboard(text);
        });
    } else {
        // 不支持 Clipboard API，直接使用降级方案
        return fallbackCopyTextToClipboard(text);
    }
}

// 降级复制方案（兼容iOS Safari）
function fallbackCopyTextToClipboard(text) {
    return new Promise((resolve, reject) => {
        // 检测是否为iOS设备
        const isIOS = /ipad|iphone|ipod/i.test(navigator.userAgent);
        
        // iOS需要使用input而不是textarea，并且不能使用readonly
        const element = document.createElement(isIOS ? 'input' : 'textarea');
        element.value = text;
        
        // 关键：iOS Safari需要这些特定的样式设置
        element.style.position = 'absolute';
        element.style.left = '-9999px';
        element.style.top = (window.pageYOffset || document.documentElement.scrollTop) + 'px';
        element.style.fontSize = '16px'; // iOS需要至少16px防止自动缩放
        element.style.border = '0';
        element.style.padding = '0';
        element.style.margin = '0';
        element.style.opacity = '0';
        
        // iOS不能设置readonly，需要contentEditable
        if (isIOS) {
            element.contentEditable = true;
            element.readOnly = false;
        } else {
            element.readOnly = true;
        }
        
        document.body.appendChild(element);
        
        let successful = false;
        
        try {
            if (isIOS) {
                // iOS专用选择方法
                const range = document.createRange();
                range.selectNodeContents(element);
                const selection = window.getSelection();
                selection.removeAllRanges();
                selection.addRange(range);
                element.setSelectionRange(0, text.length);
                
                // 必须focus才能复制
                element.focus();
                
                // 短暂延迟确保选择生效（iOS特性）
                setTimeout(() => {
                    try {
                        successful = document.execCommand('copy');
                        console.log('iOS execCommand result:', successful);
                    } catch (e) {
                        console.error('iOS execCommand error:', e);
                    }
                    
                    // 清理
                    selection.removeAllRanges();
                    document.body.removeChild(element);
                    
                    if (successful) {
                        resolve();
                    } else {
                        reject(new Error('iOS复制失败'));
                    }
                }, 100);
            } else {
                // 非iOS设备的标准方法
                element.focus();
                element.select();
                
                successful = document.execCommand('copy');
                document.body.removeChild(element);
                
                if (successful) {
                    resolve();
                } else {
                    reject(new Error('execCommand copy 失败'));
                }
            }
        } catch (err) {
            if (document.body.contains(element)) {
                document.body.removeChild(element);
            }
            console.error('降级复制方案失败:', err);
            reject(err);
        }
    });
}

// --- 动态添加/删除社交链接 ---
function addCustomSns() {
    const container = document.getElementById('custom-sns-container');
    const newItem = document.createElement('div');
    newItem.className = 'custom-sns-item';
    newItem.innerHTML = `
        <input type="text" name="customSnsName" placeholder="平台 (e.g. 微博, Twitter, QQ)" class="form-control" required>
        <input type="text" name="customSnsValue" placeholder="主页链接或号码" class="form-control" required>
        <button type="button" class="btn btn-danger btn-sm" onclick="removeCustomSns(this)">移除</button>
    `;
    container.appendChild(newItem);
}

function removeCustomSns(button) {
    button.parentElement.remove();
}


// --- 智能图标匹配和生成 ---
function generateSnsIcons(data) {
    const container = document.getElementById('sns-links-container');
    if (!container || !data) return;

    // 清空现有图标
    container.innerHTML = '';

    const iconMap = {
        'weibo': 'fab fa-weibo', '微博': 'fab fa-weibo',
        'twitter': 'fab fa-twitter', '推特': 'fab fa-twitter', 'x.com': 'fab fa-twitter',
        'instagram': 'fab fa-instagram', 'ins': 'fab fa-instagram',
        'bilibili': 'fab fa-bilibili', 'b站': 'fab fa-bilibili',
        'qq': 'fab fa-qq',
        'github': 'fab fa-github',
        'linkedin': 'fab fa-linkedin',
        'facebook': 'fab fa-facebook',
        'lofter': 'fas fa-feather-alt' // 示例自定义
    };

    data.forEach(item => {
        let iconClass = 'fas fa-link'; // 默认图标
        let isLink = true;
        let href = item.value ? item.value.trim() : '';
        const lowerCaseName = (item.name || '').toLowerCase();
        const lowerCaseValue = (item.value || '').toLowerCase();

        // 匹配图标
        for (const key in iconMap) {
            if (lowerCaseName.includes(key) || lowerCaseValue.includes(key)) {
                iconClass = iconMap[key];
                break;
            }
        }

        // 自动补全常见用户名到完整链接（如果看起来像用户名而非完整URL）
        // 简单规则：如果不包含协议或点并且长度合理，则按平台模板补全
        function looksLikeUsername(v) {
            if (!v) return false;
            if (/^(https?:\/\/|mailto:)/i.test(v)) return false;
            // 包含空格或@视为用户名也允许（如微博@user）
            return !/\./.test(v);
        }

        if (isLink && looksLikeUsername(href)) {
            const val = href.replace(/^@/, '');
            if (lowerCaseName.includes('weibo') || lowerCaseName.includes('微博')) {
                href = `https://weibo.com/${val}`;
            } else if (lowerCaseName.includes('twitter') || lowerCaseName.includes('x.com') || lowerCaseName.includes('推特')) {
                href = `https://x.com/${val}`;
            } else if (lowerCaseName.includes('instagram') || lowerCaseName.includes('ins')) {
                href = `https://www.instagram.com/${val}`;
            } else if (lowerCaseName.includes('github')) {
                href = `https://github.com/${val}`;
            } else if (lowerCaseName.includes('linkedin')) {
                href = `https://www.linkedin.com/in/${val}`;
            } else if (lowerCaseName.includes('bilibili') || lowerCaseName.includes('b站')) {
                // bilibili 用户主页可能是 /{id} 或 /user/{id}
                href = `https://space.bilibili.com/${val}`;
            }
        }

        // 特殊处理QQ：如果平台名包含 qq 且值为纯数字 (5-12位)，则不要生成链接，而生成可点击复制元素
        const qqDigitsMatch = href.match(/^\d{5,12}$/);
        if (lowerCaseName.includes('qq') && qqDigitsMatch) {
            isLink = false;
        }

        // 确保是有效的URL（如果仍当作链接）
        if (isLink && href && !/^(https?:\/\/|mailto:)/i.test(href)) {
             href = 'http://' + href;
        }

        const element = isLink
            ? document.createElement('a')
            : document.createElement('button');

    element.className = 'sns-icon';
    // 为不同平台加上小类，方便样式覆盖
    if (lowerCaseName.includes('weibo') || lowerCaseValue.includes('weibo') || lowerCaseName.includes('微博')) element.classList.add('sns-weibo');
    if (lowerCaseName.includes('twitter') || lowerCaseValue.includes('twitter') || lowerCaseName.includes('x.com')) element.classList.add('sns-twitter');
    if (lowerCaseName.includes('github') || lowerCaseValue.includes('github')) element.classList.add('sns-github');
    if (lowerCaseName.includes('qq') || lowerCaseValue.includes('qq')) element.classList.add('sns-qq');
    
    // 添加 title 属性用于浏览器原生提示
    element.setAttribute('title', `${item.name}: ${item.value}`);
    
        if(isLink) {
            element.href = href;
            element.target = '_blank';
            element.rel = 'noopener noreferrer';
        } else {
            // 对于非链接（例如QQ复制），使用 button 并绑定复制事件
            element.type = 'button';
            // 存储要复制的文本到 data 属性，避免闭包问题
            element.setAttribute('data-copy-text', qqDigitsMatch ? qqDigitsMatch[0] : item.value);
            
            // 使用 touchstart 和 click 双重绑定以提高iOS兼容性
            const handleCopy = function(e) {
                e.preventDefault();
                e.stopPropagation();
                
                const copyText = this.getAttribute('data-copy-text');
                const isIOS = /ipad|iphone|ipod/i.test(navigator.userAgent);
                console.log('准备复制:', copyText, 'iOS设备:', isIOS);
                
                // 移除之前的copied类
                this.classList.remove('copied');
                
                // iOS要求在用户事件中同步执行，不能有太多异步操作
                copyToClipboard(copyText, this).then(() => {
                    console.log('复制成功');
                    // 添加短时徽章样式
                    this.classList.add('copied');
                    setTimeout(() => this.classList.remove('copied'), 1600);
                    showToast('已复制: ' + copyText);
                }).catch((err) => {
                    console.error('复制失败:', err);
                    // iOS上如果复制失败，显示模态框让用户手动复制
                    if (isIOS) {
                        showCopyModal(copyText);
                    } else {
                        showToast('复制失败，QQ号: ' + copyText + ' (请手动记录)', 3500);
                    }
                });
            };
            
            // 同时监听 click 和 touchend，提高iOS响应性
            element.addEventListener('click', handleCopy, false);
            // iOS上touchend可能更可靠
            if (/ipad|iphone|ipod/i.test(navigator.userAgent)) {
                element.addEventListener('touchend', function(e) {
                    // 防止触发两次
                    e.preventDefault();
                    handleCopy.call(this, e);
                }, false);
            }
        }
        element.innerHTML = `<i class="${iconClass}"></i>`;
        container.appendChild(element);
    });

    // 不再需要附加工具提示事件
}


// --- 工具提示逻辑 ---
function attachTooltipEvents() {
    // SNS 图标不再需要 tooltip
    return;
}

// 简单的 toast 功能（右上角短时提示）
function showToast(text, timeout = 1800) {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = text;
    container.appendChild(toast);
    // 强制浏览器应用样式变换
    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => container.removeChild(toast), 220);
    }, timeout);
}

// iOS复制失败时的备选方案：显示复制弹窗
function showCopyModal(text) {
    // 创建模态框
    const modal = document.createElement('div');
    modal.className = 'copy-modal-overlay';
    modal.innerHTML = `
        <div class="copy-modal">
            <div class="copy-modal-header">
                <h3>QQ号</h3>
                <button class="copy-modal-close">&times;</button>
            </div>
            <div class="copy-modal-body">
                <input type="text" class="copy-modal-input" value="${text}" readonly>
                <p class="copy-modal-tip">请长按上方文本框选择"拷贝"</p>
            </div>
            <div class="copy-modal-footer">
                <button class="copy-modal-btn">我已复制</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // 自动选中文本
    const input = modal.querySelector('.copy-modal-input');
    setTimeout(() => {
        input.focus();
        input.select();
    }, 100);
    
    // 关闭按钮
    const closeBtn = modal.querySelector('.copy-modal-close');
    const confirmBtn = modal.querySelector('.copy-modal-btn');
    
    const closeModal = () => {
        modal.style.opacity = '0';
        setTimeout(() => {
            document.body.removeChild(modal);
        }, 200);
    };
    
    closeBtn.addEventListener('click', closeModal);
    confirmBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });
    
    // 动画显示
    requestAnimationFrame(() => {
        modal.style.opacity = '1';
    });
}


// --- DOMContentLoaded 事件监听器 ---
document.addEventListener('DOMContentLoaded', () => {
    // 复制按钮
    const copyButton = document.querySelector('.btn-copy');
    if (copyButton) {
        copyButton.addEventListener('click', () => {
            const linkBox = document.querySelector('.link-box');
            if (linkBox) {
                copyToClipboard(linkBox.textContent.trim(), copyButton);
            }
        });
    }

    // 如果 `snsData` 存在 (在 viewCard.jsp 中定义)，则生成社交图标
    if (typeof snsData !== 'undefined') {
        generateSnsIcons(snsData);
    }

    // 移动端侧栏菜单功能
    initMobileSidebar();
});

// --- 移动端侧栏功能 ---
function initMobileSidebar() {
    const menuBtn = document.getElementById('mobile-menu-btn');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebar-overlay');
    const closeBtn = document.getElementById('sidebar-close');
    
    if (!menuBtn || !sidebar || !overlay) {
        return;
    }
    
    // 打开侧栏
    function openSidebar() {
        sidebar.classList.add('active');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden'; // 防止背景滚动
    }
    
    menuBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        openSidebar();
    });
    
    // 关闭侧栏
    function closeSidebar() {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
        document.body.style.overflow = ''; // 恢复滚动
    }
    
    if (closeBtn) {
        closeBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeSidebar();
        });
    }
    
    overlay.addEventListener('click', closeSidebar);
    
    // 点击侧栏链接后自动关闭（可选）
    const sidebarLinks = sidebar.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(link => {
        link.addEventListener('click', () => {
            setTimeout(closeSidebar, 200); // 延迟关闭，让用户看到点击反馈
        });
    });
    
    // ESC键关闭侧栏
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && sidebar.classList.contains('active')) {
            closeSidebar();
        }
    });
}

// --- 留言板回复功能 ---
function showReplyForm(commentId, username, nickname) {
    // 隐藏所有其他回复表单
    document.querySelectorAll('.reply-form-container').forEach(form => {
        form.style.display = 'none';
    });
    
    // 显示当前回复表单
    const replyForm = document.getElementById('reply-form-' + commentId);
    if (replyForm) {
        replyForm.style.display = 'block';
        // 聚焦到文本框
        const textarea = replyForm.querySelector('textarea');
        if (textarea) {
            textarea.focus();
        }
        
        // 更新 placeholder（优先使用昵称）
        if (textarea) {
            const displayName = nickname || username;
            if (displayName) {
                textarea.placeholder = '回复 @' + displayName + '...';
            }
        }
    }
}

function hideReplyForm(commentId) {
    const replyForm = document.getElementById('reply-form-' + commentId);
    if (replyForm) {
        replyForm.style.display = 'none';
        // 清空文本框
        const textarea = replyForm.querySelector('textarea');
        if (textarea) {
            textarea.value = '';
        }
    }
}

// --- 展开/收起回复功能 ---
function toggleReplies(button) {
    const container = button.closest('.replies-container');
    if (!container) return;
    
    const hiddenReplies = container.querySelectorAll('.reply-wrapper.reply-hidden');
    const isExpanded = container.classList.contains('expanded');
    
    const showText = button.querySelector('.show-text');
    const hideText = button.querySelector('.hide-text');
    const icon = button.querySelector('i');
    
    if (isExpanded) {
        // 收起回复
        container.classList.remove('expanded');
        hiddenReplies.forEach(reply => {
            reply.style.maxHeight = '0';
            reply.style.opacity = '0';
            reply.style.marginBottom = '0';
        });
        
        if (showText) showText.style.display = 'inline';
        if (hideText) hideText.style.display = 'none';
        if (icon) {
            icon.classList.remove('fa-chevron-up');
            icon.classList.add('fa-chevron-down');
        }
        
        // 滚动到按钮位置
        setTimeout(() => {
            button.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }, 100);
    } else {
        // 展开回复
        container.classList.add('expanded');
        
        hiddenReplies.forEach((reply, index) => {
            // 先设置 display，然后用 setTimeout 触发动画
            setTimeout(() => {
                reply.style.maxHeight = reply.scrollHeight + 'px';
                reply.style.opacity = '1';
                reply.style.marginBottom = '6px';
            }, index * 50); // 依次展开，每个延迟50ms
        });
        
        if (showText) showText.style.display = 'none';
        if (hideText) hideText.style.display = 'inline';
        if (icon) {
            icon.classList.remove('fa-chevron-down');
            icon.classList.add('fa-chevron-up');
        }
    }
}

// --- 回到顶部按钮功能 ---
function initBackToTop() {
    // 创建回到顶部按钮
    const backToTopBtn = document.createElement('button');
    backToTopBtn.className = 'back-to-top';
    backToTopBtn.innerHTML = '↑';
    backToTopBtn.setAttribute('aria-label', '回到顶部');
    backToTopBtn.setAttribute('title', '回到顶部');
    document.body.appendChild(backToTopBtn);
    
    // 监听滚动事件
    let isScrolling = false;
    window.addEventListener('scroll', () => {
        if (!isScrolling) {
            window.requestAnimationFrame(() => {
                if (window.scrollY > 300) {
                    backToTopBtn.classList.add('show');
                } else {
                    backToTopBtn.classList.remove('show');
                }
                isScrolling = false;
            });
            isScrolling = true;
        }
    }, { passive: true });
    
    // 点击回到顶部
    backToTopBtn.addEventListener('click', (e) => {
        e.preventDefault();
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

// 页面加载完成后初始化回到顶部按钮
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initBackToTop);
} else {
    initBackToTop();
}

// ===== 时间格式化功能 =====
/**
 * 将UTC时间戳格式化为用户本地时区的相对时间
 * @param {number} timestamp - Unix毫秒时间戳
 * @returns {string} 格式化后的时间字符串
 */
function formatTimestamp(timestamp) {
    if (!timestamp || timestamp === 0) {
        return '未知时间';
    }
    
    const now = Date.now();
    const date = new Date(timestamp);
    const diffMs = now - timestamp;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    
    // 相对时间显示（小于7天）
    if (diffSec < 60) {
        return '刚刚';
    } else if (diffMin < 60) {
        return `${diffMin}分钟前`;
    } else if (diffHour < 24) {
        return `${diffHour}小时前`;
    } else if (diffDay < 7) {
        return `${diffDay}天前`;
    } else {
        // 超过7天显示绝对时间（使用用户本地时区）
        const options = {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        };
        return date.toLocaleString('zh-CN', options);
    }
}

/**
 * 初始化时间格式化 - 遍历所有带data-timestamp属性的元素
 */
function initTimestampFormatting() {
    // 查找所有带data-timestamp属性的元素
    const timestampElements = document.querySelectorAll('[data-timestamp]');
    
    timestampElements.forEach(element => {
        const timestamp = parseInt(element.getAttribute('data-timestamp'), 10);
        if (timestamp > 0) {
            element.textContent = formatTimestamp(timestamp);
            // 添加title属性显示完整时间（鼠标悬停可见）
            element.title = new Date(timestamp).toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            });
        }
    });
}

// 页面加载完成后初始化时间格式化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initTimestampFormatting);
} else {
    initTimestampFormatting();
}

// 定时更新相对时间（每分钟刷新一次）
setInterval(() => {
    initTimestampFormatting();
}, 60000);