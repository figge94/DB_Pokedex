package se.systementor.utbildning.repository;

import se.systementor.utbildning.model.Pokemon;

import java.util.List;
import java.util.Optional;

public interface PokemonDao {
    Optional<Pokemon> findById(int id);

    List<Pokemon> findAll();

    Pokemon insert(Pokemon pokemon);

    void save(Pokemon pokemon);

    void delete(int id);
}