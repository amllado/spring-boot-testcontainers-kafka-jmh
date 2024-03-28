package com.example.demo;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@DirtiesContext
@TestPropertySource(properties = { "spring.kafka.consumer.auto-offset-reset=earliest" })
@Import(TestDemoKafkaTestcontainersApplication.class)
class KafkaConsumerProducerTest {

	/*
		XXX amartinl: contenedor para el Kafka de los tests de esta clase. Si se desea que exista
		un contenedor común para todas las clases de test, ver el comentario en TestDemoKafkaTestcontainersApplication.
	 */
	@Container
	static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}
	
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
	public void givenKafkaDockerContainer_whenSendingWithDefaultTemplate_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", "Sending with default template".getBytes());
		
		template.send(topic, data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}

	@Test
	public void givenKafkaDockerContainer_whenSendingWithSimpleProducer_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", "Sending with our own simple KafkaProducer".getBytes());
		
		producer.send(topic, data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}

}