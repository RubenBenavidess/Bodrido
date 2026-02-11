package com.espe.edu.ec.customer_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * CUSTOMER MICROSERVICE - Arquitectura por Capas
 * 
 * RESPONSABILIDAD: Sincronizar datos de clientes desde Auth-ms
 * 
 * FLUJO PRINCIPAL:
 *  1. Auth-ms publica evento cuando se crea usuario con rol CLIENT
 *  2. RabbitMQ routing a customers.validation queue
 *  3. CustomerEventListener procesa el evento
 *  4. CustomerService crea registro en customer-db
 *  5. Si falla: DLQ reintenta → eventProducer publica compensación
 *  6. Auth-ms recibe compensación → desactiva usuario
 * 
 * GARANTÍA: Saga Pattern con compensación asegura consistencia eventual
 * 
 * TECNOLOGÍA:
 *  - Spring Boot 3.3.0
 *  - PostgreSQL (bodrido_customer_db)
 *  - RabbitMQ (event-driven)
 *  - JPA/Hibernate (ORM)
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableJpaAuditing
public class CustomerMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerMsApplication.class, args);
	}

}
