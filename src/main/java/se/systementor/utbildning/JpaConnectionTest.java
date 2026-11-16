package se.systementor.utbildning;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import se.systementor.utbildning.model.Pokemon;
import se.systementor.utbildning.repository.PokemonDao;

public class JpaConnectionTest {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context =
                     SpringApplication.run(PokemonApplication.class, args)) {

            PokemonDao repository = context.getBean(PokemonDao.class);

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
        }
    }
}