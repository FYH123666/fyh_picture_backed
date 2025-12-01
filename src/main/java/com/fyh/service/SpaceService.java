package com.fyh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fyh.model.dto.space.SpaceAddRequest;
import com.fyh.model.dto.space.SpaceQueryRequest;
import com.fyh.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fyh.model.entity.User;
import com.fyh.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 16193
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-11-26 16:29:16
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间数据
     *
     * @param space
     * @param add   是新增操作还是修改操作
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别填充空间数据
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 将分页空间实体转换为分页空间视图
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 将空间实体转换为空间视图
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间查询包装器
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 添加空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
    /**
     * 根据id删除空间及其相关图片
     *
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteSpaceAndPicturesById(long id,User loginUser);
}
