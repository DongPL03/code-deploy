package com.app.backend.controllers;

import com.app.backend.dtos.danhgia.CreateDanhGiaDTO;
import com.app.backend.models.DanhGia;
import com.app.backend.models.NguoiDung;
import com.app.backend.models.enums.LoaiDoiTuongDanhGia;
import com.app.backend.responses.ResponseObject;
import com.app.backend.responses.danhgia.DanhGiaResponse;
import com.app.backend.responses.danhgia.DanhGiaStatsResponse;
import com.app.backend.services.danhgia.DanhGiaService;
import com.app.backend.services.danhgia.IDanhGiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${api.prefix}/danh-gia")
@RequiredArgsConstructor
@Slf4j
public class DanhGiaController {

    private final IDanhGiaService danhGiaService;

    /**
     * Tạo hoặc cập nhật đánh giá
     * POST /api/v1/danh-gia
     */
    @PostMapping
    public ResponseEntity<ResponseObject> createOrUpdateDanhGia(
            @AuthenticationPrincipal NguoiDung nguoiDung,
            @Valid @RequestBody CreateDanhGiaDTO dto) {
        try {
            LoaiDoiTuongDanhGia loaiDoiTuong = LoaiDoiTuongDanhGia.valueOf(dto.getLoaiDoiTuong().toUpperCase());

            DanhGia danhGia = danhGiaService.createOrUpdateDanhGia(
                    nguoiDung.getId(),
                    loaiDoiTuong,
                    dto.getDoiTuongId(),
                    dto.getSoSao(),
                    dto.getNoiDung()
            );

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Đánh giá thành công")
                    .data(DanhGiaResponse.fromEntity(danhGia))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Loại đối tượng không hợp lệ: " + dto.getLoaiDoiTuong())
                    .build());
        } catch (Exception e) {
            log.error("Error creating rating", e);
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Lấy đánh giá của tôi cho một đối tượng
     * GET /api/v1/danh-gia/my/{loaiDoiTuong}/{doiTuongId}
     */
    @GetMapping("/my/{loaiDoiTuong}/{doiTuongId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getMyDanhGia(
            @AuthenticationPrincipal NguoiDung nguoiDung,
            @PathVariable String loaiDoiTuong,
            @PathVariable Long doiTuongId) {
        try {
            LoaiDoiTuongDanhGia loai = LoaiDoiTuongDanhGia.valueOf(loaiDoiTuong.toUpperCase());
            Optional<DanhGia> danhGia = danhGiaService.getMyDanhGia(nguoiDung.getId(), loai, doiTuongId);

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy đánh giá thành công")
                    .data(danhGia.map(DanhGiaResponse::fromEntity).orElse(null))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Loại đối tượng không hợp lệ")
                    .build());
        }
    }

    /**
     * Lấy danh sách đánh giá cho một đối tượng (phân trang)
     * GET /api/v1/danh-gia/{loaiDoiTuong}/{doiTuongId}?page=0&size=10
     */
    @GetMapping("/{loaiDoiTuong}/{doiTuongId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getDanhGiaByDoiTuong(
            @PathVariable String loaiDoiTuong,
            @PathVariable Long doiTuongId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            LoaiDoiTuongDanhGia loai = LoaiDoiTuongDanhGia.valueOf(loaiDoiTuong.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "capNhatLuc"));

            Page<DanhGia> danhGiaPage = danhGiaService.getDanhGiaByDoiTuong(loai, doiTuongId, pageable);
            Page<DanhGiaResponse> responsePage = danhGiaPage.map(DanhGiaResponse::fromEntity);

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy danh sách đánh giá thành công")
                    .data(responsePage)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Loại đối tượng không hợp lệ")
                    .build());
        }
    }

    /**
     * Lấy thống kê đánh giá cho một đối tượng
     * GET /api/v1/danh-gia/stats/{loaiDoiTuong}/{doiTuongId}
     */
    @GetMapping("/stats/{loaiDoiTuong}/{doiTuongId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getStats(
            @PathVariable String loaiDoiTuong,
            @PathVariable Long doiTuongId) {
        try {
            LoaiDoiTuongDanhGia loai = LoaiDoiTuongDanhGia.valueOf(loaiDoiTuong.toUpperCase());
            Map<String, Object> stats = danhGiaService.getStatsForDoiTuong(loai, doiTuongId);

            DanhGiaStatsResponse response = DanhGiaStatsResponse.builder()
                    .tongDanhGia((Long) stats.get("tongDanhGia"))
                    .trungBinhSao((Double) stats.get("trungBinhSao"))
                    .phanBoSao((Map<Integer, Long>) stats.get("phanBoSao"))
                    .build();

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy thống kê đánh giá thành công")
                    .data(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Loại đối tượng không hợp lệ")
                    .build());
        }
    }

    /**
     * Xóa đánh giá của tôi
     * DELETE /api/v1/danh-gia/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject> deleteDanhGia(
            @AuthenticationPrincipal NguoiDung nguoiDung,
            @PathVariable Long id) {
        try {
            danhGiaService.deleteDanhGia(id, nguoiDung.getId());

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Xóa đánh giá thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
        }
    }
}
