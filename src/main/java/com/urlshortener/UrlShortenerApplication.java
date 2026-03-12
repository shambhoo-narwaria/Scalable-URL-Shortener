package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * =============================================================
 * WHAT IS THIS CLASS?
 * =============================================================
 * This is the ENTRY POINT of the entire Spring Boot application.
 * When you run: mvn spring-boot:run
 * Java calls the main() method here first.
 *
 * @SpringBootApplication is a combined annotation that does 3 things:
 *
 *   1. @SpringBootConfiguration
 *      → Marks this as a Spring configuration class.
 *        Spring uses it to understand this IS a config source.
 *
 *   2. @EnableAutoConfiguration
 *      → Tells Spring Boot to automatically configure beans
 *        based on the JARs found on the classpath.
 *        Example: If it sees the PostgreSQL driver JAR,
 *        it auto-configures a DataSource bean.
 *        If it sees Redis JAR, it auto-configures RedisTemplate.
 *        YOU don't have to write that configuration manually.
 *
 *   3. @ComponentScan
 *      → Tells Spring to scan this package (com.urlshortener)
 *        and all sub-packages for classes annotated with:
 *        @Component, @Service, @Repository, @Controller, @RestController
 *        and register them as Spring-managed beans.
 *
 * WHAT IS A BEAN?
 *   A "bean" in Spring is simply an object that Spring creates
 *   and manages for you. Instead of doing:
 *     UrlService service = new UrlService();
 *   Spring creates it, wires dependencies, and hands it to you.
 *   This is called "Inversion of Control" (IoC).
 *
 * WHAT IS DEPENDENCY INJECTION?
 *   When Spring sees a class needs UrlRepository, it automatically
 *   "injects" the already-created UrlRepository bean into it.
 *   You just declare: @Autowired private UrlRepository repo;
 *   or use constructor injection (preferred):
 *     private final UrlRepository repo;
 *     public UrlService(UrlRepository repo) { this.repo = repo; }
 *
 * @EnableAsync
 *   Enables Spring's async method execution.
 *   Needed for our @Async method that increments click counts
 *   without blocking the redirect response.
 * =============================================================
 */
@SpringBootApplication
@EnableAsync   // Enables @Async annotation support for async methods
public class UrlShortenerApplication {

    public static void main(String[] args) {
        /*
         * SpringApplication.run() does all of this:
         *   1. Creates the ApplicationContext (Spring's IoC container)
         *   2. Scans and registers all beans
         *   3. Starts the embedded Tomcat HTTP server on port 8080
         *   4. Applies all auto-configurations
         *   5. Runs any ApplicationRunner / CommandLineRunner beans
         *
         * After this line completes, your API is live and accepting requests.
         */
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
