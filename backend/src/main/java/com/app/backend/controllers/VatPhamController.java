package com.app.backend.controllers;

import com.app.backend.dtos.MuaVatPhamDTO;
import com.app.backend.dtos.SuDungVatPhamDTO;
import com.app.backend.models.BattleState;
import com.app.backend.models.VatPham;
import com.app.backend.responses.MuaVatPhamResponse;
import com.app.backend.responses.ShopResponse;
import com.app.backend.responses.SuDungVatPhamResponse;
import com.app.backend.responses.VatPhamInventoryResponse;
import com.app.backend.services.vatpham.IVatPhamService;
import com.app.backend.services.vatpham.IVatPhamRedisService;
import com.app.backend.services.vatpham.VatPhamService;
import com.app.backend.components.JwtTokenUtils;
import com.app.backend.components.BattleStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/vat-pham")
@RequiredArgsConstructor
@Slf4j
public class VatPhamController {

    private final IVatPhamService vatPhamService;
    private final IVatPhamRedisService vatPhamRedisService;
    private final BattleStateManager battleStateManager;
    private final JwtTokenUtils jwtTokenUtils;

    /**
     * Lấy inventory của user hiện tại
     */
    @GetMapping("/inventory")
    @Transactional(readOnly = true)
    public ResponseEntity<List<VatPhamInventoryResponse>> getInventory(
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = jwtTokenUtils.extractUserId(authHeader);
            List<VatPhamInventoryResponse> inventory = vatPhamService.getInventory(userId);
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            log.error("Error getting inventory", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách tất cả vật phẩm (cho shop/display)
     */
    @GetMapping("/all")
    @Transactional(readOnly = true)
    public ResponseEntity<List<VatPham>> getAllItems() {
        // 1. Check Redis cache first
        List<VatPham> items = vatPhamRedisService.getAllActiveItems();
        
        if (items == null) {
            // 2. Cache miss -> Query DB
            items = vatPhamService.getAllActiveItems();
            // 3. Save to cache
            vatPhamRedisService.saveAllActiveItems(items);
        }
        
        return ResponseEntity.ok(items);
    }

    /**
     * Sử dụng vật phẩm trong trận đấu
     */
    @PostMapping("/use")
    public ResponseEntity<?> useItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SuDungVatPhamDTO dto) {
        try {
            Long userId = jwtTokenUtils.extractUserId(authHeader);

            // Lấy BattleState hiện tại
            BattleState battleState = battleStateManager.get(dto.getTranDauId());
            if (battleState == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Trận đấu không tồn tại hoặc chưa bắt đầu"));
            }

            // Kiểm tra user có trong trận không
            if (!battleState.getDiemNguoiChoi().containsKey(userId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Bạn không tham gia trận đấu này"));
            }

            // Kiểm tra trận đã kết thúc chưa
            if (battleState.isFinished()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Trận đấu đã kết thúc"));
            }

            SuDungVatPhamResponse response = vatPhamService.useItem(userId, dto, battleState);

            // Lưu lại state sau khi áp dụng item effect
            if (response.isThanhCong()) {
                battleStateManager.save(battleState);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error using item", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Admin: Tặng vật phẩm cho user
     */
    @PostMapping("/grant")
    public ResponseEntity<?> grantItem(
            @RequestParam Long userId,
            @RequestParam Long vatPhamId,
            @RequestParam(defaultValue = "1") int quantity) {
        try {
            vatPhamService.grantItemToUser(userId, vatPhamId, quantity);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã tặng vật phẩm thành công",
                    "user_id", userId,
                    "vat_pham_id", vatPhamId,
                    "quantity", quantity
            ));
        } catch (Exception e) {
            log.error("Error granting item", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Admin: Khởi tạo vật phẩm mặc định
     */
    @PostMapping("/init-defaults")
    public ResponseEntity<?> initDefaults() {
        try {
            vatPhamService.initDefaultItems();
            // Clear cache sau khi init
            vatPhamRedisService.clearItemsCache();
            return ResponseEntity.ok(Map.of("message", "Đã khởi tạo vật phẩm mặc định"));
        } catch (Exception e) {
            log.error("Error initializing default items", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ==================== SHOP ENDPOINTS ====================

    /**
     * Lấy danh sách vật phẩm trong Shop
     */
    @GetMapping("/shop")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getShop(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = jwtTokenUtils.extractUserId(authHeader);
            ShopResponse shop = vatPhamService.getShop(userId);
            return ResponseEntity.ok(shop);
        } catch (Exception e) {
            log.error("Error getting shop", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Mua vật phẩm từ Shop
     */
    @PostMapping("/shop/purchase")
    public ResponseEntity<?> purchaseItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MuaVatPhamDTO dto) {
        try {
            Long userId = jwtTokenUtils.extractUserId(authHeader);
            MuaVatPhamResponse response = vatPhamService.purchaseItem(userId, dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error purchasing item", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
