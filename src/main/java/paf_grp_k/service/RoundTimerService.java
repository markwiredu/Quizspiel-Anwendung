package paf_grp_k.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import paf_grp_k.websocket.GameWebSocketNotifier;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoundTimerService {

    private final GameWebSocketNotifier notifier;

    private final Map<String, ScheduledFuture<?>> roundTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timerExecutor =
            Executors.newScheduledThreadPool(10, new CustomizableThreadFactory("round-timer-"));

    private static final int ROUND_TIME_LIMIT_SECONDS = 30;

    public int getRoundTimeLimitSeconds() {
        return ROUND_TIME_LIMIT_SECONDS;
    }

    public void startTimer(Long gameId, int roundNumber, Runnable onTimeout) {
        String timerKey = key(gameId, roundNumber);
        stopTimer(gameId, roundNumber);

        log.info("⏱️ Starte Timer für Spiel {} Runde {} ({} Sekunden)",
                gameId, roundNumber, ROUND_TIME_LIMIT_SECONDS);

        notifier.broadcastTimerStart(gameId, roundNumber, ROUND_TIME_LIMIT_SECONDS);

        ScheduledFuture<?> timer = timerExecutor.schedule(() -> {
            try {
                log.warn("⏰ TIMER ABGELAUFEN für Spiel {} Runde {}", gameId, roundNumber);
                onTimeout.run(); // <-- Callback
            } catch (Exception e) {
                log.error("❌ Fehler beim Timer-Ablauf: {}", e.getMessage(), e);
            } finally {
                roundTimers.remove(timerKey);
            }
        }, ROUND_TIME_LIMIT_SECONDS, TimeUnit.SECONDS);

        roundTimers.put(timerKey, timer);

        startTicker(gameId, roundNumber, timerKey);
    }

    private void startTicker(Long gameId, int roundNumber, String timerKey) {
        final long startTime = System.currentTimeMillis();

        timerExecutor.scheduleAtFixedRate(() -> {
            try {
                ScheduledFuture<?> currentTimer = roundTimers.get(timerKey);
                if (currentTimer == null || currentTimer.isDone()) return;

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long remaining = ROUND_TIME_LIMIT_SECONDS - elapsed;

                if (remaining > 0) {
                    notifier.broadcastTimerUpdate(gameId, roundNumber, remaining);
                }
            } catch (Exception e) {
                log.error("❌ Fehler beim Timer-Update: {}", e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stopTimer(Long gameId, int roundNumber) {
        String timerKey = key(gameId, roundNumber);
        ScheduledFuture<?> timer = roundTimers.remove(timerKey);

        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
            log.info("⏱️ Timer gestoppt für Spiel {} Runde {}", gameId, roundNumber);
        }
    }

    private String key(Long gameId, int roundNumber) {
        return gameId + "_" + roundNumber;
    }
}
