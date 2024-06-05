package com.joy.photobooth_be.service.qrcode;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.joy.photobooth_be.dto.GenerateQrCodeRequestDto;
import com.joy.photobooth_be.dto.PrintRequestDto;
import com.joy.photobooth_be.service.print.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class S3Service {

    @Value(("${cloud.aws.s3.bucketName}"))
    private String bucket;

    @Autowired
    private AmazonS3 s3Client;
    private final EmailService emailService;

    public ResponseEntity<byte[]> getQrCode(GenerateQrCodeRequestDto requestDto) throws IOException {
        // 이미지를 S3에 업로드하고, S3 URL을 얻는다
        String baseUrl = "https://joljol-photobooth-s3.s3.ap-northeast-2.amazonaws.com/";
        String s3Url = upload(requestDto.getPhotoImage());
        String fileName = s3Url.substring(baseUrl.length());
        log.info("조회용 이미지 업로드 성공 -> {}", s3Url);

        // 얻은 fileName으로, 타임리프 페이지 접속 URL을 생성한다
        String downloadUrl = "http://joljol.site:8080/" + fileName;
        log.info("이미지 다운로드 경로 생성 -> {}", downloadUrl);

        // 이미지 다운로드 URL로 QR코드를 생성한다
        try {
            int width = 50;
            int height = 50;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(downloadUrl, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qr-code.png\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "image/png");

            return new ResponseEntity<>(pngData, headers, HttpStatus.OK);
        } catch (WriterException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public String upload(MultipartFile file) throws IOException {
        // UUID를 파일명에 추가
        String id = LocalDateTime.now().toString();
        String fileName = UUID.randomUUID().toString() + id;
        log.info("fileName: " + fileName);
        File uploadFile = convert(file);

        String uploadImageUrl = putS3(uploadFile, fileName);
        removeNewFile(uploadFile);
        return uploadImageUrl;
    }

    private File convert(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String uniqueFileName = uuid + "_" + originalFileName.replaceAll("\\s", "_");

        File convertFile = new File(uniqueFileName);
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            } catch (IOException e) {
                log.error("파일 변환 중 오류 발생: {}", e.getMessage());
                throw e;
            }
            return convertFile;
        }
        throw new IllegalArgumentException(String.format("파일 변환에 실패했습니다. %s", originalFileName));
    }

    private String putS3(File uploadFile, String fileName) {
        s3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile)
                .withCannedAcl(CannedAccessControlList.PublicRead));
        return s3Client.getUrl(bucket, fileName).toString();
    }

    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("파일이 삭제되었습니다.");
        } else {
            log.info("파일이 삭제되지 못했습니다.");
        }
    }

    public void deleteFile(String fileName) {
        try {
            // URL 디코딩을 통해 원래의 파일 이름을 가져옵니다.
            String decodedFileName = URLDecoder.decode(fileName, "UTF-8");
            log.info("Deleting file from S3: " + decodedFileName);
            s3Client.deleteObject(bucket, decodedFileName);
        } catch (UnsupportedEncodingException e) {
            log.error("Error while decoding the file name: {}", e.getMessage());
        }
    }

    public String updateFile(MultipartFile newFile, String oldFileName, String dirName) throws IOException {
        // 기존 파일 삭제
        log.info("S3 oldFileName: " + oldFileName);
        deleteFile(oldFileName);
        // 새 파일 업로드
        return upload((MultipartFile) newFile);
    }

    public ResponseEntity<?> printImg(PrintRequestDto requestDto) throws IOException {

        // 프린터기 메일 주소로 수량만큼 사진을 첨부허여 이메일을 전송한다
        emailService.sendMailWithRepeatedImages(requestDto.getPhotoImage(), requestDto.getCount());
        log.info("프린트 요청 메일 발송 성공");
        // 이미지를 S3에 업로드하고, S3 URL을 얻는다
        String s3Url = upload(requestDto.getPhotoImage());
        log.info("인쇄용 이미지 업로드 성공 -> {}", s3Url);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
