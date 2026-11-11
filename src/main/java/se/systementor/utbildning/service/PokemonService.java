package se.systementor.utbildning.service;

import se.systementor.utbildning.exception.InvalidPokemonException;
import se.systementor.utbildning.exception.PokemonSaveException;
import se.systementor.utbildning.model.Attack;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.model.Type;
import se.systementor.utbildning.repository.PokemonFileService;
import se.systementor.utbildning.repository.PokemonDao;

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
            List<Pokemon> pokemons,
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
            List<Pokemon> pokemons,
            Pokemon pokemon,
            Attack attack
    ) {
        // Spara den ursprungliga attacklistan.
        List<Attack> originalAttacks = pokemon.getAttacks();

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

    private static void replacePokemon(
            List<Pokemon> pokemons,
            Pokemon originalPokemon,
            Pokemon updatedPokemon
    ) {
        int index = pokemons.indexOf(originalPokemon);

        if (index == -1) {
            throw new InvalidPokemonException(
                    "Pokemon hittades inte i listan."
            );
        }

        pokemons.set(index, updatedPokemon);
    }

    private void saveOrRollback(
            List<Pokemon> pokemons,
            Pokemon originalPokemon,
            Pokemon updatedPokemon
    ) {
        try {
            pokemonDao.save(updatedPokemon);
        } catch (PokemonSaveException e) {
            // Återställ originalet om sparandet misslyckas.
            replacePokemon(pokemons, updatedPokemon, originalPokemon);
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

        Pokemon updatedPokemon = pokemon.withName(newName);

        replacePokemon(pokemons, pokemon, updatedPokemon);

        saveOrRollback(pokemons, pokemon, updatedPokemon);

        return updatedPokemon;
    }

    // Ändrar typ på en Pokemon.
    public Pokemon editPokemonType(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            Type newType
    ) {
        Pokemon updatedPokemon = pokemon.withType(newType);

        replacePokemon(pokemons, pokemon, updatedPokemon);

        saveOrRollback(pokemons, pokemon, updatedPokemon);

        return updatedPokemon;
    }

    // Ändrar currentHp på en Pokemon.
    public void editPokemonCurrentHp(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            int hp
    ) {
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
        Pokemon updatedPokemon = pokemon.withMaxHp(hp);

        replacePokemon(pokemons, pokemon, updatedPokemon);

        saveOrRollback(pokemons, pokemon, updatedPokemon);

        return updatedPokemon;
    }

    public Pokemon editPokemonSpeed(
            List<Pokemon> pokemons,
            Pokemon pokemon,
            int speed
    ) {
        Pokemon updatedPokemon = pokemon.withSpeed(speed);

        replacePokemon(pokemons, pokemon, updatedPokemon);

        saveOrRollback(pokemons, pokemon, updatedPokemon);

        return updatedPokemon;
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

        pokemons.remove(index);

        try {
            // Tar bort Pokemon från databasen.
            pokemonDao.delete(pokemon.getId());

        } catch (PokemonSaveException e) {
            // Återställer Pokemon på samma plats om sparandet misslyckas.
            pokemons.add(index, pokemon);
            throw e;
        }
    }
}