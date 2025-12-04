package com.github.CCmall.member.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.CCmall.framework.utils.PageUtils;
import com.github.CCmall.framework.utils.Query;
import com.github.CCmall.member.dao.MemberLevelDao;
import com.github.CCmall.member.entity.MemberLevelEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class MemberLevelService  {

    private final MemberLevelDao memberLevelDao;
    PageUtils queryPage(Map<String, Object> params){

        IPage<MemberLevelEntity> memberLevelEntityIPage = memberLevelDao.selectPage(
                new Query<MemberLevelEntity>().getPage(params),
                new QueryWrapper<MemberLevelEntity>()
        );

        return new PageUtils(memberLevelEntityIPage);

    }

    public MemberLevelEntity getOne() {
        MemberLevelEntity result = memberLevelDao.selectOne(new QueryWrapper<MemberLevelEntity>()
                .eq("default_status", 1));
        return result;

    }
}

