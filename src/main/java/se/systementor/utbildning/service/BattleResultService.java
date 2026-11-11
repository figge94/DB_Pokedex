package se.systementor.utbildning.service;

import se.systementor.utbildning.model.*;
import se.systementor.utbildning.repository.BattleLogRepository;
import se.systementor.utbildning.repository.BattleStatsRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BattleResultService {

    private final BattleStats battleStats;
    private final BattleStatsRepository statsRepository;
    private final BattleLogRepository battleLogRepository;

    public BattleResultService(
            BattleStats battleStats,
            BattleStatsRepository statsRepository,
            BattleLogRepository battleLogRepository
    ) {
        this.battleStats = battleStats;
        this.statsRepository = statsRepository;
        this.battleLogRepository = battleLogRepository;
    }

    // Registrerar resultatet och sparar statistiken.
    public void saveBattleResult(
            Battle battle,
            Pokemon playerPokemon,
            Pokemon wildPokemon,
            List<BattleRound> rounds
    ) {
        registerBattleResult(battle, playerPokemon);
        statsRepository.save(battleStats);

        saveBattleLog(battle, playerPokemon, wildPokemon, rounds);
    }

    private void registerBattleResult(
            Battle battle,
            Pokemon playerPokemon
    ) {
        battleStats.registerPokemonUsage(playerPokemon.getId());

        if (battle.hasFled()) {
            battleStats.addFlee();
        } else if (battle.playerWon()) {
            battleStats.addWin(playerPokemon.getId());
        } else {
            battleStats.addLoss(playerPokemon.getId());
        }
    }

    private void saveBattleLog(
            Battle battle,
            Pokemon playerPokemon,
            Pokemon wildPokemon,
            List<BattleRound> rounds
    ) {
        Integer winnerId = battle.hasFled()
                ? null
                : battle.playerWon()
                ? playerPokemon.getId()
                : wildPokemon.getId();

        BattleLog log = new BattleLog(
                UUID.randomUUID().toString(),
                Instant.now(),
                List.of(
                        playerPokemon.getId(),
                        wildPokemon.getId()
                ),
                winnerId,
                List.copyOf(rounds)
        );

        battleLogRepository.save(log);
    }
}