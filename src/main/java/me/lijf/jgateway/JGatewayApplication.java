package me.lijf.jgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.lijf.jgateway.service.JDynamicRouteServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JGatewayApplication {
	public static void main(String[] args) {
		ConfigurableApplicationContext context=SpringApplication.run(JGatewayApplication.class, args);
	}

	//防止predicates和filter在进行JSON转化时发生空值异常。
	@Bean
	public ObjectMapper objectMapper(){
		return new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	}
}

