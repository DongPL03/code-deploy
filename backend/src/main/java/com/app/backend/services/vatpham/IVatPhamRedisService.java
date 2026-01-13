package com.app.backend.services.vatpham;

import com.app.backend.models.VatPham;

import java.util.List;

public interface IVatPhamRedisService {
    /**
     * Get all active items from cache
     * @return List of active VatPham or null if cache miss
     */
    List<VatPham> getAllActiveItems();

    /**
     * Save all active items to cache
     * @param items List of VatPham to cache
     */
    void saveAllActiveItems(List<VatPham> items);

    /**
     * Clear item cache (call when items are modified)
     */
    void clearItemsCache();
}
