package com.vladislav.training.platform;

import org.springframework.boot.SpringApplication;
/**
 * Проверяет поведение {@code TestTrainingPlatformApplication}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
public class TestTrainingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(TrainingPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
