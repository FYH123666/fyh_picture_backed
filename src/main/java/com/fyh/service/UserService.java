package com.fyh.service;

import com.fyh.model.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 16193
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-04 10:56:27
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密密码
     *
     * @param userPassword
     * @return
     */
    public String getEncryptPassword(String userPassword);
}
