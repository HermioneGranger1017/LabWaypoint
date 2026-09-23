package com.itheima.controller;

import com.itheima.pojo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileUploadController {

    @Value("${upload.path}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXTS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_MIMES = Set.of("image/jpeg", "image/jpg",
            "image/png", "image/webp");
    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10MB

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "scene", defaultValue = "equipment") String scene)
            throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        if (!"equipment".equals(scene) && !"tutorial".equals(scene)) {
            return Result.error("上传场景不合法");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error("图片最大 10MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTS.contains(ext)) {
            return Result.error("仅支持 JPG/JPEG/PNG/WebP 格式");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIMES.contains(contentType.toLowerCase())) {
            return Result.error("文件 MIME 类型不合法");
        }

        String uuidName = UUID.randomUUID().toString().replace("-", "") + ext;
        File destDir = new File(uploadPath, scene);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(destDir, uuidName);
        file.transferTo(destFile);

        String url = "/uploads/" + scene + "/" + uuidName;
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }
}