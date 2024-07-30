package com.neurospark.nerdnudge.contentmgr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.neurospark.nerdnudge"})
public class NerdnudgeContentmgrApplication {
	public static void main(String[] args) {
		SpringApplication.run(NerdnudgeContentmgrApplication.class, args);
	}
}
