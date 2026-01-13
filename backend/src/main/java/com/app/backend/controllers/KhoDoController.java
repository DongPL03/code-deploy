package com.app.backend.controllers;

import com.app.backend.components.SecurityUtils;
import com.app.backend.models.VatPhamNguoiDung;
import com.app.backend.repositories.IVatPhamNguoiDungRepository;
import com.app.backend.responses.KhoDoResponse;
import com.app.backend.responses.ResponseObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/inventory")
@RequiredArgsConstructor
public class KhoDoController {

    private final IVatPhamNguoiDungRepository vatPhamNguoiDungRepository;
    private final SecurityUtils securityUtils;

    /**
     * Lấy danh sách vật phẩm trong kho của user
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getInventory() {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            
            // Lấy các vật phẩm có số lượng > 0
            List<VatPhamNguoiDung> items = vatPhamNguoiDungRepository.findAvailableByUserId(userId);
            
            // Convert to response
            List<KhoDoResponse.InventoryItem> inventoryItems = items.stream()
                    .map(KhoDoResponse.InventoryItem::from)
                    .collect(Collectors.toList());

            int totalQuantity = items.stream()
                    .mapToInt(VatPhamNguoiDung::getSoLuong)
                    .sum();

            KhoDoResponse response = KhoDoResponse.builder()
                    .tongLoaiVatPham(inventoryItems.size())
                    .tongSoLuong(totalQuantity)
                    .danhSachVatPham(inventoryItems)
                    .build();

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy kho đồ thành công")
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
     * Lấy chi tiết một vật phẩm
     */
    @GetMapping("/{vatPhamId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseObject> getItemDetail(@PathVariable Long vatPhamId) {
        try {
            Long userId = securityUtils.getLoggedInUserId();
            
            VatPhamNguoiDung item = vatPhamNguoiDungRepository
                    .findByNguoiDungIdAndVatPhamId(userId, vatPhamId)
                    .orElse(null);

            if (item == null || item.getSoLuong() <= 0) {
                return ResponseEntity.ok(ResponseObject.builder()
                        .status(HttpStatus.NOT_FOUND)
                        .message("Bạn không có vật phẩm này")
                        .build());
            }

            KhoDoResponse.InventoryItem response = KhoDoResponse.InventoryItem.from(item);

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy chi tiết vật phẩm thành công")
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
