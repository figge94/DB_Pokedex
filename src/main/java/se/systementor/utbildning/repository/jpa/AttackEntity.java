package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.*;

import se.systementor.utbildning.model.Type;

@Entity
@Table(name = "attacks")
public class AttackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attack_id")
    Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    Type type;

    @Column(name = "base_damage", nullable = false)
    int baseDamage;

    @Column(name = "accuracy", nullable = false)
    int accuracy;

    protected AttackEntity() {
    }
}