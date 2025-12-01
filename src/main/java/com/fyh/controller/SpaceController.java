package com.fyh.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fyh.annotation.AuthCheck;
import com.fyh.common.BaseResponse;
import com.fyh.common.DeleteRequest;
import com.fyh.common.ResultUtils;
import com.fyh.constant.UserConstant;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.dto.space.SpaceAddRequest;
import com.fyh.model.dto.space.SpaceEditRequest;
import com.fyh.model.dto.space.SpaceQueryRequest;
import com.fyh.model.dto.space.SpaceUpdateRequest;
import com.fyh.model.entity.Space;
import com.fyh.model.entity.User;
import com.fyh.model.enums.SpaceLevelEnum;
import com.fyh.model.vo.SpaceLevel;
import com.fyh.model.vo.SpaceVO;
import com.fyh.service.SpaceService;
import com.fyh.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/space")
@RestController
@Slf4j
@Api(tags = "空间模块")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;


    @PostMapping("add")
    @ApiOperation("添加空间")
    public BaseResponse<Long > addSpace(@RequestBody SpaceAddRequest spaceAddRequest,HttpServletRequest request)
    {
        ThrowUtils.throwIf(spaceAddRequest==null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long result = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(result, "空间创建成功");
    }


    @PostMapping("update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("更新空间")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest)
    {
        if(spaceUpdateRequest==null||spaceUpdateRequest.getId()==null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将实体类与DTO转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);


        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        //校验
        spaceService.validSpace(space,false);
        //判断是否存在空间
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true,"更新成功");
    }

    @PostMapping("delete")
    @ApiOperation("删除空间")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request)
    {
        if(deleteRequest == null || deleteRequest.getId() <= 0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        boolean result = spaceService.deleteSpaceAndPicturesById(id, loginUser);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"删除失败");
        return ResultUtils.success(true);
    }

    @GetMapping("get")
    @ApiOperation("根据id获取空间（仅管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request)
    {
        if(id<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //操作数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        return ResultUtils.success(space, "后台已提交,任务执行中！");
    }

    @GetMapping("get/vo")
    @ApiOperation("根据id获取空间VO")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request)
    {
        if(id<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        return ResultUtils.success(SpaceVO.objToVo(space), "后台已提交,任务执行中！");

    }

    /**
     * 分页获取空间列表（管理员）
     */
    @PostMapping("list/page")
    @ApiOperation("分页获取空间列表（管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request)
    {
        if(spaceQueryRequest==null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        //操作数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage, "后台已提交,任务执行中！");
    }
    /**
     * 分页获取空间列表（用户）
     */
    @PostMapping("list/page/vo")
    @ApiOperation("分页获取空间列表（用户）")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                             HttpServletRequest request)
    {
        if(spaceQueryRequest==null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();


        //操作数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request), "后台已提交,任务执行中！");
    }

    //编辑空间（用户使用）
    @PostMapping("edit")
    @ApiOperation("编辑空间")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request)
    {
        if(spaceEditRequest==null||spaceEditRequest.getId()<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将实体类和DTO转化

        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest,space);

        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        //设置编辑时间
        space.setEditTime(new Date());
        //数据校验
        spaceService.validSpace(space,false);
        User loginUser = userService.getLoginUser(request);
        //判断是否存在
        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        //仅本人或管理员可以编辑
        if(!oldSpace.getUserId().equals(loginUser.getId())&&!userService.isAdmin(loginUser))
        {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true, "后台已提交,任务执行中！");
    }
    @GetMapping("/list/level")
    @ApiOperation("获取空间等级列表")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }


}
