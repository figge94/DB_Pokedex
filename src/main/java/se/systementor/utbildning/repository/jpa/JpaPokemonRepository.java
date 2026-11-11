package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.EntityManager;
import se.systementor.utbildning.config.DbConfig;
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

public class JpaPokemonRepository implements PokemonDao {

    public List<Pokemon> findAll() {
        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

            List<PokemonEntity> entities = em.createQuery("""
                            SELECT DISTINCT p FROM PokemonEntity p
                            LEFT JOIN FETCH p.attacks a
                            ORDER BY p.id
                            """, PokemonEntity.class)
                    .getResultList();

            List<Pokemon> pokemons = new ArrayList<>();

            for (PokemonEntity entity : entities) {
                pokemons.add(toModel(entity));
            }

            em.getTransaction().commit();
            return pokemons;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new PokemonLoadException(
                    "Kunde inte läsa Pokemon från databasen.", e
            );
        } finally {
            em.close();
        }
    }

    public Optional<Pokemon> findById(int id) {
        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

            List<PokemonEntity> entities = em.createQuery("""
                            SELECT DISTINCT p FROM PokemonEntity p
                            LEFT JOIN FETCH p.attacks a
                            WHERE p.id = :id
                            """, PokemonEntity.class)
                    .setParameter("id", id)
                    .getResultList();

            PokemonEntity entity = entities.isEmpty()
                    ? null
                    : entities.get(0);

            Optional<Pokemon> result = entity == null
                    ? Optional.empty()
                    : Optional.of(toModel(entity));

            em.getTransaction().commit();
            return result;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new PokemonLoadException(
                    "Kunde inte läsa Pokemon med ID " + id, e
            );
        } finally {
            em.close();
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

    public void delete(int id) {
        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

            PokemonEntity entity = em.find(PokemonEntity.class, id);

            if (entity != null) {
                em.remove(entity);
            }

            em.getTransaction().commit();

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new PokemonSaveException(
                    "Kunde inte ta bort Pokemon med ID " + id, e
            );
        } finally {
            em.close();
        }
    }

    public void save(Pokemon pokemon) {
        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

            PokemonEntity entity =
                    em.find(PokemonEntity.class, pokemon.getId());

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

            em.getTransaction().commit();

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new PokemonSaveException(
                    "Kunde inte uppdatera Pokemon med ID " + pokemon.getId(), e
            );
        } finally {
            em.close();
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

    public Pokemon insert(Pokemon pokemon) {
        if (pokemon.getId() != 0) {
            throw new IllegalArgumentException(
                    "En ny Pokemon ska ha ID 0."
            );
        }

        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

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

            em.persist(entity);
            em.flush();

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

            em.getTransaction().commit();
            return savedPokemon;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new PokemonSaveException(
                    "Kunde inte skapa Pokemon i databasen.", e
            );
        } finally {
            em.close();
        }
    }
}