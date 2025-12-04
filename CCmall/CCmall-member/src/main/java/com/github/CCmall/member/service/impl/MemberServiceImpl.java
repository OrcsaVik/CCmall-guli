package com.github.CCmall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.CCmall.framework.exception.RRException;
import com.github.CCmall.framework.utils.HttpUtils;
import com.github.CCmall.framework.utils.PageUtils;
import com.github.CCmall.framework.utils.Query;
import com.github.CCmall.member.dao.MemberDao;
import com.github.CCmall.member.entity.MemberEntity;
import com.github.CCmall.member.entity.MemberLevelEntity;
import com.github.CCmall.member.service.MemberLevelService;
import com.github.CCmall.member.service.MemberService;
import com.github.CCmall.member.vo.MemberLoginVo;
import com.github.CCmall.member.vo.MemberRegisterVo;
import com.github.CCmall.member.vo.SocialUser;
import lombok.AllArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Vik
 * @date 2025-12-03
 * @description
 */
@Service
@AllArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity>implements MemberService {


    private final MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);


    }

    @Override
    public void register(MemberRegisterVo registerVo) {
        MemberEntity entity = MemberEntity.builder()
                .username(registerVo.getUserName())
                .mobile(registerVo.getPhone())
                .createTime(new Date())
                .build();

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        String encode = bCryptPasswordEncoder.encode(registerVo.getPassword());
        entity.setPassword(encode);

        MemberLevelEntity one = memberLevelService.getOne();

        entity.setLevelId(one.getId());

        this.save(entity);

    }

    @Override
    public MemberEntity login(MemberLoginVo loginVo) {
        MemberEntity result = this.getOne(new QueryWrapper<MemberEntity>().eq("account", loginVo.getLoginAccount()));

        if(Objects.isNull(result)) {
            throw new RRException("login failure for member");
        }

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        boolean matches = bCryptPasswordEncoder.matches(loginVo.getPassword(), result.getPassword());
        if(!matches){
            throw new RRException("login failure for member is to password wrong");
        }
        result.setPassword("");
        return result;

    }

    /**
     * 实现第三方社交登陆
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity login(SocialUser socialUser) {
        QueryWrapper<MemberEntity> queryWrapper = new QueryWrapper<MemberEntity>().eq("uid", socialUser.getUid());
        MemberEntity entity = this.baseMapper.selectOne(queryWrapper);

        if(Objects.isNull(entity)) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("access_token", socialUser.getAccess_token());
            hashMap.put("uid", socialUser.getUid());

            String jsonStr = null;
              try {
                  HttpResponse get = HttpUtils.doGet("https://api.weibo.com", "oauth2/user/show.json", "get", new HashMap<>(), hashMap);

                  jsonStr = EntityUtils.toString(get.getEntity());
              } catch (Exception e) {
                    log.error("", e);
                    throw (RuntimeException) e;
                }

            JSONObject jsonObject = JSON.parseObject(jsonStr);
            String name = jsonObject.getString("name");
            String gender = jsonObject.getString("gender");
            String profile_url = jsonObject.getString("profile_url");

            MemberEntity loginResult = MemberEntity.builder()
                    .levelId(memberLevelService.getOne().getId())
                    .nickname(name)
                    .gender(Objects.equals("m", gender) ? 0 : 1)
                    .header(profile_url)
                    .accessToken(socialUser.getAccess_token())
                    .uid(socialUser.getUid())
                    .expiresIn(socialUser.getExpires_in())
                    .build();

            this.save(loginResult);



        }else{

            //数据库存在之前uid用户
            entity.setAccessToken(socialUser.getAccess_token());
            entity.setExpiresIn(socialUser.getExpires_in());
            this.baseMapper.update(entity, new UpdateWrapper<MemberEntity>());
        }

        return entity;

    }

    @Override
    public MemberEntity getMemberById(String memberId) {
        return this.baseMapper.selectById(memberId) != null ? this.baseMapper.selectById(memberId) : null;
    }
}
