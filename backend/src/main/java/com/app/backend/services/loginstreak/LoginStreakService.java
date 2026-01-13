package com.app.backend.services.loginstreak;

import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.BangXepHang;
import com.app.backend.models.VatPham;
import com.app.backend.models.enums.PhanThuongDangNhap;
import com.app.backend.repositories.IBangXepHangRepository;
import com.app.backend.repositories.IVatPhamRepository;
import com.app.backend.responses.LoginStreakResponse;
import com.app.backend.services.levelup.ILevelUpService;
import com.app.backend.services.vatpham.IVatPhamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginStreakService implements ILoginStreakService {

    private final IBangXepHangRepository bangXepHangRepository;
    private final IVatPhamRepository vatPhamRepository;
    private final IVatPhamService vatPhamService;
    private final ILevelUpService levelUpService;

    @Override
    public LoginStreakResponse getLoginStreakInfo(Long userId) throws DataNotFoundException {
        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy thông tin xếp hạng"));

        return buildStreakResponse(bxh, false, null);
    }

    @Override
    @Transactional
    public LoginStreakResponse claimDailyReward(Long userId) throws DataNotFoundException {
        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy thông tin xếp hạng"));

        LocalDate today = LocalDate.now();
        LocalDate lastLogin = bxh.getNgayDangNhapCuoi();

        // Kiểm tra đã điểm danh hôm nay chưa
        if (lastLogin != null && lastLogin.equals(today)) {
            return buildStreakResponse(bxh, false, "Bạn đã điểm danh hôm nay rồi!");
        }

        // Tính streak mới
        int currentStreak = bxh.getStreakDangNhap() != null ? bxh.getStreakDangNhap() : 0;
        int newStreak;

        if (lastLogin == null) {
            // Lần đầu đăng nhập
            newStreak = 1;
        } else if (lastLogin.plusDays(1).equals(today)) {
            // Đăng nhập liên tục
            newStreak = currentStreak + 1;
        } else {
            // Mất streak, bắt đầu lại từ 1
            newStreak = 1;
            log.info("User {} lost streak. Last login: {}, Today: {}", userId, lastLogin, today);
        }

        // Lấy phần thưởng theo ngày trong chu kỳ
        PhanThuongDangNhap reward = PhanThuongDangNhap.getByDay(newStreak);

        // Cộng gold
        long currentGold = bxh.getTienVang() != null ? bxh.getTienVang() : 0;
        bxh.setTienVang(currentGold + reward.getGoldThuong());
        log.info("User {} claimed {} gold for day {}", userId, reward.getGoldThuong(), newStreak);

        // Cộng XP nếu có
        String vatPhamTen = null;
        String vatPhamIcon = null;
        if (reward.getXpThuong() > 0) {
            levelUpService.addXpAndProcessLevelUp(userId, reward.getXpThuong());
            log.info("User {} claimed {} XP for day {}", userId, reward.getXpThuong(), newStreak);
        }

        // Tặng vật phẩm nếu có
        if (reward.getVatPhamLoai() != null && reward.getSoLuongVatPham() > 0) {
            VatPham vatPham = vatPhamRepository.findByLoai(reward.getVatPhamLoai()).orElse(null);
            if (vatPham != null) {
                vatPhamService.grantItemToUser(userId, vatPham.getId(), reward.getSoLuongVatPham());
                vatPhamTen = vatPham.getTen();
                vatPhamIcon = vatPham.getIcon();
                log.info("User {} claimed {} x {} for day {}", userId, reward.getSoLuongVatPham(), vatPham.getTen(), newStreak);
            }
        }

        // Cập nhật streak
        bxh.setStreakDangNhap(newStreak);
        bxh.setNgayDangNhapCuoi(today);
        bangXepHangRepository.save(bxh);

        // Build thông báo
        StringBuilder sb = new StringBuilder();
        sb.append("🎉 Điểm danh ngày ").append(((newStreak - 1) % 7) + 1).append("!\n");
        sb.append("💰 +").append(reward.getGoldThuong()).append(" Gold");
        if (reward.getXpThuong() > 0) {
            sb.append("\n⭐ +").append(reward.getXpThuong()).append(" XP");
        }
        if (vatPhamTen != null) {
            sb.append("\n🎁 +").append(reward.getSoLuongVatPham()).append(" ").append(vatPhamTen);
        }

        return buildStreakResponse(bxh, true, sb.toString());
    }

    @Override
    @Transactional
    public void checkAndUpdateStreak(Long userId) throws DataNotFoundException {
        // Phương thức này được gọi khi user login, chỉ để kiểm tra streak
        // Không tự động claim reward
        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy thông tin xếp hạng"));

        LocalDate today = LocalDate.now();
        LocalDate lastLogin = bxh.getNgayDangNhapCuoi();

        // Kiểm tra nếu mất streak (không đăng nhập > 1 ngày)
        if (lastLogin != null && !lastLogin.equals(today) && !lastLogin.plusDays(1).equals(today)) {
            // Reset streak về 0
            bxh.setStreakDangNhap(0);
            bangXepHangRepository.save(bxh);
            log.info("User {} streak reset due to inactivity", userId);
        }
    }

    /**
     * Build response với thông tin streak
     */
    private LoginStreakResponse buildStreakResponse(BangXepHang bxh, boolean justClaimed, String message) {
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = bxh.getNgayDangNhapCuoi();
        int currentStreak = bxh.getStreakDangNhap() != null ? bxh.getStreakDangNhap() : 0;

        // Kiểm tra đã điểm danh hôm nay chưa
        boolean claimedToday = lastLogin != null && lastLogin.equals(today);

        // Tính ngày trong chu kỳ (1-7)
        int dayInCycle = claimedToday 
            ? ((currentStreak - 1) % 7) + 1 
            : (currentStreak % 7) + 1;

        // Ngày tiếp theo có thể claim (nếu đã claim hôm nay thì là ngày mai)
        int nextClaimDay = claimedToday ? (dayInCycle % 7) + 1 : dayInCycle;

        // Phần thưởng hôm nay (nếu chưa claim)
        PhanThuongDangNhap todayReward = PhanThuongDangNhap.getByDay(nextClaimDay);
        LoginStreakResponse.RewardDetail rewardDetail = null;
        
        if (!claimedToday) {
            VatPham vatPham = null;
            if (todayReward.getVatPhamLoai() != null) {
                vatPham = vatPhamRepository.findByLoai(todayReward.getVatPhamLoai()).orElse(null);
            }
            
            rewardDetail = LoginStreakResponse.RewardDetail.builder()
                    .gold(todayReward.getGoldThuong())
                    .xp(todayReward.getXpThuong())
                    .vatPhamTen(vatPham != null ? vatPham.getTen() : null)
                    .vatPhamIcon(vatPham != null ? vatPham.getIcon() : null)
                    .soLuongVatPham(todayReward.getSoLuongVatPham())
                    .build();
        }

        // Danh sách 7 ngày
        List<LoginStreakResponse.DayReward> days = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            PhanThuongDangNhap dayReward = PhanThuongDangNhap.getByDay(i);
            
            boolean isReceived = claimedToday && i <= dayInCycle;
            boolean isToday = !claimedToday && i == nextClaimDay;
            boolean canClaim = isToday;

            days.add(LoginStreakResponse.DayReward.builder()
                    .ngay(i)
                    .gold(dayReward.getGoldThuong())
                    .xp(dayReward.getXpThuong())
                    .coVatPham(dayReward.getVatPhamLoai() != null)
                    .moTa(dayReward.getMoTa())
                    .daNhan(isReceived)
                    .laHomNay(isToday)
                    .coTheNhan(canClaim)
                    .build());
        }

        String thongBao = message;
        if (thongBao == null) {
            if (claimedToday) {
                thongBao = "✅ Đã điểm danh hôm nay. Quay lại vào ngày mai!";
            } else {
                thongBao = "🎁 Bạn có phần thưởng đang chờ!";
            }
        }

        return LoginStreakResponse.builder()
                .streakHienTai(currentStreak)
                .ngayTrongChuKy(claimedToday ? dayInCycle : nextClaimDay)
                .daDiemDanhHomNay(claimedToday)
                .ngayDangNhapCuoi(lastLogin)
                .phanThuongHomNay(rewardDetail)
                .danhSachNgay(days)
                .thongBao(thongBao)
                .build();
    }
}
