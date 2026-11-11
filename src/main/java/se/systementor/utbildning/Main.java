
package se.systementor.utbildning;

import se.systementor.utbildning.repository.BattleLogRepository;
import se.systementor.utbildning.repository.PokemonFileService;
import se.systementor.utbildning.model.BattleStats;
import se.systementor.utbildning.model.CpuPlayer;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.repository.mongo.MongoBattleLogRepository;
import se.systementor.utbildning.service.BattleService;
import se.systementor.utbildning.service.PokemonService;
import se.systementor.utbildning.strategy.RandomStrategy;
import se.systementor.utbildning.ui.PokemonMenu;
import se.systementor.utbildning.ui.PlayMenuHandler;
import se.systementor.utbildning.ui.Messages;
import se.systementor.utbildning.ui.MainMenuHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import io.github.cdimascio.dotenv.Dotenv;

import se.systementor.utbildning.repository.PokemonDao;
import se.systementor.utbildning.repository.jpa.JpaPokemonRepository;
import se.systementor.utbildning.exception.PokemonLoadException;

import se.systementor.utbildning.config.DbConfig;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import se.systementor.utbildning.repository.BattleStatsRepository;
import se.systementor.utbildning.repository.mongo.MongoBattleStatsRepository;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        String mongoUri = dotenv.get("MONGODB_URI");
        String mongoDatabase = dotenv.get("MONGODB_DATABASE");

        if (mongoUri == null || mongoUri.isBlank()
                || mongoDatabase == null || mongoDatabase.isBlank()) {
            throw new IllegalStateException(
                    "MongoDB-konfiguration saknas i .env"
            );
        }

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            run(mongoClient.getDatabase(mongoDatabase), dotenv);
        } finally {
            DbConfig.close();
        }
    }

    private static void run(
            MongoDatabase mongoDatabase,
            Dotenv dotenv
    ) {
        PokemonDao repository = new JpaPokemonRepository();

        List<Pokemon> pokemons;

        try {
            pokemons = repository.findAll();
            Messages.success("Pokemon har laddats från databasen.");
        } catch (PokemonLoadException e) {
            Messages.error("Databasfel: " + e.getMessage());
            return;
        }

        // Anger var sparad Pokemon-data och startdata finns.
        Path path = Path.of(dotenv.get("POKEMON_FILE"));
        Path seedPath = Path.of(dotenv.get("POKEMON_SEED_FILE"));

        // Skapar filhanteraren som ansvarar för att läsa och spara Pokemon.
        // Samma objekt används av flera delar av programmet.
        PokemonFileService pokemonFileService = new PokemonFileService(path);

        // Läser in tidigare stridsstatistik eller skapar tom statistik.
        BattleStatsRepository statsRepository =
                new MongoBattleStatsRepository(mongoDatabase);

        BattleLogRepository battleLogRepository =
                new MongoBattleLogRepository(mongoDatabase);

//        // Tillfälligt test av MongoDB Update
//        List<BattleLog> logs = battleLogRepository.findAll();
//
//        if (!logs.isEmpty()) {
//            String matchId = logs.get(0).matchId();
//
//            boolean updated = battleLogRepository.markAsReviewed(matchId);
//
//            System.out.println("Stridslogg uppdaterad: " + updated);
//        }

        BattleStats stats = statsRepository.load();

        CpuPlayer cpuPlayer = new CpuPlayer(new RandomStrategy());

        BattleService battleService = new BattleService(
                stats,
                cpuPlayer,
                statsRepository,
                battleLogRepository
        );

        PlayMenuHandler playMenuHandler = new PlayMenuHandler(battleService);

        PokemonService pokemonService = new PokemonService(repository);

        PokemonMenu pokemonMenu = new PokemonMenu(pokemonService);

        MainMenuHandler mainMenu = new MainMenuHandler(
                seedPath,
                pokemonFileService,
                pokemonMenu,
                playMenuHandler
        );

        // Skapar en Scanner som används för användarens inmatning.
        // try-with-resources ser till att den stängs när programmet avslutas.
        try (Scanner scanner = new Scanner(System.in)) {

            // MainMenuHandler ansvarar för menyflödet.
            // Main behöver därför inte känna till de olika menyvalen.
            while (mainMenu.isRunning()) {
                pokemons = mainMenu.show(pokemons, scanner);
            }

        } catch (NoSuchElementException e) {
            // Hanterar avbruten inmatning utan en ohanterad exception.
            System.out.println(
                    "\nInmatningen avbröts. Programmet avslutas."
            );
        }
    }
}
