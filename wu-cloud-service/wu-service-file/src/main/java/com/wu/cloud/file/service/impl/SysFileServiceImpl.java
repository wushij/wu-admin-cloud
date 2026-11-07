package com.wu.cloud.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.file.config.FileContentTypes;
import com.wu.cloud.file.config.FileStorageProperties;
import com.wu.cloud.file.config.LocalFileStorage;
import com.wu.cloud.file.config.StorageObjectKeyBuilder;
import com.wu.cloud.file.dto.FilePageDTO;
import com.wu.cloud.file.entity.SysFileDO;
import com.wu.cloud.file.mapper.SysFileMapper;
import com.wu.cloud.file.service.SysFileService;
import jakarta.annotation.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class SysFileServiceImpl implements SysFileService {

    public static final String CHAT_IMAGE_PATH_PREFIX = "images/chat/";
    public static final String CHAT_FILE_PATH_PREFIX = "files/chat/";
    /** 新版聊天附件统一前缀：chat/images/、chat/files/（与单体一致） */
    public static final String CHAT_ROOT_PATH_PREFIX = "chat/";
    /** 兼容历史头像前缀 */
    public static final String AVATAR_PATH_PREFIX = "images/avatar/";
    private static final long TEXT_MAX_BYTES = 5L * 1024 * 1024;
    private static final int PLATFORM_MAX_MB = 500;

    @Resource
    private SysFileMapper sysFileMapper;
    @Resource
    private LocalFileStorage localFileStorage;

    @Resource
    private com.wu.cloud.file.config.StorageRoutingService storageRoutingService;
    @Resource
    private FileStorageProperties storageProperties;
    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    @Override
    public Page<SysFileDO> getFilePage(FilePageDTO pageDTO) {
        Page<SysFileDO> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
        LambdaQueryWrapper<SysFileDO> wrapper = new LambdaQueryWrapper<>();
        excludeChatInternalFiles(wrapper);
        if (StringUtils.hasText(pageDTO.getOriginalName())) {
            wrapper.like(SysFileDO::getOriginalName, pageDTO.getOriginalName());
        }
        if (pageDTO.getGroupId() != null) {
            wrapper.eq(SysFileDO::getGroupId, pageDTO.getGroupId());
        }
        wrapper.orderByDesc(SysFileDO::getCreateTime);
        return sysFileMapper.selectPage(page, wrapper);
    }

    @Override
    public SysFileDO getFileById(Long id) {
        return sysFileMapper.selectById(id);
    }

    @Override
    public SysFileDO uploadFile(MultipartFile file) {
        return uploadFile(file, (Long) null);
    }

    @Override
    public SysFileDO uploadFile(MultipartFile file, String type) {
        if ("avatar".equalsIgnoreCase(type)) {
            return uploadAvatarFile(file);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String objectName;
        if ("chat-image".equalsIgnoreCase(type)) {
            objectName = StorageObjectKeyBuilder.chatImage(originalName);
        } else if ("chat-file".equalsIgnoreCase(type)) {
            objectName = StorageObjectKeyBuilder.chatFile(originalName);
        } else if ("ticket".equalsIgnoreCase(type)) {
            objectName = StorageObjectKeyBuilder.ticketAttachment(null, originalName);
        } else if ("ai-knowledge".equalsIgnoreCase(type)) {
            objectName = StorageObjectKeyBuilder.aiKnowledgeDocument(null, originalName);
        } else if (StringUtils.hasText(type)) {
            objectName = StorageObjectKeyBuilder.sysFile(type, originalName);
        } else {
            objectName = StorageObjectKeyBuilder.sysFile(null, originalName);
        }
        return doUploadAndSave(file, originalName, objectName, null);
    }

    @Override
    public SysFileDO uploadFile(MultipartFile file, Long groupId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String objectName = StorageObjectKeyBuilder.sysFile(null, originalName);
        return doUploadAndSave(file, originalName, objectName, groupId);
    }

    /**
     * 头像上传：按 StorageObjectKeyBuilder 规范生成 users/{username}/avatar/avatar_{shortHash}.{ext}，
     * 并通过 StorageRoutingService 路由到当前激活引擎（local 落 data/uploads，或 minio/oss/cos）。
     */
    private SysFileDO uploadAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片作为头像");
        }
        String originalName = file.getOriginalFilename();
        String objectName = StorageObjectKeyBuilder.userAvatar(
                UserContextHolder.getUserId(), UserContextHolder.getUsername(), originalName);
        return doUploadAndSave(file, originalName, objectName, null);
    }

    private SysFileDO doUploadAndSave(MultipartFile file, String originalName, String objectName, Long groupId) {
        String fileSuffix = LocalFileStorage.getSuffix(originalName);
        if (!StringUtils.hasText(fileSuffix)) {
            fileSuffix = LocalFileStorage.getSuffix(objectName);
        }
        int lastSlash = objectName.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
        localFileStorage.validateUpload(fileName, file.getSize());

        String url;
        String storageType;
        String bucketName;
        try (InputStream input = file.getInputStream()) {
            // 按存储引擎路由：local 落本地磁盘 data/uploads，minio/oss/cos 走对象存储
            com.wu.cloud.file.config.StorageRoutingService.UploadResult result =
                    storageRoutingService.upload(objectName, input, file.getSize(), file.getContentType());
            url = result.url();
            storageType = result.storageType().toLowerCase();
            bucketName = result.bucketName();
        } catch (Exception e) {
            throw new BusinessException("文件写入存储失败: " + e.getMessage());
        }

        SysFileDO fileDO = new SysFileDO();
        fileDO.setOriginalName(originalName);
        fileDO.setFileName(fileName);
        fileDO.setFilePath(objectName);
        fileDO.setUrl(url);
        fileDO.setFileSize(file.getSize());
        fileDO.setFileType(FileContentTypes.resolve(originalName, file.getContentType(), null));
        fileDO.setFileSuffix(fileSuffix);
        fileDO.setStorageType(storageType);
        fileDO.setBucketName(bucketName);
        fileDO.setGroupId(groupId);
        if (StringUtils.hasText(UserContextHolder.getUsername())) {
            fileDO.setCreateBy(UserContextHolder.getUsername());
        }
        sysFileMapper.insert(fileDO);
        return fileDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        // 软删：仅标记 deleted=1，保留磁盘以便回收站恢复
        SysFileDO fileDO = sysFileMapper.selectById(id);
        if (fileDO == null) {
            return;
        }
        sysFileMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFile(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            deleteFile(id);
        }
    }

    @Override
    public byte[] readFile(Long id) {
        SysFileDO fileDO = sysFileMapper.selectById(id);
        if (fileDO == null) {
            return null;
        }
        try {
            return storageRoutingService.readBytes(fileDO.getStorageType(),
                    normalizeStoragePath(fileDO.getFilePath()));
        } catch (Exception e) {
            throw new BusinessException("读取文件内容失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId,
                                             Boolean ungrouped, String fileCategory, String originalName) {
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        LambdaQueryWrapper<SysFileDO> wrapper = buildGroupListQuery(groupId, ungrouped, fileCategory, originalName);
        wrapper.orderByDesc(SysFileDO::getCreateTime);
        Page<SysFileDO> page = sysFileMapper.selectPage(new Page<>(no, size), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    @Override
    public long countForGroupSidebar(Long groupId, Boolean ungrouped, String fileCategory) {
        return sysFileMapper.selectCount(buildGroupListQuery(groupId, ungrouped, fileCategory, null));
    }

    @Override
    public Map<String, Object> uploadPolicy() {
        Map<String, Object> policy = new HashMap<>(4);
        policy.put("maxSizeMb", dynamicConfigHelper.getFileMaxSizeMb());
        policy.put("allowedExtensions", dynamicConfigHelper.getFileAllowedExtensions());
        policy.put("platformMaxMb", PLATFORM_MAX_MB);
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToGroup(List<Long> fileIds, Long groupId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        sysFileMapper.update(null, new LambdaUpdateWrapper<SysFileDO>()
                .in(SysFileDO::getId, fileIds)
                .set(SysFileDO::getGroupId, groupId));
    }

    @Override
    public void rename(Long id, String newName) {
        if (!StringUtils.hasText(newName)) {
            throw new BusinessException("文件名不能为空");
        }
        SysFileDO record = sysFileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        record.setOriginalName(newName.trim());
        sysFileMapper.updateById(record);
    }

    @Override
    public String readText(Long id) {
        SysFileDO record = sysFileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        if (record.getFileSize() != null && record.getFileSize() > TEXT_MAX_BYTES) {
            throw new BusinessException("文件过大，无法以文本预览");
        }
        byte[] data = readFile(id);
        if (data == null) {
            throw new BusinessException(404, "文件不存在");
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public PageResult<SysFileDO> recyclePage(PageQuery pageQuery, String originalName) {
        int no = pageQuery.getPageNo() == null || pageQuery.getPageNo() < 1 ? 1 : pageQuery.getPageNo();
        int size = pageQuery.getPageSize() == null || pageQuery.getPageSize() < 1
                ? 20 : Math.min(pageQuery.getPageSize(), 100);
        Page<SysFileDO> page = new Page<>(no, size);
        Page<SysFileDO> deleted = (Page<SysFileDO>) sysFileMapper.selectDeletedPage(page, originalName);
        return PageResult.of(deleted.getRecords(), deleted.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        SysFileDO record = sysFileMapper.selectDeletedById(id);
        if (record == null) {
            throw new BusinessException(404, "回收站文件不存在");
        }
        String path = normalizeStoragePath(record.getFilePath());
        if (storageRoutingService.isLocalStorageType(record.getStorageType())) {
            if (localFileStorage.resolveExistingPath(path) == null) {
                throw new BusinessException(400, "磁盘文件已不存在，无法恢复");
            }
        } else if (storageRoutingService.size(record.getStorageType(), path) < 0) {
            throw new BusinessException(400, "对象存储文件已不存在，无法恢复");
        }
        if (sysFileMapper.restoreById(id) == 0) {
            throw new BusinessException(404, "回收站文件不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        SysFileDO record = sysFileMapper.selectDeletedById(id);
        if (record == null) {
            throw new BusinessException(404, "回收站文件不存在");
        }
        try {
            storageRoutingService.delete(record.getStorageType(), normalizeStoragePath(record.getFilePath()));
        } catch (Exception ignored) {
            // 存储侧文件可能已不存在
        }
        if (sysFileMapper.deletePhysicalById(id) == 0) {
            throw new BusinessException(404, "回收站文件不存在");
        }
    }

    @Override
    public org.springframework.core.io.Resource openFileResource(Long id) throws IOException {
        SysFileDO record = sysFileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "文件不存在");
        }
        String path = normalizeStoragePath(record.getFilePath());
        if (storageRoutingService.isLocalStorageType(record.getStorageType())) {
            Path localPath = localFileStorage.resolveExistingPath(path);
            if (localPath == null || !Files.isRegularFile(localPath)) {
                throw new BusinessException(404, "文件不存在");
            }
            return new FileSystemResource(Objects.requireNonNull(localPath.toAbsolutePath()));
        }
        // 对象存储：以流式资源返回，避免依赖本地文件系统路径
        byte[] data = storageRoutingService.readBytes(record.getStorageType(), path);
        if (data == null || data.length == 0) {
            throw new BusinessException(404, "文件不存在");
        }
        return new org.springframework.core.io.ByteArrayResource(data);
    }

    private LambdaQueryWrapper<SysFileDO> buildGroupListQuery(Long groupId, Boolean ungrouped,
                                                              String fileCategory, String originalName) {
        LambdaQueryWrapper<SysFileDO> wrapper = new LambdaQueryWrapper<>();
        excludeChatInternalFiles(wrapper);
        if (Boolean.TRUE.equals(ungrouped)) {
            wrapper.isNull(SysFileDO::getGroupId);
        } else if (groupId != null) {
            wrapper.eq(SysFileDO::getGroupId, groupId);
        }
        applyFileCategory(wrapper, fileCategory);
        if (StringUtils.hasText(originalName)) {
            wrapper.like(SysFileDO::getOriginalName, originalName);
        }
        return wrapper;
    }

    private void excludeChatInternalFiles(LambdaQueryWrapper<SysFileDO> wrapper) {
        wrapper.notLikeRight(SysFileDO::getFilePath, CHAT_ROOT_PATH_PREFIX)
                .notLikeRight(SysFileDO::getFilePath, CHAT_IMAGE_PATH_PREFIX)
                .notLikeRight(SysFileDO::getFilePath, CHAT_FILE_PATH_PREFIX);
    }

    private void applyFileCategory(LambdaQueryWrapper<SysFileDO> wrapper, String fileCategory) {
        if (!StringUtils.hasText(fileCategory) || "all".equalsIgnoreCase(fileCategory)) {
            return;
        }
        switch (fileCategory) {
            case "image" -> wrapper.likeRight(SysFileDO::getFileType, "image/");
            case "video" -> wrapper.likeRight(SysFileDO::getFileType, "video/");
            case "audio" -> wrapper.likeRight(SysFileDO::getFileType, "audio/");
            case "other" -> wrapper.and(w -> w
                    .and(x -> x.isNull(SysFileDO::getFileType).or().notLike(SysFileDO::getFileType, "image/"))
                    .and(x -> x.isNull(SysFileDO::getFileType).or().notLike(SysFileDO::getFileType, "video/"))
                    .and(x -> x.isNull(SysFileDO::getFileType).or().notLike(SysFileDO::getFileType, "audio/")));
            default -> {
            }
        }
    }

    /** 兼容历史数据：filePath 可能带 /file/ 前缀 */
    public static String normalizeStoragePath(String filePath) {
        if (filePath == null) {
            return "";
        }
        String p = filePath.trim();
        if (p.startsWith("/file/")) {
            return p.substring("/file/".length());
        }
        if (p.startsWith("file/")) {
            return p.substring("file/".length());
        }
        if (p.startsWith("/files/")) {
            return p.substring("/files/".length());
        }
        return p.startsWith("/") ? p.substring(1) : p;
    }
}