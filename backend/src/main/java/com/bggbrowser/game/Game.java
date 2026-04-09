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
