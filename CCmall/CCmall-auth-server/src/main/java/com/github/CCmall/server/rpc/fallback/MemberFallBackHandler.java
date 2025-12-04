package com.github.CCmall.server.rpc.fallback;

import com.github.CCmall.framework.exception.BizCodeEnum;
import com.github.CCmall.framework.utils.R;
import com.github.CCmall.server.model.vo.UserRespVO;
import com.github.CCmall.server.rpc.MemberFeignService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Vik
 * @date 2025-12-02
 * @description
 */
@Component
public class MemberFallBackHandler implements MemberFeignService {



    @Override
    public R register(UserRegisterVo registerVo) {

        return R.error(BizCodeEnum.READ_TIME_OUT_EXCEPTION.getCode(),  BizCodeEnum.READ_TIME_OUT_EXCEPTION.getMsg());


    }

    @Override
    public R login(UserLoginVo loginVo) {
        return R.error(BizCodeEnum.READ_TIME_OUT_EXCEPTION.getCode(),  BizCodeEnum.READ_TIME_OUT_EXCEPTION.getMsg());

    }

    @Override
    public R login(UserRespVO socialUser) {
        printInfo();
        return R.error(BizCodeEnum.READ_TIME_OUT_EXCEPTION.getCode(),  BizCodeEnum.READ_TIME_OUT_EXCEPTION.getMsg());


    }


}
