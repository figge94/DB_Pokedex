package se.systementor.utbildning.model;

import java.time.Instant;
import java.util.List;

public record BattleLog(
        String matchId,
        Instant playedAt,
        List<Integer> participants,
        Integer winnerPokemonId,
        List<BattleRound> rounds
) {}