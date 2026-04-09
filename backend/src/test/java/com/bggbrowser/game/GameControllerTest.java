package com.bggbrowser.game;

import com.bggbrowser.sync.BggSyncService;
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
    @MockBean BggSyncService syncService;

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
