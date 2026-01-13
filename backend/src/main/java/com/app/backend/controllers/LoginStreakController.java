package com.app.backend.controllers;

import com.app.backend.components.SecurityUtils;
import com.app.backend.responses.LoginStreakResponse;
import com.app.backend.responses.ResponseObject;
import com.app.backend.services.loginstreak.ILoginStreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/login-streak")
@RequiredArgsConstructor
public class LoginStreakController {

    private final ILoginStreakService loginStreakService;
    private final SecurityUtils securityUtils;

    /**
     * Lấy thông tin chuỗi đăng nhập
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getLoginStreakInfo() {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            LoginStreakResponse response = loginStreakService.getLoginStreakInfo(userId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy thông tin chuỗi đăng nhập thành công")
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
     * Điểm danh và nhận thưởng hôm nay
     */
    @PostMapping("/claim")
    public ResponseEntity<ResponseObject> claimDailyReward() {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            LoginStreakResponse response = loginStreakService.claimDailyReward(userId);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
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
