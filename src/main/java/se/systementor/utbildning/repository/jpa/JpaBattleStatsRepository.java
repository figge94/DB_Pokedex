package se.systementor.utbildning.repository.jpa;

import jakarta.persistence.EntityManager;
import se.systementor.utbildning.config.DbConfig;
import se.systementor.utbildning.model.BattleStats;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class JpaBattleStatsRepository {

    public BattleStats load() {
        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

            BattleStatsEntity totals =
                    em.find(BattleStatsEntity.class, 1);

            Map<Integer, Integer> wins = new HashMap<>();
            Map<Integer, Integer> losses = new HashMap<>();
            Map<Integer, Integer> usage = new HashMap<>();
            Map<String, Integer> attacks = new HashMap<>();

            for (PokemonBattleStatsEntity row : em.createQuery(
                    "SELECT p FROM PokemonBattleStatsEntity p",
                    PokemonBattleStatsEntity.class
            ).getResultList()) {
                wins.put(row.pokemonId, row.wins);
                losses.put(row.pokemonId, row.losses);
                usage.put(row.pokemonId, row.usageCount);
            }

            for (AttackUsageEntity row : em.createQuery(
                    "SELECT a FROM AttackUsageEntity a",
                    AttackUsageEntity.class
            ).getResultList()) {
                attacks.put(row.attackName, row.usageCount);
            }

            BattleStats stats = new BattleStats(
                    totals == null ? 0 : totals.wins,
                    totals == null ? 0 : totals.losses,
                    totals == null ? 0 : totals.flees,
                    wins,
                    losses,
                    usage,
                    attacks
            );

            em.getTransaction().commit();
            return stats;

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new IllegalStateException(
                    "Kunde inte läsa stridsstatistiken.", e
            );
        } finally {
            em.close();
        }
    }

    public void save(BattleStats stats) {
        EntityManager em = DbConfig.getEntityManager();

        try {
            em.getTransaction().begin();

            BattleStatsEntity totals =
                    em.find(BattleStatsEntity.class, 1);

            if (totals == null) {
                totals = new BattleStatsEntity();
                totals.id = 1;
                em.persist(totals);
            }

            totals.wins = stats.getWins();
            totals.losses = stats.getLosses();
            totals.flees = stats.getFlees();

            em.createQuery("DELETE FROM PokemonBattleStatsEntity")
                    .executeUpdate();

            em.createQuery("DELETE FROM AttackUsageEntity")
                    .executeUpdate();

            Map<Integer, Integer> wins = stats.getWinsByPokemon();
            Map<Integer, Integer> losses = stats.getLossesByPokemon();
            Map<Integer, Integer> usage = stats.getPokemonUsage();

            Set<Integer> pokemonIds = new HashSet<>();
            pokemonIds.addAll(wins.keySet());
            pokemonIds.addAll(losses.keySet());
            pokemonIds.addAll(usage.keySet());

            for (int pokemonId : pokemonIds) {
                PokemonBattleStatsEntity row =
                        new PokemonBattleStatsEntity();

                row.pokemonId = pokemonId;
                row.wins = wins.getOrDefault(pokemonId, 0);
                row.losses = losses.getOrDefault(pokemonId, 0);
                row.usageCount = usage.getOrDefault(pokemonId, 0);

                em.persist(row);
            }

            for (Map.Entry<String, Integer> entry
                    : stats.getAttackUsage().entrySet()) {
                AttackUsageEntity row = new AttackUsageEntity();
                row.attackName = entry.getKey();
                row.usageCount = entry.getValue();

                em.persist(row);
            }

            em.getTransaction().commit();

        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            throw new IllegalStateException(
                    "Kunde inte spara stridsstatistiken.", e
            );
        } finally {
            em.close();
        }
    }
}