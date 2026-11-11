package se.systementor.utbildning;

import se.systementor.utbildning.config.DbConfig;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.repository.jpa.JpaPokemonRepository;

public class JpaConnectionTest {

    public static void main(String[] args) {
        try {
            JpaPokemonRepository repository = new JpaPokemonRepository();

            for (Pokemon pokemon : repository.findAll()) {
                System.out.println(
                        pokemon.getName()
                                + " – antal attacker: "
                                + pokemon.getAttacks().size()
                );
            }

            repository.findById(1).ifPresentOrElse(
                    pokemon -> System.out.println(
                            "Hittade via ID: " + pokemon.getName()
                    ),
                    () -> System.out.println("Ingen Pokemon med ID 1.")
            );
        } finally {
            DbConfig.close();
        }
    }
}