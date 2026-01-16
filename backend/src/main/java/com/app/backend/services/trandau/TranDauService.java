package com.app.backend.services.trandau;

import com.app.backend.components.BattleLoopTask;
import com.app.backend.components.BattleStateManager;
import com.app.backend.components.BattleWsPublisher;
import com.app.backend.dtos.*;
import com.app.backend.dtos.cache.CauHoiCacheDTO;
import com.app.backend.exceptions.DataNotFoundException;
import com.app.backend.exceptions.PermissionDenyException;
import com.app.backend.models.*;
import com.app.backend.models.constant.*;
import com.app.backend.models.enums.RankTier;
import com.app.backend.repositories.*;
import com.app.backend.responses.achievement.AchievementResponse;
import com.app.backend.responses.admin.QuestionAnswersAdminResponse;
import com.app.backend.responses.lichsutrandau.LichSuTranDauResponse;
import com.app.backend.responses.trandau.*;
import com.app.backend.responses.websocket.FinishedEvent;
import com.app.backend.responses.websocket.LeaderboardUpdateEvent;
import com.app.backend.responses.LevelUpResponse;
import com.app.backend.services.bangxephang.IBangXepHangService;
import com.app.backend.services.bangxephang.ILeaderboardRedisService;
import com.app.backend.services.cauhoi.ICauHoiRedisService;
import com.app.backend.services.levelup.ILevelUpService;
import com.app.backend.services.nhiemvu.INhiemVuService;
import com.app.backend.services.notification.IThongBaoService;
import com.app.backend.services.thanhtich.IThanhTichService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranDauService implements ITranDauService {

    private final ITranDauRepository tranDauRepository;
    private final INguoiChoiTranDauRepository nguoiChoiTranDauRepository;
    private final IBoCauHoiRepository boCauHoiRepository;
    private final ICauHoiRepository cauHoiRepository;
    private final INguoiDungRepository nguoiDungRepository;
    private final BattleStateManager battleStateManager;
    private final BattleLoopTask battleLoopTask;
    private final BattleWsPublisher wsPublisher;
    private final ITraLoiTranDauRepository traLoiTranDauRepository;
    private final ILichSuTranDauRepository lichSuTranDauRepository;
    private final IBangXepHangRepository bangXepHangRepository;
    private final IThanhTichBoCauHoiRepository thanhTichBoCauHoiRepository;
    private final IKetBanRepository ketBanRepository;
    private final IThongBaoService thongBaoService;
    private final IBangXepHangService bangXepHangService;
    private final ILeaderboardRedisService leaderboardRedisService;
    private final ICauHoiRedisService cauHoiRedisService;
    private final IThanhTichService thanhTichService;
    private final ILevelUpService levelUpService;
    private final INhiemVuService nhiemVuService;


    /**
     * Tạo mã phòng ngẫu nhiên
     */
    private String generateRoomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    @Transactional
    @Override
    public TranDau taoPhong(TaoTranDauDTO taoTranDauDTO, Long currentUserId) throws Exception {
        NguoiDung host = nguoiDungRepository.findById(currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Người dùng không tồn tại"));

        BoCauHoi bo = boCauHoiRepository.findById(taoTranDauDTO.getBoCauHoiId())
                .orElseThrow(() -> new DataNotFoundException("Bộ câu hỏi không tồn tại"));

        if (Boolean.FALSE.equals(taoTranDauDTO.getCongKhai()) &&
                (taoTranDauDTO.getMaPin() == null || taoTranDauDTO.getMaPin().isBlank())) {
            throw new IllegalArgumentException("Phòng riêng tư bắt buộc có mã PIN");
        }

        // Bắt buộc bộ đã được duyệt
        if (!TrangThaiBoCauHoi.DA_DUYET.equals(bo.getTrangThai())) {
            throw new IllegalArgumentException("Bộ câu hỏi này chưa được duyệt, không thể dùng để thi đấu");
        }

        // Đảm bảo bộ Official luôn ở chế độ PRIVATE (ẩn đề)
        if (!CheDoHienThi.RIENG_TU.equals(bo.getCheDoHienThi())) {
            throw new IllegalArgumentException("Bộ câu hỏi Official phải ở chế độ PRIVATE");
        }

        if (taoTranDauDTO.getGioiHanNguoiChoi() < 2 || taoTranDauDTO.getGioiHanNguoiChoi() > 30) {
            throw new IllegalArgumentException("Giới hạn người chơi phải từ 2 – 30");
        }
        TranDau tranDau = new TranDau();
        tranDau.setTenPhong(taoTranDauDTO.getTenPhong());
        tranDau.setBoCauHoi(bo);
        tranDau.setChuPhong(host);
        tranDau.setCongKhai(taoTranDauDTO.getCongKhai());
        tranDau.setMaPin(taoTranDauDTO.getCongKhai() ? null : taoTranDauDTO.getMaPin());
        tranDau.setMaPhong(generateRoomCode(6));
        // Chế độ CASUAL / RANKED
        String loaiTranDau = taoTranDauDTO.getLoaiTranDau();
        if (!LoaiTranDau.THUONG.equals(loaiTranDau) && !LoaiTranDau.XEP_HANG.equals(loaiTranDau)) {
            loaiTranDau = LoaiTranDau.THUONG; // fallback an toàn
        }
        tranDau.setLoaiTranDau(loaiTranDau);

        tranDau.setGioiHanNguoiChoi(taoTranDauDTO.getGioiHanNguoiChoi());
        tranDau.setGioiHanThoiGianCauGiay(taoTranDauDTO.getGioiHanThoiGianCauGiay());
        // Luật tính điểm nếu có enum:
        tranDau.setLuatTinhDiem(taoTranDauDTO.getLuatTinhDiem());
        tranDau.setTrangThai(TrangThaiTranDau.CHO);

        TranDau saved = tranDauRepository.save(tranDau);

        // tự động cho host vào phòng
        nguoiChoiTranDauRepository.save(
                NguoiChoiTranDau.builder().tranDau(saved).nguoiDung(host).build()
        );

        return saved;
    }

    @Transactional
    @Override
    public TranDau thamGia(ThamGiaTranDauDTO thamGiaTranDauDTO, Long currentUserId) throws Exception {
        TranDau tranDau = tranDauRepository.findById(thamGiaTranDauDTO.getTranDauId())
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        if (!Objects.equals(tranDau.getTrangThai(), TrangThaiTranDau.CHO)) {
            throw new IllegalStateException("Phòng không ở trạng thái chờ");
        }

        long current = nguoiChoiTranDauRepository.countByTranDau_Id(tranDau.getId());
        if (current >= tranDau.getGioiHanNguoiChoi()) {
            throw new IllegalStateException("Phòng đã đủ người");
        }

        // 🔒 Nếu phòng private → yêu cầu mã PIN
        if (Boolean.FALSE.equals(tranDau.getCongKhai())) {
            if (thamGiaTranDauDTO.getMaPin() == null || !thamGiaTranDauDTO.getMaPin().equals(tranDau.getMaPin())) {
                throw new SecurityException("Sai mã PIN hoặc phòng này riêng tư");
            }
        }

        // đã tham gia chưa?
        boolean existed = nguoiChoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_Id(tranDau.getId(), currentUserId)
                .isPresent();
        if (existed) return tranDau; // idempotent

        NguoiDung user = nguoiDungRepository.findById(currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Người dùng không tồn tại"));

        nguoiChoiTranDauRepository.save(
                NguoiChoiTranDau.builder().tranDau(tranDau).nguoiDung(user).build()
        );

        // Phát WS
        int soNguoi = (int) nguoiChoiTranDauRepository.countByTranDau_Id(tranDau.getId());
        if (soNguoi > tranDau.getGioiHanNguoiChoi()) {
            throw new IllegalStateException("Phòng đã đủ số lượng người chơi.");
        }
        wsPublisher.publishPlayerJoined(tranDau.getId(), user.getId(), user.getHoTen(), soNguoi);

        // Phát bảng xếp hạng rỗng
        updateAndBroadcastLeaderboard(tranDau.getId(), null);

        return tranDau;
    }

    @Transactional
    @Override
    public void roiPhong(RoiTranDauDTO dto, Long currentUserId) throws Exception {
        TranDau tranDau = tranDauRepository.findById(dto.getTranDauId())
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        NguoiDung user = nguoiDungRepository.findById(currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Người dùng không tồn tại"));

        if (tranDau.getTrangThai() == TrangThaiTranDau.DANG_CHOI
                && tranDau.getChuPhong() != null
                && tranDau.getChuPhong().getId().equals(user.getId())) {
            throw new IllegalStateException("Chủ phòng không thể rời phòng khi trận đang diễn ra. Hãy kết thúc trận trước.");
        }

        NguoiChoiTranDau nctd = nguoiChoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_Id(tranDau.getId(), currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Bạn chưa ở trong phòng"));
        // Nếu host rời phòng khi đang PENDING → có thể chuyển host cho người khác hoặc xoá phòng.
        // Bước 1: đơn giản là xoá người chơi ra khỏi phòng.

        nguoiChoiTranDauRepository.delete(nctd);

        int soNguoi = (int) nguoiChoiTranDauRepository.countByTranDau_Id(tranDau.getId());
        wsPublisher.publishPlayerLeft(tranDau.getId(), nctd.getNguoiDung().getId(), nctd.getNguoiDung().getHoTen(), soNguoi);

//        // Nếu không còn ai trong phòng → xoá phòng
        long remain = nguoiChoiTranDauRepository.countByTranDau_Id(tranDau.getId());

        if (Objects.equals(tranDau.getChuPhong().getId(), currentUserId) && soNguoi > 0) {
            nguoiChoiTranDauRepository.findFirstByTranDau_IdOrderByIdAsc(tranDau.getId())
                    .ifPresent(next -> tranDau.setChuPhong(next.getNguoiDung()));
        }

        // Nếu phòng trống và chưa bắt đầu → xoá

        if (soNguoi == 0 && TrangThaiTranDau.CHO.equals(tranDau.getTrangThai()))
            tranDauRepository.delete(tranDau);

        updateAndBroadcastLeaderboard(tranDau.getId(), null);

    }

    @Transactional(readOnly = true)
    @Override
    public TranDau chiTietPhong(Long tranDauId) throws Exception {
        return tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));
    }

    @Transactional(readOnly = true)
    @Override
    public TranDauResponse getBattleDetailResponse(Long tranDauId) throws Exception {
        // 1. Lấy thông tin trận đấu
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        // 2. 🔥 Tính số lượng người chơi hiện tại trong phòng
        int soLuong = (int) nguoiChoiTranDauRepository.countByTranDau_Id(tranDauId);

        // 3. Map sang DTO và trả về (truyền số lượng vào)
        return TranDauResponse.fromEntity(td, soLuong);
    }

    @Transactional(readOnly = true)
    @Override
    public TranDauResponse getBattleDetailResponse(Long tranDauId, Long currentUserId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        int soLuong = (int) nguoiChoiTranDauRepository.countByTranDau_Id(tranDauId);

        // 👇 kiểm tra user hiện tại có đang ở bảng nguoi_choi_tran_dau không
        boolean daThamGia = nguoiChoiTranDauRepository
                .existsByTranDauIdAndNguoiDungId(tranDauId, currentUserId);

        TranDauResponse res = TranDauResponse.fromEntity(td, soLuong);
        res.setDaThamGia(daThamGia);
        return res;
    }


    @Transactional(readOnly = true)
    @Override
    public Page<TranDau> danhSachPhongCho(PageRequest pageRequest) {
        return tranDauRepository.findByTrangThai(TrangThaiTranDau.CHO, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TranDau> danhSachPhongCho(PageRequest pageRequest, String loaiTranDau) {
        if (loaiTranDau == null || loaiTranDau.isBlank()) {
            // Tất cả
            return tranDauRepository.findByTrangThai(TrangThaiTranDau.CHO, pageRequest);
        }
        // Chỉ CASUAL hoặc RANKED
        return tranDauRepository.findByTrangThaiAndLoaiTranDau(
                TrangThaiTranDau.CHO,
                loaiTranDau,
                pageRequest
        );
    }


    @Override
    @Transactional
    public BattleStartResponse startBattle(Long tranDauId, Long currentUserId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        // 🔒 Chỉ chủ phòng mới được bắt đầu
        if (!td.getChuPhong().getId().equals(currentUserId)) {
            throw new SecurityException("Chỉ chủ phòng mới có quyền bắt đầu trận");
        }
        // nếu phòng chỉ có 1 người thì không đượcbatwtws đâ
//        long soNguoi = nguoiChoiTranDauRepository.countByTranDau_Id(td.getId());
//        if (soNguoi < 2) {
//            throw new IllegalStateException("Phòng phải có ít nhất 2 người chơi để bắt đầu trận");
//        }
        // ⛔ Không được start nếu đang không ở trạng thái chờ
        if (!TrangThaiTranDau.CHO.equals(td.getTrangThai())) {
            throw new IllegalStateException("Phòng không ở trạng thái chờ");
        }

        // 📋 Lấy danh sách câu hỏi (ưu tiên từ Redis cache - dạng DTO)
        Long boCauHoiId = td.getBoCauHoi().getId();
        List<CauHoiCacheDTO> danhSachCauHoi = cauHoiRedisService.getQuestionsByBoCauHoiId(boCauHoiId);
        if (danhSachCauHoi == null) {
            // Cache miss -> query DB và convert sang DTO
            List<CauHoi> entities = cauHoiRepository.findByBoCauHoiId(boCauHoiId);
            danhSachCauHoi = entities.stream()
                    .map(CauHoiCacheDTO::fromEntity)
                    .collect(Collectors.toList());
            // Save to cache for next time
            cauHoiRedisService.saveQuestionsByBoCauHoiId(boCauHoiId, danhSachCauHoi);
        }
        if (danhSachCauHoi.isEmpty()) {
            throw new IllegalStateException("Bộ câu hỏi này không có câu hỏi nào");
        }

        // 🔀 Trộn câu hỏi (tạo bản copy để không ảnh hưởng cache)
        danhSachCauHoi = new ArrayList<>(danhSachCauHoi);
        Collections.shuffle(danhSachCauHoi);

        // 🧭 Cập nhật DB
        td.setTrangThai(TrangThaiTranDau.DANG_CHOI);
        td.setBatDauLuc(Instant.now());
        tranDauRepository.save(td);

        // 🧠 Khởi tạo BattleState mới trong RAM
        BattleState state = new BattleState();
        state.setTranDauId(td.getId());
        state.setDanhSachCauHoi(danhSachCauHoi);
        state.setStartTime(Instant.now());

        // ⏱ Thiết lập thời gian mỗi câu
        int seconds = (td.getGioiHanThoiGianCauGiay() != null)
                ? td.getGioiHanThoiGianCauGiay()
                : 15;
        state.setSecondsPerQuestion(seconds);

        // 🧍‍♂️ Khởi tạo điểm 0 cho toàn bộ người chơi
        Set<Long> playerIds = nguoiChoiTranDauRepository
                .findByTranDau_Id(td.getId(), null)
                .stream()
                .map(nctd -> nctd.getNguoiDung().getId())
                .collect(Collectors.toSet());
        // cũng thêm cả host vào
        playerIds.add(td.getChuPhong().getId());
        state.initScoresForPlayers(playerIds);

        // Lưu state vào manager
        battleStateManager.save(state);

        wsPublisher.publishBattleStarted(td.getId(), td.getTenPhong(), td.getBatDauLuc(), danhSachCauHoi.size(), seconds, 10);
        battleLoopTask.runAutoLoop(td.getId(), seconds);

        // ✅ Trả kết quả khởi tạo
        return BattleStartResponse.fromCache(td, danhSachCauHoi);
    }

    @Override
    @Transactional
    public SubmitAnswerResponse submitAnswer(SubmitAnswerDTO dto, Long currentUserId) throws Exception {
        // 1️⃣ Kiểm tra trận đấu hợp lệ
        TranDau td = tranDauRepository.findById(dto.getTranDauId())
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        if (!TrangThaiTranDau.DANG_CHOI.equals(td.getTrangThai())) {
            throw new IllegalStateException("Phòng không ở trạng thái đang diễn ra");
        }

        // 2️⃣ Xác nhận người chơi có trong phòng
        boolean inRoom = nguoiChoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_Id(td.getId(), currentUserId)
                .isPresent();
        if (!inRoom && !Objects.equals(td.getChuPhong().getId(), currentUserId)) {
            throw new SecurityException("Bạn không ở trong phòng này");
        }

        // 3️⃣ Lấy BattleState hiện tại
        BattleState state = battleStateManager.get(td.getId());
        if (state == null || state.getCurrentQuestionIndex() < 0) {
            throw new IllegalStateException("Chưa có câu hỏi nào đang bật");
        }

        int idx = state.getCurrentQuestionIndex();
        CauHoiCacheDTO q = state.getDanhSachCauHoi().get(idx);

        if (!Objects.equals(q.getId(), dto.getCauHoiId())) {
            throw new IllegalArgumentException("Câu hỏi không khớp với câu hiện tại");
        }

        // 3.5️⃣ Kiểm tra câu hỏi đã bị skip chưa
        Set<Integer> userSkipped = state.getSkippedQuestions().get(currentUserId);
        if (userSkipped != null && userSkipped.contains(idx)) {
            // Đã dùng vật phẩm bỏ qua câu này, trả về thành công nhưng không tính điểm
            return SubmitAnswerResponse.builder()
                    .correct(false)
                    .gainedPoints(0)
                    .totalPoints(state.getDiemNguoiChoi().getOrDefault(currentUserId, 0))
                    .questionIndex(idx)
                    .shieldUsed(false)
                    .build();
        }

        // 4️⃣ Ghi nhận đáp án (atomic)
        String ans = dto.getAnswer().trim().toUpperCase();
        boolean firstSubmit = state.recordAnswer(idx, currentUserId, ans);
        if (!firstSubmit) {
            throw new IllegalStateException("Bạn đã nộp đáp án cho câu này rồi");
        }

        // 5️⃣ Kiểm tra timeout
        int seconds = state.getSecondsPerQuestion();
        long totalMs = seconds * 1000L;
        long elapsedMs = Duration.between(state.getCurrentQuestionStart(), Instant.now()).toMillis();
        boolean withinTime = elapsedMs <= totalMs;
        
        // 5.1) 🛡️ ANTI-HACK: Kiểm tra thời gian tối thiểu (1.5 giây)
        // Nếu trả lời quá nhanh (<1.5s), giảm điểm đáng kể để chống người chơi đã biết đáp án
        final long MIN_ANSWER_TIME_MS = 1500L; // 1.5 giây
        boolean suspiciouslyFast = elapsedMs < MIN_ANSWER_TIME_MS;

        // 6️⃣ Tính điểm & combo
        boolean correct = withinTime && ans.equalsIgnoreCase(String.valueOf(q.getDapAnDung()));

        // 6.0) Kiểm tra khiên bảo vệ trước khi update combo
        boolean shieldUsed = false;
        if (!correct && state.getShieldedPlayers().contains(currentUserId)) {
            // Có khiên: không reset combo, xóa khiên
            state.getShieldedPlayers().remove(currentUserId);
            shieldUsed = true;
        }

        // 6.1) Cập nhật combo trong state (nếu có khiên thì coi như đúng để giữ combo)
        int comboStreak = state.updateCombo(currentUserId, idx, correct || shieldUsed);

        int basePoints = 0;
        int comboBonus = 0;
        int gained = 0;
        double comboMultiplier = 1.0;

        if (correct) {
            // a) Điểm cơ bản / speed bonus
            if (LuatTinhDiem.THUONG_TOC_DO.equalsIgnoreCase(td.getLuatTinhDiem())) {
                long remain = Math.max(0, totalMs - elapsedMs);
                double ratio = (double) remain / (double) totalMs;

                // Tối thiểu 100, tối đa 1000
                basePoints = (int) Math.max(100, Math.round(1000 * ratio));
                
                // 🛡️ ANTI-HACK: Giảm 70% điểm nếu trả lời quá nhanh (<1.5s)
                if (suspiciouslyFast) {
                    basePoints = (int) Math.round(basePoints * 0.3);
                }
            } else {
                basePoints = 100;
                // Chế độ thường cũng áp dụng penalty nếu quá nhanh
                if (suspiciouslyFast) {
                    basePoints = 30;
                }
            }

            // b) Bonus theo combo
            // Bạn có thể chỉnh lại ngưỡng cho hợp game:
            boolean isRanked = LoaiTranDau.XEP_HANG.equals(td.getLoaiTranDau());
            if (comboStreak >= 3 && comboStreak <= 4) {
                comboMultiplier = isRanked ? 1.10 : 1.05;
            } else if (comboStreak >= 5 && comboStreak <= 6) {
                comboMultiplier = isRanked ? 1.20 : 1.10;
            } else if (comboStreak >= 7) {
                comboMultiplier = isRanked ? 1.30 : 1.15;
            } else {
                comboMultiplier = 1.0;
            }

            // c) ⚡ Power-up multiplier (x2, x3...)
            double powerupMultiplier = state.getActiveMultipliers().getOrDefault(currentUserId, 1.0);
            if (powerupMultiplier > 1.0) {
                // Áp dụng power-up multiplier và reset
                comboMultiplier *= powerupMultiplier;
                state.getActiveMultipliers().remove(currentUserId);
            }

            gained = (int) Math.round(basePoints * comboMultiplier);
            comboBonus = gained - basePoints;
        } else {
            // Sai → combo đã được xử lý ở bước 6.0 (khiên bảo vệ) và 6.1 (updateCombo)
            gained = 0;
            comboBonus = 0;
            comboMultiplier = 0.0;
            // Reset power-up multiplier nếu có (không được dùng vì trả lời sai)
            state.getActiveMultipliers().remove(currentUserId);
        }

        // 7️⃣ Cập nhật điểm tổng (trong RAM)
        int total = state.addScore(currentUserId, gained);
        battleStateManager.save(state);


        // ⭐ 7.1) Cập nhật DB: diem & so_cau_dung của người nộp
        NguoiDung user = nguoiDungRepository.findById(currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Người dùng không tồn tại"));

        NguoiChoiTranDau player = nguoiChoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_Id(td.getId(), user.getId())
                .orElseThrow(() -> new DataNotFoundException("Bạn chưa tham gia trận"));

        int cur = Optional.ofNullable(player.getDiem()).orElse(0);
        player.setDiem(cur + gained);
        if (correct) {
            player.setSoCauDung(Optional.ofNullable(player.getSoCauDung()).orElse(0) + 1);
        }
        nguoiChoiTranDauRepository.save(player);
        traLoiTranDauRepository.save(TraLoiTranDau.builder()
                .tranDau(td)
                .nguoiDung(user)
                .cauHoi(cauHoiRepository.getReferenceById(q.getId()))
                .luaChon(ans.charAt(0))
                .dungHaySai(correct)
                .thoiGianMs((int) elapsedMs)
                .build());

        // 9️⃣ Phát sự kiện WS cập nhật điểm cho người chơi này
        wsPublisher.publishScoreUpdate(
                td.getId(),
                currentUserId,
                user.getHoTen(),
                correct,
                gained,
                total,
                idx,
                comboStreak,
                comboBonus,
                comboMultiplier
        );

        // 🔟 Cập nhật và broadcast leaderboard tổng thể
//        updateAndBroadcastLeaderboard(td.getId(), state);
//        updateAndBroadcastLeaderboard(td.getId());
        // 1️⃣1️⃣ Trả response cho client
        return SubmitAnswerResponse.builder()
                .correct(correct)
                .gainedPoints(gained)
                .totalPoints(total)
                .questionIndex(idx)
                .shieldUsed(shieldUsed)
                .build();
    }

    @Override
    public Page<LichSuTranDauResponse> getAllHistory(int page, int limit) {
        PageRequest pageable = PageRequest.of(page, limit);

        return lichSuTranDauRepository
                .findAllByOrderByHoanThanhLucDesc(pageable)
                .map(LichSuTranDauResponse::fromEntity);
    }


    @Override
    @Transactional
    public BattleFinishResponse finishBattle(Long tranDauId, Long currentUserId, boolean autoMode) throws Exception {
//        System.out.println(">>> [SERVICE] finishBattle CALLED, tranDauId=" + tranDauId
//                + ", currentUserId=" + currentUserId + ", autoMode=" + autoMode);

        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

//        System.out.println(">>> [SERVICE] tran_dau.trang_thai = " + td.getTrangThai());

        // 1️⃣ Quyền hạn
        if (!autoMode && !td.getChuPhong().getId().equals(currentUserId)) {
//            System.out.println("❌ [SERVICE] finishBattle: currentUserId KHÔNG phải chủ phòng");
            throw new SecurityException("Chỉ chủ phòng mới có thể kết thúc trận đấu");
        }

        // Đã kết thúc rồi → trả kết quả cũ, KHÔNG publish WS nữa
        if (TrangThaiTranDau.HOAN_THANH.equals(td.getTrangThai())) {
//            System.out.println("⚠️ [SERVICE] Trận đấu đã ở trạng thái FINISHED, trả BattleFinishResponse cũ");
            return BattleFinishResponse.from(td, null, null, null, null);
        }

        // 2️⃣ Lấy state trong RAM (nếu còn)
        BattleState state = battleStateManager.get(tranDauId);
        if (state != null && !state.markFinishedOnce()) {
            // Có người khác finish trước rồi
//            System.out.println("⚠️ [SERVICE] markFinishedOnce = false, có luồng khác đã finish trước");
            return BattleFinishResponse.from(td, state.getDiemNguoiChoi(), null, null, null);
        }

        // 3️⃣ Lấy danh sách người chơi trong phòng
        List<NguoiChoiTranDau> players = nguoiChoiTranDauRepository.findAllByTranDau_Id(td.getId());
//        System.out.println(">>> [SERVICE] So nguoi_choi_tran_dau = " + players.size());

        if (players.isEmpty()) {
            // Không có người chơi → chỉ đánh dấu FINISHED
//            System.out.println("⚠️ [SERVICE] Không có người chơi nào, chỉ set FINISHED và return");
            td.setTrangThai(TrangThaiTranDau.HOAN_THANH);
            td.setKetThucLuc(Instant.now());
            tranDauRepository.save(td);
            battleStateManager.remove(tranDauId);
            return BattleFinishResponse.from(td, null, null, null, null);
        }

        // 4️⃣ Map điểm (ưu tiên BattleState)
        Map<Long, Integer> scoreMap = new HashMap<>();
        if (state != null && state.getDiemNguoiChoi() != null && !state.getDiemNguoiChoi().isEmpty()) {
            scoreMap.putAll(state.getDiemNguoiChoi());
//            System.out.println(">>> [SERVICE] scoreMap lấy từ BattleState size = " + scoreMap.size());
        } else {
            // fallback: từ bảng nguoi_choi_tran_dau
            for (NguoiChoiTranDau p : players) {
                scoreMap.put(
                        p.getNguoiDung().getId(),
                        p.getDiem() != null ? p.getDiem() : 0
                );
            }
//            System.out.println(">>> [SERVICE] scoreMap fallback từ nguoi_choi_tran_dau size = " + scoreMap.size());
        }

        // 5️⃣ Lấy log trả lời để tính số câu đúng + tổng thời gian
        List<TraLoiTranDau> logs = traLoiTranDauRepository.findAllByTranDau_Id(td.getId());
//        System.out.println(">>> [SERVICE] So tra_loi_tran_dau = " + logs.size());

        Map<Long, Integer> correctMap = new HashMap<>();
        Map<Long, Integer> totalTimeMap = new HashMap<>();

        for (TraLoiTranDau log : logs) {
            Long uid = log.getNguoiDung().getId();

            if (Boolean.TRUE.equals(log.getDungHaySai())) {
                correctMap.merge(uid, 1, Integer::sum);
            }
            if (log.getThoiGianMs() != null) {
                totalTimeMap.merge(uid, log.getThoiGianMs(), Integer::sum);
            }
        }

        // 6️⃣ Cập nhật điểm & số câu đúng vào nguoi_choi_tran_dau
        for (NguoiChoiTranDau p : players) {
            Long uid = p.getNguoiDung().getId();
            int diem = scoreMap.getOrDefault(uid, 0);
            int soCauDung = correctMap.getOrDefault(uid, 0);

            p.setDiem(diem);
            p.setSoCauDung(soCauDung);
        }

        // Xếp hạng theo điểm giảm dần
        players.sort(Comparator.comparing(NguoiChoiTranDau::getDiem).reversed());
        AtomicInteger rankCounter = new AtomicInteger(1);
        players.forEach(p -> p.setXepHang(rankCounter.getAndIncrement()));
        nguoiChoiTranDauRepository.saveAll(players);
//        System.out.println(">>> [SERVICE] Đã cập nhật diem/so_cau_dung/xep_hang cho nguoi_choi_tran_dau");

        // 7️⃣ Cập nhật winner + trạng thái trận
        NguoiChoiTranDau winnerPlayer = players.get(0);
        td.setWinner(winnerPlayer.getNguoiDung());        // => sẽ update winner_id
        td.setTrangThai(TrangThaiTranDau.HOAN_THANH);
        td.setKetThucLuc(Instant.now());
        tranDauRepository.save(td);
//        System.out.println(">>> [SERVICE] Winner = " + winnerPlayer.getNguoiDung().getHoTen()
//                + ", diem = " + winnerPlayer.getDiem());

        // 8️⃣ Lưu lịch sử trận đấu
        Instant now = Instant.now();
        List<LichSuTranDau> lichSuList = players.stream()
                .map(p -> {
                    Long uid = p.getNguoiDung().getId();
                    Integer tongTime = totalTimeMap.getOrDefault(uid, 0);
                    Integer maxCombo = (state != null) ? state.getMaxComboStreak(uid) : 0;
                    return LichSuTranDau.builder()
                            .tranDau(td)
                            .nguoiDung(p.getNguoiDung())
                            .tongDiem(p.getDiem())
                            .soCauDung(p.getSoCauDung())
                            .tongThoiGianMs(tongTime)
                            .xepHang(p.getXepHang())
                            .maxCombo(maxCombo)
                            .hoanThanhLuc(now)
                            .build();
                })
                .toList();

        lichSuTranDauRepository.saveAll(lichSuList);
        // Tìm điểm cao nhất
        int maxScore = players.get(0).getDiem();

        // Tập user thắng (có thể >1 nếu hòa điểm)
        Set<Long> winnerIds = players.stream()
                .filter(p -> p.getDiem() == maxScore)
                .map(p -> p.getNguoiDung().getId())
                .collect(Collectors.toSet());

        // cập nhật BXH theo best-score + winners
        // cập nhật BXH VÀ lấy thưởng từng người chơi
        Map<Long, MatchRewardResponse> rewardMap = updateRankingAfterBattle(td, scoreMap, correctMap, winnerIds);


        MatchRewardResponse myReward = rewardMap.get(currentUserId);

        // 🔥 Sau khi cập nhật BXH và thưởng, xử lý thành tích
        Map<Long, List<AchievementResponse>> achievementMap = new HashMap<>();
        for (Long uid : rewardMap.keySet()) {
            List<AchievementResponse> newly = thanhTichService.processAfterBattle(uid);
            if (!newly.isEmpty()) {
                achievementMap.put(uid, newly);
            }
        }

        // Thành tích mới của user hiện tại (host)
        List<AchievementResponse> myNewAchievements =
                achievementMap.getOrDefault(currentUserId, List.of());

        // 📋 Cập nhật tiến độ nhiệm vụ cho tất cả người chơi
        boolean isRanked = LoaiTranDau.XEP_HANG.equals(td.getLoaiTranDau());
        for (NguoiChoiTranDau p : players) {
            Long uid = p.getNguoiDung().getId();
            int soCauDung = correctMap.getOrDefault(uid, 0);
            int xepHang = p.getXepHang();
            boolean isWinner = winnerIds.contains(uid);
            Integer maxCombo = (state != null) ? state.getMaxComboStreak(uid) : 0;

            try {
                // Tham gia trận
                nhiemVuService.onMatchPlayed(uid, isRanked);

                // Trả lời đúng
                if (soCauDung > 0) {
                    nhiemVuService.onCorrectAnswer(uid, soCauDung);
                }

                // Thắng trận
                if (isWinner) {
                    nhiemVuService.onMatchWon(uid, isRanked);
                }

                // Đạt combo
                if (maxCombo >= 5) {
                    nhiemVuService.onComboAchieved(uid, maxCombo);
                }

                // Đạt Top 3
                if (xepHang <= 3 && players.size() >= 3) {
                    nhiemVuService.onTop3Achieved(uid);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi cập nhật nhiệm vụ cho user " + uid + ": " + e.getMessage());
            }
        }

//        System.out.println(">>> [SERVICE] Đã lưu lich_su_tran_dau, size=" + lichSuList.size());

        // 9️⃣ Phát WS FINISHED event
        FinishedEvent.Winner winData = FinishedEvent.Winner.builder()
                .userId(winnerPlayer.getNguoiDung().getId())
                .hoTen(winnerPlayer.getNguoiDung().getHoTen())
                .avatarUrl(winnerPlayer.getNguoiDung().getAvatarUrl())
                .diem(winnerPlayer.getDiem())
                .soCauDung(winnerPlayer.getSoCauDung())
                .build();

//        System.out.println("🔥 [SERVICE] Chuẩn bị publish FINISHED WS cho tran_dau_id = " + td.getId()
//                + ", so_nguoi_choi = " + players.size());
        Map<Long, Integer> maxComboMap = lichSuList.stream()
                .collect(Collectors.toMap(
                        ls -> ls.getNguoiDung().getId(),
                        LichSuTranDau::getMaxCombo
                ));

        wsPublisher.publishFinished(
                td.getId(),
                td.getTenPhong(),
                td.getMaPhong(),
                td.getBatDauLuc(),
                td.getKetThucLuc(),
                winData,
                players.stream()
                        .map(p -> {
                            Long uid = p.getNguoiDung().getId();
                            Integer maxCombo = maxComboMap.get(uid);
                            MatchRewardResponse reward = rewardMap.get(uid);
                            List<AchievementResponse> newAch = achievementMap.getOrDefault(uid, List.of());
                            return FinishedEvent.Player.builder()
                                    .userId(uid)
                                    .hoTen(p.getNguoiDung().getHoTen())
                                    .avatarUrl(p.getNguoiDung().getAvatarUrl())
                                    .diem(p.getDiem())
                                    .soCauDung(p.getSoCauDung())
                                    .xepHang(p.getXepHang())
                                    .maxCombo(maxCombo)   // ⭐ set vào WS
                                    .xpGained(reward != null ? reward.getXpGained() : 0L)
                                    .goldGained(reward != null ? reward.getGoldGained() : 0L)
                                    .levelBefore(reward != null ? reward.getLevelBefore() : null)
                                    .levelAfter(reward != null ? reward.getLevelAfter() : null)
                                    .rankTierBefore(reward != null ? reward.getRankTierBefore() : null)
                                    .rankTierAfter(reward != null ? reward.getRankTierAfter() : null)
                                    .leveledUp(reward != null ? reward.getLeveledUp() : false)
                                    .levelUpRewards(reward != null ? reward.getLevelUpRewards() : null)
                                    .newAchievements(newAch)
                                    .build();
                        })
                        .toList()
        );

        // 🔟 Dọn state trong RAM
        battleStateManager.remove(tranDauId);

        // 🔁 Build response REST
        Map<Long, Integer> finalScores = players.stream()
                .collect(Collectors.toMap(
                        p -> p.getNguoiDung().getId(),
                        NguoiChoiTranDau::getDiem
                ));

        List<NguoiDung> allUsers = players.stream()
                .map(NguoiChoiTranDau::getNguoiDung)
                .toList();

        return BattleFinishResponse.from(td, finalScores, allUsers, myReward, myNewAchievements);
    }


    @Transactional(readOnly = true)
    @Override
    public SyncStateResponse syncState(Long tranDauId, Long currentUserId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        BattleState state = battleStateManager.get(tranDauId);
        int seconds = td.getGioiHanThoiGianCauGiay() != null
                ? td.getGioiHanThoiGianCauGiay()
                : 15;
        if (state == null || !TrangThaiTranDau.DANG_CHOI.equals(td.getTrangThai())) {
            return SyncStateResponse.builder()
                    .tranDauId(td.getId())
                    .trangThai(td.getTrangThai())
                    .secondsPerQuestion(seconds)
                    .currentQuestionIndex(-1)
                    .myTotalPoints(0)
                    .build();
        }

        int idx = state.getCurrentQuestionIndex();
        CauHoiCacheDTO q = (idx >= 0 && idx < state.getDanhSachCauHoi().size())
                ? state.getDanhSachCauHoi().get(idx)
                : null;

        return SyncStateResponse.builder()
                .tranDauId(td.getId())
                .trangThai(td.getTrangThai())
                .currentQuestionIndex(idx)
                .currentQuestionStart(state.getCurrentQuestionStart())
                .secondsPerQuestion(seconds)
                .currentQuestionId(q != null ? q.getId() : null)
                .noiDung(q != null ? q.getNoiDung() : null)
                .loaiNoiDung(q != null ? q.getLoaiNoiDung() : null)
                .duongDanTep(q != null ? q.getDuongDanTep() : null)
                .a(q != null ? q.getLuaChonA() : null)
                .b(q != null ? q.getLuaChonB() : null)
                .c(q != null ? q.getLuaChonC() : null)
                .d(q != null ? q.getLuaChonD() : null)
                .myTotalPoints(currentUserId != null
                        ? state.getDiemNguoiChoi().getOrDefault(currentUserId, 0)
                        : 0)
                .build();
    }

    /**
     * Cập nhật bảng xếp hạng sau trận đấu và trả về phần thưởng từng người chơi.
     * Chỉ áp dụng cho trận RANKED.
     * 
     * XP/Gold được tính dựa trên SỐ CÂU ĐÚNG thay vì điểm raw để cân bằng
     * giữa chế độ THUONG và THUONG_TOC_DO.
     *
     * @param tranDau     Trận đấu vừa kết thúc
     * @param scores      Bản đồ điểm số người chơi (userId -> điểm)
     * @param correctMap  Bản đồ số câu đúng (userId -> số câu đúng)
     * @param winnerIds   Tập userId của người thắng (có thể nhiều người nếu hòa điểm)
     * @return Bản đồ userId -> MatchRewardResponse
     * @throws DataNotFoundException nếu người dùng không tồn tại
     */
    private Map<Long, MatchRewardResponse> updateRankingAfterBattle(
            TranDau tranDau,
            Map<Long, Integer> scores,
            Map<Long, Integer> correctMap,
            Set<Long> winnerIds
    ) throws DataNotFoundException {
        Map<Long, MatchRewardResponse> rewardMap = new HashMap<>();

        // Nếu bạn muốn chỉ RANKED mới cộng rank/xp/gold:
        if (!LoaiTranDau.XEP_HANG.equals(tranDau.getLoaiTranDau())) {
            return rewardMap; // trận casual không có thưởng (hoặc sau này muốn đổi thì đổi chỗ này)
        }

        Long boCauHoiId = tranDau.getBoCauHoi().getId();
        int totalQuestions = tranDau.getBoCauHoi().getSoCauHoi();
        boolean isSpeedMode = LuatTinhDiem.THUONG_TOC_DO.equalsIgnoreCase(tranDau.getLuatTinhDiem());

        for (Map.Entry<Long, Integer> entry : scores.entrySet()) {
            Long userId = entry.getKey();
            int rawScore = entry.getValue() != null ? entry.getValue() : 0;
            int diemTranNay = Math.max(0, rawScore);
            int soCauDung = correctMap.getOrDefault(userId, 0);
            boolean isWinner = winnerIds.contains(userId);

            // --- 1) Thành tích theo bộ câu hỏi (delta điểm rank) ---
            ThanhTichBoCauHoi thanhTich = thanhTichBoCauHoiRepository
                    .findByNguoiDung_IdAndBoCauHoi_Id(userId, boCauHoiId)
                    .orElse(null);

            int delta = 0;
            if (thanhTich == null) {
                delta = diemTranNay;
                thanhTich = ThanhTichBoCauHoi.builder()
                        .nguoiDung(nguoiDungRepository.getReferenceById(userId))
                        .boCauHoi(tranDau.getBoCauHoi())
                        .diemCaoNhat(diemTranNay)
                        .build();
            } else if (diemTranNay > thanhTich.getDiemCaoNhat()) {
                delta = diemTranNay - thanhTich.getDiemCaoNhat();
                thanhTich.setDiemCaoNhat(diemTranNay);
            }
            thanhTichBoCauHoiRepository.save(thanhTich);

            // --- 2) Lấy hoặc tạo BXH ---
            BangXepHang bxh = bangXepHangRepository.findByNguoiDung_Id(userId)
                    .orElse(BangXepHang.builder()
                            .nguoiDung(nguoiDungRepository.getReferenceById(userId))
                            .tongDiem(0)
                            .tongTran(0)
                            .soTranThang(0)
                            .soTranThua(0)
                            .level(1)
                            .tongXp(0L)
                            .tienVang(0L)
                            .rankTier(RankTier.DONG)
                            .build());

            // Snapshot BEFORE
            int levelBefore = bxh.getLevel() != null ? bxh.getLevel() : 1;
            RankTier tierBefore = bxh.getRankTier() != null ? bxh.getRankTier() : RankTier.DONG;

            // --- 3) Cập nhật thống kê rank (chỉ RANKED) ---
            bxh.setTongTran(bxh.getTongTran() + 1);

            if (delta > 0) {
                bxh.setTongDiem(bxh.getTongDiem() + delta);
            }

            if (isWinner) {
                bxh.setSoTranThang(bxh.getSoTranThang() + 1);
            } else if (diemTranNay > 0) {
                bxh.setSoTranThua(bxh.getSoTranThua() + 1);
            }

            // --- 4) Tính XP dựa trên SỐ CÂU ĐÚNG (cân bằng 2 chế độ) ---
            long gainedXp = bangXepHangService.calculateXpFromMatch(soCauDung, totalQuestions, isWinner, isSpeedMode);
            
            // Dùng LevelUpService để xử lý XP, lên cấp và trao phần thưởng
            LevelUpResponse levelUpResult = levelUpService.addXpAndProcessLevelUp(userId, gainedXp);
            
            // Refresh bxh từ DB (vì levelUpService đã update)
            bxh = bangXepHangRepository.findByNguoiDung_Id(userId).orElse(bxh);
            int levelAfter = bxh.getLevel() != null ? bxh.getLevel() : 1;

            // --- 5) Tính RankTier & Gold dựa trên SỐ CÂU ĐÚNG ---
            RankTier tierAfter = bangXepHangService.getRankTier(bxh);
            bxh.setRankTier(tierAfter);

            long gainedGold = bangXepHangService.calculateGoldFromMatch(soCauDung, isWinner, true, tierAfter, isSpeedMode);
            long newTotalGold = (bxh.getTienVang() != null ? bxh.getTienVang() : 0L) + gainedGold;
            bxh.setTienVang(newTotalGold);

            bangXepHangRepository.save(bxh);

            // --- 6) Lưu reward cho user này (bao gồm phần thưởng lên cấp) ---
            rewardMap.put(userId, MatchRewardResponse.builder()
                    .xpGained(gainedXp)
                    .goldGained(gainedGold)
                    .levelBefore(levelBefore)
                    .levelAfter(levelAfter)
                    .rankTierBefore(tierBefore)
                    .rankTierAfter(tierAfter)
                    .leveledUp(levelUpResult.isDaLenCap())
                    .levelUpRewards(levelUpResult.isDaLenCap() ? levelUpResult.getPhanThuong() : null)
                    .build());
        }

        bangXepHangRepository.updateAllRankings();
        
        // Clear leaderboard cache sau khi cập nhật rankings
        leaderboardRedisService.clearLeaderboardCache();
        
        return rewardMap;
    }


    @Override
    public Page<LichSuTranDauResponse> getMyHistory(Long currentUserId, int page, int limit) {
        PageRequest pageable = PageRequest.of(page, limit);
        return lichSuTranDauRepository
                .findByNguoiDung_IdOrderByHoanThanhLucDesc(currentUserId, pageable)
                .map(LichSuTranDauResponse::fromEntity);
    }

    @Override
    public Page<LichSuTranDauResponse> getUserHistory(Long userId, int page, int limit) {
        PageRequest pageable = PageRequest.of(page, limit);
        return lichSuTranDauRepository
                .findByNguoiDung_IdOrderByHoanThanhLucDesc(userId, pageable)
                .map(LichSuTranDauResponse::fromEntity);
    }


    @Override
    public LichSuTranDauDetailResponse getMyHistoryDetail(Long tranDauId, Long currentUserId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        LichSuTranDau myHistory = lichSuTranDauRepository
                .findByTranDau_IdAndNguoiDung_Id(tranDauId, currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Bạn chưa tham gia trận đấu này"));

        // base info
        LichSuTranDauDetailResponse res = LichSuTranDauDetailResponse.baseFrom(td, myHistory);

        // leaderboard
        List<LichSuTranDau> all = lichSuTranDauRepository
                .findByTranDau_IdOrderByXepHangAsc(tranDauId);

        List<FinishedPlayer> leaderboard = all.stream()
                .map(ls -> FinishedPlayer.builder()
                        .userId(ls.getNguoiDung().getId())
                        .hoTen(ls.getNguoiDung().getHoTen())
                        .diem(ls.getTongDiem())
                        .soCauDung(ls.getSoCauDung())
                        .xepHang(ls.getXepHang())
                        .maxCombo(ls.getMaxCombo())
                        .build())
                .toList();

        res.setLeaderboard(leaderboard);

        // câu hỏi / đáp án của riêng user
        List<TraLoiTranDau> answers = traLoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_IdOrderByTraLoiLucAsc(tranDauId, currentUserId);

        List<LichSuTranDauQuestionResponse> qList = answers.stream()
                .map(tl -> LichSuTranDauQuestionResponse.fromEntities(
                        tl,
                        tl.getCauHoi()      // đã quan hệ @ManyToOne
                ))
                .toList();
        res.setQuestions(qList);
        return res;
    }

    @Override
    public LichSuTranDauDetailResponse getHistoryDetailAdmin(Long lichSuId) throws Exception {
        // 1) Lấy bản ghi lịch sử
        LichSuTranDau myHistory = lichSuTranDauRepository.findById(lichSuId)
                .orElseThrow(() -> new DataNotFoundException("Lịch sử trận đấu không tồn tại"));

        TranDau td = myHistory.getTranDau();
        Long userId = myHistory.getNguoiDung().getId();

        // 2) Base info
        LichSuTranDauDetailResponse res = LichSuTranDauDetailResponse.baseFrom(td, myHistory);

        // 3) Leaderboard
        List<LichSuTranDau> all = lichSuTranDauRepository
                .findByTranDau_IdOrderByXepHangAsc(td.getId());

        List<FinishedPlayer> leaderboard = all.stream()
                .map(ls -> FinishedPlayer.builder()
                        .userId(ls.getNguoiDung().getId())
                        .hoTen(ls.getNguoiDung().getHoTen())
                        .diem(ls.getTongDiem())
                        .soCauDung(ls.getSoCauDung())
                        .xepHang(ls.getXepHang())
                        .build())
                .toList();
        res.setLeaderboard(leaderboard);

        // 4) Câu hỏi / đáp án của user tương ứng
        List<TraLoiTranDau> answers = traLoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_IdOrderByTraLoiLucAsc(td.getId(), userId);

        List<LichSuTranDauQuestionResponse> qList = answers.stream()
                .map(tl -> LichSuTranDauQuestionResponse.fromEntities(
                        tl,
                        tl.getCauHoi()
                ))
                .toList();
        res.setQuestions(qList);

        return res;
    }

    @Override
    public void guiChatTrongTran(GuiChatDTO dto, Long currentUserId) throws Exception {
        NguoiDung nguoiDung = nguoiDungRepository.findById(currentUserId)
                .orElseThrow(() -> new DataNotFoundException("Người dùng không tồn tại"));

        TranDau tranDau = tranDauRepository.findById(dto.getTranDauId())
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        // Không cho chat ở trận đã kết thúc (tuỳ bạn)
        if (Objects.equals(tranDau.getTrangThai(), TrangThaiTranDau.HOAN_THANH)) {
            throw new IllegalStateException("Trận đấu đã kết thúc, không thể chat");
        }

        // Bắt buộc phải là người trong phòng
        boolean joined = nguoiChoiTranDauRepository
                .existsByTranDauIdAndNguoiDungId(tranDau.getId(), nguoiDung.getId());
        if (!joined) {
            throw new PermissionDenyException("Bạn chưa tham gia trận đấu này");
        }

        // Không lưu DB, chỉ broadcast WS
        wsPublisher.publishChatMessage(
                tranDau.getId(),
                nguoiDung.getId(),
                nguoiDung.getHoTen(),
                dto.getNoiDung(),
                false // system = false
        );
    }

    private void updateAndBroadcastLeaderboard(Long tranDauId, BattleState optionalState) {
        BattleState state = (optionalState != null)
                ? optionalState : battleStateManager.get(tranDauId);
        if (state == null) return;

        Map<Long, Integer> scores = state.getDiemNguoiChoi();
        if (scores.isEmpty()) return;

        List<Long> ids = new ArrayList<>(scores.keySet());
        Map<Long, String> nameMap = nguoiDungRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(NguoiDung::getId, NguoiDung::getHoTen));

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardUpdateEvent.Row> board = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(e -> LeaderboardUpdateEvent.Row.builder()
                        .userId(e.getKey())
                        .hoTen(nameMap.getOrDefault(e.getKey(), "Người chơi"))
                        .diem(e.getValue())
                        .xepHang(rank.getAndIncrement()).build())
                .toList();

        wsPublisher.publishLeaderboard(tranDauId, board);
    }


    @Transactional
    public void updateAndBroadcastLeaderboard(Long tranDauId) {
        List<NguoiChoiTranDau> all = nguoiChoiTranDauRepository.findAllByTranDau_Id(tranDauId);

        // Sắp xếp: điểm giảm dần, cùng điểm thì ai vào trước xếp cao hơn
        all.sort(Comparator
                .comparing(NguoiChoiTranDau::getDiem, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()
                .thenComparing(NguoiChoiTranDau::getThamGiaLuc, Comparator.nullsFirst(Comparator.naturalOrder())));

        int rank = 1;
        for (NguoiChoiTranDau p : all) p.setXepHang(rank++);
        nguoiChoiTranDauRepository.saveAll(all);

        var rows = all.stream().map(p -> LeaderboardUpdateEvent.Row.builder()
                .userId(p.getNguoiDung().getId())
                .hoTen(p.getNguoiDung().getHoTen())
                .diem(Optional.ofNullable(p.getDiem()).orElse(0))
                .soCauDung(Optional.ofNullable(p.getSoCauDung()).orElse(0))
                .xepHang(Optional.ofNullable(p.getXepHang()).orElse(0))
                .build()).toList();

        wsPublisher.publishLeaderboard(tranDauId, rows);
    }

    // 1) Admin xem chi tiết từng câu của 1 user trong trận
    @Override
    public List<LichSuTranDauQuestionResponse> getPlayerAnswersAdmin(Long tranDauId, Long userId) throws DataNotFoundException {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        List<TraLoiTranDau> answers = traLoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_IdOrderByTraLoiLucAsc(tranDauId, userId);

        return answers.stream()
                .map(tl -> LichSuTranDauQuestionResponse.fromEntities(
                        tl,
                        tl.getCauHoi()
                ))
                .toList();
    }

    // 2) Admin xem tất cả người chơi của 1 câu hỏi
    @Override
    public QuestionAnswersAdminResponse getQuestionAnswersAdmin(Long tranDauId, Long cauHoiId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        CauHoi q = cauHoiRepository.findById(cauHoiId)
                .orElseThrow(() -> new DataNotFoundException("Câu hỏi không tồn tại"));

        List<TraLoiTranDau> answers = traLoiTranDauRepository
                .findByTranDau_IdAndCauHoi_IdOrderByTraLoiLucAsc(tranDauId, cauHoiId);

        List<QuestionAnswersAdminResponse.PlayerAnswerRow> nguoiChoi = answers.stream()
                .map(tl -> QuestionAnswersAdminResponse.PlayerAnswerRow.builder()
                        .userId(tl.getNguoiDung().getId())
                        .hoTen(tl.getNguoiDung().getHoTen())
                        .luaChon(tl.getLuaChon())
                        .dungHaySai(tl.getDungHaySai())
                        .thoiGianMs(tl.getThoiGianMs())
                        .build())
                .toList();

        return QuestionAnswersAdminResponse.builder()
                .tranDauId(td.getId())
                .cauHoiId(q.getId())
                .noiDung(q.getNoiDung())
                .loaiNoiDung(q.getLoaiNoiDung())
                .duongDanTep(q.getDuongDanTep())
                .luaChonA(q.getLuaChonA())
                .luaChonB(q.getLuaChonB())
                .luaChonC(q.getLuaChonC())
                .luaChonD(q.getLuaChonD())
                .dapAnDung(q.getDapAnDung())
                .nguoiChoi(nguoiChoi)
                .build();
    }


    @Override
    @Transactional
    public void inviteFriendToBattle(Long tranDauId,
                                     Long currentUserId,
                                     Long targetUserId) throws Exception {

        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        // Chỉ chủ phòng mới mời được
        if (!td.getChuPhong().getId().equals(currentUserId)) {
            throw new SecurityException("Chỉ chủ phòng mới có thể mời bạn bè vào phòng");
        }

        // Trạng thái trận đấu phải đang chờ (tuỳ bạn, có thể cho cả ONGOING)
        if (!TrangThaiTranDau.CHO.equals(td.getTrangThai())) {
            throw new IllegalStateException("Chỉ có thể mời khi phòng đang ở trạng thái chờ");
        }

        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Không thể tự mời chính mình");
        }

        // Check user tồn tại
        NguoiDung target = nguoiDungRepository.findById(targetUserId)
                .orElseThrow(() -> new DataNotFoundException("Người được mời không tồn tại"));

        // Phải là bạn bè
        boolean areFriends = ketBanRepository.areFriends(currentUserId, targetUserId);
        if (!areFriends) {
            throw new IllegalStateException("Chỉ có thể mời những người đã là bạn bè");
        }

        // Tạo nội dung + metadata cho notification
        NguoiDung chuPhong = td.getChuPhong();
        String noiDung = chuPhong.getHoTen() + " đã mời bạn vào phòng đấu: "
                + (td.getMaPhong() != null ? td.getMaPhong() : ("#" + td.getId()));

        String metadataJson = """
                {
                  "tran_dau_id": %d,
                  "ma_phong": "%s"
                }
                """.formatted(
                td.getId(),
                td.getMaPhong() != null ? td.getMaPhong() : ""
        );

        // Gửi notification type LOI_MOI_TRAN_DAU
        thongBaoService.createNotification(
                chuPhong.getId(),
                target.getId(),
                LoaiThongBao.LOI_MOI_TRAN_DAU,
                noiDung,
                metadataJson
        );
    }

    // ===================== ADMIN METHODS =====================

    @Override
    public Map<String, Object> getAdminBattleStats() {
        Map<String, Object> stats = new HashMap<>();

        // Tổng số trận
        long totalBattles = tranDauRepository.count();
        stats.put("totalBattles", totalBattles);

        // Số trận đang chờ
        long pendingBattles = tranDauRepository.countByTrangThai(TrangThaiTranDau.CHO);
        stats.put("pendingBattles", pendingBattles);

        // Số trận đang diễn ra
        long ongoingBattles = tranDauRepository.countByTrangThai(TrangThaiTranDau.DANG_CHOI);
        stats.put("ongoingBattles", ongoingBattles);

        // Số trận đã hoàn thành
        long finishedBattles = tranDauRepository.countByTrangThai(TrangThaiTranDau.HOAN_THANH);
        stats.put("finishedBattles", finishedBattles);

        // Số trận hôm nay
        Instant startOfToday = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        List<Object[]> todayStats = tranDauRepository.countBattlesByDaySince(startOfToday);
        long todayBattles = todayStats.isEmpty() ? 0 : ((Number) todayStats.get(0)[1]).longValue();
        stats.put("todayBattles", todayBattles);

        // Tổng số lịch sử trận đấu
        long totalHistories = lichSuTranDauRepository.count();
        stats.put("totalHistories", totalHistories);

        return stats;
    }

    @Override
    public Page<LichSuTranDauResponse> getAdminHistoryFiltered(
            int page, int limit, String keyword, String loaiTranDau,
            Long boCauHoiId, String fromDate, String toDate
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        // Parse dates
        Instant from = null;
        Instant to = null;
        if (fromDate != null && !fromDate.isBlank()) {
            from = Instant.parse(fromDate + "T00:00:00Z");
        }
        if (toDate != null && !toDate.isBlank()) {
            to = Instant.parse(toDate + "T23:59:59Z");
        }

        Page<LichSuTranDau> result = lichSuTranDauRepository.findAllFiltered(
                keyword, loaiTranDau, boCauHoiId, from, to, pageRequest
        );

        return result.map(LichSuTranDauResponse::fromEntity);
    }

    @Override
    @Transactional
    public void adminCloseRoom(Long tranDauId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        if (!TrangThaiTranDau.CHO.equals(td.getTrangThai())) {
            throw new IllegalStateException("Chỉ có thể đóng phòng đang ở trạng thái chờ");
        }

        // Xóa tất cả người chơi trong phòng
        List<NguoiChoiTranDau> players = nguoiChoiTranDauRepository.findAllByTranDau_Id(tranDauId);
        nguoiChoiTranDauRepository.deleteAll(players);

        // Cập nhật trạng thái thành CANCELLED
        td.setTrangThai(TrangThaiTranDau.HUY);
        tranDauRepository.save(td);

        // Notify via WebSocket
        wsPublisher.sendRoomClosed(tranDauId, "Phòng đã bị đóng bởi Admin");
    }

    @Override
    @Transactional
    public void adminKickPlayer(Long tranDauId, Long userId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        if (!TrangThaiTranDau.CHO.equals(td.getTrangThai())) {
            throw new IllegalStateException("Chỉ có thể kick người chơi khi phòng đang ở trạng thái chờ");
        }

        // Không cho kick chủ phòng
        if (td.getChuPhong().getId().equals(userId)) {
            throw new IllegalArgumentException("Không thể kick chủ phòng");
        }

        NguoiChoiTranDau player = nguoiChoiTranDauRepository
                .findByTranDau_IdAndNguoiDung_Id(tranDauId, userId)
                .orElseThrow(() -> new DataNotFoundException("Người chơi không có trong phòng"));

        nguoiChoiTranDauRepository.delete(player);

        // Notify via WebSocket
        wsPublisher.sendPlayerKicked(tranDauId, userId, "Bạn đã bị kick bởi Admin");
    }

    @Override
    @Transactional
    public void adminDeleteHistory(Long lichSuId) throws Exception {
        LichSuTranDau lichSu = lichSuTranDauRepository.findById(lichSuId)
                .orElseThrow(() -> new DataNotFoundException("Lịch sử trận đấu không tồn tại"));

        // Xóa các câu trả lời liên quan (dựa trên tranDau và nguoiDung)
        traLoiTranDauRepository.deleteByTranDau_IdAndNguoiDung_Id(
                lichSu.getTranDau().getId(),
                lichSu.getNguoiDung().getId()
        );

        // Xóa lịch sử
        lichSuTranDauRepository.delete(lichSu);
    }

    @Override
    public Map<String, Object> adminGetRoomDetail(Long tranDauId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", td.getId());
        result.put("ma_phong", td.getMaPhong());
        result.put("ten_phong", td.getTenPhong());
        result.put("trang_thai", td.getTrangThai());
        result.put("loai_tran_dau", td.getLoaiTranDau());
        result.put("cong_khai", td.getCongKhai());
        result.put("gioi_han_nguoi_choi", td.getGioiHanNguoiChoi());
        result.put("tao_luc", td.getTaoLuc());

        // Thông tin chủ phòng
        NguoiDung chuPhong = td.getChuPhong();
        Map<String, Object> hostInfo = new HashMap<>();
        hostInfo.put("id", chuPhong.getId());
        hostInfo.put("username", chuPhong.getUsername());
        hostInfo.put("ho_ten", chuPhong.getHoTen());
        hostInfo.put("avatar", chuPhong.getAvatarUrl());
        result.put("chu_phong", hostInfo);

        // Thông tin bộ câu hỏi
        BoCauHoi bo = td.getBoCauHoi();
        Map<String, Object> boInfo = new HashMap<>();
        boInfo.put("id", bo.getId());
        boInfo.put("tieu_de", bo.getTieuDe());
        boInfo.put("chu_de", bo.getChuDe() != null ? bo.getChuDe().getTen() : null);
        result.put("bo_cau_hoi", boInfo);

        // Danh sách người chơi
        List<NguoiChoiTranDau> players = nguoiChoiTranDauRepository.findAllByTranDau_Id(tranDauId);
        List<Map<String, Object>> playerList = new ArrayList<>();
        for (NguoiChoiTranDau p : players) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("id", p.getNguoiDung().getId());
            pInfo.put("username", p.getNguoiDung().getUsername());
            pInfo.put("ho_ten", p.getNguoiDung().getHoTen());
            pInfo.put("avatar", p.getNguoiDung().getAvatarUrl());
            pInfo.put("is_host", p.getNguoiDung().getId().equals(chuPhong.getId()));
            playerList.add(pInfo);
        }
        result.put("nguoi_choi", playerList);
        result.put("so_nguoi_choi", players.size());

        return result;
    }

    @Override
    public byte[] exportHistoryCsv(String keyword, String loaiTranDau, Long boCauHoiId,
                                   String fromDate, String toDate) {
        // Parse dates
        Instant from = null;
        Instant to = null;
        if (fromDate != null && !fromDate.isBlank()) {
            from = Instant.parse(fromDate + "T00:00:00Z");
        }
        if (toDate != null && !toDate.isBlank()) {
            to = Instant.parse(toDate + "T23:59:59Z");
        }

        // Lấy tất cả dữ liệu (không phân trang)
        List<LichSuTranDau> allHistories = lichSuTranDauRepository.findAllFilteredList(
                keyword, loaiTranDau, boCauHoiId, from, to
        );

        StringBuilder csv = new StringBuilder();
        // BOM for UTF-8
        csv.append('\ufeff');
        // Header
        csv.append("ID Lịch sử,ID Trận,Tên phòng,Mã phòng,Bộ câu hỏi,Loại trận,Người chơi,Điểm,Số câu đúng,Thời gian (ms),Xếp hạng,Hoàn thành lúc\n");

        for (LichSuTranDau h : allHistories) {
            TranDau td = h.getTranDau();
            NguoiDung nd = h.getNguoiDung();

            csv.append(h.getId()).append(",");
            csv.append(td.getId()).append(",");
            csv.append(escapeCsv(td.getTenPhong())).append(",");
            csv.append(escapeCsv(td.getMaPhong())).append(",");
            csv.append(escapeCsv(td.getBoCauHoi() != null ? td.getBoCauHoi().getTieuDe() : "")).append(",");
            csv.append(escapeCsv(td.getLoaiTranDau())).append(",");
            csv.append(escapeCsv(nd.getHoTen())).append(",");
            csv.append(h.getTongDiem()).append(",");
            csv.append(h.getSoCauDung()).append(",");
            csv.append(h.getTongThoiGianMs()).append(",");
            csv.append(h.getXepHang()).append(",");
            csv.append(h.getHoanThanhLuc()).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ================== POWER-UPS / ITEMS METHODS ==================

    /**
     * Lấy BattleState hiện tại của trận đấu
     */
    public BattleState getState(Long tranDauId) {
        return battleStateManager.get(tranDauId);
    }

    /**
     * Broadcast sự kiện khi một người chơi sử dụng vật phẩm
     */
    public void broadcastItemUsed(Long tranDauId, Long userId,
                                  com.app.backend.responses.SuDungVatPhamResponse response) {
        NguoiDung user = nguoiDungRepository.findById(userId).orElse(null);
        String hoTen = user != null ? user.getHoTen() : "Người chơi";

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ITEM_USED");
        payload.put("user_id", userId);
        payload.put("ho_ten", hoTen);
        payload.put("loai_vat_pham", response.getLoaiVatPham());
        payload.put("ten_vat_pham", response.getTenVatPham());
        payload.put("hieu_ung", response.getHieuUng());

        wsPublisher.publishGeneric(tranDauId, "ITEM_USED", payload);
    }

    // ================== PLAYERS IN ROOM ==================

    /**
     * Lấy danh sách người chơi trong phòng (trước khi trận đấu bắt đầu)
     */
    @Override
    public List<NguoiChoiTrongPhongResponse> getPlayersInRoom(Long tranDauId) throws Exception {
        TranDau td = tranDauRepository.findById(tranDauId)
                .orElseThrow(() -> new DataNotFoundException("Trận đấu không tồn tại"));

        List<NguoiChoiTranDau> players = nguoiChoiTranDauRepository.findAllByTranDau_Id(tranDauId);

        // Lấy ID chủ phòng
        Long chuPhongId = td.getChuPhong() != null ? td.getChuPhong().getId() : null;

        return players.stream()
                .map(p -> {
                    NguoiDung nd = p.getNguoiDung();
                    return NguoiChoiTrongPhongResponse.builder()
                            .userId(nd.getId())
                            .hoTen(nd.getHoTen())
                            .avatarUrl(nd.getAvatarUrl())
                            .laChuPhong(nd.getId().equals(chuPhongId))
                            .daSanSang(true) // có thể thêm logic ready nếu cần
                            .thamGiaLuc(p.getThamGiaLuc() != null ? p.getThamGiaLuc().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void processQuestionTimeout(Long tranDauId) {
        try {
            BattleState state = battleStateManager.get(tranDauId);
            if (state == null) return;

            int idx = state.getCurrentQuestionIndex();
            if (idx >= 0 && idx < state.getDanhSachCauHoi().size()) {
                CauHoiCacheDTO q = state.getDanhSachCauHoi().get(idx);

                // Gửi sự kiện ANSWER_REVEAL qua Socket
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "ANSWER_REVEAL");
                payload.put("tran_dau_id", tranDauId);
                payload.put("question_index", idx);
                payload.put("dap_an_dung", q.getDapAnDung());
                payload.put("giai_thich", q.getGiaiThich());

                wsPublisher.publishGeneric(tranDauId, "ANSWER_REVEAL", payload);
            }

            // Bây giờ mới gửi Bảng xếp hạng cập nhật
            updateAndBroadcastLeaderboard(tranDauId, state);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi khi xử lý timeout câu hỏi: " + e.getMessage());
        }
    }

}
