package com.app.backend.controllers;

import com.app.backend.components.SecurityUtils;
import com.app.backend.models.enums.MaNhiemVu;
import com.app.backend.responses.NhiemVuResponse;
import com.app.backend.responses.NhanThuongNhiemVuResponse;
import com.app.backend.responses.ResponseObject;
import com.app.backend.services.nhiemvu.INhiemVuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/quests")
@RequiredArgsConstructor
public class NhiemVuController {

    private final INhiemVuService nhiemVuService;
    private final SecurityUtils securityUtils;

    /**
     * Lấy danh sách nhiệm vụ (daily + weekly)
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getQuests() {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            NhiemVuResponse response = nhiemVuService.getQuests(userId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy danh sách nhiệm vụ thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Nhận thưởng một nhiệm vụ
     */
    @PostMapping("/claim/{questCode}")
    public ResponseEntity<ResponseObject> claimReward(
            @PathVariable("questCode") String questCode) {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            MaNhiemVu maNhiemVu = MaNhiemVu.valueOf(questCode);
            NhanThuongNhiemVuResponse response = nhiemVuService.claimReward(userId, maNhiemVu);

            HttpStatus status = response.isThanhCong() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(status)
                    .message(response.getThongBao())
                    .data(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Mã nhiệm vụ không hợp lệ: " + questCode)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Nhận tất cả thưởng nhiệm vụ đã hoàn thành
     */
    @PostMapping("/claim-all")
    public ResponseEntity<ResponseObject> claimAllRewards() {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            NhanThuongNhiemVuResponse response = nhiemVuService.claimAllRewards(userId);

            HttpStatus status = response.isThanhCong() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(status)
                    .message(response.getThongBao())
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage())
                    .build());
        }
    }
}
