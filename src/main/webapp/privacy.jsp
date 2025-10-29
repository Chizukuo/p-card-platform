<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>隐私政策 - P-Card 平台</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .privacy-container { max-width: 860px; margin: 40px auto; padding: 0 16px; }
        .privacy-container h1 { margin-bottom: 12px; }
        .privacy-container p { line-height: 1.7; color: var(--text-secondary); }
        .privacy-container h2 { margin-top: 24px; }
        .notice { background: rgba(0,0,0,0.04); padding: 12px 14px; border-radius: 8px; margin: 12px 0; }
    </style>
</head>
<body>
<div class="privacy-container">
    <h1>隐私政策</h1>
    <div class="notice">
        <strong>说明：</strong>本网站仅供学习演示使用，是 <strong>小芝士_chizukuo</strong> 的 Java Web 课程作业，不构成任何商业服务或承诺。
    </div>

    <p style="color: var(--text-secondary); margin: 8px 0 20px;">生效日期：2025-10-29（版本 1.0）</p>

    <h2>1. 我们收集的信息</h2>
    <p>为提供基本功能（注册登录、名片展示、留言互动），我们可能收集并处理以下信息：</p>
    <ul>
        <li>账户信息：用户名、昵称、密码（采用单向哈希存储）。</li>
        <li>用户提交内容：名片图片、文字信息（如 P 名、担当、地区、社交链接等）。</li>
        <li>技术与日志信息：访问时间、请求路径、浏览器 User-Agent、IP 地址（可能通过反向代理/Cloudflare 提供），用于安全、反滥用与故障排查。</li>
        <li>必需 Cookie：用于会话管理（如 JSESSIONID），以保持登录状态和基础功能。</li>
    </ul>

    <h2>2. 我们如何使用信息</h2>
    <ul>
        <li>提供和维护站内功能：账户注册、登录认证、名片上传与展示、评论/留言。</li>
        <li>安全与防滥用：速率限制、防机器人访问、异常检测与日志记录。</li>
        <li>改进与调试：依据错误日志和匿名化指标定位问题、提升体验。</li>
        <li>页面可能展示广告：如有广告展示，将以上下文或非个性化方式呈现，<strong>不基于您的个人数据进行定向</strong>，也不会出售或出借您的个人信息用于广告目的。</li>
    </ul>
    <p>本项目用于教学演示；如启用广告，亦遵循“最小化收集、非个性化投放”的原则。</p>

    <h2>3. Cookie 与本地存储</h2>
    <p>我们仅使用维持核心功能所必需的 Cookie（例如会话 Cookie）。这些 Cookie 主要用于识别登录状态，不用于跨站跟踪或广告投放。</p>

    <h2>4. 第三方服务与数据存储</h2>
    <p>为实现图片存储与访问，项目可能接入外部对象存储或 CDN（例如 Google Cloud Storage、AWS S3、阿里云 OSS、Azure Blob 或自建 CDN）。上传内容可能通过这些服务以公开 URL 形式提供访问。</p>
    <ul>
        <li>请勿上传包含敏感个人信息的图片或文字。</li>
        <li>在使用第三方存储/CDN 时，访问日志可能由第三方依据其政策留存。</li>
        <li>如启用 Cloudflare 等反向代理/安全服务，请求会经过该服务并受其隐私政策约束。</li>
        <li>如未来接入第三方广告系统，将采用<strong>非个性化</strong>模式，不向广告方共享可识别的个人数据。</li>
    </ul>

    <h2>5. 信息的共享</h2>
    <p>除以下情形外，我们不会向第三方出售或出借您的个人信息：</p>
    <ul>
        <li>法律法规、司法机关或监管部门要求提供时。</li>
        <li>为保护本站与用户的合法权益，在必要且合理的范围内披露。</li>
    </ul>

    <h2>6. 数据保留与删除</h2>
    <ul>
        <li>账户与名片内容：在您主动删除或申请删除前，会持续保留以供功能使用。</li>
        <li>日志与安全记录：为保障安全与排错需要，会在合理期限内保留并定期清理。</li>
        <li>您可以通过站内功能删除名片，或删除账户（如提供此功能）；若需协助，可联系维护者处理。</li>
    </ul>

    <h2>7. 未成年人保护</h2>
    <p>本项目面向一般受众且仅作课程作业展示。若您未满法定年龄，请在监护人指导下使用，并避免提交个人敏感信息。</p>

    <h2>8. 跨境传输</h2>
    <p>若使用的第三方存储/CDN 或代理服务的服务器位于境外，您的数据可能被传输并处理于境外法域。我们仅出于功能实现与演示目的进行最小化必要处理。</p>

    <h2>9. 您的权利</h2>
    <ul>
        <li>访问与更正：您可在可用的范围内查看与编辑提交的内容。</li>
        <li>删除：可在站内删除名片、评论；如需删除账户或其他数据，可联系维护者。</li>
        <li>撤回同意：您可停止使用服务并删除内容；必要 Cookie 仅用于维持功能。</li>
    </ul>

    <h2>10. 信息安全</h2>
    <p>我们采用合理的技术与管理措施（如密码单向哈希、基础速率限制与异常处理）保护数据安全。但由于网络与系统风险客观存在，我们不对不可抗力导致的损失承担责任。</p>
    <p><strong>重要提示：</strong>请勿上传或填写敏感个人信息，例如身份证号、护照号、银行卡/支付信息、精确住址、电话号码、电子邮箱、健康与医疗信息、生物识别信息等。一经提交的公开内容（如名片图片与公开字段）可能被他人复制或转发，请谨慎发布。</p>

    <h2>11. 联系方式</h2>
    <p>如对本政策或数据处理有疑问，请在站内留言反馈，或在本项目仓库提交反馈（如 Issues）。作为课程作业，响应可能不如商业服务及时，敬请理解。</p>

    <h2>12. 本政策的变更</h2>
    <p>我们可能随课程进度适时更新本政策。重大调整会更新页面版本与日期。继续使用即表示您接受更新后的政策。</p>

    <p style="margin-top: 24px;"><a href="/" style="color: var(--text-secondary);">← 返回首页</a></p>
</div>
</body>
</html>
