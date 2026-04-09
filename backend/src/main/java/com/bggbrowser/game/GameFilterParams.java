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
