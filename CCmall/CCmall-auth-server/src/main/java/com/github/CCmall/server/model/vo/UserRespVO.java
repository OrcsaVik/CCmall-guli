package com.github.CCmall.server.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vik
 * @date 2025-12-02
 * @description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRespVO {

    private String token;

    private String remind_in;

    private Long expires;

    private String uid;

    private String isRealName;
}
