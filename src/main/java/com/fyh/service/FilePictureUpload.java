package com.fyh.service;

import cn.hutool.core.io.FileUtil;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.manager.PictureUploadTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile =(MultipartFile)inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //1.校验文件大小
        long fileSize = multipartFile.getSize();
        final long TWO_M = 2 * 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > TWO_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        //2.校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String fileSuffix = FileUtil.getSuffix(originalFilename);

        //3.允许上传的文件后缀
        final List<String> ALLOW_CONTENT_TYPES= Arrays.asList("jpeg", "png", "webp", "jpg");
        ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                "文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile =(MultipartFile)inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile =(MultipartFile)inputSource;
        multipartFile.transferTo(file);
    }

}
