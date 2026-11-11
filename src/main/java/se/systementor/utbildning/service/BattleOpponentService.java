package se.systementor.utbildning.service;

import se.systementor.utbildning.model.Pokemon;

import java.util.List;
import java.util.Random;

public class BattleOpponentService {

    private static final Random RANDOM = new Random();

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

        do {
            wildPokemon = pokemons.get(RANDOM.nextInt(pokemons.size()));
        } while (wildPokemon == null
                || wildPokemon.equals(playerPokemon));

        return wildPokemon;
    }
}