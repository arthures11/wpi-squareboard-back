package com.bryja.wpisquareboardback.controller;

import com.bryja.wpisquareboardback.dto.*;
import com.bryja.wpisquareboardback.mapper.*;
import com.bryja.wpisquareboardback.model.*;
import com.bryja.wpisquareboardback.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final DtoMapper dtoMapper;

    @PostMapping("/new")
    public ResponseEntity<GameDTO> startNewGame() {
        Game newGame = gameService.createNewGame();
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toGameDTO(newGame));
    }


    @GetMapping("/active")
    public ResponseEntity<GameDTO> getActiveGame() {
        Game activeGame = gameService.findActiveGameOrFail();
        return ResponseEntity.ok(dtoMapper.toGameDTO(activeGame));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable Long gameId) {
        Game game = gameService.findGameByIdOrFail(gameId);
        return ResponseEntity.ok(dtoMapper.toGameDTO(game));
    }

    // @GetMapping
    // public ResponseEntity<List<GameDTO>> getAllGames() { ... }

}
