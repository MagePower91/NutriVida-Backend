package cl.nutrivida.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// La clave JWT real viene de la variable de entorno JWT_SECRET. Para esta prueba se usa un valor
// propio, así funciona en cualquier computador sin configurar nada.
@SpringBootTest(properties = "app.jwt.secreto=clave-solo-para-pruebas-del-gateway-32-caracteres")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
