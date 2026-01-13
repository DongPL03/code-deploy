package com.app.backend.dtos;

import com.app.backend.models.enums.LoaiTinNhan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO gửi tin nhắn 1-1
 * JSON sẽ là: { "receiver_id": 2, "noi_dung": "hello", "loai_tin_nhan": "VAN_BAN" }
 */
public record SendMessageRequest(
        @NotNull(message = "receiver_id không được để trống")
        Long receiver_id,

        @Size(max = 5000, message = "Độ dài nội dung tối đa 5000 ký tự")
        String noi_dung,

        LoaiTinNhan loai_tin_nhan,

        String url_media,

        String ten_file,

        Long kich_thuoc_file
) {
    public LoaiTinNhan loai_tin_nhan() {
        return loai_tin_nhan != null ? loai_tin_nhan : LoaiTinNhan.VAN_BAN;
    }
}
