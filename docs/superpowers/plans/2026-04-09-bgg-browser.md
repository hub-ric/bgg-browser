# BGG Browser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a mobile-friendly web app to browse BGG games by rank, score, complexity, and player count, backed by a Spring Boot API that syncs game data from BGG into PostgreSQL.

**Architecture:** React SPA (Vite + TypeScript + Tailwind) on Vercel calls a Spring Boot REST API on Railway. The backend owns a PostgreSQL database populated by a weekly scheduled sync job that fetches game data from the BGG XML API2 in batches. Filters are stored in URL query params so browser back/forward preserves state.

**Tech Stack:** Java 21, Spring Boot 3.2, Spring Data JPA, Flyway, PostgreSQL, React 18, Vite, TypeScript, Tailwind CSS 3, TanStack Query v5, React Router v6, GitHub Actions, Railway, Vercel.

---

## File Map

```
bgg-browser/
├── .github/workflows/
│   ├── ci.yml
│   └── cd.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/bggbrowser/
│       │   ├── BggBrowserApplication.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java
│       │   │   └── RateLimitFilter.java
│       │   ├── game/
│       │   │   ├── Game.java
│       │   │   ├── GameRepository.java
│       │   │   ├── GameService.java
│       │   │   ├── GameController.java
│       │   │   ├── GameSummaryDto.java
│       │   │   ├── GameDetailDto.java
│       │   │   └── GameFilterParams.java
│       │   └── sync/
│       │       ├── BggSyncService.java
│       │       └── BggXmlParser.java
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── db/migration/V1__create_games_table.sql
│       └── test/java/com/bggbrowser/
│           ├── game/
│           │   ├── GameServiceTest.java
│           │   └── GameControllerTest.java
│           └── sync/
│               └── BggXmlParserTest.java
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.ts
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── types/game.ts
        ├── api/games.ts
        ├── hooks/useGames.ts
        ├── pages/
        │   ├── BrowsePage.tsx
        │   └── GameDetailPage.tsx
        └── components/
            ├── ComplexityStars.tsx
            ├── GameCard.tsx
            ├── FilterSidebar.tsx
            ├── GameList.tsx
            └── Pagination.tsx
```

---

## Phase 1: Backend

---

### Task 1: Monorepo scaffold + Spring Boot project

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/bggbrowser/BggBrowserApplication.java`
- Create: `backend/src/main/resources/application.properties`
- Create: `.gitignore`

- [ ] **Step 1: Create root .gitignore**

```
# Java
backend/target/
*.class
*.jar

# Node
frontend/node_modules/
frontend/dist/

# Env
.env
*.env

# IDE
.idea/
.vscode/
*.iml

# Local dev config (contains DB credentials)
backend/src/main/resources/application-dev.properties

# Superpowers
.superpowers/
```

- [ ] **Step 2: Create `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.3</version>
    <relativePath/>
  </parent>

  <groupId>com.bggbrowser</groupId>
  <artifactId>bgg-browser</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Create `backend/src/main/java/com/bggbrowser/BggBrowserApplication.java`**

```java
package com.bggbrowser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BggBrowserApplication {
    public static void main(String[] args) {
        SpringApplication.run(BggBrowserApplication.class, args);
    }
}
```

- [ ] **Step 4: Create `backend/src/main/resources/application.properties`**

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

bgg.sync.batch-size=20
bgg.sync.max-id=100000
bgg.sync.max-rank=5000
bgg.sync.delay-ms=600

# Dev override: create application-dev.properties locally with a real DB URL
```

- [ ] **Step 5: Verify project compiles**

```bash
cd backend && mvn compile -q
```
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add .gitignore backend/pom.xml backend/src/main/java/com/bggbrowser/BggBrowserApplication.java backend/src/main/resources/application.properties
git commit -m "feat: scaffold backend Spring Boot project"
```

---

### Task 2: Database migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_games_table.sql`

- [ ] **Step 1: Create `V1__create_games_table.sql`**

```sql
CREATE TABLE games (
    id               BIGINT PRIMARY KEY,
    name             TEXT NOT NULL,
    year_published   INT,
    bgg_rank         INT,
    avg_rating       NUMERIC(4,2),
    complexity       NUMERIC(3,2),
    min_players      INT,
    max_players      INT,
    thumbnail_url    TEXT,
    description      TEXT,
    play_time_min    INT,
    play_time_max    INT,
    last_synced_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_games_bgg_rank   ON games (bgg_rank);
CREATE INDEX idx_games_avg_rating ON games (avg_rating);
CREATE INDEX idx_games_complexity ON games (complexity);
CREATE INDEX idx_games_name       ON games (name);
```

- [ ] **Step 2: Start a local Postgres for development**

```bash
docker run --name bgg-pg -e POSTGRES_DB=bggbrowser -e POSTGRES_USER=bgg -e POSTGRES_PASSWORD=bgg -p 5432:5432 -d postgres:16
```

- [ ] **Step 3: Create `backend/src/main/resources/application-dev.properties`**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bggbrowser
spring.datasource.username=bgg
spring.datasource.password=bgg
```

(Add `application-dev.properties` to `.gitignore`)

- [ ] **Step 4: Run migration**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Expected: App starts, Flyway logs `Successfully applied 1 migration`.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add games table migration"
```

---

### Task 3: Game entity and repository

**Files:**
- Create: `backend/src/main/java/com/bggbrowser/game/Game.java`
- Create: `backend/src/main/java/com/bggbrowser/game/GameRepository.java`

- [ ] **Step 1: Create `Game.java`**

```java
package com.bggbrowser.game;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "games")
public class Game {

    @Id
    private Long id;
    private String name;
    private Integer yearPublished;
    private Integer bggRank;
    private BigDecimal avgRating;
    private BigDecimal complexity;
    private Integer minPlayers;
    private Integer maxPlayers;
    private String thumbnailUrl;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Integer playTimeMin;
    private Integer playTimeMax;
    private Instant lastSyncedAt;

    public Game() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getYearPublished() { return yearPublished; }
    public void setYearPublished(Integer yearPublished) { this.yearPublished = yearPublished; }
    public Integer getBggRank() { return bggRank; }
    public void setBggRank(Integer bggRank) { this.bggRank = bggRank; }
    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
    public BigDecimal getComplexity() { return complexity; }
    public void setComplexity(BigDecimal complexity) { this.complexity = complexity; }
    public Integer getMinPlayers() { return minPlayers; }
    public void setMinPlayers(Integer minPlayers) { this.minPlayers = minPlayers; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPlayTimeMin() { return playTimeMin; }
    public void setPlayTimeMin(Integer playTimeMin) { this.playTimeMin = playTimeMin; }
    public Integer getPlayTimeMax() { return playTimeMax; }
    public void setPlayTimeMax(Integer playTimeMax) { this.playTimeMax = playTimeMax; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
```

- [ ] **Step 2: Create `GameRepository.java`**

```java
package com.bggbrowser.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameRepository extends JpaRepository<Game, Long>,
        JpaSpecificationExecutor<Game> {
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/bggbrowser/game/
git commit -m "feat: add Game entity and repository"
```

---

### Task 4: BGG XML parser

**Files:**
- Create: `backend/src/main/java/com/bggbrowser/sync/BggXmlParser.java`
- Create: `backend/src/test/java/com/bggbrowser/sync/BggXmlParserTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/bggbrowser/sync/BggXmlParserTest.java`:

```java
package com.bggbrowser.sync;

import com.bggbrowser.game.Game;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BggXmlParserTest {

    private final BggXmlParser parser = new BggXmlParser();

    private static final String SAMPLE_XML = """
        <?xml version="1.0" encoding="utf-8"?>
        <items>
          <item type="boardgame" id="224517">
            <thumbnail>https://example.com/thumb.jpg</thumbnail>
            <name type="primary" sortindex="1" value="Brass: Birmingham"/>
            <description>A game about industry.</description>
            <yearpublished value="2018"/>
            <minplayers value="2"/>
            <maxplayers value="4"/>
            <minplaytime value="60"/>
            <maxplaytime value="120"/>
            <statistics page="1">
              <ratings>
                <average value="8.61"/>
                <averageweight value="3.89"/>
                <ranks>
                  <rank type="subtype" name="boardgame" value="1"/>
                </ranks>
              </ratings>
            </statistics>
          </item>
          <item type="boardgameexpansion" id="999">
            <name type="primary" value="Some Expansion"/>
          </item>
        </items>
        """;

    @Test
    void parsesGameFields() {
        List<Game> games = parser.parse(SAMPLE_XML);
        assertThat(games).hasSize(1);
        Game g = games.get(0);
        assertThat(g.getId()).isEqualTo(224517L);
        assertThat(g.getName()).isEqualTo("Brass: Birmingham");
        assertThat(g.getYearPublished()).isEqualTo(2018);
        assertThat(g.getBggRank()).isEqualTo(1);
        assertThat(g.getAvgRating()).isEqualByComparingTo("8.61");
        assertThat(g.getComplexity()).isEqualByComparingTo("3.89");
        assertThat(g.getMinPlayers()).isEqualTo(2);
        assertThat(g.getMaxPlayers()).isEqualTo(4);
        assertThat(g.getPlayTimeMin()).isEqualTo(60);
        assertThat(g.getPlayTimeMax()).isEqualTo(120);
        assertThat(g.getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(g.getDescription()).isEqualTo("A game about industry.");
    }

    @Test
    void skipsNonBoardgameItems() {
        List<Game> games = parser.parse(SAMPLE_XML);
        assertThat(games).noneMatch(g -> g.getId() == 999L);
    }

    @Test
    void returnsEmptyListForEmptyItems() {
        List<Game> games = parser.parse("<items></items>");
        assertThat(games).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl . -Dtest=BggXmlParserTest -q 2>&1 | tail -5
```
Expected: `COMPILATION ERROR` or `ClassNotFoundException` — `BggXmlParser` doesn't exist yet.

- [ ] **Step 3: Implement `BggXmlParser.java`**

```java
package com.bggbrowser.sync;

import com.bggbrowser.game.Game;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class BggXmlParser {

    public List<Game> parse(String xml) {
        List<Game> results = new ArrayList<>();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                if (!"boardgame".equals(item.getAttribute("type"))) continue;
                Game game = parseItem(item);
                if (game != null) results.add(game);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BGG XML", e);
        }
        return results;
    }

    private Game parseItem(Element item) {
        Game game = new Game();
        game.setId(Long.parseLong(item.getAttribute("id")));
        game.setLastSyncedAt(Instant.now());

        NodeList names = item.getElementsByTagName("name");
        for (int i = 0; i < names.getLength(); i++) {
            Element name = (Element) names.item(i);
            if ("primary".equals(name.getAttribute("type"))) {
                game.setName(name.getAttribute("value"));
                break;
            }
        }
        if (game.getName() == null) return null;

        game.setThumbnailUrl(textContent(item, "thumbnail"));
        game.setDescription(textContent(item, "description"));
        game.setYearPublished(intAttr(item, "yearpublished", "value"));
        game.setMinPlayers(intAttr(item, "minplayers", "value"));
        game.setMaxPlayers(intAttr(item, "maxplayers", "value"));
        game.setPlayTimeMin(intAttr(item, "minplaytime", "value"));
        game.setPlayTimeMax(intAttr(item, "maxplaytime", "value"));

        NodeList ratings = item.getElementsByTagName("ratings");
        if (ratings.getLength() > 0) {
            Element r = (Element) ratings.item(0);
            game.setAvgRating(decimalAttr(r, "average", "value"));
            game.setComplexity(decimalAttr(r, "averageweight", "value"));

            NodeList ranks = r.getElementsByTagName("rank");
            for (int i = 0; i < ranks.getLength(); i++) {
                Element rank = (Element) ranks.item(i);
                if ("boardgame".equals(rank.getAttribute("name"))) {
                    String val = rank.getAttribute("value");
                    if (!val.isBlank() && !"Not Ranked".equals(val)) {
                        game.setBggRank(Integer.parseInt(val));
                    }
                    break;
                }
            }
        }
        return game;
    }

    private String textContent(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String text = nl.item(0).getTextContent().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer intAttr(Element parent, String tag, String attr) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String val = ((Element) nl.item(0)).getAttribute(attr);
        if (val.isBlank()) return null;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal decimalAttr(Element parent, String tag, String attr) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String val = ((Element) nl.item(0)).getAttribute(attr);
        if (val.isBlank() || "0".equals(val)) return null;
        try { return new BigDecimal(val); } catch (NumberFormatException e) { return null; }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd backend && mvn test -Dtest=BggXmlParserTest -q 2>&1 | tail -5
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/bggbrowser/sync/BggXmlParser.java backend/src/test/java/com/bggbrowser/sync/BggXmlParserTest.java
git commit -m "feat: add BGG XML parser with tests"
```

---

### Task 5: BGG sync service

**Files:**
- Create: `backend/src/main/java/com/bggbrowser/sync/BggSyncService.java`

- [ ] **Step 1: Create `BggSyncService.java`**

```java
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
        List<Long> existingIds = gameRepository.findAll()
            .stream().map(Game::getId).collect(Collectors.toList());

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
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/bggbrowser/sync/BggSyncService.java
git commit -m "feat: add BGG sync service"
```

---

### Task 6: Game service (filtering + pagination)

**Files:**
- Create: `backend/src/main/java/com/bggbrowser/game/GameFilterParams.java`
- Create: `backend/src/main/java/com/bggbrowser/game/GameSummaryDto.java`
- Create: `backend/src/main/java/com/bggbrowser/game/GameDetailDto.java`
- Create: `backend/src/main/java/com/bggbrowser/game/GameService.java`
- Create: `backend/src/test/java/com/bggbrowser/game/GameServiceTest.java`

- [ ] **Step 1: Create `GameFilterParams.java`**

```java
package com.bggbrowser.game;

public record GameFilterParams(
    String name,
    Integer minPlayers,
    Integer maxPlayers,
    Double minComplexity,
    Double maxComplexity,
    Integer maxRank,
    String sort,
    String sortDir,
    int page,
    int size
) {
    public GameFilterParams {
        if (sort == null) sort = "rank";
        if (sortDir == null) sortDir = "asc";
        if (size <= 0 || size > 50) size = 20;
        if (page < 0) page = 0;
    }
}
```

- [ ] **Step 2: Create `GameSummaryDto.java`**

```java
package com.bggbrowser.game;

import java.math.BigDecimal;

public record GameSummaryDto(
    Long id,
    String name,
    Integer yearPublished,
    Integer bggRank,
    BigDecimal avgRating,
    BigDecimal complexity,
    Integer minPlayers,
    Integer maxPlayers,
    String thumbnailUrl
) {
    public static GameSummaryDto from(Game g) {
        return new GameSummaryDto(g.getId(), g.getName(), g.getYearPublished(),
            g.getBggRank(), g.getAvgRating(), g.getComplexity(),
            g.getMinPlayers(), g.getMaxPlayers(), g.getThumbnailUrl());
    }
}
```

- [ ] **Step 3: Create `GameDetailDto.java`**

```java
package com.bggbrowser.game;

import java.math.BigDecimal;

public record GameDetailDto(
    Long id,
    String name,
    Integer yearPublished,
    Integer bggRank,
    BigDecimal avgRating,
    BigDecimal complexity,
    Integer minPlayers,
    Integer maxPlayers,
    String thumbnailUrl,
    String description,
    Integer playTimeMin,
    Integer playTimeMax
) {
    public static GameDetailDto from(Game g) {
        return new GameDetailDto(g.getId(), g.getName(), g.getYearPublished(),
            g.getBggRank(), g.getAvgRating(), g.getComplexity(),
            g.getMinPlayers(), g.getMaxPlayers(), g.getThumbnailUrl(),
            g.getDescription(), g.getPlayTimeMin(), g.getPlayTimeMax());
    }
}
```

- [ ] **Step 4: Write the failing test**

Create `backend/src/test/java/com/bggbrowser/game/GameServiceTest.java`:

```java
package com.bggbrowser.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock GameRepository repository;
    @InjectMocks GameService service;

    private Game sampleGame() {
        Game g = new Game();
        g.setId(1L);
        g.setName("Brass: Birmingham");
        g.setBggRank(1);
        g.setAvgRating(new BigDecimal("8.61"));
        g.setComplexity(new BigDecimal("3.89"));
        g.setMinPlayers(2);
        g.setMaxPlayers(4);
        g.setLastSyncedAt(Instant.now());
        return g;
    }

    @Test
    void getGamesReturnsMappedDtos() {
        Game game = sampleGame();
        Page<Game> page = new PageImpl<>(List.of(game), PageRequest.of(0, 20), 1);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        GameFilterParams params = new GameFilterParams(null, null, null, null, null, null, "rank", "asc", 0, 20);
        Page<GameSummaryDto> result = service.getGames(params);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Brass: Birmingham");
    }

    @Test
    void getGameByIdThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(GameNotFoundException.class, () -> service.getGameById(99L));
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

```bash
cd backend && mvn test -Dtest=GameServiceTest -q 2>&1 | tail -5
```
Expected: compilation error — `GameService` and `GameNotFoundException` don't exist yet.

- [ ] **Step 6: Create `GameService.java`**

```java
package com.bggbrowser.game;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {

    private final GameRepository repository;

    public GameService(GameRepository repository) {
        this.repository = repository;
    }

    public Page<GameSummaryDto> getGames(GameFilterParams params) {
        Specification<Game> spec = buildSpec(params);
        Sort sort = buildSort(params.sort(), params.sortDir());
        Pageable pageable = PageRequest.of(params.page(), params.size(), sort);
        return repository.findAll(spec, pageable).map(GameSummaryDto::from);
    }

    public GameDetailDto getGameById(Long id) {
        return repository.findById(id)
            .map(GameDetailDto::from)
            .orElseThrow(() -> new GameNotFoundException(id));
    }

    private Specification<Game> buildSpec(GameFilterParams p) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (p.name() != null && !p.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                    "%" + p.name().toLowerCase() + "%"));
            }
            if (p.minPlayers() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPlayers"), p.minPlayers()));
            }
            if (p.maxPlayers() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPlayers"), p.maxPlayers()));
            }
            if (p.minComplexity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("complexity"),
                    BigDecimal.valueOf(p.minComplexity())));
            }
            if (p.maxComplexity() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("complexity"),
                    BigDecimal.valueOf(p.maxComplexity())));
            }
            if (p.maxRank() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bggRank"), p.maxRank()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sort, String dir) {
        String field = switch (sort) {
            case "rating" -> "avgRating";
            case "complexity" -> "complexity";
            case "name" -> "name";
            default -> "bggRank";
        };
        return Sort.by("desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC, field);
    }
}
```

- [ ] **Step 7: Create `GameNotFoundException.java`**

```java
package com.bggbrowser.game;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(Long id) {
        super("Game not found: " + id);
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
cd backend && mvn test -Dtest=GameServiceTest -q 2>&1 | tail -5
```
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/bggbrowser/game/
git commit -m "feat: add GameService with filter/pagination and DTOs"
```

---

### Task 7: Game REST controller

**Files:**
- Create: `backend/src/main/java/com/bggbrowser/game/GameController.java`
- Create: `backend/src/test/java/com/bggbrowser/game/GameControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.bggbrowser.game;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired MockMvc mvc;
    @MockBean GameService gameService;

    @Test
    void getGamesReturns200() throws Exception {
        GameSummaryDto dto = new GameSummaryDto(1L, "Brass: Birmingham", 2018, 1,
            new BigDecimal("8.61"), new BigDecimal("3.89"), 2, 4, null);
        Page<GameSummaryDto> page = new PageImpl<>(List.of(dto));
        when(gameService.getGames(any())).thenReturn(page);

        mvc.perform(get("/api/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Brass: Birmingham"))
            .andExpect(jsonPath("$.content[0].bggRank").value(1));
    }

    @Test
    void getGameByIdReturns404WhenNotFound() throws Exception {
        when(gameService.getGameById(99L)).thenThrow(new GameNotFoundException(99L));
        mvc.perform(get("/api/games/99")).andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -Dtest=GameControllerTest -q 2>&1 | tail -5
```
Expected: compilation error — `GameController` doesn't exist yet.

- [ ] **Step 3: Create `GameController.java`**

```java
package com.bggbrowser.game;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public Page<GameSummaryDto> getGames(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minPlayers,
        @RequestParam(required = false) Integer maxPlayers,
        @RequestParam(required = false) Double minComplexity,
        @RequestParam(required = false) Double maxComplexity,
        @RequestParam(required = false) Integer maxRank,
        @RequestParam(defaultValue = "rank") String sort,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return gameService.getGames(new GameFilterParams(name, minPlayers, maxPlayers,
            minComplexity, maxComplexity, maxRank, sort, sortDir, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDetailDto> getGame(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGameById(id));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd backend && mvn test -Dtest=GameControllerTest -q 2>&1 | tail -5
```
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && mvn test -q 2>&1 | tail -5
```
Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/bggbrowser/game/GameController.java backend/src/test/java/com/bggbrowser/game/GameControllerTest.java
git commit -m "feat: add game REST controller with tests"
```

---

### Task 8: CORS + rate limit config

**Files:**
- Create: `backend/src/main/java/com/bggbrowser/config/CorsConfig.java`
- Create: `backend/src/main/java/com/bggbrowser/config/RateLimitFilter.java`

- [ ] **Step 1: Create `CorsConfig.java`**

```java
package com.bggbrowser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.split(","))
            .allowedMethods("GET")
            .maxAge(3600);
    }
}
```

Add to `application.properties`:
```properties
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}
```

- [ ] **Step 2: Create `RateLimitFilter.java`**

```java
package com.bggbrowser.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = request.getRemoteAddr();
        RequestCounter counter = counters.computeIfAbsent(ip, k -> new RequestCounter());

        if (counter.increment() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            return;
        }
        chain.doFilter(req, res);
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        int increment() {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() > 60_000) {
                count.set(0);
                windowStart.set(now);
            }
            return count.incrementAndGet();
        }
    }
}
```

- [ ] **Step 3: Verify app starts with dev profile**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev 2>&1 | grep -E "Started|ERROR" | head -3
```
Expected: `Started BggBrowserApplication`

- [ ] **Step 4: Smoke test the API**

```bash
curl -s "http://localhost:8080/api/games?size=3" | python -m json.tool | head -20
```
Expected: JSON with `content`, `totalElements`, `totalPages` keys.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/bggbrowser/config/
git commit -m "feat: add CORS config and rate limit filter"
```

---

### Task 9: Dockerfile

**Files:**
- Create: `backend/Dockerfile`

- [ ] **Step 1: Create `backend/Dockerfile`**

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Build and verify**

```bash
cd backend && docker build -t bgg-browser-backend .
```
Expected: `Successfully tagged bgg-browser-backend:latest`

- [ ] **Step 3: Commit**

```bash
git add backend/Dockerfile
git commit -m "feat: add multi-stage Dockerfile for backend"
```

---

## Phase 2: Frontend

---

### Task 10: Bootstrap React + Vite + TypeScript + Tailwind

**Files:**
- Create: `frontend/` (via Vite scaffolding)
- Modify: `frontend/vite.config.ts`
- Create: `frontend/tailwind.config.ts`

- [ ] **Step 1: Scaffold project**

```bash
cd /path/to/bgg-browser
npm create vite@latest frontend -- --template react-ts
cd frontend && npm install
```

- [ ] **Step 2: Install dependencies**

```bash
npm install @tanstack/react-query react-router-dom
npm install -D tailwindcss postcss autoprefixer @testing-library/react @testing-library/jest-dom vitest jsdom @vitejs/plugin-react
npx tailwindcss init -p --ts
```

- [ ] **Step 3: Update `frontend/tailwind.config.ts`**

```ts
import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: { extend: {} },
  plugins: [],
} satisfies Config
```

- [ ] **Step 4: Update `frontend/src/index.css`** (replace entire file)

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 5: Update `frontend/vite.config.ts`**

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test-setup.ts',
  },
})
```

- [ ] **Step 6: Create `frontend/src/test-setup.ts`**

```ts
import '@testing-library/jest-dom'
```

- [ ] **Step 7: Add test script to `frontend/package.json`**

In `scripts`, add:
```json
"test": "vitest run",
"test:watch": "vitest"
```

- [ ] **Step 8: Verify build**

```bash
cd frontend && npm run build
```
Expected: `✓ built in`

- [ ] **Step 9: Commit**

```bash
git add frontend/
git commit -m "feat: scaffold React + Vite + TypeScript + Tailwind frontend"
```

---

### Task 11: Types and API client

**Files:**
- Create: `frontend/src/types/game.ts`
- Create: `frontend/src/api/games.ts`

- [ ] **Step 1: Create `frontend/src/types/game.ts`**

```ts
export interface GameSummary {
  id: number
  name: string
  yearPublished: number | null
  bggRank: number | null
  avgRating: number | null
  complexity: number | null
  minPlayers: number | null
  maxPlayers: number | null
  thumbnailUrl: string | null
}

export interface GameDetail extends GameSummary {
  description: string | null
  playTimeMin: number | null
  playTimeMax: number | null
}

export interface GamesPage {
  content: GameSummary[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface GameFilters {
  name?: string
  minPlayers?: number
  maxPlayers?: number
  minComplexity?: number
  maxComplexity?: number
  maxRank?: number
  sort?: 'rank' | 'rating' | 'complexity' | 'name'
  sortDir?: 'asc' | 'desc'
  page?: number
  size?: number
}
```

- [ ] **Step 2: Create `frontend/src/api/games.ts`**

```ts
import type { GameDetail, GameFilters, GamesPage } from '../types/game'

const BASE = '/api/games'

export async function fetchGames(filters: GameFilters): Promise<GamesPage> {
  const params = new URLSearchParams()
  if (filters.name) params.set('name', filters.name)
  if (filters.minPlayers != null) params.set('minPlayers', String(filters.minPlayers))
  if (filters.maxPlayers != null) params.set('maxPlayers', String(filters.maxPlayers))
  if (filters.minComplexity != null) params.set('minComplexity', String(filters.minComplexity))
  if (filters.maxComplexity != null) params.set('maxComplexity', String(filters.maxComplexity))
  if (filters.maxRank != null) params.set('maxRank', String(filters.maxRank))
  if (filters.sort) params.set('sort', filters.sort)
  if (filters.sortDir) params.set('sortDir', filters.sortDir)
  if (filters.page != null) params.set('page', String(filters.page))
  if (filters.size != null) params.set('size', String(filters.size))

  const res = await fetch(`${BASE}?${params}`)
  if (!res.ok) throw new Error(`Failed to fetch games: ${res.status}`)
  return res.json()
}

export async function fetchGame(id: number): Promise<GameDetail> {
  const res = await fetch(`${BASE}/${id}`)
  if (!res.ok) throw new Error(`Game not found: ${id}`)
  return res.json()
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/ frontend/src/api/
git commit -m "feat: add TypeScript types and API client"
```

---

### Task 12: ComplexityStars and GameCard components

**Files:**
- Create: `frontend/src/components/ComplexityStars.tsx`
- Create: `frontend/src/components/GameCard.tsx`

- [ ] **Step 1: Write failing test for ComplexityStars**

Create `frontend/src/components/ComplexityStars.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { ComplexityStars } from './ComplexityStars'

test('renders 5 stars with correct filled count', () => {
  render(<ComplexityStars complexity={3.5} />)
  const filled = document.querySelectorAll('[data-testid="star-filled"]')
  const empty = document.querySelectorAll('[data-testid="star-empty"]')
  expect(filled).toHaveLength(4) // rounds 3.5 to nearest = 4
  expect(empty).toHaveLength(1)
})

test('renders nothing for null complexity', () => {
  const { container } = render(<ComplexityStars complexity={null} />)
  expect(container).toBeEmptyDOMElement()
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && npm test 2>&1 | tail -8
```
Expected: `FAIL` — `ComplexityStars` doesn't exist.

- [ ] **Step 3: Create `ComplexityStars.tsx`**

```tsx
interface Props {
  complexity: number | null
}

export function ComplexityStars({ complexity }: Props) {
  if (complexity == null) return null
  const filled = Math.round(complexity)
  return (
    <span className="flex gap-0.5" title={`Complexity: ${complexity.toFixed(1)} / 5`}>
      {Array.from({ length: 5 }, (_, i) =>
        i < filled
          ? <span key={i} data-testid="star-filled" className="text-amber-400">★</span>
          : <span key={i} data-testid="star-empty" className="text-gray-600">★</span>
      )}
    </span>
  )
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd frontend && npm test 2>&1 | tail -5
```
Expected: `✓ ComplexityStars`

- [ ] **Step 5: Create `GameCard.tsx`**

```tsx
import { Link } from 'react-router-dom'
import type { GameSummary } from '../types/game'
import { ComplexityStars } from './ComplexityStars'

interface Props {
  game: GameSummary
}

export function GameCard({ game }: Props) {
  return (
    <Link
      to={`/games/${game.id}`}
      className="flex gap-3 p-3 rounded-lg bg-white hover:bg-gray-50 border border-gray-200 transition-colors"
    >
      {game.thumbnailUrl ? (
        <img
          src={game.thumbnailUrl}
          alt={game.name}
          className="w-14 h-14 object-cover rounded flex-shrink-0"
        />
      ) : (
        <div className="w-14 h-14 bg-gray-200 rounded flex-shrink-0" />
      )}
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold text-gray-900 truncate">{game.name}</h3>
          {game.bggRank != null && (
            <span className="text-xs text-gray-500 flex-shrink-0">#{game.bggRank}</span>
          )}
        </div>
        <div className="flex items-center gap-3 mt-1 text-sm text-gray-600">
          {game.avgRating != null && (
            <span className="text-green-600 font-medium">⭐ {game.avgRating.toFixed(1)}</span>
          )}
          <ComplexityStars complexity={game.complexity} />
          {game.minPlayers != null && game.maxPlayers != null && (
            <span>{game.minPlayers}–{game.maxPlayers}p</span>
          )}
        </div>
      </div>
    </Link>
  )
}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/
git commit -m "feat: add ComplexityStars and GameCard components"
```

---

### Task 13: FilterSidebar component

**Files:**
- Create: `frontend/src/components/FilterSidebar.tsx`

- [ ] **Step 1: Create `FilterSidebar.tsx`**

```tsx
import type { GameFilters } from '../types/game'

interface Props {
  filters: GameFilters
  onChange: (filters: GameFilters) => void
  isOpen: boolean
  onClose: () => void
}

export function FilterSidebar({ filters, onChange, isOpen, onClose }: Props) {
  const update = (patch: Partial<GameFilters>) => onChange({ ...filters, ...patch, page: 0 })

  const content = (
    <div className="flex flex-col gap-4 p-4">
      <h2 className="font-bold text-gray-800 text-sm uppercase tracking-wide">Filters</h2>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Name</label>
        <input
          type="text"
          value={filters.name ?? ''}
          onChange={e => update({ name: e.target.value || undefined })}
          placeholder="Search games…"
          className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Players</label>
        <div className="flex gap-2 items-center">
          <input type="number" min={1} max={10}
            value={filters.minPlayers ?? ''}
            onChange={e => update({ minPlayers: e.target.value ? +e.target.value : undefined })}
            placeholder="Min"
            className="w-16 border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <span className="text-gray-400">–</span>
          <input type="number" min={1} max={10}
            value={filters.maxPlayers ?? ''}
            onChange={e => update({ maxPlayers: e.target.value ? +e.target.value : undefined })}
            placeholder="Max"
            className="w-16 border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
        </div>
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">
          Complexity ({filters.minComplexity ?? 1}–{filters.maxComplexity ?? 5})
        </label>
        <div className="flex gap-2 items-center">
          <input type="range" min={1} max={5} step={0.5}
            value={filters.minComplexity ?? 1}
            onChange={e => update({ minComplexity: +e.target.value })}
            className="flex-1"
          />
          <input type="range" min={1} max={5} step={0.5}
            value={filters.maxComplexity ?? 5}
            onChange={e => update({ maxComplexity: +e.target.value })}
            className="flex-1"
          />
        </div>
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Max Rank</label>
        <select
          value={filters.maxRank ?? ''}
          onChange={e => update({ maxRank: e.target.value ? +e.target.value : undefined })}
          className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          <option value="">All ranks</option>
          <option value="100">Top 100</option>
          <option value="500">Top 500</option>
          <option value="1000">Top 1000</option>
          <option value="5000">Top 5000</option>
        </select>
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Sort by</label>
        <select
          value={`${filters.sort ?? 'rank'}-${filters.sortDir ?? 'asc'}`}
          onChange={e => {
            const [sort, sortDir] = e.target.value.split('-') as [GameFilters['sort'], GameFilters['sortDir']]
            update({ sort, sortDir })
          }}
          className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          <option value="rank-asc">Rank (best first)</option>
          <option value="rating-desc">Score (highest first)</option>
          <option value="complexity-asc">Complexity (lightest first)</option>
          <option value="complexity-desc">Complexity (heaviest first)</option>
          <option value="name-asc">Name (A–Z)</option>
        </select>
      </div>

      <button
        onClick={() => onChange({ page: 0, size: 20 })}
        className="text-sm text-blue-600 hover:underline text-left"
      >
        Reset filters
      </button>
    </div>
  )

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden md:block w-56 flex-shrink-0 border-r border-gray-200 bg-gray-50 min-h-screen">
        {content}
      </aside>

      {/* Mobile drawer */}
      {isOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={onClose} />
          <div className="relative w-72 bg-white shadow-xl overflow-y-auto">
            <button
              onClick={onClose}
              className="absolute top-3 right-3 text-gray-500 hover:text-gray-900 text-xl"
            >✕</button>
            {content}
          </div>
        </div>
      )}
    </>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/FilterSidebar.tsx
git commit -m "feat: add FilterSidebar with desktop and mobile drawer"
```

---

### Task 14: GameList, Pagination, and useGames hook

**Files:**
- Create: `frontend/src/hooks/useGames.ts`
- Create: `frontend/src/components/GameList.tsx`
- Create: `frontend/src/components/Pagination.tsx`

- [ ] **Step 1: Create `frontend/src/hooks/useGames.ts`**

```ts
import { useQuery } from '@tanstack/react-query'
import { fetchGames } from '../api/games'
import type { GameFilters } from '../types/game'

export function useGames(filters: GameFilters) {
  return useQuery({
    queryKey: ['games', filters],
    queryFn: () => fetchGames(filters),
    placeholderData: prev => prev,
  })
}
```

- [ ] **Step 2: Create `frontend/src/components/Pagination.tsx`**

```tsx
interface Props {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: Props) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-center gap-2 py-6">
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        className="px-3 py-1.5 rounded border border-gray-300 text-sm disabled:opacity-40 hover:bg-gray-50"
      >← Prev</button>
      <span className="text-sm text-gray-600">
        Page {page + 1} of {totalPages}
      </span>
      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="px-3 py-1.5 rounded border border-gray-300 text-sm disabled:opacity-40 hover:bg-gray-50"
      >Next →</button>
    </div>
  )
}
```

- [ ] **Step 3: Create `frontend/src/components/GameList.tsx`**

```tsx
import type { GamesPage } from '../types/game'
import { GameCard } from './GameCard'
import { Pagination } from './Pagination'

interface Props {
  data: GamesPage | undefined
  isLoading: boolean
  isError: boolean
  page: number
  onPageChange: (page: number) => void
}

export function GameList({ data, isLoading, isError, page, onPageChange }: Props) {
  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 p-4">
        {Array.from({ length: 8 }, (_, i) => (
          <div key={i} className="h-20 bg-gray-100 rounded-lg animate-pulse" />
        ))}
      </div>
    )
  }

  if (isError) {
    return <p className="p-8 text-center text-red-500">Failed to load games. Please try again.</p>
  }

  if (!data || data.content.length === 0) {
    return <p className="p-8 text-center text-gray-500">No games match your filters.</p>
  }

  return (
    <div className="flex flex-col">
      <p className="px-4 pt-4 pb-2 text-xs text-gray-500">
        {data.totalElements} game{data.totalElements !== 1 ? 's' : ''} found
      </p>
      <div className="flex flex-col gap-2 px-4">
        {data.content.map(game => <GameCard key={game.id} game={game} />)}
      </div>
      <Pagination page={page} totalPages={data.totalPages} onPageChange={onPageChange} />
    </div>
  )
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/hooks/ frontend/src/components/GameList.tsx frontend/src/components/Pagination.tsx
git commit -m "feat: add GameList, Pagination, and useGames hook"
```

---

### Task 15: BrowsePage

**Files:**
- Create: `frontend/src/pages/BrowsePage.tsx`

- [ ] **Step 1: Create `BrowsePage.tsx`**

```tsx
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { GameFilters } from '../types/game'
import { FilterSidebar } from '../components/FilterSidebar'
import { GameList } from '../components/GameList'
import { useGames } from '../hooks/useGames'

function paramsToFilters(params: URLSearchParams): GameFilters {
  return {
    name: params.get('name') ?? undefined,
    minPlayers: params.has('minPlayers') ? +params.get('minPlayers')! : undefined,
    maxPlayers: params.has('maxPlayers') ? +params.get('maxPlayers')! : undefined,
    minComplexity: params.has('minComplexity') ? +params.get('minComplexity')! : undefined,
    maxComplexity: params.has('maxComplexity') ? +params.get('maxComplexity')! : undefined,
    maxRank: params.has('maxRank') ? +params.get('maxRank')! : undefined,
    sort: (params.get('sort') as GameFilters['sort']) ?? 'rank',
    sortDir: (params.get('sortDir') as GameFilters['sortDir']) ?? 'asc',
    page: params.has('page') ? +params.get('page')! : 0,
    size: 20,
  }
}

function filtersToParams(filters: GameFilters): Record<string, string> {
  const p: Record<string, string> = {}
  if (filters.name) p.name = filters.name
  if (filters.minPlayers != null) p.minPlayers = String(filters.minPlayers)
  if (filters.maxPlayers != null) p.maxPlayers = String(filters.maxPlayers)
  if (filters.minComplexity != null) p.minComplexity = String(filters.minComplexity)
  if (filters.maxComplexity != null) p.maxComplexity = String(filters.maxComplexity)
  if (filters.maxRank != null) p.maxRank = String(filters.maxRank)
  if (filters.sort && filters.sort !== 'rank') p.sort = filters.sort
  if (filters.sortDir && filters.sortDir !== 'asc') p.sortDir = filters.sortDir
  if (filters.page && filters.page > 0) p.page = String(filters.page)
  return p
}

export function BrowsePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const filters = paramsToFilters(searchParams)
  const { data, isLoading, isError } = useGames(filters)

  const handleFiltersChange = (newFilters: GameFilters) => {
    setSearchParams(filtersToParams(newFilters))
  }

  return (
    <div className="flex min-h-screen">
      <FilterSidebar
        filters={filters}
        onChange={handleFiltersChange}
        isOpen={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      />
      <main className="flex-1">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
          <h1 className="font-bold text-gray-900">BGG Browser</h1>
          <button
            onClick={() => setDrawerOpen(true)}
            className="md:hidden text-sm px-3 py-1.5 border border-gray-300 rounded"
          >Filters</button>
        </div>
        <GameList
          data={data}
          isLoading={isLoading}
          isError={isError}
          page={filters.page ?? 0}
          onPageChange={page => handleFiltersChange({ ...filters, page })}
        />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/BrowsePage.tsx
git commit -m "feat: add BrowsePage with URL-persisted filters"
```

---

### Task 16: GameDetailPage

**Files:**
- Create: `frontend/src/pages/GameDetailPage.tsx`

- [ ] **Step 1: Create `GameDetailPage.tsx`**

```tsx
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchGame } from '../api/games'
import { ComplexityStars } from '../components/ComplexityStars'

export function GameDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: game, isLoading, isError } = useQuery({
    queryKey: ['game', id],
    queryFn: () => fetchGame(Number(id)),
    enabled: id != null,
  })

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <div className="h-8 w-24 bg-gray-200 rounded animate-pulse mb-6" />
        <div className="h-64 bg-gray-100 rounded-lg animate-pulse" />
      </div>
    )
  }

  if (isError || !game) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <Link to="/" className="text-blue-600 hover:underline text-sm">← Back</Link>
        <p className="mt-8 text-center text-red-500">Game not found.</p>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline text-sm">
        ← Back to list
      </button>

      <div className="mt-6 flex gap-6">
        {game.thumbnailUrl ? (
          <img src={game.thumbnailUrl} alt={game.name}
            className="w-32 h-32 object-cover rounded-lg flex-shrink-0 shadow" />
        ) : (
          <div className="w-32 h-32 bg-gray-200 rounded-lg flex-shrink-0" />
        )}
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{game.name}</h1>
          {game.yearPublished && (
            <p className="text-gray-500 text-sm mt-1">{game.yearPublished}</p>
          )}
          {game.avgRating != null && (
            <p className="text-green-600 font-semibold mt-2">⭐ {game.avgRating.toFixed(1)}</p>
          )}
          {game.bggRank != null && (
            <p className="text-gray-500 text-sm">BGG Rank #{game.bggRank}</p>
          )}
        </div>
      </div>

      <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-3">
        {game.complexity != null && (
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500 mb-1">Complexity</p>
            <ComplexityStars complexity={game.complexity} />
            <p className="text-xs text-gray-400 mt-1">{game.complexity.toFixed(1)} / 5</p>
          </div>
        )}
        {game.minPlayers != null && (
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500 mb-1">Players</p>
            <p className="font-semibold">{game.minPlayers}–{game.maxPlayers}</p>
          </div>
        )}
        {game.playTimeMin != null && (
          <div className="bg-gray-50 rounded-lg p-3">
            <p className="text-xs text-gray-500 mb-1">Play Time</p>
            <p className="font-semibold">
              {game.playTimeMin === game.playTimeMax
                ? `${game.playTimeMin}m`
                : `${game.playTimeMin}–${game.playTimeMax}m`}
            </p>
          </div>
        )}
      </div>

      {game.description && (
        <div className="mt-6">
          <h2 className="font-semibold text-gray-800 mb-2">About</h2>
          <p className="text-gray-600 text-sm leading-relaxed whitespace-pre-line">
            {game.description.replace(/&#\d+;/g, c =>
              String.fromCharCode(parseInt(c.slice(2, -1)))
            ).replace(/&amp;/g, '&').replace(/&quot;/g, '"')}
          </p>
        </div>
      )}

      <div className="mt-6">
        <a
          href={`https://boardgamegeek.com/boardgame/${game.id}`}
          target="_blank"
          rel="noopener noreferrer"
          className="text-blue-600 hover:underline text-sm"
        >View on BoardGameGeek ↗</a>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/GameDetailPage.tsx
git commit -m "feat: add GameDetailPage"
```

---

### Task 17: App routing and entry point

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`

- [ ] **Step 1: Replace `frontend/src/App.tsx`**

```tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowsePage } from './pages/BrowsePage'
import { GameDetailPage } from './pages/GameDetailPage'

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 5 * 60 * 1000 } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<BrowsePage />} />
          <Route path="/games/:id" element={<GameDetailPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
```

- [ ] **Step 2: Replace `frontend/src/main.tsx`**

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
```

- [ ] **Step 3: Build to verify**

```bash
cd frontend && npm run build 2>&1 | tail -5
```
Expected: `✓ built in`

- [ ] **Step 4: Run against local backend (optional smoke test)**

```bash
cd frontend && npm run dev
```
Open http://localhost:5173 — game list should appear if backend is running.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/App.tsx frontend/src/main.tsx
git commit -m "feat: wire up React Router and QueryClient in App"
```

---

## Phase 3: CI/CD

---

### Task 18: GitHub repository setup

- [ ] **Step 1: Create GitHub repository**

Go to https://github.com/new. Create a public repo named `bgg-browser`. Do not initialize with README.

- [ ] **Step 2: Push local repo**

```bash
cd /path/to/bgg-browser
git remote add origin https://github.com/<your-username>/bgg-browser.git
git push -u origin master
```

- [ ] **Step 3: Rename default branch to `main`**

```bash
git branch -m master main
git push -u origin main
```

- [ ] **Step 4: Enable branch protection on `main`**

On GitHub: Settings → Branches → Add rule:
- Branch name pattern: `main`
- Check: **Require a pull request before merging**
- Check: **Require status checks to pass before merging**
- Add status checks: `backend-ci` and `frontend-ci` (these will appear after first CI run)
- Check: **Do not allow bypassing the above settings**

---

### Task 19: CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  pull_request:
    branches: [main]

jobs:
  backend-ci:
    name: Backend tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Run tests
        run: cd backend && mvn test -q

  frontend-ci:
    name: Frontend build + lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install dependencies
        run: cd frontend && npm ci
      - name: Run tests
        run: cd frontend && npm test
      - name: Build
        run: cd frontend && npm run build
```

- [ ] **Step 2: Create a test PR and verify CI runs**

```bash
git checkout -b test/ci-check
git commit --allow-empty -m "ci: trigger CI check"
git push origin test/ci-check
```

Open a PR on GitHub. Both `backend-ci` and `frontend-ci` jobs should appear and pass.

- [ ] **Step 3: Merge and delete the test branch**

Merge the PR on GitHub, then:
```bash
git checkout main && git pull && git branch -d test/ci-check
```

- [ ] **Step 4: Commit CI workflow**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add CI workflow for PRs"
git push
```

---

### Task 20: CD — Frontend (Vercel)

- [ ] **Step 1: Connect repo to Vercel**

1. Go to https://vercel.com → New Project → Import from GitHub → select `bgg-browser`
2. Set **Root Directory** to `frontend`
3. Framework preset: **Vite**
4. Add environment variable:
   - `VITE_API_BASE_URL` = (leave blank — uses Vite proxy in dev, relative `/api` in prod)
5. Click **Deploy**

Vercel will deploy on every push to `main` automatically via its GitHub integration — no GitHub Actions CD step needed for the frontend.

- [ ] **Step 2: Note your Vercel production URL**

It will be `https://bgg-browser-<hash>.vercel.app` or your custom domain. Save it — you'll need it for the backend CORS config.

- [ ] **Step 3: Set CORS in Railway (next task)**

This step depends on the Railway deployment in Task 21 — skip for now.

---

### Task 21: CD — Backend (Railway)

- [ ] **Step 1: Create Railway project**

1. Go to https://railway.app → New Project → Deploy from GitHub repo → select `bgg-browser`
2. Set **Root Directory** to `backend`
3. Railway will auto-detect the Dockerfile

- [ ] **Step 2: Add PostgreSQL service**

In your Railway project: New Service → Database → PostgreSQL. Railway automatically injects `DATABASE_URL` into your app.

- [ ] **Step 3: Add environment variables in Railway**

In your backend service settings → Variables:
```
CORS_ALLOWED_ORIGINS=https://bgg-browser-<your-hash>.vercel.app
BGG_SYNC_MAX_ID=100000
BGG_SYNC_MAX_RANK=5000
BGG_SYNC_DELAY_MS=600
```

- [ ] **Step 4: Add Railway deploy step to CD workflow**

Update `.github/workflows/ci.yml` — add a new workflow file `.github/workflows/cd.yml`:

```yaml
name: CD

on:
  push:
    branches: [main]
    paths:
      - 'backend/**'

jobs:
  deploy-backend:
    name: Deploy backend to Railway
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Install Railway CLI
        run: npm install -g @railway/cli
      - name: Deploy
        run: railway up --service bgg-browser-backend --detach
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}
```

- [ ] **Step 5: Add Railway token to GitHub secrets**

In Railway: Account Settings → Tokens → Create token. Copy the token.

In GitHub repo: Settings → Secrets → Actions → New secret:
- Name: `RAILWAY_TOKEN`
- Value: (paste Railway token)

- [ ] **Step 6: Push and verify deployment**

```bash
git add .github/workflows/cd.yml
git commit -m "ci: add CD workflow for Railway backend deployment"
git push
```

Check GitHub Actions and Railway dashboard — deploy should complete within ~5 minutes.

- [ ] **Step 7: Verify the full stack**

Open your Vercel URL in a browser. You should see the BGG Browser with an empty game list (sync is running in the background on Railway). After ~30–60 minutes, games will appear.

---

## Done

At this point you have:
- A fully functional BGG browser app at your Vercel URL
- Spring Boot backend on Railway with PostgreSQL and weekly BGG sync
- CI that blocks broken PRs from merging
- CD that auto-deploys backend on push to `main`, with Vercel handling frontend automatically
