package com.visage.cloudstorage.Configurations;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400)
@Profile("!test")
public class RedisSaveSassionConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory(){
        return new LettuceConnectionFactory();
    }

}
