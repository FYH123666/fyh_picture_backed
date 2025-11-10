package com.fyh.service;

import com.fyh.model.dto.picture.PictureUploadRequest;
import com.fyh.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fyh.model.entity.User;
import com.fyh.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 16193
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-10 14:40:13
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

}
