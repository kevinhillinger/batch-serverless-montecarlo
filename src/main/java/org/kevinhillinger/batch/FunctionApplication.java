package org.kevinhillinger.batch;

import org.kevinhillinger.batch.model.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class FunctionApplication {
    @Bean
    public Function<User, Greeting> hello() {
        return user -> new Greeting("Welcome, " + user.getName());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(FunctionApplication.class, args);
    }
}