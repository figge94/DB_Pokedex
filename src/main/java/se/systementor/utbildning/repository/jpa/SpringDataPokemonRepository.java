package se.systementor.utbildning.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface SpringDataPokemonRepository
        extends JpaRepository<PokemonEntity, Integer> {

    @Query("""
        SELECT DISTINCT p FROM PokemonEntity p
        LEFT JOIN FETCH p.attacks
        ORDER BY p.id
        """)
    List<PokemonEntity> findAllWithAttacks();

    @Query("""
    SELECT DISTINCT p FROM PokemonEntity p
    LEFT JOIN FETCH p.attacks
    WHERE p.id = :id
    """)
    Optional<PokemonEntity> findByIdWithAttacks(@Param("id") Integer id);
}