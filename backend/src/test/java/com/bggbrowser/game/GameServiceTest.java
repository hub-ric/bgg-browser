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
