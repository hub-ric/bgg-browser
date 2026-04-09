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
