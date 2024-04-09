package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.testcontainers.containers.KafkaContainer;
//import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestDemoKafkaTestcontainersApplication {
	
/*
	XXX amllado: definiendo el componente de este modo sería el contenedor elegido
	para ser compartido por todos los tests. De momento lo descartamos, dejando que
	cada clase de test elija cuál instanciar.
	
	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
	}
*/
	
	public static void main(String[] args) {
		SpringApplication.from(DemoKafkaTestcontainersApplication::main).with(TestDemoKafkaTestcontainersApplication.class).run(args);
	}

}
