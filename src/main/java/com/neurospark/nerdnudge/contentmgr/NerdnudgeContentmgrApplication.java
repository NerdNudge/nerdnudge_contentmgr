package com.neurospark.nerdnudge.contentmgr;

import com.neurospark.nerdnudge.metrics.metrics.Metronome;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.neurospark.nerdnudge"})
public class NerdnudgeContentmgrApplication {
	public static void main(String[] args) {
		Metronome.initiateMetrics(60000);
		SpringApplication.run(NerdnudgeContentmgrApplication.class, args);
	}
}
