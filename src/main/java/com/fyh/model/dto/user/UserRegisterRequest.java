package com.fyh.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {


    private static final long serialVersionUID = 124712648124L;
    /**
     * 用户账户
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 校验密码
     */
    private String checkPassword;
}
