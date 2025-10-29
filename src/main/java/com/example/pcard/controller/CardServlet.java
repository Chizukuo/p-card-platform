package com.example.pcard.controller;

import com.example.pcard.dao.CardDao;
import com.example.pcard.model.Card;
import com.example.pcard.model.User;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * 名片操作控制器
 * 处理名片的创建、更新、删除操作
 */
@WebServlet("/cardAction")
@MultipartConfig
public class CardServlet extends HttpServlet {
    private final CardDao cardDao = new CardDao();
    private final Gson gson = new Gson();

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L; // 5MB
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_CODE_LENGTH = 7;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect("dashboard");
            return;
        }

        try {
            switch (action) {
                case "create":
                    createCard(request, user);
                    break;
                case "update":
                    updateCard(request, user);
                    break;
                case "delete":
                    deleteCard(request, user);
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException("Database operation failed", e);
        }

        // Deleting from admin panel should redirect back to admin panel
        if (user.isAdmin() && "delete".equals(action)) {
            response.sendRedirect("admin");
        } else {
            response.sendRedirect("dashboard");
        }
    }

    /**
     * 保存上传的文件
     * @param request HTTP请求
     * @param partName 文件字段名
     * @return 保存后的文件路径
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    private String saveUploadedFile(HttpServletRequest request, String partName) throws IOException, ServletException {
        Part filePart = request.getPart(partName);
        if (filePart != null && filePart.getSize() > 0) {
            // 文件大小验证
            if (filePart.getSize() > MAX_FILE_SIZE) {
                throw new ServletException("Uploaded file is too large");
            }

            String submitted = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // 文件名清理
            String sanitized = submitted.replaceAll("[^A-Za-z0-9._-]", "_");
            String ext = "";
            int dot = sanitized.lastIndexOf('.');
            if (dot >= 0) {
                ext = sanitized.substring(dot).toLowerCase();
            }

            // 允许的文件扩展名
            Set<String> allowedExt = new HashSet<>();
            allowedExt.add(".png");
            allowedExt.add(".jpg");
            allowedExt.add(".jpeg");
            allowedExt.add(".gif");
            allowedExt.add(".webp");

            if (!allowedExt.contains(ext)) {
                throw new ServletException("Unsupported file type");
            }

            // read magic bytes to verify file type
            try (java.io.InputStream is = filePart.getInputStream()) {
                byte[] header = new byte[12];
                int read = is.read(header);
                if (read < 8) {
                    throw new ServletException("Uploaded file is not a valid image");
                }
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                if (header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                    // png ok
                } else if (header[0] == (byte)0xFF && header[1] == (byte)0xD8) {
                    // jpeg ok
                } else if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F') {
                    // gif ok
                } else if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                    // webp ok
                } else {
                    throw new ServletException("Uploaded file header does not match allowed image types");
                }
            }

            String uniqueFileName = java.util.UUID.randomUUID().toString() + ext;
            
            // 使用环境变量或固定路径（Cloud Run 挂载 GCS 到 /uploads）
            String uploadPath = System.getenv("UPLOAD_DIR");
            if (uploadPath == null) {
                uploadPath = "/uploads";  // Cloud Run 默认挂载路径
            }
            
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();
            
            // write the part to disk
            String fullPath = uploadPath + File.separator + uniqueFileName;
            filePart.write(fullPath);
            
            // 如果使用 GCS，返回公开 URL（包含 uploads/ 前缀）
            String bucketName = System.getenv("GCS_BUCKET_NAME");
            if (bucketName != null && "true".equals(System.getenv("USE_EXTERNAL_STORAGE"))) {
                return "https://storage.googleapis.com/" + bucketName + "/uploads/" + uniqueFileName;
            }
            
            // 否则返回相对路径
            return "uploads/" + uniqueFileName;
        }
        return null;
    }

    /**
     * Detect image orientation based on width and height
     * @param imagePath the absolute path to the image file
     * @return "HORIZONTAL" if width >= height, "VERTICAL" otherwise
     */
    private String detectImageOrientation(String imagePath) {
        try {
            File imageFile = new File(getServletContext().getRealPath("") + File.separator + imagePath.replace('/', File.separatorChar));
            if (!imageFile.exists()) {
                return "HORIZONTAL"; // Default to horizontal if file doesn't exist
            }
            
            BufferedImage image = ImageIO.read(imageFile);
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();
                return width >= height ? "HORIZONTAL" : "VERTICAL";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "HORIZONTAL"; // Default to horizontal on error
    }

    private void populateCardFromRequest(Card card, HttpServletRequest request) throws IOException, ServletException {
        card.setProducerName(request.getParameter("producerName"));
        card.setRegion(request.getParameter("region"));
        card.setIdolName(request.getParameter("idolName"));
        String visibility = request.getParameter("visibility");
        if (visibility != null) {
            card.setVisibility(visibility);
        }

        List<Card.SnsLink> snsLinks = new ArrayList<>();
        String[] customNames = request.getParameterValues("customSnsName");
        String[] customValues = request.getParameterValues("customSnsValue");
        if (customNames != null && customValues != null) {
            for (int i = 0; i < customNames.length; i++) {
                if (!customNames[i].isEmpty() && !customValues[i].isEmpty()) {
                    Card.SnsLink link = new Card.SnsLink();
                    link.setName(customNames[i]);
                    link.setValue(customValues[i]);
                    snsLinks.add(link);
                }
            }
        }
        card.setCustomSns(gson.toJson(snsLinks));
    }

    private void createCard(HttpServletRequest request, User user) throws SQLException, IOException, ServletException {
        Card card = new Card();
        card.setUserId(user.getId());
        populateCardFromRequest(card, request);

        String frontPath = saveUploadedFile(request, "cardFront");
        String backPath = saveUploadedFile(request, "cardBack");
        card.setCardFrontPath(frontPath);
        card.setCardBackPath(backPath);
        
        // Auto-detect image orientation from the front image (if available)
        if (frontPath != null) {
            String orientation = detectImageOrientation(frontPath);
            card.setImageOrientation(orientation);
        } else if (backPath != null) {
            // If no front image, use back image
            String orientation = detectImageOrientation(backPath);
            card.setImageOrientation(orientation);
        } else {
            // Default to horizontal if no images
            card.setImageOrientation("HORIZONTAL");
        }
        
        // ensure shortCode for PUBLIC and LINK_ONLY; ensure shareToken for LINK_ONLY
        if ("LINK_ONLY".equalsIgnoreCase(card.getVisibility())) {
            if (card.getShareToken() == null || card.getShareToken().isEmpty()) {
                card.setShareToken(java.util.UUID.randomUUID().toString());
            }
        }
        if ("PUBLIC".equalsIgnoreCase(card.getVisibility()) || "LINK_ONLY".equalsIgnoreCase(card.getVisibility())) {
            if (card.getShortCode() == null || card.getShortCode().isEmpty()) {
                String code = generateShortCode();
                Card exists = null;
                int attempts = 0;
                while (attempts < 5) {
                    try {
                        exists = cardDao.getCardByShortCode(code);
                    } catch (Exception ignored) { exists = null; }
                    if (exists == null) break;
                    code = generateShortCode();
                    attempts++;
                }
                card.setShortCode(code);
            }
        }

        cardDao.addCard(card);
    }

    private void updateCard(HttpServletRequest request, User user) throws SQLException, IOException, ServletException {
        int cardId = Integer.parseInt(request.getParameter("cardId"));
        Card card = cardDao.getCardById(cardId);

        if (card == null || (card.getUserId() != user.getId() && !user.isAdmin())) {
            return;
        }

        populateCardFromRequest(card, request);

        // handle replacement: if a new file is uploaded, delete old file from disk
        String frontPath = saveUploadedFile(request, "cardFront");
        String backPath = saveUploadedFile(request, "cardBack");
        String uploadBase = getServletContext().getRealPath("") + File.separator;
        
        boolean orientationChanged = false;
        if (frontPath != null) {
            // delete old
            if (card.getCardFrontPath() != null) {
                File old = new File(uploadBase + card.getCardFrontPath().replace('/', File.separatorChar));
                if (old.exists()) old.delete();
            }
            card.setCardFrontPath(frontPath);
            orientationChanged = true;
        }
        if (backPath != null) {
            if (card.getCardBackPath() != null) {
                File old = new File(uploadBase + card.getCardBackPath().replace('/', File.separatorChar));
                if (old.exists()) old.delete();
            }
            card.setCardBackPath(backPath);
            if (!orientationChanged) {
                orientationChanged = true;
            }
        }
        
        // Re-detect orientation if any image was changed
        if (orientationChanged) {
            if (card.getCardFrontPath() != null) {
                String orientation = detectImageOrientation(card.getCardFrontPath());
                card.setImageOrientation(orientation);
            } else if (card.getCardBackPath() != null) {
                String orientation = detectImageOrientation(card.getCardBackPath());
                card.setImageOrientation(orientation);
            }
        }

        // ensure shortCode for PUBLIC and LINK_ONLY; ensure shareToken for LINK_ONLY
        if ("LINK_ONLY".equalsIgnoreCase(card.getVisibility())) {
            if (card.getShareToken() == null || card.getShareToken().isEmpty()) {
                card.setShareToken(java.util.UUID.randomUUID().toString());
            }
        }
        if ("PUBLIC".equalsIgnoreCase(card.getVisibility()) || "LINK_ONLY".equalsIgnoreCase(card.getVisibility())) {
            if (card.getShortCode() == null || card.getShortCode().isEmpty()) {
                String code = generateShortCode();
                Card exists = null;
                int attempts = 0;
                while (attempts < 5) {
                    try {
                        exists = cardDao.getCardByShortCode(code);
                    } catch (Exception ignored) { exists = null; }
                    if (exists == null) break;
                    code = generateShortCode();
                    attempts++;
                }
                card.setShortCode(code);
            }
        }

        cardDao.updateCard(card);
    }

    private void deleteCard(HttpServletRequest request, User user) throws SQLException {
        int cardId = Integer.parseInt(request.getParameter("cardId"));
        Card card = cardDao.getCardById(cardId);

        if (card != null && (card.getUserId() == user.getId() || user.isAdmin())) {
            // delete files from disk if present
            try {
                String uploadBase = getServletContext().getRealPath("") + File.separator;
                if (card.getCardFrontPath() != null) {
                    File f = new File(uploadBase + card.getCardFrontPath().replace('/', File.separatorChar));
                    if (f.exists()) f.delete();
                }
                if (card.getCardBackPath() != null) {
                    File b = new File(uploadBase + card.getCardBackPath().replace('/', File.separatorChar));
                    if (b.exists()) b.delete();
                }
            } catch (Exception ignored) {}
            cardDao.deleteCard(cardId);
        }
    }

    /**
     * 生成短代码
     * @return 7位随机字符串
     */
    private String generateShortCode() {
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(BASE62.charAt(rnd.nextInt(BASE62.length())));
        }
        return sb.toString();
    }
}