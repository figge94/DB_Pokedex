package se.systementor.utbildning.service;

import se.systementor.utbildning.model.*;
import se.systementor.utbildning.ui.BattlePresentation;

import java.util.ArrayList;
import java.util.List;

// Hanterar spelarens och CPU:ns handlingar under striden.
public class BattleTurnService {

    // Dependency Injection: statistik och CPU-spelare
    // skickas in via konstruktorn.
    private final BattleStats battleStats;
    private final CpuPlayer cpuPlayer;

    public BattleTurnService(BattleStats battleStats, CpuPlayer cpuPlayer) {
        this.battleStats = battleStats;
        this.cpuPlayer = cpuPlayer;
    }

    // Polymorfism gör att rätt execute-metod anropas
    // oavsett vilken BattleAction som skickas in.
    private ActionResult executeAction(
            BattleAction action,
            Pokemon attacker,
            Pokemon defender
    ) {
        return action.execute(attacker, defender);
    }

    public BattleRound playerTurn(
            Pokemon playerPokemon,
            Pokemon wildPokemon,
            BattleAction playerAction,
            int roundNumber
    ) {
        int hpBefore = wildPokemon.getCurrentHp();

        ActionResult result = executeAction(
                playerAction,
                playerPokemon,
                wildPokemon
        );

        BattlePresentation.showActionResult(
                playerAction,
                result,
                playerPokemon,
                wildPokemon
        );

        battleStats.registerAttackUsage(playerAction.label());

        int damage = Math.max(0, hpBefore - wildPokemon.getCurrentHp());

        return new BattleRound(
                roundNumber,
                playerPokemon.getId(),
                playerAction.label(),
                damage
        );
    }

    // Utför CPU:ns handling och returnerar information om attacken.
    public BattleRound wildTurn(
            Pokemon wildPokemon,
            Pokemon playerPokemon,
            int roundNumber
    ) {
        // CPU:n väljer en attack.
        List<BattleAction> actions =
                new ArrayList<>(wildPokemon.getAttacks());

        BattleAction selectedAction = cpuPlayer.chooseAction(actions);

        // Sparar spelarens HP innan attacken.
        int hpBefore = playerPokemon.getCurrentHp();

        // Utför attacken.
        ActionResult result = executeAction(
                selectedAction,
                wildPokemon,
                playerPokemon
        );

        BattlePresentation.showActionResult(
                selectedAction,
                result,
                wildPokemon,
                playerPokemon
        );

        // Beräknar faktisk HP-förlust.
        int damage = Math.max(0, hpBefore - playerPokemon.getCurrentHp());

        // Returnerar information som ska sparas i MongoDB.
        return new BattleRound(
                roundNumber,
                wildPokemon.getId(),
                selectedAction.label(),
                damage
        );
    }
}