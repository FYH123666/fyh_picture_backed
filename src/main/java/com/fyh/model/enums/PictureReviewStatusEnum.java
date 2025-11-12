package com.fyh.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {


    REVIEWING( "待审核", 0),
    PASS( "审核通过", 1),
    REJECT( "审核不通过", 2);


    private final String text;
    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value )
    {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for(PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()){
            if(pictureReviewStatusEnum.value==value){
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
