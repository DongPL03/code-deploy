package com.app.backend.services.vatpham;

import com.app.backend.dtos.MuaVatPhamDTO;
import com.app.backend.dtos.SuDungVatPhamDTO;
import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.BattleState;
import com.app.backend.models.VatPham;
import com.app.backend.models.enums.LoaiVatPham;
import com.app.backend.responses.MuaVatPhamResponse;
import com.app.backend.responses.ShopResponse;
import com.app.backend.responses.SuDungVatPhamResponse;
import com.app.backend.responses.VatPhamInventoryResponse;

import java.util.List;

public interface IVatPhamService {
    /**
     * Lấy danh sách inventory của user
     */
    List<VatPhamInventoryResponse> getInventory(Long userId);

    /**
     * Lấy tất cả vật phẩm đang active (cho shop/display)
     */
    List<VatPham> getAllActiveItems();

    /**
     * Thêm vật phẩm cho user (khi thắng trận, nhận thưởng...)
     */
    void grantItemToUser(Long userId, Long vatPhamId, int quantity) throws DataNotFoundException;

    /**
     * Thêm vật phẩm theo loại
     */
    public void grantItemByType(Long userId, LoaiVatPham loai, int quantity) throws DataNotFoundException;

    /**
     * Sử dụng vật phẩm trong trận đấu
     */
    SuDungVatPhamResponse useItem(Long userId, SuDungVatPhamDTO dto, BattleState battleState)
            throws DataNotFoundException;

    /**
     * Thưởng vật phẩm ngẫu nhiên sau trận đấu
     */
    VatPham rewardRandomItem(Long userId, boolean isWinner) throws DataNotFoundException;

    /**
     * Khởi tạo vật phẩm mặc định (chạy khi startup)
     */
    void initDefaultItems();

    /**
     * Lấy danh sách vật phẩm trong Shop
     */
    ShopResponse getShop(Long userId) throws DataNotFoundException;

    /**
     * Mua vật phẩm từ Shop
     */
    MuaVatPhamResponse purchaseItem(Long userId, MuaVatPhamDTO dto) throws DataNotFoundException;

}
