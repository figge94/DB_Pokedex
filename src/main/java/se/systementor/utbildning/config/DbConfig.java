package se.systementor.utbildning.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class DbConfig {

    private static final EntityManagerFactory emf;

    static {
        Dotenv dotenv = Dotenv.load();

        Map<String, Object> properties = new HashMap<>();

        String host = dotenv.get("POSTGRES_HOST");
        String port = dotenv.get("POSTGRES_PORT");
        String database = dotenv.get("POSTGRES_DB");
        String user = dotenv.get("POSTGRES_USER");
        String password = dotenv.get("POSTGRES_PASSWORD");

        if (host == null || host.isBlank()
                || port == null || port.isBlank()
                || database == null || database.isBlank()
                || user == null || user.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "PostgreSQL-konfiguration saknas i .env"
            );
        }

        String url = "jdbc:postgresql://"
                + host + ":" + port + "/" + database;

        properties.put("jakarta.persistence.jdbc.url", url);
        properties.put("jakarta.persistence.jdbc.user", user);
        properties.put("jakarta.persistence.jdbc.password", password);
        properties.put("jakarta.persistence.jdbc.driver",
                "org.postgresql.Driver");

        emf = Persistence.createEntityManagerFactory(
                "pokemonPU",
                properties
        );
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        emf.close();
    }
}