package com.fyh.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fyh.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片缩略图url
     */
    private String thumbnailUrl;


    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * vo 转 obj
     * 封装类转对象
     */
    public static Picture voToObj(PictureVO vo)
    {
        if(vo == null)
        {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(vo,picture);
        picture.setTags(JSONUtil.toJsonStr(vo.getTags()));//将一个对象转换为JSON字符串，然后设置给另一个对象的属性。
        return picture;
    }

    /**
     * obj 转 vo
     * 对象转封装类
     */
    public static PictureVO objToVo(Picture obj)
    {
        if (obj == null)
        {
            return null;
        }
        PictureVO vo = new PictureVO();
        BeanUtils.copyProperties(obj,vo);
        vo.setTags(JSONUtil.toList(obj.getTags(), String.class));
        return vo;

    }

}
