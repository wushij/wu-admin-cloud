package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysPostDO;
import com.wu.cloud.system.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理（对齐前端 POST/PUT /system/post、DELETE /{id}、POST /{id}/move）
 */
@Tag(name = "岗位管理")
@RestController
@RequestMapping("/api/system/post")
public class PostController {

    @Resource
    private PostService postService;

    @RequiresPermissions("system:post:query")
    @Operation(summary = "获取树形结构")
    @GetMapping("/tree")
    public R<List<SysPostDO>> tree() {
        return R.ok(postService.getPostTree());
    }

    @RequiresPermissions("system:post:query")
    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    public R<List<SysPostDO>> list(@RequestParam(value = "postName", required = false) String postName,
                                   @RequestParam(value = "status", required = false) Integer status) {
        return R.ok(postService.getPostList(postName, status));
    }

    @RequiresPermissions("system:post:query")
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<SysPostDO>> page(PageQuery pageQuery,
                                   @RequestParam(value = "postName", required = false) String postName) {
        Page<SysPostDO> page = postService.getPostPage(pageQuery, postName);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:post:query")
    @Operation(summary = "获取菜单详情")
    @GetMapping("/get")
    public R<SysPostDO> get(@RequestParam("id") Long id) {
        return R.ok(postService.getPost(id));
    }

    /** 对齐前端 GET /system/post/{id} */
    @RequiresPermissions("system:post:query")
    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<SysPostDO> getById(@PathVariable("id") Long id) {
        return R.ok(postService.getPost(id));
    }

    @RequiresPermissions("system:post:create")
    @Log(title = "岗位管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "上传工单附件")
    @PostMapping
    public R<Long> create(@RequestBody SysPostDO postDO) {
        return R.ok(postService.createPost(postDO));
    }

    @RequiresPermissions("system:post:create")
    @Log(title = "岗位管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> createCompat(@RequestBody SysPostDO postDO) {
        return R.ok(postService.createPost(postDO));
    }

    @RequiresPermissions("system:post:update")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@RequestBody SysPostDO postDO) {
        postService.updatePost(postDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:update")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    public R<Boolean> updateCompat(@RequestBody SysPostDO postDO) {
        postService.updatePost(postDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:delete")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        postService.deletePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:delete")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    public R<Boolean> deleteCompat(@RequestParam("id") Long id) {
        postService.deletePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:update")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "移动")
    @PostMapping("/{id}/move")
    public R<Boolean> move(@PathVariable("id") Long id,
                           @RequestParam(value = "parentId", required = false) Long parentId) {
        postService.movePost(id, parentId);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:update")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "移动")
    @PostMapping("/move")
    public R<Boolean> moveCompat(@RequestParam("id") Long id,
                                 @RequestParam(value = "parentId", required = false) Long parentId) {
        postService.movePost(id, parentId);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:query")
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/recycle/page")
    public R<PageResult<SysPostDO>> recyclePage(PageQuery pageQuery,
                                          @RequestParam(value = "postName", required = false) String postName) {
        Page<SysPostDO> page = postService.getRecyclePage(pageQuery, postName);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:post:update")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        postService.restorePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:update")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复数据")
    @PostMapping("/restore")
    public R<Boolean> restoreByPost(@RequestParam("id") Long id) {
        postService.restorePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:delete")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        postService.deletePostPermanent(id);
        return R.ok(true);
    }
}
