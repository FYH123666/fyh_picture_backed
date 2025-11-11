package com.fyh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.dto.user.UserQueryRequest;
import com.fyh.model.entity.User;
import com.fyh.model.enums.UserRoleEnum;
import com.fyh.model.vo.LoginUserVO;
import com.fyh.model.vo.UserVO;
import com.fyh.service.UserService;
import com.fyh.mapper.UserMapper;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 获取脱敏的userVO
     * @param user
     * @return
     */
    public UserVO getUserVO(User user)
    {
        if(user==null){
            return null;
        }
        UserVO userVO=new UserVO();
        BeanUtil.copyProperties(user,userVO);
        return userVO;
    }
    /**
     * 获取脱敏的userVOList
     * @param userList
     * @return
     */
    public List<UserVO> getUserVOList(List<User> userList )
    {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     * 将查询请求转化为QueryWrapper对象
     * @param userQueryRequest
     * @return
     */
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest)
    {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id=userQueryRequest.getId();
        String userName=userQueryRequest.getUserName();
        String userAccount=userQueryRequest.getUserAccount();
        String userProfile=userQueryRequest.getUserProfile();
        String userRole=userQueryRequest.getUserRole();
        String sortField=userQueryRequest.getSortField();
        String sortOrder=userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;

    }


    /**
     * 判断是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user!=null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}





