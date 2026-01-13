package com.app.backend.models.enums;

import lombok.Getter;

@Getter
public enum AchievementCode {

    // 🏟 Trận đấu & chiến thắng
    TRAN_DAU_DAU_TIEN("TRAN_DAU_DAU_TIEN", "Trận đầu tiên", "Hoàn thành 1 trận đấu bất kỳ"),
    MUOI_TRAN("MUOI_TRAN", "Lính mới chăm chỉ", "Hoàn thành 10 trận đấu"),
    NAM_MUOI_TRAN("NAM_MUOI_TRAN", "Cao thủ cày cuốc", "Hoàn thành 50 trận đấu"),
    MOT_TRAM_TRAN("MOT_TRAM_TRAN", "Huyền thoại đấu trường", "Hoàn thành 100 trận đấu"),

    CHIEN_THANG_DAU_TIEN("CHIEN_THANG_DAU_TIEN", "Chiến thắng đầu tiên", "Thắng 1 trận đấu bất kỳ"),
    MUOI_CHIEN_THANG("MUOI_CHIEN_THANG", "Chuỗi chiến thắng", "Thắng tổng cộng 10 trận"),
    NAM_MUOI_CHIEN_THANG("NAM_MUOI_CHIEN_THANG", "Cao thủ lão luyện", "Thắng tổng cộng 50 trận"),


    // 🎚 Level
    CAP_DO_5("CAP_DO_5", "Tân binh lên hạng", "Đạt cấp độ 5"),
    CAP_DO_10("CAP_DO_10", "Chiến binh dày dạn", "Đạt cấp độ 10"),
    CAP_DO_20("CAP_DO_20", "Bậc thầy tri thức", "Đạt cấp độ 20"),
    CAP_DO_30("CAP_DO_30", "Huyền thoại đấu trường", "Đạt cấp độ 30"),
    CAP_DO_40("CAP_DO_40", "Thần đồng chiến thắng", "Đạt cấp độ 40"),
    CAP_DO_50("CAP_DO_50", "Vô địch Đấu Trường", "Đạt cấp độ 50"),

    // 💰 Vàng
    VANG_350("VANG_350", "Người chơi tiềm năng", "Tích lũy ít nhất 350 vàng"),
    VANG_400("VANG_400", "Chiến binh dũng mãnh", "Tích lũy ít nhất 400 vàng"),
    VANG_500("VANG_500", "Tay chơi có điều kiện", "Tích lũy ít nhất 500 vàng"),
    VANG_1000("VANG_1000", "Chiến binh giàu có", "Tích lũy ít nhất 1000 vàng"),
    VANG_2000("VANG_2000", "Đại gia Đấu Trường", "Tích lũy ít nhất 2000 vàng"),

    // 🏅 Rank tier
    DAT_BAC("DAT_BAC", "Bước vào Bạc", "Đạt rank BẠC hoặc cao hơn"),
    DAT_VANG("DAT_VANG", "Vươn tới Vàng", "Đạt rank VÀNG hoặc cao hơn"),
    DAT_BACH_KIM("DAT_BACH_KIM", "Chạm tới Bạch Kim", "Đạt rank BẠCH KIM hoặc cao hơn"),
    DAT_KIM_CUONG("DAT_KIM_CUONG", "Chiến binh Kim Cương", "Đạt rank KIM CƯƠNG hoặc cao hơn"),
    DAT_CAO_THU("DAT_CAO_THU", "Bậc thầy Đấu Trường", "Đạt rank CAO THỦ"),

    // 📚 Khóa học
    HOAN_THANH_KHOA_HOC_DAU_TIEN("HOAN_THANH_KHOA_HOC_DAU_TIEN", "Bước đầu thành công", "Hoàn thành khóa học đầu tiên"),
    HOAN_THANH_5_KHOA_HOC("HOAN_THANH_5_KHOA_HOC", "Học viên chăm chỉ", "Hoàn thành 5 khóa học"),
    HOAN_THANH_10_KHOA_HOC("HOAN_THANH_10_KHOA_HOC", "Chuyên gia học tập", "Hoàn thành 10 khóa học"),
    HOAN_THANH_20_KHOA_HOC("HOAN_THANH_20_KHOA_HOC", "Bậc thầy tri thức", "Hoàn thành 20 khóa học"),
    DIEM_CAO_KHOA_HOC("DIEM_CAO_KHOA_HOC", "Xuất sắc", "Đạt điểm trung bình >= 90% trong một khóa học"),

    // 🎯 Bộ câu hỏi được chọn
    BO_CAU_HOI_DUOC_CHON_KHOA_HOC("BO_CAU_HOI_DUOC_CHON_KHOA_HOC", "Bộ câu hỏi được chọn", "Bộ câu hỏi của bạn được admin chọn làm bộ câu hỏi khóa học"),
    BO_CAU_HOI_DUOC_CHON_RANKED("BO_CAU_HOI_DUOC_CHON_RANKED", "Bộ câu hỏi chính thức", "Bộ câu hỏi của bạn được admin chọn làm bộ câu hỏi thi đấu ranked");

    private final String code;
    private final String title;
    private final String description;

    AchievementCode(String code, String title, String description) {
        this.code = code;
        this.title = title;
        this.description = description;
    }
}

