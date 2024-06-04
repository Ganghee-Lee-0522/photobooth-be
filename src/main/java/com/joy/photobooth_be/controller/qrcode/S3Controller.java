package com.joy.photobooth_be.controller.qrcode;

import com.google.zxing.WriterException;
import com.joy.photobooth_be.dto.GenerateQrCodeRequestDto;
import com.joy.photobooth_be.dto.PrintRequestDto;
import com.joy.photobooth_be.service.qrcode.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class S3Controller {
    private final S3Service service;

    @PostMapping("/qrcode")
    public ResponseEntity<byte[]> generateQrCode(GenerateQrCodeRequestDto requestDto) throws IOException, WriterException {
        return service.getQrCode(requestDto);
    }

    @PostMapping("/print")
    public ResponseEntity<?> printImg(PrintRequestDto requestDto) throws IOException {
        return service.printImg(requestDto);
    }
}
