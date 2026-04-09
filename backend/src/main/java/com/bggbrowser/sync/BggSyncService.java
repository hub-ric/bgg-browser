package com.bggbrowser.sync;

import com.bggbrowser.game.Game;
import com.bggbrowser.game.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class BggSyncService {

    private static final Logger log = LoggerFactory.getLogger(BggSyncService.class);
    private static final String BGG_THING_URL =
        "https://boardgamegeek.com/xmlapi2/thing?id={ids}&stats=1&type=boardgame";

    private final GameRepository gameRepository;
    private final BggXmlParser parser;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${bgg.sync.batch-size:20}")
    private int batchSize;

    @Value("${bgg.sync.max-id:100000}")
    private int maxId;

    @Value("${bgg.sync.max-rank:5000}")
    private int maxRank;

    @Value("${bgg.sync.delay-ms:600}")
    private long delayMs;

    public BggSyncService(GameRepository gameRepository, BggXmlParser parser) {
        this.gameRepository = gameRepository;
        this.parser = parser;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (gameRepository.count() == 0) {
            log.info("Database empty — running initial BGG sync (this may take ~1 hour)");
            runFullSync();
        } else {
            log.info("Database populated ({} games) — skipping initial sync", gameRepository.count());
        }
    }

    @Scheduled(cron = "0 0 3 * * MON") // every Monday at 03:00
    public void weeklySync() {
        log.info("Starting weekly BGG sync");
        runIncrementalSync();
    }

    /**
     * Full sync: iterate IDs 1..maxId in batches, keep games with rank <= maxRank.
     */
    public void runFullSync() {
        for (int start = 1; start <= maxId; start += batchSize) {
            List<Integer> ids = IntStream.range(start, Math.min(start + batchSize, maxId + 1))
                .boxed().collect(Collectors.toList());
            fetchAndSave(ids, maxRank);
            sleep(delayMs);
        }
        log.info("Full sync complete. Total games: {}", gameRepository.count());
    }

    /**
     * Incremental sync: re-fetch only already-known game IDs to update their stats.
     */
    public void runIncrementalSync() {
        List<Long> ids = gameRepository.findAll()
            .stream().map(Game::getId).collect(Collectors.toList());
        List<List<Long>> batches = partition(ids, batchSize);
        for (List<Long> batch : batches) {
            List<Integer> intIds = batch.stream().map(Long::intValue).collect(Collectors.toList());
            fetchAndSave(intIds, Integer.MAX_VALUE);
            sleep(delayMs);
        }
        log.info("Incremental sync complete. Total games: {}", gameRepository.count());
    }

    private void fetchAndSave(List<Integer> ids, int rankLimit) {
        String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            String xml = restTemplate.getForObject(BGG_THING_URL, String.class, idsParam);
            if (xml == null) return;
            List<Game> games = parser.parse(xml);
            List<Game> ranked = games.stream()
                .filter(g -> g.getBggRank() != null && g.getBggRank() <= rankLimit)
                .collect(Collectors.toList());
            if (!ranked.isEmpty()) gameRepository.saveAll(ranked);
        } catch (Exception e) {
            log.warn("Failed to fetch batch {}: {}", idsParam, e.getMessage());
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
