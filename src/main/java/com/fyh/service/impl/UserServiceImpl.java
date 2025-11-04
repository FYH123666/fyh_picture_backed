package com.fyh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.User;
import com.fyh.model.enums.UserRoleEnum;
import com.fyh.service.UserService;
import com.fyh.mapper.UserMapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
* @author 16193
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-11-04 10:56:27
*/
@Service
@Data
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数为空
        ThrowUtils.throwIf(userAccount == null || userPassword == null || checkPassword == null, ErrorCode.PARAMS_ERROR, "参数为空");
        // 2. 校验账户长度不小于4位
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        // 3. 校验密码长度不小于8位
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        // 4. 校验密码和校验密码相同
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "密码和校验密码不一致");
        // 5. 校验账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long cout = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(cout>0.0, ErrorCode.OPERATION_ERROR, "用户账号重复");
        // 6. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 7. 插入数据
        User user=new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("翠花");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }
        return user.getId();
    }


    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "fyh";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

}





