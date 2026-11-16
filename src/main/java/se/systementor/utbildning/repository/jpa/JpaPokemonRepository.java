package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.EntityManager;
import se.systementor.utbildning.exception.PokemonLoadException;
import se.systementor.utbildning.exception.PokemonSaveException;
import se.systementor.utbildning.model.DamageAttack;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.model.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import se.systementor.utbildning.model.Attack;

import se.systementor.utbildning.repository.PokemonDao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaPokemonRepository implements PokemonDao {

    @PersistenceContext
    private EntityManager em;

    private final SpringDataPokemonRepository springDataRepository;

    public JpaPokemonRepository(SpringDataPokemonRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pokemon> findAll() {

        try {
            List<PokemonEntity> entities = springDataRepository.findAllWithAttacks();

            List<Pokemon> pokemons = new ArrayList<>();

            for (PokemonEntity entity : entities) {
                pokemons.add(toModel(entity));
            }

            return pokemons;

        } catch (RuntimeException e) {
            throw new PokemonLoadException(
                    "Kunde inte läsa Pokemon från databasen.", e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pokemon> findById(int id) {
        try {
            return springDataRepository.findByIdWithAttacks(id)
                    .map(this::toModel);

        } catch (RuntimeException e) {
            throw new PokemonLoadException(
                    "Kunde inte läsa Pokemon med ID " + id, e
            );
        }
    }

    private Pokemon toModel(PokemonEntity entity) {
        Pokemon pokemon = new Pokemon(
                entity.id,
                entity.name,
                entity.type,
                entity.maxHp,
                entity.speed
        );

        for (AttackEntity attack : entity.attacks) {
            pokemon.addAttack(new DamageAttack(
                    attack.name,
                    attack.baseDamage,
                    attack.accuracy,
                    attack.type
            ));
        }

        return pokemon;
    }

    @Override
    @Transactional
    public void delete(int id) {
        try {
            if (springDataRepository.existsById(id)) {
                springDataRepository.deleteById(id);
            }
        } catch (RuntimeException e) {
            throw new PokemonSaveException(
                    "Kunde inte ta bort Pokemon med ID " + id, e
            );
        }
    }

    @Override
    @Transactional
    public void save(Pokemon pokemon) {

        try {
            PokemonEntity entity = springDataRepository
                    .findById(pokemon.getId())
                    .orElse(null);

            if (entity == null) {
                throw new IllegalArgumentException(
                        "Pokemon med ID " + pokemon.getId() + " hittades inte."
                );
            }

            entity.name = pokemon.getName();
            entity.type = pokemon.getType();
            entity.maxHp = pokemon.getMaxHp();
            entity.speed = pokemon.getSpeed();

            List<AttackEntity> attacks = new ArrayList<>();

            for (Attack attack : pokemon.getAttacks()) {
                AttackEntity attackEntity = findOrCreateAttack(em, attack);

                if (attacks.contains(attackEntity)) {
                    throw new IllegalArgumentException(
                            "Samma attack får inte läggas till flera gånger."
                    );
                }

                attacks.add(attackEntity);
            }

            entity.attacks.clear();
            entity.attacks.addAll(attacks);

        } catch (RuntimeException e) {
            throw new PokemonSaveException(
                    "Kunde inte uppdatera Pokemon med ID "
                            + pokemon.getId(), e
            );
        }
    }

    private AttackEntity findOrCreateAttack(EntityManager em, Attack attack) {
        Type type = attack.getType();

        List<AttackEntity> matches = em.createQuery("""
            SELECT a FROM AttackEntity a
            WHERE a.name = :name
              AND a.type = :type
              AND a.baseDamage = :damage
              AND a.accuracy = :accuracy
            """, AttackEntity.class)
                .setParameter("name", attack.getName())
                .setParameter("type", type)
                .setParameter("damage", attack.getBaseDamage())
                .setParameter("accuracy", attack.getAccuracy())
                .getResultList();

        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        Long count = em.createQuery("""
            SELECT COUNT(a) FROM AttackEntity a
            WHERE a.name = :name
            """, Long.class)
                .setParameter("name", attack.getName())
                .getSingleResult();

        if (count > 0) {
            throw new IllegalArgumentException(
                    "Attacknamnet finns redan med andra egenskaper: "
                            + attack.getName()
            );
        }

        AttackEntity entity = new AttackEntity();
        entity.name = attack.getName();
        entity.type = attack.getType();
        entity.baseDamage = attack.getBaseDamage();
        entity.accuracy = attack.getAccuracy();

        em.persist(entity);
        return entity;
    }

    @Override
    @Transactional
    public Pokemon insert(Pokemon pokemon) {
        if (pokemon.getId() != 0) {
            throw new IllegalArgumentException(
                    "En ny Pokemon ska ha ID 0."
            );
        }

        try {
            PokemonEntity entity = new PokemonEntity();
            entity.name = pokemon.getName();
            entity.type = pokemon.getType();
            entity.maxHp = pokemon.getMaxHp();
            entity.speed = pokemon.getSpeed();

            for (Attack attack : pokemon.getAttacks()) {
                AttackEntity attackEntity = findOrCreateAttack(em, attack);

                if (entity.attacks.contains(attackEntity)) {
                    throw new IllegalArgumentException(
                            "Samma attack får inte läggas till flera gånger."
                    );
                }

                entity.attacks.add(attackEntity);
            }

            entity = springDataRepository.saveAndFlush(entity);

            Pokemon savedPokemon = new Pokemon(
                    entity.id,
                    pokemon.getName(),
                    pokemon.getType(),
                    pokemon.getMaxHp(),
                    pokemon.getSpeed()
            );

            for (Attack attack : pokemon.getAttacks()) {
                savedPokemon.addAttack(attack);
            }

            return savedPokemon;

        } catch (RuntimeException e) {
            throw new PokemonSaveException(
                    "Kunde inte skapa Pokemon i databasen.", e
            );
        }
    }
}