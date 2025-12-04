package com.github.CCmall.member.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author Vik
 * @date 2025-12-03
 * @description
 */
@Data
public class MemberCouponsDTO {

    private List<String> coupons;

    private Long userId;

}
