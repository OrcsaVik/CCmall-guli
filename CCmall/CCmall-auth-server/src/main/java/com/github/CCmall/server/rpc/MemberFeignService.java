package com.github.CCmall.server.rpc;

import com.github.CCmall.framework.utils.R;
import com.github.CCmall.server.model.vo.UserLoginVo;
import com.github.CCmall.server.model.vo.UserRegisterVo;
import com.github.CCmall.server.model.vo.UserRespVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 远程调用用户服务
 */
@FeignClient(value = "gulimall-member",fallback = MemberFallbackService.class)
public interface MemberFeignService {

    @Value("'你好'")
    String msg = "";

    @RequestMapping("member/member/register")
    R register(@RequestBody UserRegisterVo registerVo);


    @RequestMapping("member/member/login")
     R login(@RequestBody UserLoginVo loginVo);

    @RequestMapping("member/member/oauth2/login")
    R login(@RequestBody UserRespVO socialUser);


    default void printInfo(){
        System.out.println("info is printing");
    }
}
