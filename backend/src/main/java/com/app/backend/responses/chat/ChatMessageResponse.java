package com.app.backend.responses.chat;

import com.app.backend.models.TinNhan;
import com.app.backend.models.enums.LoaiTinNhan;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatMessageResponse(
        @JsonProperty("tin_nhan_id")
        Long tinNhanId,
        @JsonProperty("gui_boi_id")
        Long guiBoiId,
        @JsonProperty("gui_boi_ten")
        String guiBoiTen,
        @JsonProperty("gui_boi_avatar")
        String guiBoiAvatar,
        @JsonProperty("nhan_boi_id")
        Long nhanBoiId,
        @JsonProperty("nhan_boi_ten")
        String nhanBoiTen,
        @JsonProperty("noi_dung")
        String noiDung,
        @JsonProperty("loai_tin_nhan")
        LoaiTinNhan loaiTinNhan,
        @JsonProperty("url_media")
        String urlMedia,
        @JsonProperty("ten_file")
        String tenFile,
        @JsonProperty("kich_thuoc_file")
        Long kichThuocFile,
        @JsonProperty("gui_luc")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant guiLuc,
        @JsonProperty("la_toi")
        boolean laToi
) {
    public static ChatMessageResponse fromEntity(TinNhan e, Long currentUserId) {
        boolean isMe = currentUserId != null
                && e.getGuiBoi() != null
                && currentUserId.equals(e.getGuiBoi().getId());

        return ChatMessageResponse.builder()
                .tinNhanId(e.getId())
                .guiBoiId(e.getGuiBoi() != null ? e.getGuiBoi().getId() : null)
                .guiBoiTen(e.getGuiBoi() != null ? e.getGuiBoi().getHoTen() : null)
                .guiBoiAvatar(e.getGuiBoi() != null ? e.getGuiBoi().getAvatarUrl() : null)
                .nhanBoiId(e.getNhanBoi() != null ? e.getNhanBoi().getId() : null)
                .nhanBoiTen(e.getNhanBoi() != null ? e.getNhanBoi().getHoTen() : null)
                .noiDung(e.getNoiDung())
                .loaiTinNhan(e.getLoaiTinNhan() != null ? e.getLoaiTinNhan() : LoaiTinNhan.VAN_BAN)
                .urlMedia(e.getUrlMedia())
                .tenFile(e.getTenFile())
                .kichThuocFile(e.getKichThuocFile())
                .guiLuc(e.getGuiLuc())
                .laToi(isMe)
                .build();
    }

    /**
     * Dùng cho chỗ nào chưa cần `la_toi` (ví dụ WebSocket cũ),
     * mặc định la_toi = false.
     */
    public static ChatMessageResponse fromEntity(TinNhan e) {
        return fromEntity(e, null);
    }
}

