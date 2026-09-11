package com.novamall.api.api.mall.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * AI 商品问答 param
 */
@Data
public class AiChatParam implements Serializable {

    @ApiModelProperty("用户问题")
    @NotEmpty(message = "问题不能为空")
    private String question;
}
