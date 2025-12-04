package com.github.CCmall.member.controlller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.CCmall.framework.exception.RRException;
import com.github.CCmall.framework.utils.R;
import com.github.CCmall.member.entity.MemberEntity;
import com.github.CCmall.member.model.dto.MemberCouponsDTO;
import com.github.CCmall.member.rpc.CouponFeginRpcService;
import com.github.CCmall.member.service.MemberService;
import com.github.CCmall.member.vo.MemberLoginVo;
import com.github.CCmall.member.vo.MemberRegisterVo;
import com.github.CCmall.member.vo.SocialUser;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author Vik
 * @date 2025-12-03
 * @description
 */
@RestController
@RequestMapping("/member")
@AllArgsConstructor
public class MemberController {

    private final MemberService memberService;

    private final CouponFeginRpcService couponFeginRpcService;

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVo) {

        MemberEntity result = memberService.login(memberLoginVo);

        if(Objects.nonNull(result)) {
            return R.ok().put("memberEntity", result);
        }

        return R.error().put("501", "登录失败");

    }

    @PostMapping("/oauth2/login")
    public R login(@RequestBody SocialUser requestParam) {

        MemberEntity result = memberService.login(requestParam);

        if(Objects.nonNull(result)) {
            return R.ok().put("memberEntity", result);
        }

        return R.error().put("502", "oauth2登录失败");
    }



    @GetMapping("/coupons/{memberId}")
    public R getMemberCoupons(@PathVariable("memberId") String memberId) {

        MemberEntity member = memberService.getMemberById(memberId);
        if(Objects.isNull(member)) {
            throw new RRException("不存在该用户");
        }

        R r = couponFeginRpcService.memberCouponsById(Long.valueOf(memberId));

        if(r.getCode() != 0 || r.get("data") == null) {
            throw new RRException("获取优惠券失败");
        }

        MemberCouponsDTO memberCouponsDTO = r.getData(new TypeReference<MemberCouponsDTO>() {
        });


        return R.ok().put("memberEntity", member)
                .put("coupons", JSON.toJSONString(memberCouponsDTO.getCoupons()));



    }


    /**
     * 针对表单实现注册
     * @param memberRegisterVo
     */
    @PostMapping("/member/register")
    public void register( MemberRegisterVo memberRegisterVo) {
        if(memberRegisterVo == null || memberRegisterVo.getPassword() == null || memberRegisterVo.getUserName() == null) {
            throw new RRException("WRONG- register");
        }
        memberService.register(memberRegisterVo);
    }


}



