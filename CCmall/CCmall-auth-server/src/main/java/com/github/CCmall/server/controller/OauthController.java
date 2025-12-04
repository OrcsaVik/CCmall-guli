package com.github.CCmall.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.github.CCmall.framework.constant.AuthServerConstant;
import com.github.CCmall.framework.utils.HttpUtils;
import com.github.CCmall.framework.utils.R;
import com.github.CCmall.framework.vo.MemberResponseVo;
import com.github.CCmall.server.model.vo.UserRespVO;
import com.github.CCmall.server.rpc.MemberFeignService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vik
 * @date 2025-12-02
 * @description
 */
@Controller
@AllArgsConstructor
public class OauthController {

    private final MemberFeignService memberFeignService;
    private final View error;

    @PostMapping("/oauth2/weibo/callback")
    @SneakyThrows
    public String oatuh2(String code, HttpSession session){
        Map<String, String> query = new HashMap<>();
        query.put("client_id", "2144471074");
        query.put("client_secret", "ff63a0d8d591a85a29a19492817316ab");
        query.put("grant_type", "authorization_code");
        query.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        query.put("code", code);

        //唤醒token 且获取需要的个人信息进行注册
        HttpResponse post = HttpUtils.doPost("http://api.weibo.com", "/oauth2/access_token", "post",
                new HashMap<>(), query, new HashMap<>());

        Map<String, String> errors = new HashMap<>();

        //验证响应数据
        if(post.getStatusLine().getStatusCode() == 200) {
            String body = EntityUtils.toString(post.getEntity());
            UserRespVO userRespVO = JSON.parseObject(body, new TypeReference<UserRespVO>(){});
            R login = memberFeignService.login(userRespVO);
            //实现登陆 请求sso服务器

            if(login.getCode() == 0) {
                String jsonString = JSON.toJSONString(login.get("memberEntity"));
                System.out.println("----------------"+jsonString);
                MemberResponseVo memberResponseVo = JSON.parseObject(jsonString, new TypeReference<MemberResponseVo>() {
                });
                System.out.println("----------------"+memberResponseVo);
                session.setAttribute(AuthServerConstant.LOGIN_USER, memberResponseVo);
                return "redirect:http://gulimall.com";
            }else {
                //2.2 否则返回登录页
                errors.put("msg", "登录失败，请重试");
                session.setAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/login.html";

            }

        }else{
            errors.put("msg", "登录失败，请重试");
            session.setAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }



    }
}
