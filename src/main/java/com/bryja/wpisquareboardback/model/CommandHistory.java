package com.bryja.wpisquareboardback.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "command_history")
public class CommandHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    private PlayerColor executingPlayer;

    @Enumerated(EnumType.STRING)
    private CommandType commandType;


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="x", column=@Column(name="target_x")),
            @AttributeOverride(name="y", column=@Column(name="target_y"))
    })
    private Position targetPosition;

    @CreationTimestamp
    private Instant timestamp;

    private String resultDescription;

    public CommandHistory(Game game, Unit unit, PlayerColor executingPlayer, CommandType commandType, Position targetPosition, String resultDescription) {
        this.game = game;
        this.unit = unit;
        this.executingPlayer = executingPlayer;
        this.commandType = commandType;
        this.targetPosition = targetPosition;
        this.timestamp = Instant.now();
        this.resultDescription = resultDescription;
    }
}
