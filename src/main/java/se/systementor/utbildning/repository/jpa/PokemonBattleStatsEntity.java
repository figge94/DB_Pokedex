package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "pokemon_battle_stats")
public class PokemonBattleStatsEntity {

    @Id
    @Column(name = "pokemon_id")
    Integer pokemonId;

    @Column(name = "wins", nullable = false)
    int wins;

    @Column(name = "losses", nullable = false)
    int losses;

    @Column(name = "usage_count", nullable = false)
    int usageCount;

    protected PokemonBattleStatsEntity() {
    }
}