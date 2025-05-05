package com.bryja.wpisquareboardback.repository;

import com.bryja.wpisquareboardback.model.CommandHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandHistoryRepository extends JpaRepository<CommandHistory, Long> {
    List<CommandHistory> findByGameIdOrderByTimestampDesc(Long gameId);
}
