package com.joy.photobooth_be.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailMessageDto {
    private String to; // 수신자
    private String subject; // 메일 제목
    private String message; // 메일 내용
}
