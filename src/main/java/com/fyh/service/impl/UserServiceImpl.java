package com.fyh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.User;
import com.fyh.model.enums.UserRoleEnum;
import com.fyh.model.vo.LoginUserVO;
import com.fyh.service.UserService;
import com.fyh.mapper.UserMapper;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.fyh.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 16193
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-11-04 10:56:27
*/
@Service
@Data
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{


    public long userRegister(String userAccount, String userPassword, String checkPassword) {
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
        long cout = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(cout>0, ErrorCode.OPERATION_ERROR, "用户账号重复");
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败,数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1. 校验参数为空
        ThrowUtils.throwIf(userAccount == null || userPassword == null, ErrorCode.PARAMS_ERROR, "参数为空");
        //2. 校验账户长度不小于4位
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号错误");
        //3. 校验密码长度不小于8位
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码错误");
        //加密
        String encryptPassword = getEncryptPassword(userPassword);
        //4. 查询用户数据库是否存在
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        //5. 用户不存在
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        //6.记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);

    }


    /**
     * 获取脱敏的已登录用户信息
     * @param user
     * @return
     */
    public LoginUserVO getLoginUserVO(User user)
    {
        if(user==null)
        {
            return null;
        }
        LoginUserVO loginUserVO=new LoginUserVO();
        BeanUtils.copyProperties(user,loginUserVO);
        return loginUserVO;
    }




    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "fyh";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }



    /**
     * 用户注销
     * @param request
     * @return
     */
    public boolean userLogout(HttpServletRequest request) {
        //判断是否登录
        Object Obj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user=(User) Obj;
        if(user==null)
        {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //移除登录态
        request.removeAttribute(USER_LOGIN_STATE);
        return true;
    }

}





