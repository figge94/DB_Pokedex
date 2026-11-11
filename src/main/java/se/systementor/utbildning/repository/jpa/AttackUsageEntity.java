package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "attack_usage")
public class AttackUsageEntity {

    @Id
    @Column(name = "attack_name", length = 50)
    String attackName;

    @Column(name = "usage_count", nullable = false)
    int usageCount;

    protected AttackUsageEntity() {
    }
}