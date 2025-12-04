package com.github.CCmall.order.skill;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vik
 * @date 2025-12-04
 * @description
 */
@Configuration
public class RedissonConfig {


    @Bean
    public RedissonClient redissonClient() {

        Config config = new Config();
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;

    }

    @Bean
    public RabbitTemplate rabbitTemplate(){
        return new RabbitTemplate();
    }




}
