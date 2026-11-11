package se.systementor.utbildning.service;

import se.systementor.utbildning.exception.InvalidPokemonException;
import se.systementor.utbildning.exception.PokemonSaveException;
import se.systementor.utbildning.model.Attack;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.model.Type;
import se.systementor.utbildning.repository.PokemonDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PokemonService {

    private final PokemonDao pokemonDao;

    public PokemonService(PokemonDao pokemonDao) {
        this.pokemonDao = pokemonDao;
    }

    // Söker efter en Pokemon utan att skilja på stora och små bokstäver.
    // Optional visar att sökningen kan ge ett resultat eller vara tom.
    public Optional<Pokemon> findPokemonOptional(
            List<Pokemon> pokemons,
            String name
    ) {
        return pokemons.stream()
                .filter(pokemon ->
                        pokemon.getName().equalsIgnoreCase(name)
                )
                .findFirst();
    }


    // Söker efter en Pokemon via Optional-metoden.
    // Returnerar null tills övriga klasser har anpassats till Optional.
    public Pokemon findPokemonByName(
            List<Pokemon> pokemons,
            String name
    ) {
        return findPokemonOptional(pokemons, name)
                .orElse(null);
    }

    // Kontrollerar om namnet används av någon annan Pokemon än den som redigeras.
    private static boolean isNameTakenByAnotherPokemon(
            List<Pokemon> pokemons,
            String name,
            int id
    ) {
        return pokemons.stream()
                .anyMatch(pokemon ->
                        pokemon.getId() != id
                                && pokemon.getName().equalsIgnoreCase(name)
                );
    }

    public Pokemon createPokemon(
            List<Pokemon> pokemons,
            String name,
            int hp,
            Type type,
            int speed
    ) {
        // Kontrollerar om namnet redan används.
        // ifPresent hanterar resultatet utan en separat null-kontroll.
        findPokemonOptional(pokemons, name)
                .ifPresent(existingPokemon -> {
                    throw new InvalidPokemonException(
                            name + " finns redan."
                    );
                });

        // ID 0 betyder att Pokemonen ännu inte är sparad.
        // Databasen genererar ID:t vid insert.
        int id = 0;

        Pokemon pokemon = new Pokemon(
                id,
                name,
                type,
                hp,
                speed
        );

        Attack attack = AttackService.createDefaultAttack(type);
        pokemon.addAttack(attack);

        return pokemon;
    }

    public void addPokemon(
            List<Pokemon> pokemons,
            Pokemon pokemon
    ) {
        Pokemon savedPokemon = pokemonDao.insert(pokemon);
        pokemons.add(savedPokemon);
    }

    public void addAttackToPokemon(
            Pokemon pokemon,
            Attack attack
    ) {
        pokemon.addAttack(attack);

        try {
            pokemonDao.save(pokemon);
        } catch (PokemonSaveException e) {
            // Ångra attacken om sparandet misslyckas.
            pokemon.removeAttack(attack);
            throw e;
        }
    }

    public void removeAttackFromPokemon(
            Pokemon pokemon,
            Attack attack
    ) {
        // Spara den ursprungliga attacklistan.
        List<Attack> originalAttacks = new ArrayList<>(pokemon.getAttacks());

        // Pokemon kontrollerar att attacken får tas bort.
        pokemon.removeAttack(attack);

        try {
            pokemonDao.save(pokemon);
        } catch (PokemonSaveException e) {
            // Återställ attackerna och deras ursprungliga ordning.
            pokemon.replaceAttacks(originalAttacks);
            throw e;
        }
    }

    // Ändrar namnet på en Pokemon.
    public Pokemon editPokemonName(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            String newName
    ) {
        if (isNameTakenByAnotherPokemon(
                pokemons, newName, pokemon.getId())) {
            throw new InvalidPokemonException(
                    newName + " finns redan."
            );
        }

        return updatePokemon(
                pokemons,
                pokemon,
                pokemon.withName(newName)
        );
    }

    // Ändrar typ på en Pokemon.
    public Pokemon editPokemonType(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            Type newType
    ) {
        return updatePokemon(
                pokemons,
                pokemon,
                pokemon.withType(newType)
        );
    }

    // Ändrar currentHp på en Pokemon.
    public void editPokemonCurrentHp(Pokemon pokemon, int hp) {
        int difference = hp - pokemon.getCurrentHp();

        if (difference > 0) {
            pokemon.heal(difference);
        } else if (difference < 0) {
            pokemon.takeDamage(-difference);
        }
    }

    // Ändrar maxHp på en Pokemon.
    public Pokemon editPokemonMaxHp(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            int hp
    ) {
        return updatePokemon(
                pokemons,
                pokemon,
                pokemon.withMaxHp(hp)
        );
    }

    public Pokemon editPokemonSpeed(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            int speed
    ) {
        return updatePokemon(
                pokemons,
                pokemon,
                pokemon.withSpeed(speed)
        );
    }

    // Tar bort en Pokemon från listan.
    public void removePokemon(
            List<Pokemon> pokemons,
            Pokemon pokemon
    ) {
        int index = pokemons.indexOf(pokemon);

        if (index == -1) {
            throw new InvalidPokemonException(
                    "Pokemon hittades inte i listan."
            );
        }

        pokemonDao.delete(pokemon.getId());
        pokemons.remove(index);
    }

    private Pokemon updatePokemon(
            List<Pokemon> pokemons,
            Pokemon original,
            Pokemon updated
    ) {
        int index = pokemons.indexOf(original);

        if (index == -1) {
            throw new InvalidPokemonException(
                    "Pokemon hittades inte i listan."
            );
        }

        pokemonDao.save(updated);
        pokemons.set(index, updated);

        return updated;
    }
}