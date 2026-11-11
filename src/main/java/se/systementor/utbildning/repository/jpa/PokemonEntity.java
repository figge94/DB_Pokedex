package se.systementor.utbildning.repository.jpa;

import se.systementor.utbildning.model.Type;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pokemon")
public class PokemonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pokemon_id")
    Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    Type type;

    @Column(name = "max_hp", nullable = false)
    int maxHp;

    @Column(name = "speed", nullable = false)
    int speed;

    @ManyToMany
    @JoinTable(
            name = "pokemon_attacks",
            joinColumns = @JoinColumn(name = "pokemon_id"),
            inverseJoinColumns = @JoinColumn(name = "attack_id")
    )
    @OrderBy("id ASC")
    List<AttackEntity> attacks = new ArrayList<>();

    protected PokemonEntity() {
    }
}