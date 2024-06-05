package com.joy.photobooth_be.service.print;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final String printerEmail = "joy.joljol@print.epsonconnect.com";
    private final double inchesToCentimeters = 2.54; // 1 inch = 2.54 centimeters
    private final int targetWidthCm = 10; // 10 centimeters
    private final int targetHeightCm = 15; // 15 centimeters
    private final int dpi = 300; // DPI (dots per inch)

    public void sendMailWithRepeatedImages(MultipartFile image, int quantity) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true); // true: multipart message
            mimeMessageHelper.setTo(printerEmail); // 메일 수신자
            mimeMessageHelper.setSubject("print:" + quantity); // 메일 제목

            // 본문 없이 이미지만 첨부
            mimeMessageHelper.setText(""); // 빈 문자열로 본문 설정

            // 동일한 이미지를 지정된 수량만큼 MimeMessage에 첨부
            for (int i = 0; i < quantity; i++) {
                String imageName = "joljol" + i + image.getOriginalFilename(); // 이미지 파일 이름
                byte[] resizedImageBytes = resizeImage(image.getBytes(), targetWidthCm, targetHeightCm); // 이미지 크기 조정

                // 이미지 첨부
                ByteArrayDataSource dataSource = new ByteArrayDataSource(resizedImageBytes, "image/jpeg");
                mimeMessageHelper.addAttachment(imageName, dataSource);
            }

            javaMailSender.send(mimeMessage);
            log.info("Success: Image email sent to {}", printerEmail);
        } catch (MessagingException | IOException e) {
            log.error("Failed to send image email to {}", printerEmail, e);
            throw new RuntimeException(e);
        }
    }

    private byte[] resizeImage(byte[] originalImageBytes, int targetWidthCm, int targetHeightCm) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(originalImageBytes);
        BufferedImage originalImage = ImageIO.read(bais);

        // 이미지 크기를 픽셀로 변환
        int targetWidth = (int) (targetWidthCm * dpi / inchesToCentimeters);
        int targetHeight = (int) (targetHeightCm * dpi / inchesToCentimeters);

        // 이미지 크기 조정
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(outputImage, "jpg", baos);
        return baos.toByteArray();
    }
}
