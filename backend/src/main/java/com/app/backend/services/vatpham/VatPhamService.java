package com.app.backend.services.vatpham;

import com.app.backend.dtos.MuaVatPhamDTO;
import com.app.backend.dtos.SuDungVatPhamDTO;
import com.app.backend.dtos.cache.CauHoiCacheDTO;
import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.models.*;
import com.app.backend.models.enums.LoaiVatPham;
import com.app.backend.repositories.*;
import com.app.backend.responses.MuaVatPhamResponse;
import com.app.backend.responses.ShopResponse;
import com.app.backend.responses.SuDungVatPhamResponse;
import com.app.backend.responses.VatPhamInventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VatPhamService implements IVatPhamService {

    private final IVatPhamRepository vatPhamRepository;
    private final IVatPhamNguoiDungRepository IVatPhamNguoiDungRepository;
    private final ISuDungVatPhamTranDauRepository suDungVatPhamTranDauRepository;
    private final INguoiDungRepository nguoiDungRepository;
    private final ITranDauRepository tranDauRepository;
    private final IBangXepHangRepository bangXepHangRepository;
    private final ILichSuMuaVatPhamRepository lichSuMuaVatPhamRepository;

    // Giới hạn mua vật phẩm Epic/Legendary mỗi tuần
    private static final int MAX_EPIC_PER_WEEK = 2;
    private static final int MAX_LEGENDARY_PER_WEEK = 1;

    /**
     * Lấy danh sách inventory của user
     */
    @Override
    public List<VatPhamInventoryResponse> getInventory(Long userId) {
        List<VatPhamNguoiDung> items = IVatPhamNguoiDungRepository.findAvailableByUserId(userId);
        // chỉ lấy 2 vật phẩm trong items
//        Collections.shuffle(items);
//        if (items.size() > 2) {
//            items = items.subList(0, 2);
//        }
        return items.stream()
                .map(this::toInventoryResponse)
                .toList();
    }

    /**
     * Lấy tất cả vật phẩm đang active (cho shop/display)
     */
    @Override
    public List<VatPham> getAllActiveItems() {
        return vatPhamRepository.findByKichHoatTrue();
    }

    /**
     * Thêm vật phẩm cho user (khi thắng trận, nhận thưởng...)
     */
    @Override
    @Transactional
    public void grantItemToUser(Long userId, Long vatPhamId, int quantity) throws DataNotFoundException {
        NguoiDung user = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng"));
        VatPham vatPham = vatPhamRepository.findById(vatPhamId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy vật phẩm"));

        Optional<VatPhamNguoiDung> existing = IVatPhamNguoiDungRepository
                .findByNguoiDungIdAndVatPhamId(userId, vatPhamId);

        if (existing.isPresent()) {
            VatPhamNguoiDung inv = existing.get();
            inv.setSoLuong(inv.getSoLuong() + quantity);
            inv.setNhanLuc(LocalDateTime.now());
            IVatPhamNguoiDungRepository.save(inv);
        } else {
            VatPhamNguoiDung newInv = VatPhamNguoiDung.builder()
                    .nguoiDung(user)
                    .vatPham(vatPham)
                    .soLuong(quantity)
                    .nhanLuc(LocalDateTime.now())
                    .build();
            IVatPhamNguoiDungRepository.save(newInv);
        }

        log.info("Granted {} x {} to user {}", quantity, vatPham.getTen(), userId);
    }

    /**
     * Thêm vật phẩm theo loại
     */
    @Override
    @Transactional
    public void grantItemByType(Long userId, LoaiVatPham loai, int quantity) throws DataNotFoundException {
        VatPham vatPham = vatPhamRepository.findByLoai(loai)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy vật phẩm loại " + loai));
        grantItemToUser(userId, vatPham.getId(), quantity);
    }

    /**
     * Sử dụng vật phẩm trong trận đấu
     */
    @Override
    @Transactional
    public SuDungVatPhamResponse useItem(Long userId, SuDungVatPhamDTO dto, BattleState battleState)
            throws DataNotFoundException {

        // Xác định vật phẩm từ ID hoặc loại
        VatPham vatPham;
        if (dto.getVatPhamId() != null) {
            vatPham = vatPhamRepository.findById(dto.getVatPhamId())
                    .orElseThrow(() -> new DataNotFoundException("Không tìm thấy vật phẩm"));
        } else if (dto.getLoaiVatPham() != null) {
            vatPham = vatPhamRepository.findByLoai(dto.getLoaiVatPham())
                    .orElseThrow(() -> new DataNotFoundException("Không tìm thấy vật phẩm loại " + dto.getLoaiVatPham()));
        } else {
            throw new IllegalArgumentException("Phải cung cấp vat_pham_id hoặc loai_vat_pham");
        }

        // Kiểm tra inventory
        VatPhamNguoiDung inventory = IVatPhamNguoiDungRepository
                .findByNguoiDungIdAndVatPhamId(userId, vatPham.getId())
                .orElseThrow(() -> new DataNotFoundException("Bạn không có vật phẩm này"));

        if (inventory.getSoLuong() <= 0) {
            return SuDungVatPhamResponse.builder()
                    .thanhCong(false)
                    .thongBao("Bạn đã hết vật phẩm " + vatPham.getTen())
                    .build();
        }

        // Kiểm tra giới hạn sử dụng trong ngày (mỗi loại chỉ dùng 1 lần/ngày)
        if (hasExceededDailyLimit(userId, vatPham.getLoai())) {
            return SuDungVatPhamResponse.builder()
                    .thanhCong(false)
                    .loaiVatPham(vatPham.getLoai())
                    .thongBao("Bạn đã sử dụng " + vatPham.getTen() + " hôm nay! Mỗi loại vật phẩm chỉ được dùng 1 lần/ngày.")
                    .build();
        }

        // Kiểm tra giới hạn sử dụng trong trận (mỗi loại chỉ dùng 1 lần/trận)
        if (isLimitedPerBattle(vatPham.getLoai())) {
            boolean alreadyUsed = suDungVatPhamTranDauRepository
                    .existsByTranDauIdAndNguoiDungIdAndLoaiVatPham(dto.getTranDauId(), userId, vatPham.getLoai());
            if (alreadyUsed) {
                return SuDungVatPhamResponse.builder()
                        .thanhCong(false)
                        .loaiVatPham(vatPham.getLoai())
                        .thongBao("Vật phẩm này chỉ được dùng 1 lần trong trận!")
                        .build();
            }
        }

        // 🔥 Debug log
        log.info("🎁 useItem - battleState: tranDauId={}, currentQuestionIndex={}, danhSachCauHoi.size={}",
                battleState.getTranDauId(),
                battleState.getCurrentQuestionIndex(),
                battleState.getDanhSachCauHoi() != null ? battleState.getDanhSachCauHoi().size() : 0);

        // 🔧 FIX: Nếu currentQuestionIndex < 0 nhưng có cauHoiIndex từ frontend, sử dụng nó
        if (battleState.getCurrentQuestionIndex() < 0 && dto.getCauHoiIndex() != null && dto.getCauHoiIndex() >= 0) {
            log.info("🔧 Fixing currentQuestionIndex from DTO: {}", dto.getCauHoiIndex());
            battleState.setCurrentQuestionIndex(dto.getCauHoiIndex());
        }

        // Áp dụng hiệu ứng
        SuDungVatPhamResponse.HieuUngVatPham hieuUng = applyItemEffect(vatPham, battleState, userId);

        // Trừ số lượng
        inventory.setSoLuong(inventory.getSoLuong() - 1);
        inventory.setSuDungLuc(LocalDateTime.now());
        IVatPhamNguoiDungRepository.save(inventory);

        // Ghi lịch sử
        TranDau tranDau = tranDauRepository.findById(dto.getTranDauId()).orElse(null);
        NguoiDung user = nguoiDungRepository.findById(userId).orElse(null);

        if (tranDau != null && user != null) {
            SuDungVatPhamTranDau lichSu = SuDungVatPhamTranDau.builder()
                    .tranDau(tranDau)
                    .nguoiDung(user)
                    .vatPham(vatPham)
                    .loaiVatPham(vatPham.getLoai())
                    .cauHoiIndex(dto.getCauHoiIndex())
                    .suDungLuc(LocalDateTime.now())
                    .ketQua(hieuUng.toString())
                    .build();
            suDungVatPhamTranDauRepository.save(lichSu);
        }

        log.info("User {} used item {} in battle {}", userId, vatPham.getTen(), dto.getTranDauId());

        return SuDungVatPhamResponse.builder()
                .thanhCong(true)
                .loaiVatPham(vatPham.getLoai())
                .tenVatPham(vatPham.getTen())
                .thongBao("Đã sử dụng " + vatPham.getTen() + " thành công!")
                .hieuUng(hieuUng)
                .soLuongConLai(inventory.getSoLuong())
                .build();
    }

    /**
     * Áp dụng hiệu ứng vật phẩm vào BattleState
     */
    private SuDungVatPhamResponse.HieuUngVatPham applyItemEffect(VatPham vatPham, BattleState state, Long userId) {
        SuDungVatPhamResponse.HieuUngVatPham.HieuUngVatPhamBuilder builder =
                SuDungVatPhamResponse.HieuUngVatPham.builder();

        switch (vatPham.getLoai()) {
            case X2_DIEM:
                // Đánh dấu người chơi có x2 điểm cho câu tiếp theo
                state.getActiveMultipliers().put(userId, 2.0);
                builder.heSoDiem(2.0);
                break;

            case X3_DIEM:
                state.getActiveMultipliers().put(userId, 3.0);
                builder.heSoDiem(3.0);
                break;

            // DONG_BANG_THOI_GIAN đã bị loại bỏ - không hợp lý trong gameplay

            case GOI_Y_50_50:
                // Loại bỏ 2 đáp án sai
                CauHoiCacheDTO currentQuestion = state.getCurrentQuestion();
                if (currentQuestion != null) {
                    List<String> wrongAnswers = getWrongAnswers(currentQuestion);
                    Collections.shuffle(wrongAnswers);
                    List<String> toRemove = wrongAnswers.subList(0, Math.min(2, wrongAnswers.size()));
                    state.getEliminatedOptions().put(userId, new HashSet<>(toRemove));
                    builder.dapAnBiLoai(toRemove);
                }
                break;

            case KHIEN_BAO_VE:
                // Bảo vệ combo cho câu tiếp theo
                state.getShieldedPlayers().add(userId);
                builder.baoVeCombo(true);
                break;

            case BO_QUA_CAU_HOI:
                // Đánh dấu bỏ qua câu này, không tính điểm
                state.getSkippedQuestions().computeIfAbsent(userId, k -> new HashSet<>())
                        .add(state.getCurrentQuestionIndex());
                builder.boQuaThanhCong(true);
                break;

            case HIEN_DAP_AN:
                // Hiển thị đáp án đúng (rất hiếm)
//                log.info("🎁 HIEN_DAP_AN - currentQuestionIndex: {}, danhSachCauHoi size: {}",
//                        state.getCurrentQuestionIndex(),
//                        state.getDanhSachCauHoi() != null ? state.getDanhSachCauHoi().size() : 0);
                CauHoiCacheDTO q = state.getCurrentQuestion();
                // Tránh gọi toString() trên entity để tránh LazyInitializationException
//                log.info("🎁 HIEN_DAP_AN - currentQuestion id: {}", q != null ? q.getId() : "null");
                if (q != null) {
                    String correctAnswer = String.valueOf(q.getDapAnDung());
//                    log.info("🎁 HIEN_DAP_AN - dapAnDung: {}", correctAnswer);
                    builder.dapAnDung(correctAnswer);
                }
//                else {
//                    log.warn("🎁 HIEN_DAP_AN - currentQuestion is NULL! index={}, listSize={}",
//                            state.getCurrentQuestionIndex(),
//                            state.getDanhSachCauHoi() != null ? state.getDanhSachCauHoi().size() : 0);
//                }
                break;
        }

        return builder.build();
    }

    /**
     * Lấy các đáp án sai của câu hỏi
     */
    private List<String> getWrongAnswers(CauHoiCacheDTO cauHoi) {
        List<String> wrong = new ArrayList<>();
        String correct = String.valueOf(cauHoi.getDapAnDung());
        for (String opt : Arrays.asList("A", "B", "C", "D")) {
            if (!opt.equals(correct)) {
                wrong.add(opt);
            }
        }
        return wrong;
    }

    /**
     * Kiểm tra vật phẩm có giới hạn 1 lần/trận không
     * TẤT CẢ vật phẩm đều giới hạn 1 lần/trận để đảm bảo công bằng
     */
    private boolean isLimitedPerBattle(LoaiVatPham loai) {
        // Tất cả vật phẩm đều giới hạn 1 lần/trận
        return true;
    }

    /**
     * Kiểm tra user đã vượt quá giới hạn sử dụng vật phẩm trong ngày chưa
     * Mỗi loại vật phẩm chỉ được dùng 1 lần/ngày
     */
    private boolean hasExceededDailyLimit(Long userId, LoaiVatPham loai) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        int usageToday = suDungVatPhamTranDauRepository.countUsageToday(userId, loai, startOfDay, endOfDay);
        return usageToday >= 1; // Giới hạn 1 lần/ngày cho mỗi loại
    }

    /**
     * Thưởng vật phẩm ngẫu nhiên sau trận đấu
     */
    @Override
    @Transactional
    public VatPham rewardRandomItem(Long userId, boolean isWinner) throws DataNotFoundException {
        List<VatPham> activeItems = vatPhamRepository.findByKichHoatTrue();
        if (activeItems.isEmpty()) return null;

        // Xác suất nhận item cao hơn nếu thắng
        double chance = isWinner ? 0.4 : 0.15;
        if (Math.random() > chance) return null;

        // Weighted random theo độ hiếm
        List<VatPham> eligibleItems = new ArrayList<>();
        for (VatPham item : activeItems) {
            int weight = switch (item.getDoHiem()) {
                case "LEGENDARY" -> 1;
                case "EPIC" -> 3;
                case "RARE" -> 6;
                default -> 10; // COMMON
            };
            for (int i = 0; i < weight; i++) {
                eligibleItems.add(item);
            }
        }

        if (eligibleItems.isEmpty()) return null;

        VatPham selected = eligibleItems.get(new Random().nextInt(eligibleItems.size()));
        grantItemToUser(userId, selected.getId(), 1);
        return selected;
    }

    /**
     * Khởi tạo vật phẩm mặc định (chạy khi startup)
     */
    @Override
    @Transactional
    public void initDefaultItems() {
        if (vatPhamRepository.count() > 0) return;

        List<VatPham> defaults = Arrays.asList(
                VatPham.builder()
                        .ten("Nhân đôi điểm")
                        .moTa("Nhân đôi điểm cho câu trả lời đúng tiếp theo")
                        .loai(LoaiVatPham.X2_DIEM)
                        .giaTriHieuUng(2.0)
                        .icon("⚡")
                        .mauSac("#FFD700")
                        .doHiem("COMMON")
                        .giaXu(100)
                        .build(),

                VatPham.builder()
                        .ten("Đóng băng thời gian")
                        .moTa("Dừng đồng hồ thêm 5 giây")
                        .loai(LoaiVatPham.DONG_BANG_THOI_GIAN)
                        .thoiGianHieuLucGiay(5)
                        .icon("❄️")
                        .mauSac("#00BFFF")
                        .doHiem("COMMON")
                        .giaXu(80)
                        .build(),

                VatPham.builder()
                        .ten("Gợi ý 50/50")
                        .moTa("Loại bỏ 2 đáp án sai")
                        .loai(LoaiVatPham.GOI_Y_50_50)
                        .giaTriHieuUng(2.0)
                        .icon("🎯")
                        .mauSac("#9932CC")
                        .doHiem("RARE")
                        .giaXu(150)
                        .build(),

                VatPham.builder()
                        .ten("Khiên bảo vệ")
                        .moTa("Bảo vệ combo khi trả lời sai 1 lần")
                        .loai(LoaiVatPham.KHIEN_BAO_VE)
                        .icon("🛡️")
                        .mauSac("#228B22")
                        .doHiem("RARE")
                        .giaXu(120)
                        .build(),

                VatPham.builder()
                        .ten("Bỏ qua câu hỏi")
                        .moTa("Bỏ qua câu hỏi hiện tại mà không mất điểm hay combo")
                        .loai(LoaiVatPham.BO_QUA_CAU_HOI)
                        .icon("⏭️")
                        .mauSac("#FF6347")
                        .doHiem("EPIC")
                        .giaXu(200)
                        .build(),

                VatPham.builder()
                        .ten("Nhân ba điểm")
                        .moTa("Nhân ba điểm cho câu trả lời đúng tiếp theo (1 lần/trận)")
                        .loai(LoaiVatPham.X3_DIEM)
                        .giaTriHieuUng(3.0)
                        .icon("💎")
                        .mauSac("#E6E6FA")
                        .doHiem("EPIC")
                        .giaXu(300)
                        .build(),

                VatPham.builder()
                        .ten("Tiết lộ đáp án")
                        .moTa("Hiển thị đáp án đúng (cực hiếm, 1 lần/trận)")
                        .loai(LoaiVatPham.HIEN_DAP_AN)
                        .icon("👁️")
                        .mauSac("#FF1493")
                        .doHiem("LEGENDARY")
                        .giaXu(500)
                        .build()
        );

        vatPhamRepository.saveAll(defaults);
    }

    private VatPhamInventoryResponse toInventoryResponse(VatPhamNguoiDung inv) {
        VatPham vp = inv.getVatPham();
        return VatPhamInventoryResponse.builder()
                .vatPhamId(vp.getId())
                .ten(vp.getTen())
                .moTa(vp.getMoTa())
                .loai(vp.getLoai())
                .icon(vp.getIcon())
                .mauSac(vp.getMauSac())
                .doHiem(vp.getDoHiem())
                .soLuong(inv.getSoLuong())
                .giaTriHieuUng(vp.getGiaTriHieuUng())
                .thoiGianHieuLucGiay(vp.getThoiGianHieuLucGiay())
                .build();
    }

    // ==================== SHOP METHODS ====================

    /**
     * Lấy danh sách vật phẩm trong Shop với thông tin giới hạn mua
     */
    @Override
    public ShopResponse getShop(Long userId) throws DataNotFoundException {
        // Lấy thông tin user và số xu hiện tại
        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy thông tin xếp hạng của user"));

        Long userGold = bxh.getTienVang() != null ? bxh.getTienVang() : 0L;

        // Lấy danh sách vật phẩm active (không bao gồm DONG_BANG_THOI_GIAN)
        List<VatPham> activeItems = vatPhamRepository.findByKichHoatTrue();

        // Tính toán giới hạn mua trong tuần
        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);

        List<ShopResponse.ShopItemResponse> shopItems = new ArrayList<>();

        for (VatPham vp : activeItems) {
            int soLuongConLaiTuan;
            String thongBaoGioiHan = null;

            // Tính giới hạn dựa trên độ hiếm
            if ("LEGENDARY".equals(vp.getDoHiem())) {
                int daMua = lichSuMuaVatPhamRepository.countPurchasesByRarityInPeriod(
                        userId, "LEGENDARY", startOfWeek, endOfWeek);
                soLuongConLaiTuan = Math.max(0, MAX_LEGENDARY_PER_WEEK - daMua);
                if (soLuongConLaiTuan == 0) {
                    thongBaoGioiHan = "Đã đạt giới hạn " + MAX_LEGENDARY_PER_WEEK + " vật phẩm Legendary/tuần";
                }
            } else if ("EPIC".equals(vp.getDoHiem())) {
                int daMua = lichSuMuaVatPhamRepository.countPurchasesByRarityInPeriod(
                        userId, "EPIC", startOfWeek, endOfWeek);
                soLuongConLaiTuan = Math.max(0, MAX_EPIC_PER_WEEK - daMua);
                if (soLuongConLaiTuan == 0) {
                    thongBaoGioiHan = "Đã đạt giới hạn " + MAX_EPIC_PER_WEEK + " vật phẩm Epic/tuần";
                }
            } else {
                // Common và Rare không giới hạn
                soLuongConLaiTuan = 99;
            }

            shopItems.add(ShopResponse.ShopItemResponse.fromVatPham(vp, userGold, soLuongConLaiTuan, thongBaoGioiHan));
        }

        return ShopResponse.builder()
                .vatPhamList(shopItems)
                .tienVangHienTai(userGold)
                .build();
    }

    /**
     * Mua vật phẩm từ Shop
     */
    @Override
    @Transactional
    public MuaVatPhamResponse purchaseItem(Long userId, MuaVatPhamDTO dto) throws DataNotFoundException {
        // Validate số lượng
        int soLuong = dto.getSoLuong() != null && dto.getSoLuong() > 0 ? dto.getSoLuong() : 1;

        // Lấy vật phẩm
        VatPham vatPham = vatPhamRepository.findById(dto.getVatPhamId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy vật phẩm"));

        if (!vatPham.getKichHoat()) {
            return MuaVatPhamResponse.builder()
                    .thanhCong(false)
                    .thongBao("Vật phẩm này không còn được bán")
                    .build();
        }

        // Lấy thông tin user
        NguoiDung user = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng"));

        BangXepHang bxh = bangXepHangRepository.findByNguoiDungId(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy thông tin xếp hạng"));

        Long userGold = bxh.getTienVang() != null ? bxh.getTienVang() : 0L;
        int tongGia = vatPham.getGiaXu() * soLuong;

        // Kiểm tra đủ tiền
        if (userGold < tongGia) {
            return MuaVatPhamResponse.builder()
                    .thanhCong(false)
                    .thongBao("Không đủ xu! Bạn cần " + tongGia + " xu nhưng chỉ có " + userGold + " xu")
                    .tienVangConLai(userGold)
                    .build();
        }

        // Kiểm tra giới hạn mua tuần (cho Epic/Legendary)
        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);

        if ("LEGENDARY".equals(vatPham.getDoHiem())) {
            int daMua = lichSuMuaVatPhamRepository.countPurchasesByRarityInPeriod(
                    userId, "LEGENDARY", startOfWeek, endOfWeek);
            if (daMua + soLuong > MAX_LEGENDARY_PER_WEEK) {
                return MuaVatPhamResponse.builder()
                        .thanhCong(false)
                        .thongBao("Đã đạt giới hạn " + MAX_LEGENDARY_PER_WEEK + " vật phẩm Legendary/tuần! " +
                                "Bạn đã mua " + daMua + ", chỉ còn " + (MAX_LEGENDARY_PER_WEEK - daMua) + " slot")
                        .build();
            }
        } else if ("EPIC".equals(vatPham.getDoHiem())) {
            int daMua = lichSuMuaVatPhamRepository.countPurchasesByRarityInPeriod(
                    userId, "EPIC", startOfWeek, endOfWeek);
            if (daMua + soLuong > MAX_EPIC_PER_WEEK) {
                return MuaVatPhamResponse.builder()
                        .thanhCong(false)
                        .thongBao("Đã đạt giới hạn " + MAX_EPIC_PER_WEEK + " vật phẩm Epic/tuần! " +
                                "Bạn đã mua " + daMua + ", chỉ còn " + (MAX_EPIC_PER_WEEK - daMua) + " slot")
                        .build();
            }
        }

        // Trừ tiền
        bxh.setTienVang(userGold - tongGia);
        bangXepHangRepository.save(bxh);

        // Thêm vật phẩm vào inventory
        grantItemToUser(userId, vatPham.getId(), soLuong);

        // Ghi lịch sử mua
        LichSuMuaVatPham lichSu = LichSuMuaVatPham.builder()
                .nguoiDung(user)
                .vatPham(vatPham)
                .soLuong(soLuong)
                .giaMua(vatPham.getGiaXu())
                .tongGia(tongGia)
                .muaLuc(LocalDateTime.now())
                .build();
        lichSuMuaVatPhamRepository.save(lichSu);

        // Lấy số lượng trong inventory sau khi mua
        int inventoryCount = IVatPhamNguoiDungRepository
                .findByNguoiDungIdAndVatPhamId(userId, vatPham.getId())
                .map(VatPhamNguoiDung::getSoLuong)
                .orElse(soLuong);

        log.info("User {} purchased {} x {} for {} gold", userId, soLuong, vatPham.getTen(), tongGia);

        return MuaVatPhamResponse.builder()
                .thanhCong(true)
                .thongBao("Đã mua " + soLuong + " x " + vatPham.getTen() + " thành công!")
                .tenVatPham(vatPham.getTen())
                .soLuong(soLuong)
                .tongGia(tongGia)
                .tienVangConLai(bxh.getTienVang())
                .soLuongTrongInventory(inventoryCount)
                .build();
    }
}
