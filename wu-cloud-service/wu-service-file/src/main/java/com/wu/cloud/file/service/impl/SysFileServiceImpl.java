package com.wu.cloud.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.file.config.FileStorageProperties;
import com.wu.cloud.file.config.LocalFileStorage;
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
    /** 与单体 ProfileServiceImpl 一致：images/avatar/yyyy/MM/dd */
    public static final String AVATAR_PATH_PREFIX = "images/avatar/";
    private static final long TEXT_MAX_BYTES = 5L * 1024 * 1024;
    private static final int PLATFORM_MAX_MB = 500;
    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Resource
    private SysFileMapper sysFileMapper;
    @Resource
    private LocalFileStorage localFileStorage;
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
        return uploadFile(file, (Long) null);
    }

    @Override
    public SysFileDO uploadFile(MultipartFile file, Long groupId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String fileSuffix = LocalFileStorage.getSuffix(originalName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + fileSuffix;
        localFileStorage.validateUpload(fileName, file.getSize());

        String storagePath = LocalDate.now().format(DIR_FMT);
        String url;
        try (InputStream input = file.getInputStream()) {
            url = localFileStorage.upload(input, storagePath, fileName);
        } catch (IOException e) {
            throw new BusinessException("文件写入磁盘失败: " + e.getMessage());
        }

        // filePath 存相对存储路径（不含 /file 前缀），url 存可访问路径
        String relativePath = storagePath + "/" + fileName;
        SysFileDO fileDO = new SysFileDO();
        fileDO.setOriginalName(originalName);
        fileDO.setFileName(fileName);
        fileDO.setFilePath(relativePath);
        fileDO.setUrl(url);
        fileDO.setFileSize(file.getSize());
        fileDO.setFileType(LocalFileStorage.resolveType(fileName, file.getContentType()));
        fileDO.setFileSuffix(fileSuffix);
        fileDO.setStorageType("LOCAL");
        fileDO.setGroupId(groupId);
        sysFileMapper.insert(fileDO);
        return fileDO;
    }

    /**
     * 头像上传：落盘 images/avatar/yyyy/MM/dd，URL 返回 /api/files/...（与单体、sys_user.avatar 历史数据一致）
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
        String fileSuffix = LocalFileStorage.getSuffix(originalName);
        if (!StringUtils.hasText(fileSuffix)) {
            fileSuffix = ".jpg";
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + fileSuffix;
        localFileStorage.validateUpload(fileName, file.getSize());

        String storagePath = AVATAR_PATH_PREFIX + LocalDate.now().format(DIR_FMT);
        try (InputStream input = file.getInputStream()) {
            localFileStorage.upload(input, storagePath, fileName);
        } catch (IOException e) {
            throw new BusinessException("文件写入磁盘失败: " + e.getMessage());
        }

        String relativePath = storagePath + "/" + fileName;
        String url = "/api/files/" + relativePath;

        SysFileDO fileDO = new SysFileDO();
        fileDO.setOriginalName(originalName);
        fileDO.setFileName(fileName);
        fileDO.setFilePath(relativePath);
        fileDO.setUrl(url);
        fileDO.setFileSize(file.getSize());
        fileDO.setFileType(LocalFileStorage.resolveType(fileName, file.getContentType()));
        fileDO.setFileSuffix(fileSuffix);
        fileDO.setStorageType("LOCAL");
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
            return localFileStorage.readBytes(normalizeStoragePath(fileDO.getFilePath()));
        } catch (IOException e) {
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
        Path path = localFileStorage.resolvePath(normalizeStoragePath(record.getFilePath()));
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(400, "磁盘文件已不存在，无法恢复");
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
            localFileStorage.delete(normalizeStoragePath(record.getFilePath()));
        } catch (Exception ignored) {
            // 磁盘可能已不存在
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
        Path path = localFileStorage.resolvePath(normalizeStoragePath(record.getFilePath()));
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "文件不存在");
        }
        return new FileSystemResource(Objects.requireNonNull(path.toAbsolutePath()));
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
        wrapper.notLikeRight(SysFileDO::getFilePath, CHAT_IMAGE_PATH_PREFIX)
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