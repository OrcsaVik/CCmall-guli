package com.github.CCmall.order.feign;

import io.niceseason.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {
    @RequestMapping("product/spuinfo/skuId/{skuId}")
    R getSpuBySkuId(@PathVariable("skuId") Long skuId);

    @RequestMapping("product/skuinfo/info/{skuId}")
    R info(@P
athVariable("skuId") Long skuId);
}