package com.fyh.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.manager.FileManager;
import com.fyh.model.dto.file.UploadPictureResult;
import com.fyh.model.dto.picture.PictureUploadRequest;
import com.fyh.model.entity.Picture;
import com.fyh.model.entity.User;
import com.fyh.model.vo.PictureVO;
import com.fyh.service.PictureService;
import com.fyh.mapper.PictureMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
* @author 16193
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-11-10 14:40:13
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    private final FileManager fileManager;

    public PictureServiceImpl(FileManager fileManager) {
        this.fileManager = fileManager;
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
            boolean exist=this.lambdaQuery()
                    .eq(Picture::getId,pictureId)
                    .exists();
            ThrowUtils.throwIf(!exist,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
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
}




