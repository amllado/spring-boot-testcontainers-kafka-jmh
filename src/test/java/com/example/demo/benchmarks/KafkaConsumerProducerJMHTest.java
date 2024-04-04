package com.example.demo.benchmarks;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
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

import com.example.demo.ImiMessage;
import com.example.demo.KafkaConsumer;
import com.example.demo.KafkaProducer;
import com.example.demo.TestDemoKafkaTestcontainersApplication;

/**
 * XXX amartinl: clase que sirve como POC de combinación de las siguientes tecnologías:
 * 
 * 1. Spring Boot como framework base de la solución.
 * 2. JUnit 5, para implementación de tests.
 * 3. Testcontainers, para lanzar pruebas de integración desde tests de JUnit, levantando contenedores con la integración a probar (Kafka, en este caso).
 * 4. JMH como framework para la ejecución de benchmarks invocables vía tests de JUnit.
 * 
 */
// Spring Boot & Testcontainers properties.
@SpringBootTest
@Testcontainers
@DirtiesContext
@TestPropertySource(properties = { "spring.kafka.consumer.auto-offset-reset=earliest" })
@Import(TestDemoKafkaTestcontainersApplication.class)
// JMH properties.
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class KafkaConsumerProducerJMHTest {

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
	
	/*
		XXX amartinl: para ligar las funcionalidades de Spring Boot con JMH necesitamos que los componentes que
		manejará el contenedor IoC de Spring sean estáticos y se establezca su valor mediante setters. Si no,
		si usamos el método "normal" de inyección de dependencias en elementos no estáticos, no funcionará.
		
		Fuente: https://gist.github.com/msievers/ce80d343fc15c44bea6cbb741dde7e45
	 */
	private static KafkaTemplate<String, ImiMessage> template;
	private static KafkaConsumer consumer;
	private static KafkaProducer producer;
	
	// Topic de prueba (se inyecta con @Value en setter al ser estático).
	private static String TEST_TOPIC;
	
	private static byte[] bytes512KiB;
	private static byte[] bytes1MiB;
	private static byte[] bytes10MiB;
	
	@Autowired
	void setKafkaTemplate(KafkaTemplate<String, ImiMessage> template) {
		KafkaConsumerProducerJMHTest.template = template;
	}
	
	@Autowired
	void setKafkaConsumer(KafkaConsumer consumer) {
		KafkaConsumerProducerJMHTest.consumer = consumer;
	}
	
	@Autowired
	void setKafkaProducer(KafkaProducer producer) {
		KafkaConsumerProducerJMHTest.producer = producer;
	}
	
	@Value("${test.topic}")
	public void setTestTopic(String name) {
		TEST_TOPIC = name;
	}

	public static String getTestTopic() {
	    return TEST_TOPIC;
	}
	
	@BeforeEach
	void setUp() {
		consumer.resetLatch();
	}
	
	/**
	 * XXX amartinl: test que se podrá lanzar mediante plugin de JUnit del IDE o mediante el goal de tests de Maven.
	 * Se encarga de configurar la infraestructura para ejecutar todos los métodos que tenga una anotación @Benchmark.
	 * De este modo ligamos JUnit + JMH.
	 * 
	 * @throws RunnerException
	 * @throws IOException 
	 */
	@Test
    public void executeJmhRunner() throws RunnerException, IOException {
		
		bytes512KiB = FileUtils.readFileToByteArray(new File("file512KiB.test"));
		bytes1MiB = FileUtils.readFileToByteArray(new File("file1MiB.test"));
		bytes10MiB = FileUtils.readFileToByteArray(new File("file10MiB.test"));
		
        Options jmhRunnerOptions = new OptionsBuilder()
            // set the class name regex for benchmarks to search for to the current class
            .include("\\." + this.getClass().getSimpleName() + "\\.")
            .warmupIterations(1)
            .measurementIterations(1)
            // do not use forking or the benchmark methods will not see references stored within its class
            .forks(0)
            // do not use multiple threads
            .threads(1)
            .shouldDoGC(true)
            .shouldFailOnError(true)
            .resultFormat(ResultFormatType.TEXT)
            .result("jmh.log") // set this to a valid filename if you want reports
            .jvmArgs("-server")
            .build();
            
        new Runner(jmhRunnerOptions).run();
        
    }
    
    @Benchmark
	public void givenKafkaDockerContainer_whenSendingWithDefaultTemplate_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", "Sending with default template".getBytes());
		
		template.send(getTestTopic(), data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}
    
    @Benchmark
    public void givenKafkaDockerContainer_whenSendingWithSimpleProducer_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", "Sending with our own simple KafkaProducer".getBytes());
		
		producer.send(getTestTopic(), data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}
    
    @Benchmark
    public void givenKafkaDockerContainer_whenSendingWithDefaultTemplate512KiB_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", bytes512KiB);
		
		template.send(getTestTopic(), data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}
    
    // @Benchmark
    // TODO amartinl: configurar contenedor de Kafka/Producer/Consumer para soportar este tamaño de mensaje.
    public void givenKafkaDockerContainer_whenSendingWithDefaultTemplate1MiB_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", bytes1MiB);
		
		template.send(getTestTopic(), data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}
    
    // @Benchmark
    // TODO amartinl: configurar contenedor de Kafka/Producer/Consumer para soportar este tamaño de mensaje.
    public void givenKafkaDockerContainer_whenSendingWithDefaultTemplate10MiB_thenMessageReceived() throws Exception {

		ImiMessage data = new ImiMessage(new Timestamp(new Date().getTime()), "srcApp", "dstApp", bytes10MiB);
		
		template.send(getTestTopic(), data);

		boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
		assertTrue(messageConsumed);
		assertThat(consumer.getPayload().srcApp(), containsString("srcApp"));
		
	}

}