
package se.systementor.utbildning;

import se.systementor.utbildning.repository.PokemonFileService;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.service.BattleService;
import se.systementor.utbildning.service.PokemonService;
import se.systementor.utbildning.ui.PokemonMenu;
import se.systementor.utbildning.ui.PlayMenuHandler;
import se.systementor.utbildning.ui.Messages;
import se.systementor.utbildning.ui.MainMenuHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import se.systementor.utbildning.repository.PokemonDao;
import se.systementor.utbildning.exception.PokemonLoadException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class Main implements CommandLineRunner {

    private final PokemonDao pokemonDao;
    private final BattleService battleService;
    private final PokemonService pokemonService;
    private final Path pokemonSeedPath;
    private final PokemonFileService pokemonFileService;

    public Main(
            PokemonDao pokemonDao,
            BattleService battleService,
            PokemonService pokemonService,
            PokemonFileService pokemonFileService,
            @Value("${pokemon.seed-file}") String pokemonSeedFile
    ) {
        this.pokemonDao = pokemonDao;
        this.battleService = battleService;
        this.pokemonService = pokemonService;
        this.pokemonFileService = pokemonFileService;
        this.pokemonSeedPath = Path.of(pokemonSeedFile);
    }

    @Override
    public void run(String... args) {
        startMenu();
    }

    private void startMenu() {

        List<Pokemon> pokemons;

        try {
            pokemons = pokemonDao.findAll();
            Messages.success("Pokemon har laddats från databasen.");
        } catch (PokemonLoadException e) {
            Messages.error("Databasfel: " + e.getMessage());
            return;
        }

        // Anger var sparad Pokemon-data och startdata finns.
        Path seedPath = pokemonSeedPath;

        PlayMenuHandler playMenuHandler = new PlayMenuHandler(battleService);

        PokemonMenu pokemonMenu = new PokemonMenu(pokemonService);

        MainMenuHandler mainMenu = new MainMenuHandler(
                seedPath,
                pokemonFileService,
                pokemonMenu,
                playMenuHandler
        );

        try (Scanner scanner = new Scanner(System.in)) {

            while (mainMenu.isRunning()) {
                pokemons = mainMenu.show(pokemons, scanner);
            }

        } catch (NoSuchElementException e) {
            System.out.println(
                    "\nInmatningen avbröts. Programmet avslutas."
            );
        }
    }
}
