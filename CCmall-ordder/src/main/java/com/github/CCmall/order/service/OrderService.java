package com.github.CCmall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.CCmall.framework.to.mq.SeckillOrderTo;
import com.github.CCmall.framework.utils.PageUtils;
import com.github.CCmall.order.entity.OrderEntity;
import com.github.CCmall.order.vo.*;

import java.util.Map;

public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder();

    SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    PageUtils getMemberOrderPage(Map<String, Object> params);

    void createSeckillOrder(SeckillOrderTo orderTo);

    /// -----------------------------------------------

    PayVo getOrderPay(String orderSn);

    void handlerPayResult(PayAsyncVo payAsyncVo);

}

