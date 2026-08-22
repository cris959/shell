package saludfinanciera.finanzas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanzasApplication {

	static void main(String[] args) {
		// Genera e imprime el hash en consola al arrancar
	//	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	//	String hash = encoder.encode("123456");
	//	System.out.println("\n>>> HASH GENERADO: " + hash + "\n");


		SpringApplication.run(FinanzasApplication.class, args);
	}

}
