package com.ancientbooks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "提问内容不能为空")
    @Size(max = 5000, message = "单次输入不超过5000字")
    private String query;

    private String conversationId;
}
