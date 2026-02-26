package kr.co.weeds.analyzer1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"kr.co.weeds.analyzer1"})
public class Analyzer1Application {

   public static void main(String[] args) {
      SpringApplication.run(Analyzer1Application.class, args);
   }
}