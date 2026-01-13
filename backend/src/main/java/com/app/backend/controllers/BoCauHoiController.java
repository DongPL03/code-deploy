package com.app.backend.controllers;

import com.app.backend.components.SecurityUtils;
import com.app.backend.dtos.BoCauHoiDTO;
import com.app.backend.dtos.TuChoiBoCauHoiDTO;
import com.app.backend.dtos.cache.BoCauHoiCacheDTO;
import com.app.backend.dtos.cache.BoCauHoiPageCacheDTO;
import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.exceptions.PermissionDenyException;
import com.app.backend.models.BoCauHoi;
import com.app.backend.repositories.IBoCauHoiRepository;
import com.app.backend.responses.PageResponse;
import com.app.backend.responses.ResponseObject;
import com.app.backend.responses.bocauhoi.BoCauHoiResponse;
import com.app.backend.responses.bocauhoi.UnlockBoCauHoiResponse;
import com.app.backend.services.bocauhoi.IBoCauHoiService;
import com.app.backend.repositories.IKhoaHocBoCauHoiRepository;
import com.app.backend.services.bocauhoi.IQuestionSetRedisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("${api.prefix}/boCauHoi")
@RequiredArgsConstructor
public class BoCauHoiController {
    private static final Logger logger = LoggerFactory.getLogger(BoCauHoiController.class);
    private final IBoCauHoiService boCauHoiService;
    private final IKhoaHocBoCauHoiRepository khoaHocBoCauHoiRepository;
    private final IBoCauHoiRepository boCauHoiRepository;
    private final IQuestionSetRedisService questionSetRedisService;
    private final SecurityUtils securityUtils;

    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getAll(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0", name = "chu_de_id") Long chuDeId,
            @RequestParam(defaultValue = "", name = "che_do_hien_thi") String cheDoHienThi,
            @RequestParam(defaultValue = "", name = "trang_thai") String trangThai,
            @RequestParam(defaultValue = "", name = "loai_su_dung") String loaiSuDung,
            @RequestParam(required = false, name = "muon_tao_tra_phi") Boolean muonTaoTraPhi,
            @RequestParam(defaultValue = "0", name = "nguoi_tao_id") Long nguoiTaoId,
            @RequestParam(required = false, name = "min_rating") Double minRating,
            @RequestParam(required = false, name = "max_rating") Double maxRating,
            @RequestParam(defaultValue = "NEWEST") String sort_order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) { // Redis service đã handle exception nội bộ
        logger.info("🔍 [BoCauHoi] GET request - keyword={}, chuDeId={}, page={}, limit={}", keyword, chuDeId, page, limit);

        Long currentUserId = securityUtils.getLoggedInUserId();
        boolean isAdmin = securityUtils.isAdmin();
        logger.info("🔍 [BoCauHoi] currentUserId={}, isAdmin={}", currentUserId, isAdmin);

        // 1. Cấu hình Sort
        Sort sort = sort_order.equalsIgnoreCase("OLDEST")
                ? Sort.by(Sort.Direction.ASC, "taoLuc")
                : sort_order.equalsIgnoreCase("RATING_DESC")
                ? Sort.by(Sort.Direction.DESC, "trungBinhSao")
                : sort_order.equalsIgnoreCase("RATING_ASC")
                ? Sort.by(Sort.Direction.ASC, "trungBinhSao")
                : Sort.by(Sort.Direction.DESC, "taoLuc");
        PageRequest pageRequest = PageRequest.of(page, limit, sort);

        // 2. CHECK REDIS CACHE TRƯỚC (Cache DTO thay vì Entity)
        BoCauHoiPageCacheDTO cachedPage = questionSetRedisService.getQuestionListFromCache(
                pageRequest, keyword, chuDeId, cheDoHienThi, trangThai, loaiSuDung,
                muonTaoTraPhi, nguoiTaoId, minRating, maxRating, currentUserId, isAdmin
        );

        List<BoCauHoiResponse> responseList;
        long totalElements;
        int totalPages;

        if (cachedPage != null) {
            // ===== CACHE HIT: Convert từ Cache DTO sang Response =====
            logger.info("🔍 [BoCauHoi] CACHE HIT - {} items from Redis", cachedPage.getContent().size());
            responseList = new ArrayList<>();
            for (BoCauHoiCacheDTO cache : cachedPage.getContent()) {
                boolean isOwner = cache.getNguoiTaoId() != null && cache.getNguoiTaoId().equals(currentUserId);
                boolean daMoKhoa = boCauHoiService.hasUserUnlockedBo(cache.getId(), currentUserId);
                
                if (isOwner || isAdmin) {
                    daMoKhoa = true;
                }
                
                BoCauHoiResponse dto = BoCauHoiResponse.fromCache(cache, daMoKhoa);
                
                // Bổ sung thông tin khóa học (query nhẹ, có thể cache riêng nếu cần)
                khoaHocBoCauHoiRepository.findByBoCauHoiId(cache.getId()).ifPresent(khbch -> {
                    if (khbch.getKhoaHoc() != null && !Boolean.TRUE.equals(khbch.getKhoaHoc().getIsXoa())) {
                        dto.setThuocKhoaHoc(true);
                        dto.setKhoaHocId(khbch.getKhoaHoc().getId());
                        dto.setKhoaHocTen(khbch.getKhoaHoc().getTieuDe());
                    }
                });
                
                responseList.add(dto);
            }
            totalElements = cachedPage.getTotalElements();
            totalPages = cachedPage.getTotalPages();
            
        } else {
            // ===== CACHE MISS: Query DB và lưu vào cache =====
            logger.info("🔍 [BoCauHoi] CACHE MISS - Querying DB...");
            Page<BoCauHoi> resultPage = boCauHoiService.findAllBoCauHoi(
                    pageRequest, keyword, chuDeId, cheDoHienThi, trangThai, loaiSuDung,
                    muonTaoTraPhi, nguoiTaoId, minRating, maxRating, currentUserId, isAdmin
            );
            logger.info("🔍 [BoCauHoi] DB result: {} items, totalElements={}", resultPage.getContent().size(), resultPage.getTotalElements());

            totalElements = resultPage.getTotalElements();
            totalPages = resultPage.getTotalPages();

            // Convert Entity sang Cache DTO và lưu vào Redis
            List<BoCauHoiCacheDTO> cacheDTOs = resultPage.getContent().stream()
                    .map(BoCauHoiCacheDTO::fromEntity)
                    .toList();
            
            BoCauHoiPageCacheDTO cacheData = BoCauHoiPageCacheDTO.builder()
                    .content(cacheDTOs)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .pageNumber(pageRequest.getPageNumber())
                    .pageSize(pageRequest.getPageSize())
                    .build();
            
            questionSetRedisService.saveQuestionListToCache(
                    cacheData, pageRequest, keyword, chuDeId, cheDoHienThi, trangThai,
                    loaiSuDung, muonTaoTraPhi, nguoiTaoId, minRating, maxRating, currentUserId, isAdmin
            );

            // Convert Entity sang Response (với thông tin user-specific)
            responseList = new ArrayList<>();
            for (BoCauHoi bo : resultPage.getContent()) {
                boolean isOwner = bo.getTaoBoi() != null && bo.getTaoBoi().getId().equals(currentUserId);
                boolean daMoKhoa = boCauHoiService.hasUserUnlockedBo(bo.getId(), currentUserId);
                
                if (isOwner || isAdmin) {
                    daMoKhoa = true;
                }
                
                BoCauHoiResponse dto = BoCauHoiResponse.from(bo, daMoKhoa);
                
                khoaHocBoCauHoiRepository.findByBoCauHoiId(bo.getId()).ifPresent(khbch -> {
                    if (khbch.getKhoaHoc() != null && !Boolean.TRUE.equals(khbch.getKhoaHoc().getIsXoa())) {
                        dto.setThuocKhoaHoc(true);
                        dto.setKhoaHocId(khbch.getKhoaHoc().getId());
                        dto.setKhoaHocTen(khbch.getKhoaHoc().getTieuDe());
                    }
                });
                
                responseList.add(dto);
            }
        }

        // 3. Tạo PageResponse từ danh sách Response
        logger.info("🔍 [BoCauHoi] Final response: {} items, totalElements={}", responseList.size(), totalElements);
        Page<BoCauHoiResponse> dtoPage = new PageImpl<>(responseList, pageRequest, totalElements);
        PageResponse<BoCauHoiResponse> data = PageResponse.fromPage(dtoPage);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Lấy danh sách thành công")
                        .status(HttpStatus.OK)
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/practice-sets")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getPracticeSets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int limit
    ) {
        Long currentUserId = securityUtils.getLoggedInUserId();
        boolean isAdmin = securityUtils.isAdmin();

        // sort mới nhất trước
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "taoLuc"));

        Page<BoCauHoi> result = boCauHoiService.findPracticeSets(
                pageRequest,
                currentUserId,
                isAdmin
        );

        Page<BoCauHoiResponse> dtoPage = result.map(bo -> {
            boolean isOwner = bo.getTaoBoi() != null && bo.getTaoBoi().getId().equals(currentUserId);
            boolean daMoKhoa = boCauHoiService.hasUserUnlockedBo(bo.getId(), currentUserId);
            // Nếu là owner hoặc admin thì coi như đã mở khóa
            if (isOwner || isAdmin) {
                daMoKhoa = true;
            }
            return BoCauHoiResponse.from(bo, daMoKhoa);
        });
        PageResponse<BoCauHoiResponse> data = PageResponse.fromPage(dtoPage);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Lấy danh sách bộ câu hỏi luyện tập thành công")
                        .status(HttpStatus.OK)
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/battle-sets")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getBattleSets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int limit
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "taoLuc"));

        Page<BoCauHoi> result = boCauHoiService.findBattleSets(pageRequest);
        Page<BoCauHoiResponse> dtoPage = result.map(BoCauHoiResponse::from);
        PageResponse<BoCauHoiResponse> data = PageResponse.fromPage(dtoPage);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Lấy danh sách bộ câu hỏi thi đấu thành công")
                        .status(HttpStatus.OK)
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/battle-sets/casual")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getCasualBattleSets() throws Exception {
        List<BoCauHoi> list = boCauHoiService.getBattleSetsCasualForCurrentUser();
        List<BoCauHoiResponse> data = list.stream()
                .map(BoCauHoiResponse::from)
                .toList();

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Danh sách bộ câu hỏi cho trận CASUAL")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/battle-sets/ranked")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getRankedBattleSets() throws Exception {
        List<BoCauHoi> list = boCauHoiService.getBattleSetsRankedForCurrentUser();
        List<BoCauHoiResponse> data = list.stream()
                .map(BoCauHoiResponse::from)
                .toList();

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Danh sách bộ câu hỏi cho trận RANKED")
                        .data(data)
                        .build()
        );
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getById(@PathVariable Long id) throws DataNotFoundException, PermissionDenyException {
        Long currentUserId = securityUtils.getLoggedInUserId();
        boolean isAdmin = securityUtils.isAdmin();
        BoCauHoi boCauHoi = boCauHoiService.getById(id, currentUserId, isAdmin);
        BoCauHoiResponse boCauHoiResponse = BoCauHoiResponse.from(
                boCauHoi,
                boCauHoiService.hasUserUnlockedBo(boCauHoi.getId(), currentUserId)
        );
        // Gắn thêm thông tin khóa học nếu bộ câu hỏi thuộc khóa học
        khoaHocBoCauHoiRepository.findByBoCauHoiId(boCauHoi.getId()).ifPresent(khbch -> {
            if (khbch.getKhoaHoc() != null && !Boolean.TRUE.equals(khbch.getKhoaHoc().getIsXoa())) {
                boCauHoiResponse.setThuocKhoaHoc(true);
                boCauHoiResponse.setKhoaHocId(khbch.getKhoaHoc().getId());
                boCauHoiResponse.setKhoaHocTen(khbch.getKhoaHoc().getTieuDe());
            }
        });
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Lấy bộ câu hỏi thành công")
                        .status(HttpStatus.OK)
                        .data(boCauHoiResponse)
                        .build()
        );
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> create(@Valid @RequestBody BoCauHoiDTO boCauHoiDTO, BindingResult result) throws DataNotFoundException, PermissionDenyException {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ResponseObject.builder()
                            .message(String.join(", ", errorMessages))
                            .status(HttpStatus.BAD_REQUEST)
                            .data(null)
                            .build()
            );

        }
        Long userId = securityUtils.getLoggedInUser().getId();
        BoCauHoi boCauHoi = boCauHoiService.create(boCauHoiDTO, userId);
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Tao bo cau hoi thanh cong")
                .status(HttpStatus.OK)
                .data(boCauHoi)
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> update(@PathVariable Long id, @RequestBody BoCauHoiDTO boCauHoiDTO) throws DataNotFoundException, PermissionDenyException {
        Long userId = securityUtils.getLoggedInUser().getId();
        boolean isAdmin = securityUtils.isAdmin();
        BoCauHoi boCauHoi = boCauHoiService.update(id, boCauHoiDTO, userId, isAdmin);
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Cập nhật bộ câu hỏi thành công")
                .status(HttpStatus.OK)
                .data(boCauHoi)
                .build());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> approve(@PathVariable Long id) {
        securityUtils.requireAdmin();
        Long adminId = securityUtils.getLoggedInUserId();
        BoCauHoi boCauHoi = boCauHoiService.approve(id, adminId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Duyệt thành công")
                .status(HttpStatus.OK)
                .data(BoCauHoiResponse.from(boCauHoi))
                .build());
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> reject(@PathVariable Long id, @RequestBody TuChoiBoCauHoiDTO lyDo) {
        securityUtils.requireAdmin();
        Long adminId = securityUtils.getLoggedInUser().getId();
//        return boCauHoiService.reject(id, lyDo, adminId);
        BoCauHoi boCauHoi = boCauHoiService.reject(id, lyDo, adminId);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Từ chối thành công")
                .status(HttpStatus.OK)
                .data(BoCauHoiResponse.from(boCauHoi))
                .build());
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> bulkApprove(@RequestBody List<Long> ids) {
        securityUtils.requireAdmin();
        Long adminId = securityUtils.getLoggedInUserId();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long id : ids) {
            try {
                boCauHoiService.approve(id, adminId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("Bộ câu hỏi ID " + id + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "errors", errors
        );

        return ResponseEntity.ok(ResponseObject.builder()
                .message(String.format("Đã duyệt %d/%d bộ câu hỏi", successCount, ids.size()))
                .status(HttpStatus.OK)
                .data(result)
                .build());
    }

    @PostMapping("/bulk-reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> bulkReject(@RequestBody Map<String, Object> request) {
        securityUtils.requireAdmin();
        Long adminId = securityUtils.getLoggedInUserId();
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        String lyDo = (String) request.get("lyDo");

        if (lyDo == null || lyDo.trim().isEmpty()) {
            lyDo = "Không đạt yêu cầu chất lượng";
        }

        int successCount = 0;
        int failCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        TuChoiBoCauHoiDTO dto = new TuChoiBoCauHoiDTO();
        dto.setLyDoTuChoi(lyDo);

        for (Long id : ids) {
            try {
                boCauHoiService.reject(id, dto, adminId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("Bộ câu hỏi ID " + id + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "errors", errors
        );

        return ResponseEntity.ok(ResponseObject.builder()
                .message(String.format("Đã từ chối %d/%d bộ câu hỏi", successCount, ids.size()))
                .status(HttpStatus.OK)
                .data(result)
                .build());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getStatistics() {
        securityUtils.requireAdmin();
        long total = boCauHoiRepository.count();
        long choDuyet = boCauHoiRepository.countByTrangThai("CHO_DUYET");
        long daDuyet = boCauHoiRepository.countByTrangThai("DA_DUYET");
        long tuChoi = boCauHoiRepository.countByTrangThai("TU_CHOI");

        Map<String, Object> stats = Map.of(
                "total", total,
                "choDuyet", choDuyet,
                "daDuyet", daDuyet,
                "tuChoi", tuChoi
        );

        return ResponseEntity.ok(ResponseObject.builder()
                .message("Thống kê thành công")
                .status(HttpStatus.OK)
                .data(stats)
                .build());
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> duplicate(
            @PathVariable Long id,
            @RequestParam(name = "loai_su_dung", required = true) String loaiSuDung,
            @RequestParam(name = "purpose", required = true) String purpose
    ) throws DataNotFoundException, PermissionDenyException {
        securityUtils.requireAdmin();
        Long adminId = securityUtils.getLoggedInUserId();
        BoCauHoi duplicated = boCauHoiService.duplicate(id, adminId, loaiSuDung, purpose);
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Đã duplicate bộ câu hỏi thành công")
                .status(HttpStatus.OK)
                .data(BoCauHoiResponse.from(duplicated))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> delete(@PathVariable Long id) throws DataNotFoundException, PermissionDenyException {
        Long userId = securityUtils.getLoggedInUserId();
        boolean isAdmin = securityUtils.isAdmin();
        boCauHoiService.softDelete(id, userId, isAdmin);
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Xóa bộ câu hỏi thành công")
                        .status(HttpStatus.OK)
                        .data(null)
                        .build()
        );
    }

    @PutMapping("/{id}/mark-official")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> markOfficial(@PathVariable Long id) throws DataNotFoundException, PermissionDenyException {
        Long adminId = securityUtils.getLoggedInUserId(); // hoặc method tương đương của bạn
        BoCauHoi updated = boCauHoiService.markOfficial(id, adminId);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Đánh dấu bộ câu hỏi chính thức thành công")
                        .status(HttpStatus.OK)
                        .data(BoCauHoiResponse.from(updated))
                        .build()
        );
    }

    @PutMapping("/{id}/dis-mark-official")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseObject> disMarkOfficial(@PathVariable Long id) throws DataNotFoundException, PermissionDenyException {
        Long adminId = securityUtils.getLoggedInUserId(); // hoặc method tương đương của bạn
        BoCauHoi updated = boCauHoiService.disMarkOfficial(id, adminId);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Hủy đánh dấu bộ câu hỏi chính thức thành công")
                        .status(HttpStatus.OK)
                        .data(BoCauHoiResponse.from(updated))
                        .build()
        );
    }


    @GetMapping("/{id}/thong-ke")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> thongKeBoCauHoi(@PathVariable Long id) throws DataNotFoundException {
        Map<String, Object> data = boCauHoiService.thongKeBoCauHoi(id);
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Thống kê bộ câu hỏi thành công")
                        .status(HttpStatus.OK)
                        .data(data)
                        .build()
        );
    }

    @PutMapping("/unlock/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ResponseObject> unlockBoCauHoi(@PathVariable("id") Long boCauHoiId) throws Exception {
        Long userId = securityUtils.getLoggedInUserId();

        UnlockBoCauHoiResponse data = boCauHoiService.unlockBoCauHoi(boCauHoiId, userId);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message(data.isDaMoKhoaTruocDo()
                                ? "Bộ câu hỏi đã được mở khóa từ trước"
                                : "Mở khóa bộ câu hỏi thành công")
                        .data(data)
                        .build()
        );
    }


}
