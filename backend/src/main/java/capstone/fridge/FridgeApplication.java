package capstone.fridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FridgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FridgeApplication.class, args);
	}

}
