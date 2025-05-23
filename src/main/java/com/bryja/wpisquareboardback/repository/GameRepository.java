package com.bryja.wpisquareboardback.repository;


import com.bryja.wpisquareboardback.model.Game;
import com.bryja.wpisquareboardback.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByStatus(GameStatus status);
}
