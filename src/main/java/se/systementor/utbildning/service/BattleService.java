package se.systementor.utbildning.service;

import se.systementor.utbildning.model.*;
import se.systementor.utbildning.repository.BattleLogRepository;
import se.systementor.utbildning.repository.BattleStatsRepository;
import se.systementor.utbildning.ui.BattleMenu;
import se.systementor.utbildning.ui.BattlePresentation;
import se.systementor.utbildning.ui.Messages;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.ArrayList;

// Samordnar striden mellan spelaren och CPU:n.
// Använder andra klasser för handlingar, presentation och statistik.
public class BattleService {

    private final BattleStatsRepository statsRepository;
    private final BattleStats battleStats;
    private final BattleTurnService battleTurnService;
    private final BattleLogRepository battleLogRepository;

    // Dependency Injection: statistik och CPU-spelare
    // skickas in via konstruktorn i stället för att skapas här.
    public BattleService(
            BattleStats battleStats,
            CpuPlayer cpuPlayer,
            BattleStatsRepository statsRepository,
            BattleLogRepository battleLogRepository
    ) {
        this.battleStats = battleStats;
        this.battleTurnService = new BattleTurnService(battleStats, cpuPlayer);
        this.statsRepository = statsRepository;
        this.battleLogRepository = battleLogRepository;
    }

    // static final gör att samma slumpgenerator återanvänds.
    private static final Random RANDOM = new Random();

    // Väljer en slumpmässig motståndare som inte är spelarens Pokemon.
    public static Pokemon chooseRandomWildPokemon(
            List<Pokemon> pokemons,
            Pokemon playerPokemon
    ) {
        if (pokemons == null || playerPokemon == null) {
            throw new IllegalArgumentException(
                    "Pokemon-listan och spelarens Pokemon får inte vara null."
            );
        }

        if (pokemons.size() < 2) {
            throw new IllegalArgumentException(
                    "Det måste finnas minst två Pokemon för att starta en strid."
            );
        }

        // Stream och anyMatch kontrollerar om minst en annan Pokemon finns.
        // equals jämför Pokemon utifrån ID.
        boolean hasOpponent = pokemons.stream()
                .anyMatch(pokemon ->
                        pokemon != null && !pokemon.equals(playerPokemon)
                );

        if (!hasOpponent) {
            throw new IllegalArgumentException(
                    "Det finns ingen annan Pokemon att möta."
            );
        }

        Pokemon wildPokemon;

        // Slumpar tills en Pokemon med ett annat ID hittas.
        do {
            int index = RANDOM.nextInt(pokemons.size());
            wildPokemon = pokemons.get(index);
        } while (wildPokemon == null || wildPokemon.equals(playerPokemon));

        return wildPokemon;
    }

    // Hanterar en strid tills någon besegras eller spelaren flyr.
    public void startBattle(
            List<Pokemon> pokemons,
            Pokemon playerPokemon,
            Scanner scanner
    ) {
        // Deklareras utanför try så att HP kan återställas i finally.
        Pokemon wildPokemon = null;

        try {
            wildPokemon = chooseRandomWildPokemon(
                    pokemons, playerPokemon
            );

            // Båda deltagarna måste ha minst en attack.
            if (playerPokemon.getAttacks().isEmpty()
                    || wildPokemon.getAttacks().isEmpty()) {
                throw new IllegalArgumentException(
                        "Båda Pokemon måste ha minst en attack för att strida."
                );
            }

            // Battle håller reda på deltagare, turordning och stridens tillstånd.
            Battle battle = new Battle(
                    playerPokemon,
                    wildPokemon
            );

            List<BattleRound> rounds = new ArrayList<>();
            int roundNumber = 0;

            BattleMenu.showBattleStart(
                    battle.getPlayerPokemon(),
                    battle.getWildPokemon()
            );

            while (!battle.isOver()) {
                roundNumber++;

                // Spelarens val kan vara en attack eller flykt.
                BattleAction playerAction = BattleMenu.chooseAction(
                        playerPokemon,
                        scanner
                );

                // endsBattle gör att olika handlingar själva kan ange
                // om de avslutar striden.
                if (playerAction.endsBattle()) {

                    ActionResult result = playerAction.execute(
                            playerPokemon,
                            wildPokemon
                    );

                    // instanceof kontrollerar om resultatet är ett FleeResult.
                    if (result instanceof FleeResult) {
                        battle.flee();
                        break;
                    }

                    // Skyddar mot avslutande handlingar med oväntade resultat.
                    throw new IllegalStateException(
                            "Handlingen avslutar striden utan ett giltigt resultat."
                    );
                }

                // Snabbheten avgör turordningen, slumpen avgör vid lika snabbhet.
                Pokemon[] turnOrder = battle.getTurnOrder();

                BattlePresentation.showFirstAttacker(turnOrder[0]);

                for (Pokemon attacker : turnOrder) {

                    // Förhindrar att någon attackerar efter att striden avslutats.
                    if (battle.isOver()) {
                        break;
                    }

                    // == jämför objektreferenser för att avgöra vems tur det är.
                    if (attacker == playerPokemon) {

                        BattlePresentation.showTurn(playerPokemon, true);
                        BattlePresentation.pause();

                        // Låter BattleTurnService utföra spelarens tur.
                        BattleRound round = battleTurnService.playerTurn(
                                playerPokemon,
                                wildPokemon,
                                playerAction,
                                roundNumber
                        );

                        rounds.add(round);

                    } else {

                        BattlePresentation.showTurn(wildPokemon, false);
                        BattlePresentation.pause();

                        // Låter BattleTurnService utföra CPU:ns tur.
                        BattleRound round = battleTurnService.wildTurn(
                                wildPokemon,
                                playerPokemon,
                                roundNumber
                        );

                        rounds.add(round);
                    }

                    BattleMenu.showBattleHp(playerPokemon, wildPokemon);
                    BattlePresentation.pause();
                }

                // Visar den besegrade Pokemonen om spelaren inte har flytt.
                if (battle.isOver() && !battle.hasFled()) {

                    // Ternary väljer den Pokemon som har noll HP.
                    Pokemon knockedOut = playerPokemon.isKnockedOut()
                            ? playerPokemon
                            : wildPokemon;

                    BattleMenu.showKnockedOut(knockedOut);
                }
            }

            // Visar olika slutresultat beroende på om spelaren flydde.
            if (battle.hasFled()) {
                BattlePresentation.showFlee();
            } else {
                BattleMenu.showBattleResult(
                        battle.getPlayerPokemon(),
                        battle.getWildPokemon()
                );
            }

            // Registrerar stridens resultat och sparar statistiken.
            System.out.println("DEBUG: Sparar battle_stats...");
            saveBattleStatistics(battle, playerPokemon);

            System.out.println("DEBUG: Sparar battleLogs...");
            saveBattleLog(battle, playerPokemon, wildPokemon, rounds);

            System.out.println("DEBUG: Båda sparningarna klara!");

        } catch (IllegalArgumentException e) {
            Messages.error(
                    "Ett fel uppstod under striden: " + e.getMessage()
            );

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

    // Ger spelmenyn tillgång till den aktuella statistiken.
    public BattleStats getBattleStats() {
        return battleStats;
    }

    // Registrerar vinst, förlust eller flykt.
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

    // Registrerar stridsresultatet och sparar statistiken till JSON.
    private void saveBattleStatistics(
            Battle battle,
            Pokemon playerPokemon
    ) {
        registerBattleResult(battle, playerPokemon);

        // Filhanteringen sköts av BattleStatsFileService.
        statsRepository.save(battleStats);
    }
}