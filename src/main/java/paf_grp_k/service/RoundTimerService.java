package paf_grp_k.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import paf_grp_k.websocket.GameWebSocketNotifier;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Service zur Verwaltung von Rundentimern für Spiele.
 *
 * <p>Diese Klasse startet und stoppt Timer pro Spielrunde und informiert Clients
 * über WebSockets über:</p>
 * <ul>
 *   <li>Start des Timers (Initialwert in Sekunden)</li>
 *   <li>Countdown-Updates (verbleibende Sekunden)</li>
 *   <li>Timeout-Ereignisse (via Callback)</li>
 * </ul>
 *
 * <p>Threading:</p>
 * <ul>
 *   <li>Timer und Ticker laufen in einem {@link ScheduledExecutorService}.</li>
 *   <li>Pro Runde wird ein Haupt-Timer (Timeout) gespeichert.</li>
 *   <li>Zusätzlich wird ein Ticker (scheduleAtFixedRate) gestartet, der jede Sekunde Updates broadcastet.</li>
 * </ul>
 *
 * <p>Hinweis: Der Timeout-Fall wird über ein {@link Runnable} Callback an den Caller delegiert
 * (z. B. Orchestrator), damit die Spiellogik getrennt bleibt.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoundTimerService {

    /**
     * Notifier zum Broadcasten von Timer-Events an Clients.
     */
    private final GameWebSocketNotifier notifier;

    /**
     * Map der aktiven Rundentimer.
     *
     * <p>Key-Format: {@code "{gameId}_{roundNumber}"}.</p>
     */
    private final Map<String, ScheduledFuture<?>> roundTimers = new ConcurrentHashMap<>();

    /**
     * Executor für Timer- und Ticker-Aufgaben.
     *
     * <p>Ein benannter ThreadFactory erleichtert Debugging in Logs/Thread-Dumps.</p>
     */
    private final ScheduledExecutorService timerExecutor =
            Executors.newScheduledThreadPool(10, new CustomizableThreadFactory("round-timer-"));

    /**
     * Maximale Zeit pro Runde in Sekunden.
     */
    private static final int ROUND_TIME_LIMIT_SECONDS = 30;

    /**
     * Liefert das konfigurierte Zeitlimit pro Runde.
     *
     * @return Rundentimer-Limit in Sekunden
     */
    public int getRoundTimeLimitSeconds() {
        return ROUND_TIME_LIMIT_SECONDS;
    }

    /**
     * Startet den Timer für eine bestimmte Runde eines Spiels.
     *
     * <p>Ablauf:</p>
     * <ol>
     *   <li>Vorherigen Timer (falls vorhanden) stoppen</li>
     *   <li>Timerstart an Clients broadcasten</li>
     *   <li>Timeout-Task planen (nach {@link #ROUND_TIME_LIMIT_SECONDS})</li>
     *   <li>Ticker starten, der jede Sekunde Countdown-Updates broadcastet</li>
     * </ol>
     *
     * <p>Beim Timeout wird das übergebene {@code onTimeout}-Callback ausgeführt.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     * @param onTimeout Callback, das beim Ablauf des Timers ausgeführt wird
     */
    public void startTimer(Long gameId, int roundNumber, Runnable onTimeout) {
        String timerKey = key(gameId, roundNumber);

        // Sicherheit: doppelte Timer vermeiden
        stopTimer(gameId, roundNumber);

        log.info("⏱️ Starte Timer für Spiel {} Runde {} ({} Sekunden)",
                gameId, roundNumber, ROUND_TIME_LIMIT_SECONDS);

        // Clients informieren, dass Timer läuft
        notifier.broadcastTimerStart(gameId, roundNumber, ROUND_TIME_LIMIT_SECONDS);

        // Timeout-Task planen
        ScheduledFuture<?> timer = timerExecutor.schedule(() -> {
            try {
                log.warn("⏰ TIMER ABGELAUFEN für Spiel {} Runde {}", gameId, roundNumber);
                onTimeout.run();
            } catch (Exception e) {
                log.error("❌ Fehler beim Timer-Ablauf: {}", e.getMessage(), e);
            } finally {
                roundTimers.remove(timerKey);
            }
        }, ROUND_TIME_LIMIT_SECONDS, TimeUnit.SECONDS);

        roundTimers.put(timerKey, timer);

        // Countdown-Updates starten
        startTicker(gameId, roundNumber, timerKey);
    }

    /**
     * Startet einen Ticker, der jede Sekunde die verbleibende Zeit broadcastet.
     *
     * <p>Der Ticker beendet sich implizit, sobald der Haupttimer nicht mehr existiert
     * oder bereits {@code done} ist.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     * @param timerKey Key des zugehörigen Haupttimers
     */
    private void startTicker(Long gameId, int roundNumber, String timerKey) {
        final long startTime = System.currentTimeMillis();

        timerExecutor.scheduleAtFixedRate(() -> {
            try {
                ScheduledFuture<?> currentTimer = roundTimers.get(timerKey);
                if (currentTimer == null || currentTimer.isDone()) {
                    return;
                }

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

    /**
     * Stoppt den Timer einer bestimmten Runde (falls aktiv).
     *
     * <p>Der Timer wird aus der Map entfernt und (falls noch nicht abgeschlossen)
     * mittels {@link ScheduledFuture#cancel(boolean)} abgebrochen.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     */
    public void stopTimer(Long gameId, int roundNumber) {
        String timerKey = key(gameId, roundNumber);
        ScheduledFuture<?> timer = roundTimers.remove(timerKey);

        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
            log.info("⏱️ Timer gestoppt für Spiel {} Runde {}", gameId, roundNumber);
        }
    }

    /**
     * Erzeugt den Key für einen Rundentimer.
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     * @return Key im Format {@code "{gameId}_{roundNumber}"}
     */
    private String key(Long gameId, int roundNumber) {
        return gameId + "_" + roundNumber;
    }
}
