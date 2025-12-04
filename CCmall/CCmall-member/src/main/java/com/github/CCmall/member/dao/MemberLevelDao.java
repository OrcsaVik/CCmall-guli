package com.github.CCmall.member.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.CCmall.member.entity.MemberLevelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级
 * 
 * @author Ethan
 * @email hongshengmo@163.com
 * @date 2020-05-27 23:01:00
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {
	
}
