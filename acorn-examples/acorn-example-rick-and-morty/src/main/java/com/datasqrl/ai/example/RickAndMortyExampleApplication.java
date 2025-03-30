package com.datasqrl.ai.example;

import com.datasqrl.ai.acorn.EnableAcorn;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAcorn
public class RickAndMortyExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(RickAndMortyExampleApplication.class, args);
  }
}
