package com.app.backend.components;

import com.app.backend.dtos.cache.CauHoiCacheDTO;
import com.app.backend.models.BattleState;
import com.app.backend.models.TranDau;
import com.app.backend.repositories.ITranDauRepository;
import com.app.backend.services.trandau.ITranDauService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BattleLoopTask {

    private final BattleStateManager battleStateManager;
    private final ITranDauRepository tranDauRepository;

    @Lazy
    @Autowired
    private ITranDauService tranDauService;

    private final BattleWsPublisher wsPublisher;

    @Async
    public void runAutoLoop(Long tranDauId, int secondsPerQuestion) {
        BattleState state = battleStateManager.get(tranDauId);
        if (state == null) return;
        if (state.isAutoLoopRunning()) return;

        state.setAutoLoopRunning(true);
        if (state.getSecondsPerQuestion() <= 0) state.setSecondsPerQuestion(secondsPerQuestion);
        if (state.getStartTime() == null) state.setStartTime(Instant.now());
        battleStateManager.save(state);

        TranDau td = tranDauRepository.findById(tranDauId).orElse(null);
        if (td == null) {
            state.setAutoLoopRunning(false);
            battleStateManager.save(state);
            return;
        }

        try {
            int preCountdownSeconds = 10;
            wsPublisher.publishBattleStarted(
                    tranDauId,
                    td.getTenPhong() != null ? td.getTenPhong() : ("Phòng #" + tranDauId),
                    state.getStartTime(),
                    state.getDanhSachCauHoi().size(),
                    state.getSecondsPerQuestion(),
                    preCountdownSeconds
            );

            try {
                Thread.sleep(preCountdownSeconds * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            List<CauHoiCacheDTO> cauHoiList = state.getDanhSachCauHoi();
            for (int i = 0; i < cauHoiList.size(); i++) {
                BattleState latest = battleStateManager.get(tranDauId);
                if (latest == null || !latest.isAutoLoopRunning()) break;
                latest.setCurrentQuestionIndex(i);
                latest.setCurrentQuestionStart(Instant.now());
                battleStateManager.save(latest);
                CauHoiCacheDTO q = cauHoiList.get(i);
                wsPublisher.publishNewQuestion(tranDauId, i, q, latest.getSecondsPerQuestion());
                try {
                    Thread.sleep(latest.getSecondsPerQuestion() * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                BattleState afterSleep = battleStateManager.get(tranDauId);
                if (afterSleep == null || !afterSleep.isAutoLoopRunning()) break;
                tranDauService.processQuestionTimeout(tranDauId);
                try {
                    int timeBreak = 5000;
//                    System.out.println("--- Nghỉ " + timeBreak + "ms xem BXH ---");
                    Thread.sleep(timeBreak);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            tranDauService.finishBattle(tranDauId, null, true);

        } catch (Exception e) {
//            System.err.println("❌ Lỗi trong BattleLoopTask: " + e.getMessage());
            e.printStackTrace();
        } finally {
            state.setAutoLoopRunning(false);
            battleStateManager.save(state);
        }
    }
}