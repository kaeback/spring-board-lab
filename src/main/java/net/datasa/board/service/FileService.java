package net.datasa.board.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 첨부파일을 디스크에 저장/조회/삭제하는 서비스.
 *
 * 저장 경로는 application.yml의 {@code file.upload-dir}로 지정한다.
 */
@Slf4j
@Service
public class FileService {

    private final Path uploadDir;

    public FileService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir);
    }

    /**
     * 업로드 파일을 저장하고 "서버에 저장된 파일명"을 반환한다.
     * 파일이 없으면 null을 반환한다(첨부 없이 진행).
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Files.createDirectories(uploadDir);
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String storedName = UUID.randomUUID() + ext;   // 중복 방지용 파일명
            file.transferTo(uploadDir.resolve(storedName));
            return storedName;
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장에 실패했습니다", e);
        }
    }

    /** 저장된 파일을 다운로드용 Resource로 읽어 온다. */
    public Resource loadAsResource(String storedFileName) {
        try {
            Path file = uploadDir.resolve(storedFileName);
            Resource resource = new UrlResource(file.toUri());
            return (resource.exists() && resource.isReadable()) ? resource : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 저장된 파일을 삭제한다(있으면). */
    public void delete(String storedFileName) {
        if (storedFileName == null) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(storedFileName));
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", storedFileName, e);
        }
    }
}
