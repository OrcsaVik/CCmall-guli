package com.github.CCmall.order.config;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQImpl;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author Vik
 * @date 2025-12-03
 * @description
 */
@Configuration
public class RabbitMQConfig {


    @Bean
    public Exchange orderEventExchange() {

        return new TopicExchange("orderEvent", true, false);
    }


    @Bean
    public AMQP.Queue orderDelayQueue() {

        HashMap<String, Object> params = new HashMap<>();
        //死信交换机
        params.put("x-dead-letter-exchange", "order-event-exchange");
        //死信路由键
        params.put("x-dead-letter-routing-key", "order.release.order");
        params.put("x-message-ttl", 60000); // 消息过期时间 1分钟

        return new AMQImpl.Queue("order.delay.queue",true,false,false,params);
    }

    @Bean
    public Queue orderRelQueue()  {
        Queue queue = new Queue("order-release-queue", true, false, fasle);

        return queue;
    }


    @Bean
    public Binding orderCreateBinding() {
        /**
         * String destination, 目的地（队列名或者交换机名字）
         * DestinationType destinationType, 目的地类型（Queue、Exhcange）
         * String exchange,
         * String routingKey,
         * Map<String, Object> arguments
         * */
        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.create.order", null);
    }

    @Bean
    public Binding orderReleaseBinding() {
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
    }

    @Bean
    public Binding orderReleaseOrderBinding() {
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);
    }

    /**
     * 商品秒杀队列
     * @return
     */
    @Bean
    public Queue orderSecKillOrrderQueue() {
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Binding orderSecKillOrrderQueueBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        // 			Map<String, Object> arguments
        Binding binding = new Binding(
                "order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);

        return binding;
    }





    }
