package com.joy.photobooth_be.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
public class GenerateQrCodeRequestDto {
    private MultipartFile photoImage;
}
