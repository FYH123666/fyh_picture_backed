package com.fyh.controller;

import com.fyh.annotation.AuthCheck;
import com.fyh.common.BaseResponse;
import com.fyh.common.ResultUtils;
import com.fyh.constant.UserConstant;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.manager.CosManager;
import com.fyh.model.dto.picture.PictureUploadRequest;
import com.fyh.model.entity.User;
import com.fyh.model.vo.PictureVO;
import com.fyh.service.PictureService;
import com.fyh.service.UserService;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
@Api(tags = "文件接口")
public class FileController {




    @Resource
    private CosManager cosManager;
    @Autowired
    private UserService userService;
    @Autowired
    private PictureService pictureService;

    /**
     * 测试上传文件
     * @param multipartFile
     * @return
     */

    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  
    public BaseResponse<String> testUploadFie(@RequestPart("file")MultipartFile multipartFile) throws Exception {
        //文件目录
        String filename=multipartFile.getOriginalFilename();
        String filePath=String.format(("test/%s"),filename);
        File file=null;
        try {
        file= File.createTempFile(filePath,null);

            multipartFile.transferTo(file);
            cosManager.putObject(filePath,file);
            //返回文件可访问地址
            return ResultUtils.success(filePath, "后台已提交,任务执行中！");
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文件上传失败");
        }finally {
            if(file!=null)
            {
                //删除临时文件
                boolean delete = file.delete();
                if(!delete)
                {
                    log.error("文件删除失败,filePath={}", filePath);
                }
            }
        }

    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }



    @PostMapping("/upload")
    @ApiOperation("上传文件")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                              PictureUploadRequest pictureUploadRequest,
                                              HttpServletRequest  request)
    {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO, "后台已提交,任务执行中！");

    }

}
