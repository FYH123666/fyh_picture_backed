package com.fyh.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @Description: 图片标签分类
 * @Author: fyh
 * @CreateTime: 2020/10/26 15:09
 */
@Data
public class PictureTagCategory {
    private List<String> tagList;
    private List<String> categoryList;

}
