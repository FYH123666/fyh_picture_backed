package com.fyh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.manager.FileManager;
import com.fyh.model.dto.file.UploadPictureResult;
import com.fyh.model.dto.picture.PictureQueryRequest;
import com.fyh.model.dto.picture.PictureReviewRequest;
import com.fyh.model.dto.picture.PictureUploadRequest;
import com.fyh.model.entity.Picture;
import com.fyh.model.entity.User;
import com.fyh.model.enums.PictureReviewStatusEnum;
import com.fyh.model.vo.PictureVO;
import com.fyh.model.vo.UserVO;
import com.fyh.service.PictureService;
import com.fyh.mapper.PictureMapper;
import com.fyh.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 16193
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-11-10 14:40:13
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    private final FileManager fileManager;
    private final UserService userService;


    public PictureServiceImpl(FileManager fileManager, UserService userService) {
        this.fileManager = fileManager;
        this.userService = userService;

    }

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NO_AUTH_ERROR);
        //用于判断是新增还是更新图片
        Long pictureId = null;
        if(pictureUploadRequest!=null)
        {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新图片，需要判断图片是否存在
        if(pictureId!=null)
        {

            Picture oldPicture=this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
            //仅本人或管理员才允许更新
            if(!loginUser.getId().equals(oldPicture.getUserId()) && !userService.isAdmin(loginUser))
            {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        //上传图片，得到信息
        //根据用户Id划分目录

        String uploadPathPrefix = String.format("public/%d", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //补充审查参数
        this.fileReviewParams(picture,loginUser);

        //如果pictureId不为空，则更新图片，否则是新增
        if(pictureId!=null)
        {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败");
        return PictureVO.objToVo(picture);

    }

    /**
     * 分页查询 图片
     * @param pictureQueryRequest
     *
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if(pictureQueryRequest==null){
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortOrder = pictureQueryRequest.getSortOrder();
        String sortField = pictureQueryRequest.getSortField();
        //从多字段中搜索
        if(StrUtil.isNotBlank(searchText))
        {
            //拼接查询条件
            queryWrapper.and(qw -> qw.like("name",searchText)
                    .or()
                    .like("introduction",searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId",userId);
        queryWrapper.like(StrUtil.isNotBlank(name),"name",name);
        queryWrapper.like(StrUtil.isNotBlank(introduction),"introduction",introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category),"category",category);
        queryWrapper.like(StrUtil.isNotBlank(picFormat),"picFormat",picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize),"picSize",picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth),"picWidth",picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight),"picHeight",picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale),"picScale",picScale);

        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        //标签是JSON数组，特殊处理
        if(CollUtil.isNotEmpty(tags))
        {
            for(String tag:tags)
            {
                queryWrapper.like("tags","\""+tag +"\"");
            }
        }
        queryWrapper.orderBy(
                StrUtil.isNotBlank(sortField),  // 条件：是否排序
                sortOrder.equals("ascend"),     // 排序方向：true=升序，false=降序
                sortField                       // 排序字段
        );//ascend升序的意思，对应sql中asc
        return queryWrapper;
    }


    /**
     * 获取单个图片封装类
     * @param request
     * @return
     */

    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //获取用户信息
        Long userId = picture.getId();
        if(userId!=null && userId>0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }


    /**
     * 获取图片分页封装类
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList=picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if(CollUtil.isEmpty(pictureList))
        {
            return pictureVOPage;
        }
        //对象列表--->封装类列表
        List<PictureVO> pictureVOList=pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());


        //关联查询用户信息
        Set<Long> userIdSet=pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long,List< User>> userIdUserMap=userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId=pictureVO.getUserId();
            User user=null;
            if(userIdUserMap.containsKey(userId))
            {
                user=userIdUserMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));

        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    public void ValidPicture(Picture picture)
    {
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR);
        String introduction=picture.getIntroduction();
        Long id=picture.getId();
        String url=picture.getUrl();
        ThrowUtils.throwIf(ObjUtil.isNull( id),ErrorCode.PARAMS_ERROR,"图片id不能为空");
        if(StrUtil.isNotBlank( introduction))
        {
            ThrowUtils.throwIf(introduction.length()>800,ErrorCode.PARAMS_ERROR,"简介过长");

        }
        if(StrUtil.isNotBlank( url))
        {
            ThrowUtils.throwIf(url.length()>1024,ErrorCode.PARAMS_ERROR,"图片地址过长");
        }
    }


    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    public void doPictureReview(PictureReviewRequest pictureReviewRequest ,User loginUser)
    {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if(id==null||pictureReviewStatusEnum==null||PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum))
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR);
        //已经是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus))
        {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"图片已审核,无需重复操作");
        }
        //执行审核操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest,updatePicture);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);


    }

    /**
     * 文件审核参数
     * @param picture
     * @param loginUser
     */
    public void fileReviewParams(Picture  picture,User loginUser)
    {
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR);
        if(userService.isAdmin(loginUser))
        {
            //管理员自动 通过
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动通过");

        }
        else {
            //普通用户 创建和编辑-->待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());

        }
    }
}




