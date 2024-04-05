package com.example.demo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * XXX amartinl: clase que sirve como POC de combinación de las siguientes tecnologías:
 * 
 * 1. Spring Boot como framework base de la solución.
 * 2. JUnit 5, para implementación de tests.
 * 
 */
@SpringBootTest
@Testcontainers
@DirtiesContext
@Import(TestDemoKafkaTestcontainersApplication.class)
@TestPropertySource(properties = { "spring.kafka.consumer.auto-offset-reset=earliest" })
class KafkaExternoConsumerProducerTest {

	@Autowired
	private KafkaTemplate<String, ImiMessage> template;

	@Autowired
	private KafkaConsumer consumer;

	@Autowired
	private KafkaProducer producer;

	@Value("${test.topic}")
	private String topic;
	
	@BeforeEach
	void setUp() {
		consumer.resetLatch();
	}

	@Test
	@Disabled("Comentar esta anotación para probar este test. Requiere de un Kafka externo levantado en localhost:9092 (ver application.properties)")
	public void givenKafkaDockerContainer_whenSendingWithDefaultTemplate_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", "Sending with default template".getBytes());
		
		template.send(topic, data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}

	@Test
	@Disabled("Comentar esta anotación para probar este test. Requiere de un Kafka externo levantado en localhost:9092 (ver application.properties)")
	public void givenKafkaDockerContainer_whenSendingWithSimpleProducer_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", "Sending with our own simple KafkaProducer".getBytes());
		
		producer.send(topic, data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}

}