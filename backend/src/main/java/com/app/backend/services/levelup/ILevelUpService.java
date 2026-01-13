package com.app.backend.services.levelup;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.responses.LevelUpResponse;

public interface ILevelUpService {

    /**
     * Thêm XP cho người dùng và xử lý lên cấp
     * @param userId ID người dùng
     * @param xpAmount Số XP cộng thêm
     * @return Response chứa thông tin lên cấp và phần thưởng
     */
    LevelUpResponse addXpAndProcessLevelUp(Long userId, long xpAmount) throws DataNotFoundException;

    /**
     * Tính cấp độ từ tổng XP
     */
    int calculateLevel(long totalXp);

    /**
     * Tính XP cần để đạt cấp độ
     */
    long xpRequiredForLevel(int level);
}
