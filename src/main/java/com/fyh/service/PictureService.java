package com.fyh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fyh.model.dto.picture.PictureQueryRequest;
import com.fyh.model.dto.picture.PictureReviewRequest;
import com.fyh.model.dto.picture.PictureUploadByBatchRequest;
import com.fyh.model.dto.picture.PictureUploadRequest;
import com.fyh.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fyh.model.entity.User;
import com.fyh.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 16193
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-10 14:40:13
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 分页查询 图片
     * @param pictureQueryRequest
     *
     */

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片封装类
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest  request);

    /**
     * 分页获取图片封装
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture
     */
    void ValidPicture(Picture picture);


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 图片审核参数校验
     *
     * @param picture
     * @param loginUser
     */
    void fileReviewParams(Picture  picture,User loginUser);


    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 异步批量上传图片
     *
     * @param request
     * @param loginUser
     */
    @Async
    public void asyncUploadPictureByBatch(PictureUploadByBatchRequest request, User loginUser);


    /**
     * 删除图片
     *
     * @param oldpicture
     */
    void clearPictureFile(Picture oldpicture);

}
