package se.systementor.utbildning;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PokemonApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        System.setProperty("POSTGRES_USER", dotenv.get("POSTGRES_USER"));
        System.setProperty("POSTGRES_PASSWORD", dotenv.get("POSTGRES_PASSWORD"));
        System.setProperty("POKEMON_FILE", dotenv.get("POKEMON_FILE"));
        System.setProperty("POKEMON_SEED_FILE", dotenv.get("POKEMON_SEED_FILE"));

        SpringApplication.run(PokemonApplication.class, args);
    }
}