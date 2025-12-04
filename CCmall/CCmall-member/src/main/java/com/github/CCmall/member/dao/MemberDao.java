package com.github.CCmall.member.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.CCmall.member.entity.MemberEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author Ethan
 * @email hongshengmo@163.com
 * @date 2020-05-27 23:01:00
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
