package com.github.CCmall.server.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.CCmall.framework.constant.AuthServerConstant;
import com.github.CCmall.framework.exception.RRException;
import com.github.CCmall.framework.utils.R;
import com.github.CCmall.framework.vo.MemberResponseVo;
import com.github.CCmall.server.model.vo.UserLoginVo;
import com.github.CCmall.server.model.vo.UserRegisterVo;
import com.github.CCmall.server.rpc.MemberFeignService;
import com.github.CCmall.server.rpc.ThirdPartFeignService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Vik
 * @date 2025-12-02
 * @description
 */
@Controller
@AllArgsConstructor
public class AuthLoginController {

    private ThirdPartFeignService thirdPartFeignService;

    private StringRedisTemplate stringRedisTemplate;

    private MemberFeignService memberFeignService;


    @RequestMapping("/login.html")
    public String loginPage(HttpSession session) {
        if (session.getAttribute(AuthServerConstant.LOGIN_USER) != null) {
            return "redirect:http://" + AuthServerConstant.DOMAIN_BASE;
        } else {
            return "login";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo requestParams, RedirectAttributes redirectAttributes, HttpSession session) {
        R login = memberFeignService.login(requestParams);

        if (login.getCode() == 0) {
            String jsonStr = JSON.toJSONString(login.get("memberEntity"));
            MemberResponseVo memberResponseVo = JSON.parseObject(jsonStr, new TypeReference<MemberResponseVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, memberResponseVo);

            return "redirect:http://" + AuthServerConstant.DOMAIN_BASE;
        } else {
            String msg = (String) login.get("msg");
            HashMap<Object, Object> errors = new HashMap<>();
            errors.put("msg", msg);
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://" + AuthServerConstant.DOMAIN_BASE + "/login.html";
        }
    }

    @PostMapping("/sms/code")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        String s = AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone;

        //检查是否过期
        Boolean b = stringRedisTemplate.hasKey(s);
        if (b) {
            throw new RRException("验证码发送过于频繁");
        }

        String code = RandomUtil.randomNumbers(6);

        stringRedisTemplate.opsForValue().set(s, code, 1, TimeUnit.MINUTES);


        thirdPartFeignService.sendCode(phone, code);

        return R.ok();

    }


    //注册逻辑处理
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo userRegisterVo, BindingResult result, RedirectAttributes redirectAttributes) {

        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        //检验处理 可以使用全局异常捕获
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(each -> {
                objectObjectHashMap.put(each.getField(), each.getDefaultMessage());
                redirectAttributes.addFlashAttribute("errors", objectObjectHashMap);
            });
            return "redirect:http://auth.cc.com/reg.html";
        } else {
            String code = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegisterVo.getPhone());
            if(StringUtils.isNotEmpty(code) && userRegisterVo.getCode().equals(code)) {
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegisterVo.getPhone());
                try {
                    R r = memberFeignService.register(userRegisterVo);
                    if(r.getCode() == 0){
                       return "redirect:http://auth.cc.com/login.html";
                    } else {
                        objectObjectHashMap.put("msg", r.get("msg"));
                        redirectAttributes.addFlashAttribute("errors", objectObjectHashMap);
                        return "redirect:http://auth.cc.com/reg.html";
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }else{
                objectObjectHashMap.put("msg", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", objectObjectHashMap);
                return "redirect:http://auth.cc.com/reg.html";
            }
        }

    }


}
