package se.systementor.utbildning.repository;

import se.systementor.utbildning.model.BattleStats;

public interface BattleStatsRepository {
    BattleStats load();

    void save(BattleStats stats);
}