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
