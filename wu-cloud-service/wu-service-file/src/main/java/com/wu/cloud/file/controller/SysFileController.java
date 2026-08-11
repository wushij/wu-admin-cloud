package com.wu.cloud.file.controller;

import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.file.entity.SysFileDO;
import com.wu.cloud.file.service.SysFileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 文件管理 REST（路径对齐 wu-admin-web /infra/file）
 */
@RestController
@RequestMapping("/api/infra/file")
@RequiredArgsConstructor
public class SysFileController {

    private final SysFileService sysFileService;

    @RequiresPermissions("sys:file:list")
    @GetMapping("/upload-policy")
    public R<Map<String, Object>> uploadPolicy() {
        return R.ok(sysFileService.uploadPolicy());
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/page-by-group")
    public R<PageResult<SysFileDO>> pageByGroup(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Boolean ungrouped,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) String originalName) {
        return R.ok(sysFileService.pageByGroup(pageNo, pageSize, groupId, ungrouped, fileCategory, originalName));
    }

    /** 兼容旧分页 */
    @RequiresPermissions("sys:file:list")
    @GetMapping("/page")
    public R<PageResult<SysFileDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String originalName) {
        return R.ok(sysFileService.pageByGroup(pageNo, pageSize, groupId, null, null, originalName));
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.INSERT)
    @RequiresPermissions("sys:file:upload")
    @PostMapping("/upload")
    public R<SysFileDO> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "groupId", required = false) Long groupId) {
        return R.ok(sysFileService.uploadFile(file, groupId), "上传文件成功");
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @DeleteMapping("/delete")
    public R<Boolean> deleteFile(@RequestParam("id") Long id) {
        sysFileService.deleteFile(id);
        return R.ok(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @DeleteMapping("/batch")
    public R<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        sysFileService.batchDeleteFile(ids);
        return R.ok(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @DeleteMapping("/delete-batch")
    public R<Boolean> deleteBatchCompat(@RequestBody List<Long> ids) {
        sysFileService.batchDeleteFile(ids);
        return R.ok(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @PostMapping("/batch-delete")
    public R<Boolean> deleteBatchPost(@RequestBody List<Long> ids) {
        sysFileService.batchDeleteFile(ids);
        return R.ok(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        sysFileService.deletePermanent(id);
        return R.ok(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @DeleteMapping("/{id}")
    public R<Boolean> deleteByPath(@PathVariable("id") Long id) {
        sysFileService.deleteFile(id);
        return R.ok(true);
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) throws Exception {
        return buildResponse(id, false);
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadQuery(@RequestParam("id") Long id) throws Exception {
        return buildResponse(id, false);
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> preview(@PathVariable("id") Long id) throws Exception {
        return buildResponse(id, true);
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/preview")
    public ResponseEntity<Resource> previewQuery(@RequestParam("id") Long id) throws Exception {
        return buildResponse(id, true);
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/text/{id}")
    public R<String> text(@PathVariable("id") Long id) {
        return R.ok(sysFileService.readText(id));
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.UPDATE)
    @RequiresPermissions("sys:file:upload")
    @PostMapping("/move")
    public R<Boolean> move(@RequestBody MoveReq req) {
        sysFileService.moveToGroup(req.getFileIds(), req.getGroupId());
        return R.ok(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.UPDATE)
    @RequiresPermissions("sys:file:upload")
    @PutMapping("/{id}/rename")
    public R<Boolean> rename(@PathVariable("id") Long id, @RequestBody RenameReq req) {
        sysFileService.rename(id, req.getNewName());
        return R.ok(true);
    }

    @RequiresPermissions("sys:file:list")
    @GetMapping("/recycle/page")
    public R<PageResult<SysFileDO>> recyclePage(PageQuery pageQuery,
                                                @RequestParam(required = false) String originalName) {
        return R.ok(sysFileService.recyclePage(pageQuery, originalName));
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.UPDATE)
    @RequiresPermissions("sys:file:upload")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        sysFileService.restore(id);
        return R.ok(true);
    }

    private ResponseEntity<Resource> buildResponse(Long id, boolean inline) throws Exception {
        SysFileDO fileDO = sysFileService.getFileById(id);
        if (fileDO == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = sysFileService.openFileResource(id);
        String originalName = fileDO.getOriginalName() != null ? fileDO.getOriginalName() : fileDO.getFileName();
        String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentType = fileDO.getFileType() != null ? fileDO.getFileType() : "application/octet-stream";
        String disposition = (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encoded;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @Data
    public static class MoveReq {
        private List<Long> fileIds;
        private Long groupId;
    }

    @Data
    public static class RenameReq {
        private String newName;
    }
}
