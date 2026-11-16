package se.systementor.utbildning.model;

import se.systementor.utbildning.exception.InvalidAttackException;
import java.util.ArrayList;
import java.util.List;

public class PokemonAttacks {

    private final List<Attack> attacks = new ArrayList<>();

    public List<Attack> getAttacks() {
        return List.copyOf(attacks);
    }

    public void addAttack(Attack attack) {
        if (attack == null) {
            throw new InvalidAttackException(
                    "Attack får inte vara null."
            );
        }

        if (attacks.size() >= 4) {
            throw new InvalidAttackException(
                    "En Pokemon kan ha max 4 attacker."
            );
        }

        attacks.add(attack);
    }

    public void replaceAttacks(List<Attack> newAttacks) {
        if (newAttacks == null ||
                newAttacks.isEmpty() ||
                newAttacks.size() > 4) {
            throw new InvalidAttackException(
                    "En Pokemon måste ha mellan 1 och 4 attacker."
            );
        }

        if (newAttacks.contains(null)) {
            throw new InvalidAttackException(
                    "Attacklistan får inte innehålla null."
            );
        }

        attacks.clear();
        attacks.addAll(newAttacks);
    }

    public void removeAttack(Attack attack) {
        if (attack == null) {
            throw new InvalidAttackException(
                    "Attack får inte vara null."
            );
        }

        if (attacks.size() <= 1) {
            throw new InvalidAttackException(
                    "En Pokemon måste ha minst 1 attack."
            );
        }

        if (!attacks.contains(attack)) {
            throw new InvalidAttackException(
                    "Pokemon har inte den attacken."
            );
        }

        attacks.remove(attack);
    }
}