package se.systementor.utbildning.model;

public record BattleRound(
        int round,
        int attackerPokemonId,
        String attack,
        int damage
) {}