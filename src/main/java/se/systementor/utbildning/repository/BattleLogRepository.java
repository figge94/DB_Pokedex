package se.systementor.utbildning.repository;

import se.systementor.utbildning.model.BattleLog;
import java.util.List;

public interface BattleLogRepository {

    void save(BattleLog log);

    List<BattleLog> findAll();

    List<BattleLog> findByPokemonId(int pokemonId);

    boolean deleteByMatchId(String matchId);

    boolean markAsReviewed(String matchId);
}


