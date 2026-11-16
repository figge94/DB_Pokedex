package se.systementor.utbildning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.systementor.utbildning.repository.PokemonFileService;

import java.nio.file.Path;

@Configuration
public class FileConfig {

    @Bean
    public PokemonFileService pokemonFileService(
            @Value("${pokemon.file}") String pokemonFile
    ) {
        return new PokemonFileService(Path.of(pokemonFile));
    }
}