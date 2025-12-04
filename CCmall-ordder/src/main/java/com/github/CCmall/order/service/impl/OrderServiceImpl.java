package com.github.CCmall.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.CCmall.framework.constant.CartConstant;
import com.github.CCmall.framework.exception.NoStockException;
import com.github.CCmall.framework.to.SkuHasStockVo;
import com.github.CCmall.framework.to.mq.SeckillOrderTo;
import com.github.CCmall.framework.utils.PageUtils;
import com.github.CCmall.framework.vo.MemberResponseVo;
import com.github.CCmall.order.constant.OrderConstant;
import com.github.CCmall.order.entity.OrderEntity;
import com.github.CCmall.order.entity.OrderItemEntity;
import com.github.CCmall.order.enume.OrderStatusEnum;
import com.github.CCmall.order.feign.CartFeignService;
import com.github.CCmall.order.feign.MemberFeignService;
import com.github.CCmall.order.feign.ProductFeignService;
import com.github.CCmall.order.feign.WareFeignService;
import com.github.CCmall.order.interceptor.LoginInterceptor;
import com.github.CCmall.order.service.OrderService;
import com.github.CCmall.order.service.PayCallBackHandler;
import com.github.CCmall.order.to.OrderCreateTo;
import com.github.CCmall.order.to.SpuInfoTo;
import com.github.CCmall.order.vo.*;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private PayCallBackHandler payCallBackHandler;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {


    }

    @Override
    @SneakyThrows
    public OrderConfirmVo confirmOrder() {

        MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();;

        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> fu1 = CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> checkedItems = (List<OrderItemVo>) cartFeignService.getCheckedItems();
            orderConfirmVo.setItems(checkedItems);

            return checkedItems;
        }, executor).thenAcceptAsync(item -> {
            List<Long> collect = item.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            Map<Long, Boolean> hasStockMap = wareFeignService.getSkuHasStocks(collect).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
            confirmVo.setStocks(hasStockMap);
        }, executor);

        CompletableFuture<Void> fu2 = CompletableFuture.runAsync(() -> {
            List<MemberAddressVo> addressByUserId = (List<MemberAddressVo>) memberFeignService.getAddressByUserId(memberResponseVo.getId());

            orderConfirmVo.setMemberAddressVos(addressByUserId);


        }, executor);

        //3. 积分
        confirmVo.setIntegration(memberResponseVo.getIntegration());

        CompletableFuture.allOf(fu1,fu2).get();

        //5. 总价自动计算
        //6. 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);
        try {
            CompletableFuture.allOf(itemAndStockFuture, addressFuture).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return confirmVo;







                }

        )


    }

    @Override
    @Transactional
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        //1. 验证防重令牌
        MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long execute = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), submitVo.getOrderToken());
        if (execute == 0L) {
            //1.1 防重令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            //2. 创建订单、订单项
            OrderCreateTo order = createOrderTo(memberResponseVo, submitVo);
            //3. 验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //4. 保存订单
                saveOrder(order);
                //5. 锁定库存
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    return orderItemVo;
                }).collect(Collectors.toList());
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                lockVo.setLocks(orderItemVos);
                R r = wareFeignService.orderLockStock(lockVo);
                //5.1 锁定库存成功
                if (r.getCode()==0){
//                    int i = 10 / 0;
                    responseVo.setOrder(order.getOrder());
                    responseVo.setCode(0);

                    //发送消息到订单延迟队列，判断过期订单
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());

                    //清除购物车记录
                    BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(CartConstant.CART_PREFIX + memberResponseVo.getId());
                    for (OrderItemEntity orderItem : order.getOrderItems()) {
                        ops.delete(orderItem.getSkuId().toString());
                    }
                    return responseVo;
                }else {
                    //5.1 锁定库存失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
        }else {
                //验价失败
                responseVo.setCode(2);
                return responseVo;
            }
    }
}

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return null;
    }

        /**
         * 实现关闭延时订单
         */
    @Override
    public void closeOrder(OrderEntity orderEntity) {

        OrderEntity newOrderEntity;

        if(newOrderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.SERVICING.getCode()) {
            OrderEntity order = new OrderEntity();
            order.setId(newOrderEntity.getId());
            order.setOrderSn(OrderStatusEnum.CANCLED.getCode());
            this.updateById(order);

            //解锁库存 异步处理
        }



        //发送延时信息
    }

    @Override
    public PageUtils getMemberOrderPage(Map<String, Object> params) {
        return null;
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {

    }

    @Override
    PayVo getOrderPay(String orderSn) {
        return null;
    }


private void compute(OrderEntity entity, List<OrderItemEntity> orderItemEntities) {
    //总价
    BigDecimal total = BigDecimal.ZERO;
    //优惠价格
    BigDecimal promotion=new BigDecimal("0.0");
    BigDecimal integration=new BigDecimal("0.0");
    BigDecimal coupon=new BigDecimal("0.0");
    //积分
    Integer integrationTotal = 0;
    Integer growthTotal = 0;

    for (OrderItemEntity orderItemEntity : orderItemEntities) {
        total=total.add(orderItemEntity.getRealAmount());
        promotion=promotion.add(orderItemEntity.getPromotionAmount());
        integration=integration.add(orderItemEntity.getIntegrationAmount());
        coupon=coupon.add(orderItemEntity.getCouponAmount());
        integrationTotal += orderItemEntity.getGiftIntegration();
        growthTotal += orderItemEntity.getGiftGrowth();
    }

    entity.setTotalAmount(total);
    entity.setPromotionAmount(promotion);
    entity.setIntegrationAmount(integration);
    entity.setCouponAmount(coupon);
    entity.setIntegration(integrationTotal);
    entity.setGrowth(growthTotal);

    //付款价格=商品价格+运费
    entity.setPayAmount(entity.getFreightAmount().add(total));

    //设置删除状态(0-未删除，1-已删除)
    entity.setDeleteStatus(0);
}

private List<OrderItemEntity> buildOrderItems(String orderSn) {
    List<OrderItemVo> checkedItems = cartFeignService.getCheckedItems();
    List<OrderItemEntity> orderItemEntities = checkedItems.stream().map((item) -> {
        OrderItemEntity orderItemEntity = buildOrderItem(item);
        //1) 设置订单号
        orderItemEntity.setOrderSn(orderSn);
        return orderItemEntity;
    }).collect(Collectors.toList());
    return orderItemEntities;
}

private OrderItemEntity buildOrderItem(OrderItemVo item) {
    OrderItemEntity orderItemEntity = new OrderItemEntity();
    Long skuId = item.getSkuId();
    //2) 设置sku相关属性
    orderItemEntity.setSkuId(skuId);
    orderItemEntity.setSkuName(item.getTitle());
    orderItemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(item.getSkuAttrValues(), ";"));
    orderItemEntity.setSkuPic(item.getImage());
    orderItemEntity.setSkuPrice(item.getPrice());
    orderItemEntity.setSkuQuantity(item.getCount());
    //3) 通过skuId查询spu相关属性并设置
    R r = productFeignService.getSpuBySkuId(skuId);
    if (r.getCode() == 0) {
        SpuInfoTo spuInfo = r.getData(new TypeReference<SpuInfoTo>() {
        });
        orderItemEntity.setSpuId(spuInfo.getId());
        orderItemEntity.setSpuName(spuInfo.getSpuName());
        orderItemEntity.setSpuBrand(spuInfo.getBrandName());
        orderItemEntity.setCategoryId(spuInfo.getCatalogId());
    }
    //4) 商品的优惠信息(不做)

    //5) 商品的积分成长，为价格x数量
    orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
    orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

    //6) 订单项订单价格信息
    orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
    orderItemEntity.setCouponAmount(BigDecimal.ZERO);
    orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

    //实际需要请求优惠卷服务
    //计算出最优惠价格
    //且查询所有优惠卷

    //使用hash表实现存储‘
    // 计算最佳组合后的价格


    //7) 实际价格
    BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
    BigDecimal realPrice = origin.subtract(orderItemEntity.getPromotionAmount())
            .subtract(orderItemEntity.getCouponAmount())
            .subtract(orderItemEntity.getIntegrationAmount());
    orderItemEntity.setRealAmount(realPrice);

    return orderItemEntity;



    private OrderEntity buildOrder(MemberResponseVo memberResponseVo, OrderSubmitVo submitVo, String orderSn) {

        OrderEntity orderEntity =new OrderEntity();

        orderEntity.setOrderSn(orderSn);

        //2) 设置用户信息
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setMemberUsername(memberResponseVo.getUsername());

        //3) 获取邮费和收件人信息并设置
        FareVo fareVo = wareFeignService.getFare(submitVo.getAddrId());
        BigDecimal fare = fareVo.getFare();
        orderEntity.setFreightAmount(fare);
        MemberAddressVo address = fareVo.getAddress();
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        //4) 设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setConfirmStatus(0);
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }
}

    private class OrderItemService {
    }

    private class PaymentInfoService {
    }
}
