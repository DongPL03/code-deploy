package com.app.backend.responses;

import com.app.backend.models.VatPham;
import com.app.backend.models.VatPhamNguoiDung;
import com.app.backend.models.enums.LoaiVatPham;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response cho kho đồ của người dùng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhoDoResponse {

    @JsonProperty("tong_loai_vat_pham")
    private int tongLoaiVatPham;         // Số loại vật phẩm khác nhau

    @JsonProperty("tong_so_luong")
    private int tongSoLuong;             // Tổng số lượng tất cả vật phẩm

    @JsonProperty("danh_sach_vat_pham")
    private List<InventoryItem> danhSachVatPham;

    /**
     * Một vật phẩm trong kho
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        @JsonProperty("id")
        private Long id;                  // ID của VatPhamNguoiDung

        @JsonProperty("vat_pham_id")
        private Long vatPhamId;

        @JsonProperty("ten")
        private String ten;

        @JsonProperty("mo_ta")
        private String moTa;

        @JsonProperty("icon")
        private String icon;

        @JsonProperty("loai")
        private LoaiVatPham loai;

        @JsonProperty("do_hiem")
        private String doHiem;

        @JsonProperty("so_luong")
        private int soLuong;

        @JsonProperty("nhan_luc")
        private LocalDateTime nhanLuc;

        @JsonProperty("su_dung_luc")
        private LocalDateTime suDungLuc;

        @JsonProperty("co_the_su_dung")
        private boolean coTheSuDung;      // Có thể sử dụng trong trận không

        @JsonProperty("hieu_ung")
        private String hieuUng;           // Mô tả hiệu ứng khi dùng

        /**
         * Factory method từ entity
         */
        public static InventoryItem from(VatPhamNguoiDung vpnd) {
            VatPham vp = vpnd.getVatPham();
            return InventoryItem.builder()
                    .id(vpnd.getId())
                    .vatPhamId(vp.getId())
                    .ten(vp.getTen())
                    .moTa(vp.getMoTa())
                    .icon(vp.getIcon())
                    .loai(vp.getLoai())
                    .doHiem(vp.getDoHiem())
                    .soLuong(vpnd.getSoLuong())
                    .nhanLuc(vpnd.getNhanLuc())
                    .suDungLuc(vpnd.getSuDungLuc())
                    .coTheSuDung(isUsableInBattle(vp.getLoai()))
                    .hieuUng(getEffectDescription(vp.getLoai()))
                    .build();
        }

        /**
         * Kiểm tra vật phẩm có thể dùng trong trận không
         */
        private static boolean isUsableInBattle(LoaiVatPham loai) {
            if (loai == null) return false;
            return switch (loai) {
                case GOI_Y_50_50, BO_QUA_CAU_HOI,
                     X2_DIEM, X3_DIEM, KHIEN_BAO_VE -> true;
                default -> false;
            };
        }

        /**
         * Mô tả hiệu ứng theo loại
         */
        private static String getEffectDescription(LoaiVatPham loai) {
            if (loai == null) return "";
            return switch (loai) {
                case GOI_Y_50_50 -> "Loại bỏ 2 đáp án sai";
                case BO_QUA_CAU_HOI -> "Bỏ qua câu hỏi hiện tại";
                case X2_DIEM -> "Nhân đôi điểm câu hỏi tiếp theo";
                case X3_DIEM -> "Nhân ba điểm câu hỏi tiếp theo";
                case KHIEN_BAO_VE -> "Bảo vệ khỏi mất điểm 1 lần";
                default -> "";
            };
        }
    }
}
