package com.joy.photobooth_be.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
public class PrintRequestDto {
    private MultipartFile photoImage;
    private int count;
}
