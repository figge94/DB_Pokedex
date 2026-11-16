package se.systementor.utbildning.repository.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import se.systementor.utbildning.model.BattleLog;
import se.systementor.utbildning.model.BattleRound;
import se.systementor.utbildning.repository.BattleLogRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import org.springframework.stereotype.Repository;

@Repository
public class MongoBattleLogRepository implements BattleLogRepository {

    private final MongoCollection<Document> collection;

    public MongoBattleLogRepository(MongoDatabase database) {
        this.collection = database.getCollection("battleLogs");
    }

    @Override
    public void save(BattleLog log) {
        Document document = new Document("matchId", log.matchId())
                .append("playedAt", Date.from(log.playedAt()))
                .append("participants", log.participants().stream()
                        .map(id -> new Document("pokemonId", id))
                        .toList())
                .append("winnerPokemonId", log.winnerPokemonId())
                .append("rounds", log.rounds().stream()
                        .map(round -> new Document("round", round.round())
                                .append("attackerPokemonId", round.attackerPokemonId())
                                .append("attack", round.attack())
                                .append("damage", round.damage()))
                        .toList());

        System.out.println("DEBUG MongoDB: " + document.toJson());
        collection.insertOne(document);
    }

    @Override
    public List<BattleLog> findAll() {
        return collection.find()
                .map(this::toBattleLog)
                .into(new ArrayList<>());
    }

    @Override
    public List<BattleLog> findByPokemonId(int pokemonId) {
        return collection.find(eq("participants.pokemonId", pokemonId))
                .map(this::toBattleLog)
                .into(new ArrayList<>());
    }

    @Override
    public boolean deleteByMatchId(String matchId) {
        return collection.deleteOne(eq("matchId", matchId))
                .getDeletedCount() > 0;
    }

    private BattleLog toBattleLog(Document document) {
        List<Integer> participants = document
                .getList("participants", Document.class)
                .stream()
                .map(participant -> participant.getInteger("pokemonId"))
                .toList();

        List<BattleRound> rounds = document
                .getList("rounds", Document.class, List.of())
                .stream()
                .map(round -> new BattleRound(
                        round.getInteger("round"),
                        round.getInteger("attackerPokemonId"),
                        round.getString("attack"),
                        round.getInteger("damage")
                ))
                .toList();

        return new BattleLog(
                document.getString("matchId"),
                document.getDate("playedAt").toInstant(),
                participants,
                document.getInteger("winnerPokemonId"),
                rounds
        );
    }

    @Override
    public boolean markAsReviewed(String matchId) {
        return collection.updateOne(
                eq("matchId", matchId),
                set("reviewed", true)
        ).getModifiedCount() > 0;
    }
}