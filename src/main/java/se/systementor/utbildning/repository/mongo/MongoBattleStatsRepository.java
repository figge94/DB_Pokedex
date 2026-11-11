package se.systementor.utbildning.repository.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import se.systementor.utbildning.model.BattleStats;
import se.systementor.utbildning.repository.BattleStatsRepository;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;

public class MongoBattleStatsRepository implements BattleStatsRepository {
    private static final String PLAYER_ID = "local-player";

    private final MongoCollection<Document> collection;
    private final ObjectMapper mapper = new ObjectMapper();

    public MongoBattleStatsRepository(MongoDatabase database) {
        collection = database.getCollection("battle_stats");
    }

    @Override
    public BattleStats load() {
        Document document = collection.find(eq("_id", PLAYER_ID))
                .projection(excludeId())
                .first();

        if (document == null) {
            return new BattleStats();
        }

        try {
            return mapper.readValue(document.toJson(), BattleStats.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunde inte läsa stridsstatistiken.", e);
        }
    }

    @Override
    public void save(BattleStats stats) {
        try {
            Document document = Document.parse(mapper.writeValueAsString(stats))
                    .append("_id", PLAYER_ID);

            collection.replaceOne(
                    eq("_id", PLAYER_ID),
                    document,
                    new ReplaceOptions().upsert(true)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunde inte spara stridsstatistiken.", e);
        }
    }
}