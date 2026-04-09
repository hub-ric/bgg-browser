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
