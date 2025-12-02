package com.fyh.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.manager.CosManager;
import com.fyh.manager.FileManager;
import com.fyh.manager.PictureUploadTemplate;
import com.fyh.model.dto.file.UploadPictureResult;
import com.fyh.model.dto.picture.*;
import com.fyh.model.entity.Picture;
import com.fyh.model.entity.Space;
import com.fyh.model.entity.User;
import com.fyh.model.enums.PictureReviewStatusEnum;
import com.fyh.model.vo.PictureVO;
import com.fyh.model.vo.UserVO;
import com.fyh.service.*;
import com.fyh.mapper.PictureMapper;
import com.fyh.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 16193
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-11-10 14:40:13
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {


    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    @Lazy
    //todo 解决循环依赖问题
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //仅本人或管理员才允许操作
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该空间");
            }
            if(space.getTotalCount()>=space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间图片数量已达上限，无法上传");
            }
            if(space.getTotalSize()>=space.getMaxSize())
            {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间图片存储已达上限，无法上传");
            }
        }
        //用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新图片，需要判断图片是否存在
        Picture oldPicture = null;
        if (pictureId != null) {

            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //仅本人或管理员才允许更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //        1.校验空间是否一致
//        2.如果没传spaceId，则复用原有图片的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else
//                3.如果穿了spaceId，必须和原有图片一致
            {
                //1.校验空间是否一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间不一致，无法更新");
                }
            }
        }

        //上传图片，得到信息
        //根据用户Id划分目录
        String uploadPathPrefix;
//        4，按照用户id划分目录=>按照空间划分目录
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%d", loginUser.getId());

        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        //根据参数类型选择上传图片方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        //补充审查参数
        this.fileReviewParams(picture, loginUser);

        //如果pictureId不为空，则更新图片，否则是新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        //开启事务
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });

        if (pictureId != null) {
            this.clearPictureFile(oldPicture);
        }
        return PictureVO.objToVo(picture);
    }

    /**
     * 分页查询 图片
     *
     * @param pictureQueryRequest
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        //从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            //拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);

        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");


        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.le(ObjectUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        //标签是JSON数组，特殊处理
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
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
     *
     * @param request
     * @return
     */

    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //获取用户信息
        Long userId = picture.getId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }


    /**
     * 获取图片分页封装类
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //对象列表--->封装类列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());


        //关联查询用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserMap.containsKey(userId)) {
                user = userIdUserMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));

        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    public void ValidPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        String introduction = picture.getIntroduction();
        Long id = picture.getId();
        String url = picture.getUrl();
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片id不能为空");
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");

        }
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片地址过长");
        }
    }


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || pictureReviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //已经是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片已审核,无需重复操作");
        }
        //执行审核操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);


    }

    /**
     * 文件审核参数
     *
     * @param picture
     * @param loginUser
     */
    public void fileReviewParams(Picture picture, User loginUser) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        if (userService.isAdmin(loginUser)) {
            //管理员自动 通过
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动通过");

        } else {
            //普通用户 创建和编辑-->待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());

        }
    }

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser) {
        //校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();

        //名称前缀默认搜索内容
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索内容不能为空");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "搜索数量不能大于30");
        //抓取参数
        String fetchUrl = String.format("https://www.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.info("批量抓取图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量抓取图片失败");
        }
        Element div = document.getElementsByClass("dgControl").first();//选择所有class中含dgControl的元素
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空,已跳过:{}", fetchUrl);
                continue;
            }
            //处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //上传 图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            try {

//               pictureUploadRequest.setPicName(namePrefix +(uploadCount+1));

                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功:{}", pictureVO);
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 异步批量上传图片
     *
     * @param request
     * @param loginUser
     */
    @Override
    public void asyncUploadPictureByBatch(PictureUploadByBatchRequest request, User loginUser) {
        try {
            // 调用原来的同步批量上传逻辑，但不返回前端
            uploadPictureByBatch(request, loginUser);
        } catch (Exception e) {
            log.error("异步批量上传失败", e);
        }

    }

    @Async
    @Override
    public void clearPictureFile(Picture oldpicture) {
        //判断该图片是否被多条记录调用
        String pictureUrl = oldpicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        if (count > 1) {
            //多条记录调用不删除
            return;
        }
        try {
            // 提取路径部分
            String picturePath = new URL(pictureUrl).getPath();


            //删除图片文件
            cosManager.deleteObject(picturePath);
            System.out.println("删除图片文件成功:" + pictureUrl);

            //删除缩略图
            String thumbnailUrl = oldpicture.getThumbnailUrl();
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                String thumbnailKey = new URL(thumbnailUrl).getPath().substring(1);
                cosManager.deleteObject(thumbnailKey);
//                cosManager.deleteObject(thumbnailUrl);
                System.out.println("删除缩略图成功:" + thumbnailKey);

                //根据缩略图URL推原图URL
                String originalUrl = thumbnailUrl.replace("_thumbnail", "");
                String originalKey = new URL(originalUrl).getPath().substring(1);
                cosManager.deleteObject(originalKey);
                System.out.println("删除原图成功:" + originalUrl);
            }

        } catch (MalformedURLException e) {
            log.error("处理图片删除时遇到格式错误的 URL。图片 URL: {}", pictureUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "格式错误的 URL");
        }


    }


    /**
     * 检查图片操作权限
     *
     * @param loginUser
     * @param picture
     */
    public void checkPictureAuth(User loginUser, Picture picture) {
        //判断是公共图库图片还是私有
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            //公共图库，进本人或管理员操作


            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该图片");
            }
        }
        else {
            //私有空间，仅空间管理员操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该图片");
            }
        }
    }

    public void deletePicture(long pictureId, User loginUser) {
        //处理为空
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        //判断照片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //权限校验
        this.checkPictureAuth(loginUser, oldPicture);
        //操作数据库
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
            //更新空间额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId!=null)
            {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });

        //异步清理文件
        this.clearPictureFile(oldPicture);
    }



    /**
     * 删除空间及关联图片
     *
     * @param pictureId
     */
    public void deleteSpacePicture(long pictureId) {
        //处理为空
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);

        //判断照片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //操作数据库,事务删除数据库记录
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
            return true;
        });

        //异步清理文件
        this.clearPictureFile(oldPicture);
    }


    /**
     * 图片编辑
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        //实体类和DTO进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        //照片标签list处理为string字符串
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        //设置编辑时间
        picture.setEditTime(new Date());
        //数据校验
        this.ValidPicture(picture);

        //判断是否存在id，oldPicture
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);



        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //权限校验
        checkPictureAuth(loginUser, oldPicture);

        //填充审核参数
        this.fileReviewParams(picture, loginUser);
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片更新失败");
    }

    public List<PictureVO> searchPictureByColor(Long spaceId,String picColor,User loginUser)
    {
        //1.校验参数
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceId==null||StrUtil.isBlank(picColor),ErrorCode.PARAMS_ERROR);

        //2.校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        if (!loginUser.getId().equals(space.getUserId()))
        {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该空间");
        }
        //3.查询改空间下所有图片，必须有主色调
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        //如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList))
        {
            return Collections.emptyList();
        }

            //将目标颜色转为Color对象
        Color targetColor = Color.decode(picColor);

            //4.计算相似度并排序
        List<Picture> sortedPictures=pictureList.stream()
                .sorted(Comparator.comparingDouble(picture->
                {
                    //提取图片主色调
                    String hexColor = picture.getPicColor();
                    //没有主色调的图片放在最后
                    if(StrUtil.isBlank(hexColor))
                    {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor =Color.decode(hexColor);
                    //数值越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor,pictureColor);
                    //取前12个
                }))
                .limit(12)
                .collect(Collectors.toList());



            //转换为PictureVO
        return sortedPictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }




}




