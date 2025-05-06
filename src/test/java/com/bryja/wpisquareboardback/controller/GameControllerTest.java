package com.bryja.wpisquareboardback.controller;
import com.bryja.wpisquareboardback.dto.*;
import com.bryja.wpisquareboardback.exception.GameNotFoundException;
import com.bryja.wpisquareboardback.mapper.DtoMapper;
import com.bryja.wpisquareboardback.model.Game;
import com.bryja.wpisquareboardback.model.GameStatus;
import com.bryja.wpisquareboardback.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is; // For jsonPath checks

@WebMvcTest(GameController.class) // Test only this controller, mock dependencies
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private DtoMapper dtoMapper;

    @Test
    void startNewGame_withDefaultConfig_returnsCreatedGame() throws Exception {
        Game createdGame = new Game(10,10);
        createdGame.setId(1L);
        createdGame.setCreatedAt(Instant.now());
        createdGame.setStatus(GameStatus.ACTIVE);
        createdGame.setBoardWidth(10);
        GameDTO expectedDto = new GameDTO();
        expectedDto.setId(1L);
        expectedDto.setStatus(GameStatus.ACTIVE);
        expectedDto.setBoardWidth(10);

        when(gameService.createNewGame()).thenReturn(createdGame);
        when(dtoMapper.toGameDTO(createdGame)).thenReturn(expectedDto);

        ResultActions resultActions = mockMvc.perform(post("/api/games/new"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.boardWidth", is(createdGame.getBoardWidth())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(gameService).createNewGame();
        verify(dtoMapper).toGameDTO(createdGame);
    }

    @Test
    void getActiveGame_whenActiveGameExists_returnsGameDto() throws Exception {
        long activeGameId = 5L;
        Game activeGameEntity = mock(Game.class);
        when(activeGameEntity.getId()).thenReturn(activeGameId);

        GameDTO activeGameDto = new GameDTO();
        activeGameDto.setId(activeGameId);
        activeGameDto.setStatus(GameStatus.ACTIVE);

        when(gameService.findActiveGameOrFail()).thenReturn(activeGameEntity);
        when(dtoMapper.toGameDTO(activeGameEntity)).thenReturn(activeGameDto);

        ResultActions resultActions = mockMvc.perform(get("/api/games/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is((int)activeGameId)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void getActiveGame_whenNoActiveGame_returnsNotFound() throws Exception {
        String errorMessage = "No active game found.";
        when(gameService.findActiveGameOrFail()).thenThrow(new GameNotFoundException(errorMessage));

        mockMvc.perform(get("/api/games/active"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is(errorMessage)));

        verify(dtoMapper, never()).toGameDTO(any());
    }

    @Test
    void getGameById_whenGameExists_returnsGameDto() throws Exception {
        // Arrange
        long requestedGameId = 7L;
        Game foundGameEntity = mock(Game.class);
        when(foundGameEntity.getId()).thenReturn(requestedGameId);

        GameDTO foundGameDto = new GameDTO();
        foundGameDto.setId(requestedGameId);
        foundGameDto.setStatus(GameStatus.FINISHED);

        when(gameService.findGameByIdOrFail(requestedGameId)).thenReturn(foundGameEntity);
        when(dtoMapper.toGameDTO(foundGameEntity)).thenReturn(foundGameDto);

        mockMvc.perform(get("/api/games/{gameId}", requestedGameId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is((int)requestedGameId)))
                .andExpect(jsonPath("$.status", is("FINISHED")));
    }

    @Test
    void getGameById_whenGameDoesNotExist_returnsNotFound() throws Exception {
        long nonExistentGameId = 99L;
        String errorMessage = "Game not found with ID: " + nonExistentGameId;
        when(gameService.findGameByIdOrFail(nonExistentGameId))
                .thenThrow(new GameNotFoundException(errorMessage));

        mockMvc.perform(get("/api/games/{gameId}", nonExistentGameId))
                .andExpect(status().isNotFound()) // Expect 404
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is(errorMessage)));

        verify(dtoMapper, never()).toGameDTO(any());
    }

}
