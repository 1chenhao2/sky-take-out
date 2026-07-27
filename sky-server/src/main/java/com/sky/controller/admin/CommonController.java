package com.sky.controller.admin;

import com.sky.properties.LocalFileProperties;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/common")
public class CommonController {

    @Autowired
    private LocalFileProperties localFileProperties;

    /**
     * 文件上传
     * @param file 上传的文件
     * @return 文件访问URL
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file.getOriginalFilename());

        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || originalFilename.isEmpty()) {
                return Result.error("文件名不能为空");
            }

            // 截取文件后缀名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 验证文件类型（可选：只允许图片）
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
            boolean isAllowed = false;
            for (String allowedExt : allowedExtensions) {
                if (extension.equalsIgnoreCase(allowedExt)) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                return Result.error("不支持的文件类型");
            }

            // 生成新的文件名（使用UUID避免重名）
            String fileName = UUID.randomUUID().toString() + extension;

            // 构建上传目录路径
            String uploadPath = localFileProperties.getUploadPath();
            File uploadDir = new File(uploadPath);

            // 如果目录不存在，创建目录
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 构建完整的文件路径
            String filePath = uploadPath + File.separator + fileName;
            File destFile = new File(filePath);

            // 保存文件到本地
            file.transferTo(destFile);

            log.info("文件上传成功，保存路径：{}", filePath);

            // 返回文件访问路径（完整URL）
            String accessUrl = "http://localhost:8080" + localFileProperties.getAccessPath() + "/" + fileName;
            log.info("文件访问URL：{}", accessUrl);
            return Result.success(accessUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
