package se.systementor.utbildning.service;

import se.systementor.utbildning.model.ActionResult;
import se.systementor.utbildning.model.Battle;
import se.systementor.utbildning.model.BattleAction;
import se.systementor.utbildning.model.BattleStats;
import se.systementor.utbildning.model.BattleRound;
import se.systementor.utbildning.model.CpuPlayer;
import se.systementor.utbildning.model.FleeResult;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.repository.BattleLogRepository;
import se.systementor.utbildning.repository.BattleStatsRepository;
import se.systementor.utbildning.ui.BattleMenu;
import se.systementor.utbildning.ui.BattlePresentation;
import se.systementor.utbildning.ui.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Samordnar striden mellan spelaren och CPU:n.
// Använder andra klasser för handlingar, presentation och statistik.
public class BattleService {

    private final BattleStats battleStats;
    private final BattleTurnService battleTurnService;
    private final BattleResultService battleResultService;

    // Dependency Injection: statistik och CPU-spelare
    // skickas in via konstruktorn i stället för att skapas här.
    public BattleService(
            BattleStats battleStats,
            CpuPlayer cpuPlayer,
            BattleStatsRepository statsRepository,
            BattleLogRepository battleLogRepository
    ) {
        this.battleStats = battleStats;

        this.battleTurnService =
                new BattleTurnService(battleStats, cpuPlayer);

        this.battleResultService = new BattleResultService(
                battleStats,
                statsRepository,
                battleLogRepository
        );
    }

    // Hanterar en strid tills någon besegras eller spelaren flyr.
    public void startBattle(
            List<Pokemon> pokemons,
            Pokemon playerPokemon,
            Scanner scanner
    ) {
        Pokemon wildPokemon = null;

        try {
            wildPokemon = BattleOpponentService.chooseRandomWildPokemon(
                    pokemons, playerPokemon
            );

            validateAttacks(playerPokemon, wildPokemon);

            Battle battle = new Battle(playerPokemon, wildPokemon);

            BattleMenu.showBattleStart(playerPokemon, wildPokemon);

            List<BattleRound> rounds = playBattle(
                    battle, playerPokemon, wildPokemon, scanner
            );

            showBattleResult(battle, playerPokemon, wildPokemon);

            battleResultService.saveBattleResult(
                    battle, playerPokemon, wildPokemon, rounds
            );

        } catch (IllegalArgumentException e) {
            Messages.error("Ett fel uppstod under striden: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("Fel under striden eller sparningen:");
            e.printStackTrace();

        } finally {
            if (playerPokemon != null) {
                playerPokemon.restoreHealth();
            }

            if (wildPokemon != null) {
                wildPokemon.restoreHealth();
            }
        }
    }

    private List<BattleRound> playBattle(
            Battle battle,
            Pokemon player,
            Pokemon wild,
            Scanner scanner
    ) {
        List<BattleRound> rounds = new ArrayList<>();
        int roundNumber = 0;

        while (!battle.isOver()) {
            roundNumber++;

            BattleAction action = BattleMenu.chooseAction(player, scanner);

            if (action.endsBattle()) {
                handleFlee(battle, action, player, wild);
                break;
            }

            executeRound(
                    battle, player, wild,
                    action, roundNumber, rounds
            );
        }

        return rounds;
    }

    private void executeRound(
            Battle battle,
            Pokemon player,
            Pokemon wild,
            BattleAction playerAction,
            int roundNumber,
            List<BattleRound> rounds
    ) {
        Pokemon[] turnOrder = battle.getTurnOrder();

        BattlePresentation.showFirstAttacker(turnOrder[0]);

        for (Pokemon attacker : turnOrder) {
            if (battle.isOver()) {
                break;
            }

            BattleRound round;

            if (attacker == player) {
                BattlePresentation.showTurn(player, true);
                BattlePresentation.pause();

                round = battleTurnService.playerTurn(
                        player, wild, playerAction, roundNumber
                );
            } else {
                BattlePresentation.showTurn(wild, false);
                BattlePresentation.pause();

                round = battleTurnService.wildTurn(
                        wild, player, roundNumber
                );
            }

            rounds.add(round);

            BattleMenu.showBattleHp(player, wild);
            BattlePresentation.pause();
        }

        if (battle.isOver() && !battle.hasFled()) {
            Pokemon knockedOut = player.isKnockedOut()
                    ? player
                    : wild;

            BattleMenu.showKnockedOut(knockedOut);
        }
    }

    private void handleFlee(
            Battle battle,
            BattleAction action,
            Pokemon player,
            Pokemon wild
    ) {
        ActionResult result = action.execute(player, wild);

        if (result instanceof FleeResult) {
            battle.flee();
            return;
        }

        throw new IllegalStateException(
                "Handlingen avslutar striden utan ett giltigt resultat."
        );
    }

    private void validateAttacks(Pokemon player, Pokemon wild) {
        if (player.getAttacks().isEmpty()
                || wild.getAttacks().isEmpty()) {
            throw new IllegalArgumentException(
                    "Båda Pokemon måste ha minst en attack för att strida."
            );
        }
    }

    private void showBattleResult(
            Battle battle,
            Pokemon player,
            Pokemon wild
    ) {
        if (battle.hasFled()) {
            BattlePresentation.showFlee();
        } else {
            BattleMenu.showBattleResult(player, wild);
        }
    }

    // Ger spelmenyn tillgång till den aktuella statistiken.
    public BattleStats getBattleStats() {
        return battleStats;
    }


}