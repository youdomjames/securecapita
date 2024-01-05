package com.youdomjames.securecapita.configuration;

import com.vonage.client.VonageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SMSConfig {
    @Value("${vonage.api.key}")
    private String API_KEY;
    @Value("${vonage.api.secret}")
    private String API_SECRET;
    @Bean
    public VonageClient vonageClient (){
        return  VonageClient.builder()
                .apiKey(API_KEY)
                .apiSecret(API_SECRET)
                .build();
    }
}
