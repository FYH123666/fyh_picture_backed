package com.fyh.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fyh.annotation.AuthCheck;
import com.fyh.api.imagesearch.ImageSearchApiFacade;
import com.fyh.api.imagesearch.model.ImageSearchResult;
import com.fyh.common.BaseResponse;
import com.fyh.common.DeleteRequest;
import com.fyh.common.ResultUtils;
import com.fyh.constant.UserConstant;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.dto.picture.*;
import com.fyh.model.dto.searchPicture.SearchPictureByPictureRequest;
import com.fyh.model.entity.Picture;
import com.fyh.model.entity.Space;
import com.fyh.model.entity.User;
import com.fyh.model.enums.PictureReviewStatusEnum;
import com.fyh.model.vo.PictureTagCategory;
import com.fyh.model.vo.PictureVO;
import com.fyh.service.PictureService;
import com.fyh.service.SpaceService;
import com.fyh.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequestMapping("/picture")
@RestController
@Slf4j
@Api(tags = "图片接口")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 本地缓存构造
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();
    @Autowired
    private SpaceService spaceService;

    @PostMapping("/upload")
    @ApiOperation("上传图片")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request)
    {
        User loginUser = userService.getLoginUser(request);

        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO, "后台已提交,任务执行中！");
    }

    @PostMapping("delete")
    @ApiOperation("删除图片")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request)
    {
        if(deleteRequest == null || deleteRequest.getId() <= 0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
//        // 判断是否存在照片
//        Picture picture = pictureService.getById(id);
//        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "照片不存在");
//        // 仅本人或管理员可删除
//        if(!picture.getUserId().equals(loginUser.getId()) &&!userService.isAdmin(loginUser))
//        {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
//        }
//        //操作操作系统
//        boolean result = pictureService.removeById(id);
//        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
//        //清理图片资源
//        pictureService.clearPictureFile(picture);

        pictureService.deletePicture( id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片(管理员)
     */
    @PostMapping("update")
    @ApiOperation("更新图片(管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request)
    {
        if(pictureUpdateRequest==null||pictureUpdateRequest.getId()<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //将实体类和DTO转化
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest,picture);


        //将list tags转化为String
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //数据校验

        pictureService.ValidPicture(picture);

        //判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture=pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");

        //补充审核参数
        pictureService.fileReviewParams(picture,userService.getLoginUser( request));
        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true, "后台已提交,任务执行中！");
    }

    /**
     * 根据id获取图片(仅管理员)
     */
    @GetMapping("get")
    @ApiOperation("根据id获取图片（仅管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request)
    {
        if(id<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //操作数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        return ResultUtils.success(picture, "后台已提交,任务执行中！");
    }

    @GetMapping("get/vo")
    @ApiOperation("根据id获取图片VO")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request)
    {
        if(id<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");

        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if(spaceId!=null)
        {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        return ResultUtils.success(PictureVO.objToVo(picture), "后台已提交,任务执行中！");

    }


    /**
     * 分页获取图片列表（管理员）
     */
    @PostMapping("list/page")
    @ApiOperation("分页获取图片列表（管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                         HttpServletRequest request)
    {
        if(pictureQueryRequest==null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage, "后台已提交,任务执行中！");
    }
    /**
     * 分页获取图片列表（用户）
     */
    @PostMapping("list/page/vo")
    @ApiOperation("分页获取图片列表（用户）")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request)
    {
        if(pictureQueryRequest==null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize>20,ErrorCode.PARAMS_ERROR,"单页请求数量过多");

        Long spaceId = pictureQueryRequest.getSpaceId();
        //空间权限校验
        if(spaceId==null) {
            //普通用户只能查看审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else {
            //有空间id时，校验权限,s私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if(!loginUser.getId().equals( space.getUserId())) {
               throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该空间图片");
            }
        }
        //操作数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request), "后台已提交,任务执行中！");
    }

    //编辑图片（用户使用）
    @PostMapping("edit")
    @ApiOperation("编辑图片")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request)
    {
        if(pictureEditRequest==null||pictureEditRequest.getId()<=0)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true, "后台已提交,任务执行中！");
    }

    /**
     * 获取预制图片标签和分类
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory, "后台已提交,任务执行中！");
    }

    @PostMapping("review")
    @ApiOperation("图片审核(管理员) ")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewPictre(@RequestBody PictureReviewRequest pictureReviewRequest,
                                              HttpServletRequest request)
    {
        if (pictureReviewRequest == null)
        {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest,loginUser);
        return ResultUtils.success(true, "后台已提交,任务执行中！");
    }


    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @ApiOperation("通过 URL 上传图片")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO, "后台已提交,任务执行中！");
    }
    /**
     * 批量上传图片(管理员）
     */
//    @PostMapping("/upload/batch")
//    @ApiOperation("批量上传图片(管理员）")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
//                                                      HttpServletRequest request)
//    {
//
//        ThrowUtils.throwIf(pictureUploadByBatchRequest==null,ErrorCode.PARAMS_ERROR);
//        User loginUser = userService.getLoginUser(request);
//        Integer result = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
//        return ResultUtils.success(result);
//    }



    @PostMapping("/upload/batch")
    @ApiOperation("批量上传图片(管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request)
    {

        ThrowUtils.throwIf(pictureUploadByBatchRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        //调用异步方法
        pictureService.asyncUploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(true,"任务已提交,后台执行中");
    }


    @PostMapping("/list/page/vo/cache")
    @ApiOperation("分页获取图片列表（用户）(多级缓存）")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request)
    {
        int current = pictureQueryRequest.getCurrent();
        int size=pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);

        //普通用户只能查看审核通过的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //构建缓存key
        String queryCondition=JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey= DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey="fyhpicture:listPictureVOByPage:"+hashKey;
        //1.优先从本地缓存中查询
        String cachedValue=LOCAL_CACHE.getIfPresent(cacheKey);
        if(cachedValue!=null)
        {
            //缓存命中
            Page<PictureVO> cachedPage=JSONUtil.toBean(cachedValue,Page.class);
            return ResultUtils.success(cachedPage, "后台已提交,任务执行中！");
        }
        //2.从redis缓存中查询
        ValueOperations<String,String> valueOps=stringRedisTemplate.opsForValue();
        cachedValue=valueOps.get(cacheKey);
        if(cachedValue!=null)
        {
            //缓存命中，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey,cachedValue);
            Page<PictureVO> cachedPage=JSONUtil.toBean(cachedValue,Page.class);
            return ResultUtils.success(cachedPage, "后台已提交,任务执行中！");
        }
        //3.从数据库中查询
        Page<Picture> picturePage=pictureService.page(new Page<>(current,size),
                pictureService.getQueryWrapper(pictureQueryRequest));

        //获取封装类
        Page<PictureVO> pictureVOPage=pictureService.getPictureVOPage(picturePage,request);
        //更新缓存
        String cacheValue=JSONUtil.toJsonStr(pictureVOPage);
        //更新本地缓存
        LOCAL_CACHE.put(cacheKey,cacheValue);

        //更新Redis缓存,过期时间设置5分钟
        //缓存时间
        valueOps.set(cacheKey,cacheValue,5, TimeUnit.MINUTES);

        //返回结果
        return ResultUtils.success(pictureVOPage, "后台已提交,任务执行中！");
    }
    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }

    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }


}
