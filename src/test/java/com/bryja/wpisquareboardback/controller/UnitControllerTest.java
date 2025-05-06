package com.bryja.wpisquareboardback.controller;

import com.bryja.wpisquareboardback.dto.*;
import com.bryja.wpisquareboardback.exception.*; // Import custom exceptions
import com.bryja.wpisquareboardback.mapper.DtoMapper;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.service.CommandService;
import com.bryja.wpisquareboardback.service.UnitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@WebMvcTest(UnitController.class)
class UnitControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean
    private UnitService unitService;
    @MockitoBean private CommandService commandService;
    @MockitoBean private DtoMapper dtoMapper;

    private final Long GAME_ID = 1L;
    private final Long UNIT_ID = 10L;

    @Test
    void listUnits_withPlayerFilter_returnsUnitList() throws Exception {
        UnitDTO unitDto = new UnitDTO();
        unitDto.setId(UNIT_ID);
        unitDto.setPlayerColor(PlayerColor.WHITE);
        unitDto.setPosition(new Position(1,1));

        when(unitService.getActiveUnitsForPlayer(GAME_ID, PlayerColor.WHITE)).thenReturn(Collections.emptyList());

        when(dtoMapper.toUnitDTOList(anyList())).thenReturn(Collections.singletonList(unitDto));

        mockMvc.perform(get("/api/games/{gameId}/units", GAME_ID)
                        .param("playerColor", "WHITE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id", is(UNIT_ID.intValue())))
                .andExpect(jsonPath("$[0].playerColor", is("WHITE")))
                .andExpect(jsonPath("$[0].position.x", is(1)))
                .andExpect(jsonPath("$[0].position.y", is(1)));
    }

    @Test
    void executeSpecificCommand_validMove_returnsOkWithUpdatedUnit() throws Exception {
        // Arrange
        CommandRequestDTO commandDto = new CommandRequestDTO();
        commandDto.setPlayerColor(PlayerColor.WHITE);
        commandDto.setCommandType(CommandType.MOVE);
        commandDto.setTargetX(1);
        commandDto.setTargetY(2);

        Unit updatedUnitEntity = mock(Unit.class);
        UnitDTO updatedUnitDto = new UnitDTO();
        updatedUnitDto.setId(UNIT_ID);
        updatedUnitDto.setPosition(new Position(1,2));
        when(commandService.executeCommand(GAME_ID, UNIT_ID, commandDto)).thenReturn(updatedUnitEntity);
        when(dtoMapper.toUnitDTO(updatedUnitEntity)).thenReturn(updatedUnitDto);

        mockMvc.perform(post("/api/games/{gameId}/units/{unitId}/command", GAME_ID, UNIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(UNIT_ID.intValue())))
                .andExpect(jsonPath("$.position.x", is(1)))
                .andExpect(jsonPath("$.position.y", is(2)));
    }

    @Test
    void executeSpecificCommand_whenCooldownException_returnsTooManyRequests() throws Exception {

        CommandRequestDTO commandDto = new CommandRequestDTO();
        commandDto.setPlayerColor(PlayerColor.WHITE);
        commandDto.setCommandType(CommandType.MOVE);
        commandDto.setTargetX(1);
        commandDto.setTargetY(2);

        when(commandService.executeCommand(anyLong(), anyLong(), any(CommandRequestDTO.class)))
                .thenThrow(new CooldownException("Unit 10 cannot move yet. Cooldown remaining: 3.0 seconds."));

        mockMvc.perform(post("/api/games/{gameId}/units/{unitId}/command", GAME_ID, UNIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandDto)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", is("Unit 10 cannot move yet. Cooldown remaining: 3.0 seconds.")));
    }

    @Test
    void executeSpecificCommand_whenOptimisticLock_returnsConflict() throws Exception {
        CommandRequestDTO commandDto = new CommandRequestDTO();
        commandDto.setPlayerColor(PlayerColor.WHITE);
        commandDto.setCommandType(CommandType.MOVE);
        commandDto.setTargetX(1);
        commandDto.setTargetY(2);
        when(commandService.executeCommand(anyLong(), anyLong(), any(CommandRequestDTO.class)))
                .thenThrow(new ConcurrencyConflictException("Action failed due to a conflict..."));
               //.thenThrow(new OptimisticLockingFailureException("Simulated..."));


        mockMvc.perform(post("/api/games/{gameId}/units/{unitId}/command", GAME_ID, UNIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Action failed due to a conflict...")));
    }


    @Test
    void executeSpecificCommand_withInvalidEnumInBody_returnsBadRequest() throws Exception {
        String badJsonPayload = """
              {
                  "playerColor": "RED",
                  "commandType": "MOVE",
                  "targetX": 1,
                  "targetY": 1
              }
              """;

        mockMvc.perform(post("/api/games/{gameId}/units/{unitId}/command", GAME_ID, UNIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid value 'RED' for field 'playerColor'. Must be one of: ['WHITE', 'BLACK']")));
    }


    // todo tests:
    // - executeRandomCommand (success, errors)
    // - listUnits without filter
    // - listUnits with filter but no units found
    // - Get unit by ID (success, not found)
    // - Different command types (shoot, random)
    // - Other exception scenarios (UnitNotFound, GameNotFound, ActionNotAllowed etc.) mapped to correct HTTP statuses.
}
