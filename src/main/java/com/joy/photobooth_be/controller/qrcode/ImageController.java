package com.joy.photobooth_be.controller.qrcode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Controller
public class ImageController {

    @GetMapping("/{imgUrl}")
    public String index(@PathVariable String imgUrl, Model model) {
        log.info("이미지 다운로드 페이지 접속 요청 들어옴");
        String s3ImageUrl = "https://joljol-photobooth-s3.s3.ap-northeast-2.amazonaws.com/" + imgUrl;
        model.addAttribute("s3ImageUrl", s3ImageUrl);
        return "index";
    }

    @GetMapping("/api/downloadImage/{s3ImageUrl}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String s3ImageUrl) {
        try {
            // S3에서 이미지를 다운로드합니다.
            URL url = new URL("https://joljol-photobooth-s3.s3.ap-northeast-2.amazonaws.com/" + s3ImageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 이미지를 byte 배열로 읽어옵니다.
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = outputStream.toByteArray();

            // 이미지를 클라이언트에게 전송합니다.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(imageBytes.length);
            headers.setContentDispositionFormData("attachment", "image.jpg");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
