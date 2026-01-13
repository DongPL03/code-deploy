package com.app.backend.responses;

import com.app.backend.models.TienDoNhiemVu;
import com.app.backend.models.enums.LoaiNhiemVu;
import com.app.backend.models.enums.MaNhiemVu;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Response cho danh sách nhiệm vụ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhiemVuResponse {

    @JsonProperty("daily_quests")
    private List<QuestItem> dailyQuests;

    @JsonProperty("weekly_quests")
    private List<QuestItem> weeklyQuests;

    @JsonProperty("daily_completed")
    private int dailyCompleted;

    @JsonProperty("daily_total")
    private int dailyTotal;

    @JsonProperty("weekly_completed")
    private int weeklyCompleted;

    @JsonProperty("weekly_total")
    private int weeklyTotal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestItem {
        @JsonProperty("ma")
        private String ma;

        @JsonProperty("mo_ta")
        private String moTa;

        @JsonProperty("icon")
        private String icon;

        @JsonProperty("loai")
        private LoaiNhiemVu loai;

        @JsonProperty("tien_do")
        private int tienDo;

        @JsonProperty("muc_tieu")
        private int mucTieu;

        @JsonProperty("phan_tram")
        private double phanTram;

        @JsonProperty("da_hoan_thanh")
        private boolean daHoanThanh;

        @JsonProperty("da_nhan_thuong")
        private boolean daNhanThuong;

        @JsonProperty("gold_thuong")
        private int goldThuong;

        @JsonProperty("xp_thuong")
        private int xpThuong;

        @JsonProperty("vat_pham_loai")
        private String vatPhamLoai;

        /**
         * Build từ entity + enum
         */
        public static QuestItem from(MaNhiemVu ma, TienDoNhiemVu tienDo) {
            int currentProgress = tienDo != null ? tienDo.getTienDo() : 0;
            boolean completed = tienDo != null && tienDo.getDaHoanThanh();
            boolean claimed = tienDo != null && tienDo.getDaNhanThuong();

            return QuestItem.builder()
                    .ma(ma.name())
                    .moTa(ma.getMoTa())
                    .icon(ma.getIcon())
                    .loai(ma.getLoai())
                    .tienDo(currentProgress)
                    .mucTieu(ma.getMucTieu())
                    .phanTram(ma.getMucTieu() > 0 ? (double) currentProgress / ma.getMucTieu() * 100 : 0)
                    .daHoanThanh(completed)
                    .daNhanThuong(claimed)
                    .goldThuong(ma.getGoldThuong())
                    .xpThuong(ma.getXpThuong())
                    .vatPhamLoai(ma.getVatPhamLoai())
                    .build();
        }
    }
}
