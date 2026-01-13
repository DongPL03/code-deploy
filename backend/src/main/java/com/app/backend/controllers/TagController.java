package com.app.backend.controllers;

import com.app.backend.dtos.community.TagDTO;
import com.app.backend.responses.ResponseObject;
import com.app.backend.responses.community.TagResponse;
import com.app.backend.services.community.ITagRedisService;
import com.app.backend.services.community.ITagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/tags")
@RequiredArgsConstructor
public class TagController {

    private final ITagService tagService;
    private final ITagRedisService tagRedisService;

    /**
     * Lấy tất cả tags đang hiển thị (public)
     */
    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getAllTags() {
        // 1. Check Redis cache first
        List<TagResponse> tags = tagRedisService.getAllTags();
        
        if (tags == null) {
            // 2. Cache miss -> Query DB
            tags = tagService.getAllTags();
            // 3. Save to cache
            tagRedisService.saveAllTags(tags);
        }
        
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách tags thành công")
                .data(tags)
                .build());
    }

    /**
     * Lấy tất cả tags bao gồm ẩn (admin)
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getAllTagsAdmin() {
        List<TagResponse> tags = tagService.getAllTagsAdmin();
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy danh sách tags thành công")
                .data(tags)
                .build());
    }

    /**
     * Lấy tag theo ID
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getTagById(@PathVariable Long id) {
        try {
            TagResponse tag = tagService.getTagById(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy tag thành công")
                    .data(tag)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Lấy tag theo slug
     */
    @GetMapping("/slug/{slug}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getTagBySlug(@PathVariable String slug) {
        try {
            TagResponse tag = tagService.getTagBySlug(slug);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy tag thành công")
                    .data(tag)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Lấy tags phổ biến
     */
        @GetMapping("/popular")
        @Transactional(readOnly = true)
        public ResponseEntity<ResponseObject> getPopularTags(
            @RequestParam(defaultValue = "10") int limit) {
        List<TagResponse> tags = tagService.getPopularTags(limit);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy tags phổ biến thành công")
                .data(tags)
                .build());
    }

    /**
     * Tìm kiếm tags
     */
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> searchTags(@RequestParam String keyword) {
        List<TagResponse> tags = tagService.searchTags(keyword);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Tìm kiếm tags thành công")
                .data(tags)
                .build());
    }

    /**
     * Tạo tag mới (Admin only)
     */
    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject> createTag(@Valid @RequestBody TagDTO dto) {
        try {
            TagResponse tag = tagService.createTag(dto);
            // Clear cache khi có thay đổi
            tagRedisService.clearTagCache();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.CREATED)
                            .message("Tạo tag thành công")
                            .data(tag)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Cập nhật tag (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDTO dto) {
        try {
            TagResponse tag = tagService.updateTag(id, dto);
            // Clear cache khi có thay đổi
            tagRedisService.clearTagCache();
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Cập nhật tag thành công")
                    .data(tag)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Xóa tag - soft delete (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject> deleteTag(@PathVariable Long id) {
        try {
            tagService.deleteTag(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Xóa tag thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Toggle ẩn/hiện tag (Admin only)
     */
    @PatchMapping("/{id}/toggle-visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject> toggleVisibility(@PathVariable Long id) {
        try {
            TagResponse tag = tagService.toggleVisibility(id);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Cập nhật trạng thái tag thành công")
                    .data(tag)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }
}
