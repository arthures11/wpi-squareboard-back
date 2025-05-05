package com.bryja.wpisquareboardback.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "unit_discriminator", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@Table(name = "units")
public abstract class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerColor playerColor;

//    @Enumerated(EnumType.STRING)
//    @Column(insertable=false, updatable=false)
//    private UnitType unitType;

    @Enumerated(EnumType.STRING)
// @Column(insertable=false, updatable=false)
    private UnitType unitType;

    @Embedded
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status = UnitStatus.ACTIVE;

    private int moveCount = 0;

    private Instant lastActionTimestamp;

    @Version
    private Long version;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("timestamp DESC")
    private List<CommandHistory> commandHistory = new ArrayList<>();

    public Unit(Game game, PlayerColor playerColor, Position position) {
        this.game = game;
        this.playerColor = playerColor;
        this.position = position;
        this.status = UnitStatus.ACTIVE;
        this.lastActionTimestamp = Instant.EPOCH;
    }

    public void addCommandHistory(CommandHistory history) {
        commandHistory.add(history);
        history.setUnit(this);
    }
}
