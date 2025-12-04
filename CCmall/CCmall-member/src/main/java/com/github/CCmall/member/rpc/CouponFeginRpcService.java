package com.github.CCmall.member.rpc;

import com.github.CCmall.framework.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Vik
 * @date 2025-12-03
 * @description
 */
@FeignClient("CCmall-coupon")
public interface CouponFeginRpcService {


    @RequestMapping("/coupon/coupon/member/list")
    R memberCouponsById(Long memberId);
}
