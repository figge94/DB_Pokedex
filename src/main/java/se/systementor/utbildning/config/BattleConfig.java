package se.systementor.utbildning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.systementor.utbildning.model.BattleStats;
import se.systementor.utbildning.model.CpuPlayer;
import se.systementor.utbildning.strategy.RandomStrategy;
import se.systementor.utbildning.repository.BattleStatsRepository;

@Configuration
public class BattleConfig {

    @Bean
    public BattleStats battleStats(BattleStatsRepository statsRepository) {
        return statsRepository.load();
    }

    @Bean
    public CpuPlayer cpuPlayer() {
        return new CpuPlayer(new RandomStrategy());
    }
}