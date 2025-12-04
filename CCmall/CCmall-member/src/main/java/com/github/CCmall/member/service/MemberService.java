package com.github.CCmall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.CCmall.framework.utils.PageUtils;
import com.github.CCmall.member.entity.MemberEntity;
import com.github.CCmall.member.vo.MemberLoginVo;
import com.github.CCmall.member.vo.MemberRegisterVo;
import com.github.CCmall.member.vo.SocialUser;

import java.util.Map;

public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVo registerVo);

    MemberEntity login(MemberLoginVo loginVo);

    MemberEntity login(SocialUser socialUser);

    MemberEntity getMemberById(String memberId);

}

