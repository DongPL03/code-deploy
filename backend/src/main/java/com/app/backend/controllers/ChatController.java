package com.app.backend.controllers;

import com.app.backend.components.SecurityUtils;
import com.app.backend.dtos.SendMessageRequest;
import com.app.backend.responses.PageResponse;
import com.app.backend.responses.ResponseObject;
import com.app.backend.responses.chat.ChatInboxItemResponse;
import com.app.backend.responses.chat.ChatMessageResponse;
import com.app.backend.services.tinnhan.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;
    private final SecurityUtils securityUtils;

    /**
     * 📩 Gửi tin nhắn 1-1
     * POST /api/v1/chat/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ResponseObject> sendMessage(
            @Valid @RequestBody SendMessageRequest dto
    ) throws Exception {
        Long currentUserId = securityUtils.getLoggedInUserId();

        ChatMessageResponse data = chatService.sendMessage(currentUserId, dto);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Gửi tin nhắn thành công")
                        .data(data)
                        .build()
        );
    }

    /**
     * 💬 Lịch sử hội thoại 1-1
     * GET /api/v1/chat/conversation?friend_user_id=2&page=0&limit=20
     */
    @GetMapping("/conversation")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getConversation(
            @RequestParam("friend_user_id") Long friendUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {
        Long currentUserId = securityUtils.getLoggedInUserId();

        PageRequest pageRequest = PageRequest.of(page, limit);
        PageResponse<ChatMessageResponse> data =
                chatService.getConversation(currentUserId, friendUserId, pageRequest);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Lấy lịch sử hội thoại thành công")
                        .data(data)
                        .build()
        );
    }

    /**
     * 📥 Inbox – tin nhắn gần đây (optional)
     * GET /api/v1/chat/inbox?page=0&limit=20
     */
    @GetMapping("/inbox")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {
        Long currentUserId = securityUtils.getLoggedInUserId();

        PageRequest pageRequest = PageRequest.of(page, limit);
        PageResponse<ChatMessageResponse> data =
                chatService.getMyInbox(currentUserId, pageRequest);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Lấy inbox tin nhắn thành công")
                        .data(data)
                        .build()
        );
    }

    /**
     * 📥 Inbox v2 – Lấy danh sách cuộc trò chuyện gần đây với thông tin partner
     * GET /api/v1/chat/inbox-list?limit=10
     */
    @GetMapping("/inbox-list")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getInboxList(
            @RequestParam(defaultValue = "10") int limit
    ) throws Exception {
        Long currentUserId = securityUtils.getLoggedInUserId();

        List<ChatInboxItemResponse> data = chatService.getInboxWithPartnerInfo(currentUserId, limit);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Lấy danh sách cuộc trò chuyện thành công")
                        .data(data)
                        .build()
        );
    }

    /**
     * 📤 Upload file cho chat (ảnh, file, audio)
     * POST /api/v1/chat/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ResponseObject> uploadChatFile(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("File không được để trống")
                                .build());
            }

            // Giới hạn kích thước file: 10MB
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ResponseObject.builder()
                                .status(HttpStatus.BAD_REQUEST)
                                .message("File không được vượt quá 10MB")
                                .build());
            }

            String contentType = file.getContentType();
            String subFolder;

            // Xác định thư mục lưu dựa vào loại file
            if (contentType != null && contentType.startsWith("image/")) {
                subFolder = "images";
            } else if (contentType != null && contentType.startsWith("audio/")) {
                subFolder = "audios";
            } else {
                subFolder = "files";
            }

            // Tạo tên file unique
            String originalFilename = StringUtils.cleanPath(
                    Objects.requireNonNull(file.getOriginalFilename())
            );
            String extension = FilenameUtils.getExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + "_" + System.currentTimeMillis() + "." + extension;

            // Tạo thư mục nếu chưa có
            Path uploadPath = Paths.get("uploads", "chat", subFolder);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Lưu file
            Path destination = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn file (chỉ subFolder/filename, không có prefix chat/)
            String fileUrl = subFolder + "/" + uniqueFilename;

            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .status(HttpStatus.OK)
                            .message("Upload file thành công")
                            .data(fileUrl)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi upload file: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 📥 Lấy file chat
     * GET /api/v1/chat/files/** (supports paths like images/file.png or chat/images/file.png)
     */
    @GetMapping("/files/**")
    public ResponseEntity<?> getChatFile(
            jakarta.servlet.http.HttpServletRequest request
    ) {
        try {
            // Extract the path after /files/
            String fullPath = request.getRequestURI();
            String filesPrefix = "/chat/files/";
            int idx = fullPath.indexOf(filesPrefix);
            if (idx == -1) {
                return ResponseEntity.notFound().build();
            }
            String relativePath = fullPath.substring(idx + filesPrefix.length());
            
            // Handle both old format (chat/images/file.png) and new format (images/file.png)
            Path filePath;
            if (relativePath.startsWith("chat/")) {
                // Old format: chat/images/file.png -> uploads/chat/images/file.png
                filePath = Paths.get("uploads", relativePath);
            } else {
                // New format: images/file.png -> uploads/chat/images/file.png
                filePath = Paths.get("uploads", "chat", relativePath);
            }
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Get original filename from the stored file
            String storedFileName = filePath.getFileName().toString();
            // Remove UUID prefix if exists (format: uuid_timestamp.extension)
            String originalFileName = storedFileName;
            if (storedFileName.contains("_")) {
                // Try to preserve the extension
                int lastDotIdx = storedFileName.lastIndexOf('.');
                String extension = lastDotIdx > 0 ? storedFileName.substring(lastDotIdx) : "";
                originalFileName = "file" + extension;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"" + storedFileName + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
