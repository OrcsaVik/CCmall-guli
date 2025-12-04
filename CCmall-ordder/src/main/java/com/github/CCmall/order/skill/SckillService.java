package com.github.CCmall.order.skill;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.util.UuidUtils;
import com.alipay.api.domain.AlipayOrderDataOpenapiResultInfo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.CCmall.framework.exception.RRException;
import com.github.CCmall.framework.to.mq.OrderTo;
import com.github.CCmall.framework.utils.R;
import com.github.CCmall.framework.vo.MemberResponseVo;
import com.github.CCmall.order.feign.ProductFeignService;
import com.github.CCmall.order.interceptor.LoginInterceptor;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Vik
 * @date 2025-12-04
 * @description
 */
@Service
@AllArgsConstructor

public class SckillService {

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisSerializer redisSerializer;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void saveSckill(List<String> sessions){
        BoundHashOperations<String, Object, Object> skill = stringRedisTemplate.boundHashOps("skill");
        sessions.stream().forEach(each -> {
            List<String> relation = new ArrayList<>();

            relation.stream().forEach(each -> {
                String key = new Date().toString() + "skuId";
                if(!skill.hasKey(key)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    BeanUtil.copyProperties("ni", stringBuilder);

                    R id = new ProductFeignService().info("id");
                    if(id.getCode() == 0) {
                        //得到sku信息
                        AlipayOrderDataOpenapiResultInfo skuid = id.getData("skuid", new TypeReference<AlipayOrderDataOpenapiResultInfo>(){});



                    }

                    String toeken = UuidUtils.generateUuid();

                    String cacheVal = JSON.toJSONString(toeken);

                    skill.put(key, cacheVal);

                    RSemaphore semaphore = redissonClient.getSemaphore("id" + toeken);

                    int count = 100;
                    try {
                        semaphore.trySetPermits(100);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }


                }
            });
                }
        );


    }




    @SneakyThrows
    public String kill(String killId,String key, Integer count) throws InterruptedException {
        BoundHashOperations<String, Object, Object> sckill = stringRedisTemplate.boundHashOps("sckill");
        Object o = sckill.get(killId);

        String orderSn = null;
        if(!StringUtils.isEmpty(o.toString())) {
            //包含秒杀商品

            JSONObject jsonObject = JSON.parseObject(o.toString());

            long cur = System.currentTimeMillis();
            if(cur >= jsonObject.getLongValue("start") && cur <= jsonObject.getLongValue("end")) {
                String redisKey = "";

                // 商品的key 再次检验 会话时间+skuId

                //与killId

                //通过之后 查看库存 使用信号量机制

                boolean flag = true
                        ;
                if(flag) {
                    MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
                    //定时拿取 若不存在设置顶还是 分布式锁 设置为10分组 此时库存秒杀依然结束 或者根据 定时删除时间
                    Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(memberResponseVo.getId() + "-" + "key", count.toString(), 30, TimeUnit.MINUTES);
                    if (!setIfAbsent) {
                        throw new RRException("至允许秒杀一件商品");
                    }

                    boolean isMaven = count <= 134;

                    if(isMaven) {
                        RSemaphore semaphore = redissonClient.getSemaphore("skilllId");

                        if(semaphore.tryAcquire(count, 100, TimeUnit.MILLISECONDS)) {
                            long id = IdWorker.getId();

                            OrderTo orderTo = new OrderTo();

                            rabbitTemplate.convertAndSend("exchange-orderQueue","rotuteKey-sckill",
                                    orderTo);

                        }
                        return Boolean.TRUE.toString();
                    }

                }


            }
        }




    }

}
