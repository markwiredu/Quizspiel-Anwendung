package paf_grp_k.service;

import paf_grp_k.dto.LobbyStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service zur Verwaltung von Lobbys und Matchmaking-Warteschlangen.
 *
 * <p>Diese Klasse verwaltet pro Kategorie eine Queue von Spielern und stellt Methoden bereit, um:</p>
 * <ul>
 *   <li>Spieler einer Lobby beitreten zu lassen</li>
 *   <li>Spieler aus einer Lobby zu entfernen</li>
 *   <li>Matches (2 Spieler) aus einer Lobby zu bilden</li>
 *   <li>Lobby-Informationen für Broadcasts bereitzustellen</li>
 * </ul>
 *
 * <p>Thread-Safety:</p>
 * <ul>
 *   <li>Queues und Sets sind concurrent-fähig (z. B. {@link ConcurrentLinkedQueue}, {@link ConcurrentHashMap})</li>
 *   <li>Pro Lobby (Kategorie) wird zusätzlich ein {@link ReentrantLock} verwendet,
 *       um kritische Abschnitte (Join/Leave/Match) zu serialisieren.</li>
 * </ul>
 *
 * <p>Hinweis: Der Service liefert Statusinformationen an den Caller (z. B. Orchestrator),
 * sendet jedoch selbst keine WebSocket-Nachrichten.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyService {

    /**
     * Lobby-Warteschlangen pro Kategorie (normalisierter Key).
     *
     * <p>Jede Queue enthält Spieler-IDs in Join-Reihenfolge.</p>
     */
    private final Map<String, ConcurrentLinkedQueue<Long>> lobbyQueues = new ConcurrentHashMap<>();

    /**
     * Locks pro Kategorie, um Änderungen an der jeweiligen Queue zu synchronisieren.
     */
    private final Map<String, ReentrantLock> lobbyLocks = new ConcurrentHashMap<>();

    /**
     * Set aller Spieler, die aktuell in irgendeiner Lobby sind.
     *
     * <p>Dient als globaler Guard gegen Beitritt in mehrere Lobbys gleichzeitig.</p>
     */
    private final Set<Long> playersInAnyLobby = ConcurrentHashMap.newKeySet();

    /**
     * Set aller Spieler, die aktuell als "im Matchmaking" markiert sind.
     *
     * <p>Verhindert, dass ein Spieler parallel mehrfach gematcht wird.</p>
     */
    private final Set<Long> playersInMatchmaking = ConcurrentHashMap.newKeySet();

    /**
     * Debug-Map zur Nachverfolgung des internen Status einzelner Spieler.
     *
     * <p>Wird für Logging/Debugging verwendet und ist nicht Teil der Kernlogik.</p>
     */
    private final Map<Long, String> playerStatus = new ConcurrentHashMap<>();

    /**
     * Fügt einen Spieler in die Lobby-Queue der angegebenen Kategorie ein.
     *
     * <p>Regeln:</p>
     * <ul>
     *   <li>Ein Spieler darf nur in einer Lobby gleichzeitig sein (globales Guard-Set).</li>
     *   <li>Ein Spieler darf nicht doppelt in derselben Queue auftauchen (harte Dedupe).</li>
     * </ul>
     *
     * <p>Bei Erfolg wird ein {@link LobbyStatusDTO} mit Status {@code WAITING} geliefert,
     * inklusive aktueller Position und Gesamtanzahl in der Queue.</p>
     *
     * @param playerId ID des Spielers
     * @param category gewünschte Kategorie (kann {@code null} oder leer sein)
     * @return Lobby-Status für den Client (WAITING / ALREADY_IN_LOBBY / ERROR)
     */
    public LobbyStatusDTO joinLobby(Long playerId, String category) {
        String lobbyKey = normalizeCategory(category);

        log.info("🚪 Spieler {} versucht Lobby {} (key: {}) zu betreten. Aktuell in Lobby: {}",
                playerId, category, lobbyKey, playersInAnyLobby.contains(playerId));

        // Verhindere doppelten Lobby-Beitritt (in irgendeiner Lobby)
        if (playersInAnyLobby.contains(playerId)) {
            log.warn("⚠️ Spieler {} ist bereits in einer Lobby", playerId);
            return LobbyStatusDTO.alreadyInLobby(category);
        }

        lobbyLocks.putIfAbsent(lobbyKey, new ReentrantLock());
        ReentrantLock lock = lobbyLocks.get(lobbyKey);

        lock.lock();
        try {
            lobbyQueues.putIfAbsent(lobbyKey, new ConcurrentLinkedQueue<>());
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);

            // Harte Dedupe: Spieler darf nicht doppelt in der Queue sein
            if (queue.contains(playerId)) {
                log.warn("⚠️ Spieler {} ist bereits in der Queue der Lobby {}", playerId, category);

                // Zustand geradeziehen (falls playersInAnyLobby wegen Race/Reload nicht gesetzt war)
                playersInAnyLobby.add(playerId);
                playerStatus.put(playerId, "WAITING_IN_LOBBY_" + category);

                int position = getPositionInQueue(playerId, queue);
                int total = queue.size();

                return LobbyStatusDTO.waiting(position, total, category);
            }

            // Normaler Beitritt
            queue.add(playerId);
            playersInAnyLobby.add(playerId);
            playerStatus.put(playerId, "WAITING_IN_LOBBY_" + category);

            int position = getPositionInQueue(playerId, queue);
            int total = queue.size();

            log.info("✅ Spieler {} Lobby {} beigetreten. Position: {}/{}. Warteschlange: {}",
                    playerId, category, position, total, queue);

            return LobbyStatusDTO.waiting(position, total, category);

        } catch (Exception e) {
            log.error("❌ Fehler beim Beitritt für Spieler {}: {}", playerId, e.getMessage(), e);
            return LobbyStatusDTO.error("Fehler beim Beitritt: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Prüft, ob in einer Kategorie mindestens zwei unterschiedliche Spieler vorhanden sind,
     * und erstellt bei Erfolg ein Match (ohne die Spieler direkt aus der Queue zu entfernen).
     *
     * <p>Wichtige Eigenschaften:</p>
     * <ul>
     *   <li>Es wird ein Lock pro Kategorie verwendet, um konsistente Queue-Zugriffe sicherzustellen.</li>
     *   <li>Self-Matches werden geblockt (auch wenn doppelte IDs in der Queue stehen).</li>
     *   <li>Spieler werden als {@code playersInMatchmaking} markiert, um Doppelmatches zu verhindern.</li>
     * </ul>
     *
     * @param category Kategorie, in der geprüft werden soll
     * @return {@link Optional} mit {@link MatchResult} oder leer, wenn kein Match möglich ist
     */
    public Optional<MatchResult> checkAndCreateMatch(String category) {
        String lobbyKey = normalizeCategory(category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            log.debug("📭 Keine Lobby für Kategorie {}", category);
            return Optional.empty();
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);

            if (queue == null || queue.size() < 2) {
                log.debug("👥 Nicht genug Spieler in Lobby {}: {}/2", category, queue != null ? queue.size() : 0);
                return Optional.empty();
            }

            Iterator<Long> iterator = queue.iterator();
            Long player1Id = iterator.next();

            // Finde den ersten ANDEREN Spieler als Gegner
            Long player2Id = null;
            while (iterator.hasNext()) {
                Long candidate = iterator.next();
                if (!candidate.equals(player1Id)) {
                    player2Id = candidate;
                    break;
                }
            }

            // Wenn nur doppelte IDs drin sind (z.B. [1,1]), kein Match erzeugen
            if (player2Id == null) {
                log.warn("⚠️ Kein gültiger Gegner gefunden (nur gleiche IDs) in Lobby {}: {}", category, queue);
                return Optional.empty();
            }

            // Zusätzlicher Schutz: Self-Match blocken
            if (player1Id.equals(player2Id)) {
                log.error("❌ SELF-MATCH BLOCKED: {} vs {} in {}", player1Id, player2Id, category);
                return Optional.empty();
            }

            log.info("🔍 Prüfe Match für {} vs {} in Kategorie {}",
                    player1Id, player2Id, category);

            // Prüfe, ob Spieler bereits im Matchmaking
            if (playersInMatchmaking.contains(player1Id) || playersInMatchmaking.contains(player2Id)) {
                log.warn("⚠️ Spieler bereits im Matchmaking: {} oder {}", player1Id, player2Id);
                return Optional.empty();
            }

            // Markiere Spieler als "im Matchmaking"
            playersInMatchmaking.add(player1Id);
            playersInMatchmaking.add(player2Id);
            playerStatus.put(player1Id, "MATCHMAKING");
            playerStatus.put(player2Id, "MATCHMAKING");

            log.info("🎯 POTENTIELLES MATCH: {} vs {} (Kategorie: {})",
                    player1Id, player2Id, category);

            return Optional.of(new MatchResult(player1Id, player2Id, category));

        } finally {
            lock.unlock();
        }
    }

    /**
     * Entfernt zwei Spieler nach erfolgreichem Match aus der Lobby-Queue
     * und bereinigt alle zugehörigen Status-Sets.
     *
     * <p>Zusätzlich wird die Lobby (Queue + Lock) gelöscht, wenn sie danach leer ist.</p>
     *
     * @param player1Id ID von Spieler 1
     * @param player2Id ID von Spieler 2
     * @param category Lobby-Kategorie
     */
    public void removePlayersAfterMatch(Long player1Id, Long player2Id, String category) {
        String lobbyKey = normalizeCategory(category);

        log.info("🗑️ Entferne Spieler {} und {} aus Lobby {}", player1Id, player2Id, category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            log.warn("⚠️ Lobby {} existiert nicht beim Entfernen der Spieler", lobbyKey);
            return;
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);
            if (queue != null) {
                boolean removed1 = queue.remove(player1Id);
                boolean removed2 = queue.remove(player2Id);

                playersInAnyLobby.remove(player1Id);
                playersInAnyLobby.remove(player2Id);

                playersInMatchmaking.remove(player1Id);
                playersInMatchmaking.remove(player2Id);

                playerStatus.remove(player1Id);
                playerStatus.remove(player2Id);

                log.info("✅ Spieler entfernt: {}={}, {}={}, verbleibend in Queue: {}",
                        player1Id, removed1, player2Id, removed2, queue);

                // Lösche leere Queue
                if (queue.isEmpty()) {
                    lobbyQueues.remove(lobbyKey);
                    lobbyLocks.remove(lobbyKey);
                    log.info("🗑️ Leere Lobby {} wurde gelöscht", lobbyKey);
                }
            } else {
                log.warn("⚠️ Queue für Lobby {} ist null", lobbyKey);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Setzt den Matchmaking-Status für zwei Spieler zurück.
     *
     * <p>Wird genutzt, wenn nach einem "potenziellen Match" ein Fehler auftritt
     * (z. B. beim Erstellen eines Spiels).</p>
     *
     * @param player1Id ID von Spieler 1
     * @param player2Id ID von Spieler 2
     * @param category Lobby-Kategorie
     */
    public void resetMatchmaking(Long player1Id, Long player2Id, String category) {
        String lobbyKey = normalizeCategory(category);

        log.warn("🔄 Setze Matchmaking für {} und {} zurück", player1Id, player2Id);

        if (lobbyLocks.containsKey(lobbyKey)) {
            ReentrantLock lock = lobbyLocks.get(lobbyKey);
            lock.lock();
            try {
                playersInMatchmaking.remove(player1Id);
                playersInMatchmaking.remove(player2Id);
                playerStatus.put(player1Id, "RESET_TO_WAITING");
                playerStatus.put(player2Id, "RESET_TO_WAITING");
            } finally {
                lock.unlock();
            }
        } else {
            playersInMatchmaking.remove(player1Id);
            playersInMatchmaking.remove(player2Id);
        }
    }

    /**
     * Entfernt einen Spieler aus einer Lobby (Leave).
     *
     * <p>Bereinigt neben der Queue auch die globalen Status-Sets
     * ({@code playersInAnyLobby}, {@code playersInMatchmaking}) und löscht leere Lobbys.</p>
     *
     * @param playerId ID des Spielers
     * @param category Lobby-Kategorie
     */
    public void leaveLobby(Long playerId, String category) {
        String lobbyKey = normalizeCategory(category);

        log.info("🚶 Spieler {} verlässt Lobby {}", playerId, category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            log.warn("⚠️ Lobby {} existiert nicht beim Verlassen", lobbyKey);
            return;
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);
            if (queue != null) {
                boolean removed = queue.remove(playerId);
                playersInAnyLobby.remove(playerId);
                playersInMatchmaking.remove(playerId);
                playerStatus.remove(playerId);

                log.info("✅ Spieler {} erfolgreich entfernt: {}. Verbleibend: {}",
                        playerId, removed, queue);

                if (queue.isEmpty()) {
                    lobbyQueues.remove(lobbyKey);
                    lobbyLocks.remove(lobbyKey);
                    log.info("🗑️ Leere Lobby {} gelöscht", lobbyKey);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Liefert eine Snapshot-Ansicht der Lobby einer Kategorie.
     *
     * <p>Die Rückgabe enthält die Spieler-IDs in aktueller Queue-Reihenfolge
     * sowie die Anzahl der Spieler.</p>
     *
     * @param category Lobby-Kategorie
     * @return {@link LobbyInfo} mit Spieler-IDs und Anzahl
     */
    public LobbyInfo getLobbyInfo(String category) {
        String lobbyKey = normalizeCategory(category);
        ConcurrentLinkedQueue<Long> queue = lobbyQueues.getOrDefault(lobbyKey, new ConcurrentLinkedQueue<>());
        List<Long> playerList = new ArrayList<>(queue);
        log.debug("📊 Lobby Info für {}: {} Spieler: {}", category, playerList.size(), playerList);
        return new LobbyInfo(playerList);
    }

    /**
     * Liefert Debug-Informationen über den internen Lobby-Zustand.
     *
     * <p>Geeignet für Diagnosezwecke (z. B. Admin-Endpoint / Logs).</p>
     *
     * @return Map mit Debug-Daten (Queues, Sets, Status)
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("lobbyQueues", lobbyQueues);
        debugInfo.put("playersInAnyLobby", playersInAnyLobby);
        debugInfo.put("playersInMatchmaking", playersInMatchmaking);
        debugInfo.put("playerStatus", playerStatus);
        return debugInfo;
    }

    /**
     * Liefert den (Debug-)Status eines Spielers.
     *
     * @param playerId ID des Spielers
     * @return Status-String oder {@code "UNKNOWN"}, falls nicht vorhanden
     */
    public String getPlayerStatus(Long playerId) {
        return playerStatus.getOrDefault(playerId, "UNKNOWN");
    }

    /**
     * Berechnet die 1-basierte Position eines Spielers in einer Queue.
     *
     * @param playerId ID des Spielers
     * @param queue Queue der Lobby
     * @return Position (1..n) oder 0, falls Spieler nicht in der Queue ist
     */
    private int getPositionInQueue(Long playerId, Queue<Long> queue) {
        int position = 1;
        for (Long id : queue) {
            if (id.equals(playerId)) {
                return position;
            }
            position++;
        }
        return 0;
    }

    /**
     * Normalisiert eine Kategorie zu einem stabilen Key für Maps/Locks.
     *
     * <p>{@code null} oder leer wird zu {@code "DEFAULT"}.</p>
     *
     * @param category Kategorie-String
     * @return normalisierter Lobby-Key (uppercase oder DEFAULT)
     */
    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "DEFAULT";
        }
        return category.toUpperCase();
    }

    // ========== INNER CLASSES ==========

    /**
     * Immutable Snapshot der Lobby-Informationen für eine Kategorie.
     *
     * <p>Enthält die aktuellen Spieler-IDs (Queue-Reihenfolge) sowie die Anzahl.</p>
     */
    public static class LobbyInfo {
        /** Spieler-IDs in aktueller Queue-Reihenfolge. */
        public final List<Long> playerIds;

        /** Anzahl der Spieler in der Lobby. */
        public final int playerCount;

        /**
         * Erstellt ein LobbyInfo-Objekt aus einer Liste von Spieler-IDs.
         *
         * @param playerIds aktuelle Spieler-IDs
         */
        public LobbyInfo(List<Long> playerIds) {
            this.playerIds = playerIds;
            this.playerCount = playerIds.size();
        }
    }

    /**
     * Ergebnisobjekt für ein potenzielles Match zweier Spieler in einer Kategorie.
     *
     * <p>Dieses Objekt ist ein Zwischenschritt: Es signalisiert,
     * dass zwei Spieler gematcht werden können. Die tatsächliche Spielerzeugung
     * (Game) erfolgt an anderer Stelle (z. B. im Orchestrator).</p>
     */
    public static class MatchResult {
        /** ID von Spieler 1. */
        public final Long player1Id;

        /** ID von Spieler 2. */
        public final Long player2Id;

        /** Kategorie, in der das Match erstellt wurde. */
        public final String category;

        /**
         * Erstellt ein MatchResult.
         *
         * @param player1Id ID von Spieler 1
         * @param player2Id ID von Spieler 2
         * @param category Kategorie des Matchings
         */
        public MatchResult(Long player1Id, Long player2Id, String category) {
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.category = category;
        }

        /**
         * Liefert eine lesbare String-Repräsentation für Debugging/Logging.
         *
         * @return String-Repräsentation des MatchResult
         */
        @Override
        public String toString() {
            return "MatchResult{" +
                    "player1Id=" + player1Id +
                    ", player2Id=" + player2Id +
                    ", category='" + category + '\'' +
                    '}';
        }
    }
}
