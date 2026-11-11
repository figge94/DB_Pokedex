package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "battle_stats")
public class BattleStatsEntity {

    @Id
    Integer id;

    @Column(name = "wins", nullable = false)
    int wins;

    @Column(name = "losses", nullable = false)
    int losses;

    @Column(name = "flees", nullable = false)
    int flees;

    protected BattleStatsEntity() {
    }
}