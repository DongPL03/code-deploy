package com.app.backend.services.levelup;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.BangXepHang;
import com.app.backend.models.VatPham;
import com.app.backend.models.enums.PhanThuongCapDo;
import com.app.backend.repositories.IBangXepHangRepository;
import com.app.backend.repositories.IVatPhamRepository;
import com.app.backend.responses.LevelUpResponse;
import com.app.backend.services.vatpham.IVatPhamService;
import com.app.backend.utils.XpCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LevelUpService implements ILevelUpService {

    private final IBangXepHangRepository bangXepHangRepository;
    private final IVatPhamRepository vatPhamRepository;
    private final IVatPhamService vatPhamService;

    @Override
    @Transactional
    public LevelUpResponse addXpAndProcessLevelUp(Long userId, long xpAmount) throws DataNotFoundException {
        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy thông tin xếp hạng"));

        // Lưu cấp độ cũ
        int oldLevel = bxh.getLevel() != null ? bxh.getLevel() : 1;
        long oldXp = bxh.getTongXp() != null ? bxh.getTongXp() : 0;

        // Cộng XP
        long newXp = oldXp + xpAmount;
        bxh.setTongXp(newXp);

        // Tính cấp độ mới - dùng XpCalculator
        int newLevel = XpCalculator.calculateLevel(newXp);
        bxh.setLevel(newLevel);

        // Danh sách phần thưởng nhận được
        List<LevelUpResponse.RewardItem> rewards = new ArrayList<>();

        // Nếu lên cấp
        if (newLevel > oldLevel) {
            log.info("🎉 User {} leveled up from {} to {}", userId, oldLevel, newLevel);

            // Xử lý phần thưởng cho TẤT CẢ các cấp đã lên
            for (int level = oldLevel + 1; level <= newLevel; level++) {
                List<LevelUpResponse.RewardItem> levelRewards = grantLevelRewards(userId, level, bxh);
                rewards.addAll(levelRewards);
            }
        }

        // Lưu thay đổi
        bangXepHangRepository.save(bxh);

        // Tính thông tin XP hiện tại - dùng XpCalculator
        XpCalculator.LevelInfo levelInfo = XpCalculator.computeLevelInfo(newXp);

        String thongBao = newLevel > oldLevel
                ? "🎉 Chúc mừng! Bạn đã lên cấp " + newLevel + "!"
                : "+" + xpAmount + " XP";

        return LevelUpResponse.builder()
                .daLenCap(newLevel > oldLevel)
                .capDoCu(oldLevel)
                .capDoMoi(newLevel)
                .xpHienTai(levelInfo.getXpInCurrentLevel())
                .xpCanLenCap(levelInfo.getXpNeededForNext())
                .phanTramTienDo(levelInfo.getProgressPercent())
                .phanThuong(rewards)
                .thongBao(thongBao)
                .build();
    }

    /**
     * Trao phần thưởng cho một cấp độ cụ thể
     * Sử dụng enum PhanThuongCapDo thay vì DB
     */
    private List<LevelUpResponse.RewardItem> grantLevelRewards(Long userId, int level, BangXepHang bxh)
            throws DataNotFoundException {
        List<LevelUpResponse.RewardItem> rewards = new ArrayList<>();

        // Tìm phần thưởng milestone từ enum
        PhanThuongCapDo milestone = PhanThuongCapDo.findByLevel(level);

        if (milestone != null) {
            // Có milestone → thưởng đặc biệt
            // 1. Thưởng xu
            if (milestone.getXuThuong() > 0) {
                bxh.setTienVang(bxh.getTienVang() + milestone.getXuThuong());
                rewards.add(LevelUpResponse.RewardItem.builder()
                        .loai("GOLD")
                        .ten("Xu")
                        .soLuong(milestone.getXuThuong())
                        .icon("💰")
                        .capDo(level)
                        .build());
                log.info("  💰 +{} gold (milestone) for level {}", milestone.getXuThuong(), level);
            }

            // 2. Thưởng vật phẩm
            if (milestone.getVatPhamLoai() != null && milestone.getSoLuongVatPham() > 0) {
                VatPham vatPham = vatPhamRepository.findByLoai(milestone.getVatPhamLoai()).orElse(null);
                if (vatPham != null) {
                    vatPhamService.grantItemToUser(userId, vatPham.getId(), milestone.getSoLuongVatPham());
                    rewards.add(LevelUpResponse.RewardItem.builder()
                            .loai("VAT_PHAM")
                            .ten(vatPham.getTen())
                            .soLuong(milestone.getSoLuongVatPham())
                            .icon(vatPham.getIcon())
                            .capDo(level)
                            .build());
                    log.info("  🎁 +{} {} for level {}", milestone.getSoLuongVatPham(), vatPham.getTen(), level);
                }
            }
        } else {
            // Không phải milestone → thưởng gold mặc định từ công thức
            int goldReward = PhanThuongCapDo.getDefaultGold(level);
            if (goldReward > 0) {
                bxh.setTienVang(bxh.getTienVang() + goldReward);
                rewards.add(LevelUpResponse.RewardItem.builder()
                        .loai("GOLD")
                        .ten("Xu")
                        .soLuong(goldReward)
                        .icon("💰")
                        .capDo(level)
                        .build());
                log.info("  💰 +{} gold (formula) for level {}", goldReward, level);
            }
        }

        return rewards;
    }

    @Override
    public int calculateLevel(long totalXp) {
        return XpCalculator.calculateLevel(totalXp);
    }

    @Override
    public long xpRequiredForLevel(int level) {
        return XpCalculator.xpRequiredForLevel(level);
    }
}
