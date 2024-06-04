package com.joy.photobooth_be.controller.qrcode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}
