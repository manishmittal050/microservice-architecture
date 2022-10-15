package com.mmittal.gatewayservice.filter;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class AuthGatewayFilter extends AbstractGatewayFilterFactory<AuthGatewayFilter.Config>{

	Logger log = LoggerFactory.getLogger(AuthGatewayFilter.class);
	
	private final WebClient.Builder webClientBuilder;

	@Autowired
	private ObjectMapper objectMapper;

	final List<String> excludedUrls = List.of("/login");
	
	public AuthGatewayFilter(WebClient.Builder webBuilder) {
		super(Config.class);
		this.webClientBuilder=webBuilder;
	}

	public static class Config {
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			log.info("**************************************************************************");
			log.info("URL is - " + request.getURI().getPath());
			String bearerToken = request.getHeaders().getFirst("Authorization");
			log.info("Bearer Token: "+ bearerToken);

			if(isSecured.test(request)) {
				return webClientBuilder.build().post()
						.uri("lb://AUTH-SERVICE/auth/validate")
						.header("Authorization", bearerToken)
						.retrieve().bodyToMono(String.class)
						.map(response -> {
							exchange.getRequest().mutate().header("check", response);
							return exchange;
						}).flatMap(chain::filter).onErrorResume(error -> {
							log.info("Error Happened");
							HttpStatus errorCode = null;
							String errorMsg = "";
							if (error instanceof WebClientResponseException) {
								WebClientResponseException webCLientException = (WebClientResponseException) error;
								errorCode = webCLientException.getStatusCode();
								errorMsg = webCLientException.getStatusText();

							} else {
								errorCode = HttpStatus.BAD_GATEWAY;
								errorMsg = HttpStatus.BAD_GATEWAY.getReasonPhrase();
							}
							//	                            AuthorizationFilter.AUTH_FAILED_CODE
							return onError(exchange, String.valueOf(errorCode.value()) ,errorMsg, "JWT Authentication Failed", errorCode);
						});
			}

			return chain.filter(exchange);
		};
	}

	public Predicate<ServerHttpRequest> isSecured = request -> excludedUrls.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));
	
	private Mono<Void> onError(ServerWebExchange exchange, String errCode, String err, String errDetails, HttpStatus httpStatus) {
		DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
		//	        ObjectMapper objMapper = new ObjectMapper();
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		try {
			response.getHeaders().add("Content-Type", "application/json");
//			ExceptionResponseModel data = new ExceptionResponseModel(errCode, err, errDetails, null, new Date());
			byte[] byteData = objectMapper.writeValueAsBytes(errDetails);
			return response.writeWith(Mono.just(byteData).map(t -> dataBufferFactory.wrap(t)));

		} catch (JsonProcessingException e) {
			e.printStackTrace();

		}
		return response.setComplete();
	}


}
