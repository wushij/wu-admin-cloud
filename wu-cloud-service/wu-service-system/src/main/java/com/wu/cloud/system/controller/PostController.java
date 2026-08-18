package com.wu.cloud.system.controller;

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
@RestController
@RequestMapping("/api/system/post")
public class PostController {

    @Resource
    private PostService postService;

    @RequiresPermissions("system:post:list")
    @GetMapping("/tree")
    public R<List<SysPostDO>> tree() {
        return R.ok(postService.getPostTree());
    }

    @RequiresPermissions("system:post:list")
    @GetMapping("/list")
    public R<List<SysPostDO>> list(@RequestParam(value = "postName", required = false) String postName,
                                   @RequestParam(value = "status", required = false) Integer status) {
        return R.ok(postService.getPostList(postName, status));
    }

    @RequiresPermissions("system:post:list")
    @GetMapping("/page")
    public R<PageResult<SysPostDO>> page(PageQuery pageQuery,
                                   @RequestParam(value = "postName", required = false) String postName) {
        Page<SysPostDO> page = postService.getPostPage(pageQuery, postName);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:post:query")
    @GetMapping("/get")
    public R<SysPostDO> get(@RequestParam("id") Long id) {
        return R.ok(postService.getPost(id));
    }

    /** 对齐前端 GET /system/post/{id} */
    @RequiresPermissions("system:post:query")
    @GetMapping("/{id}")
    public R<SysPostDO> getById(@PathVariable("id") Long id) {
        return R.ok(postService.getPost(id));
    }

    @RequiresPermissions("system:post:add")
    @Log(title = "岗位管理", businessType = Log.BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@RequestBody SysPostDO postDO) {
        return R.ok(postService.createPost(postDO));
    }

    @RequiresPermissions("system:post:add")
    @Log(title = "岗位管理", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Long> createCompat(@RequestBody SysPostDO postDO) {
        return R.ok(postService.createPost(postDO));
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> update(@RequestBody SysPostDO postDO) {
        postService.updatePost(postDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/update")
    public R<Boolean> updateCompat(@RequestBody SysPostDO postDO) {
        postService.updatePost(postDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:remove")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        postService.deletePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:remove")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    public R<Boolean> deleteCompat(@RequestParam("id") Long id) {
        postService.deletePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/{id}/move")
    public R<Boolean> move(@PathVariable("id") Long id,
                           @RequestParam(value = "parentId", required = false) Long parentId) {
        postService.movePost(id, parentId);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/move")
    public R<Boolean> moveCompat(@RequestParam("id") Long id,
                                 @RequestParam(value = "parentId", required = false) Long parentId) {
        postService.movePost(id, parentId);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:list")
    @GetMapping("/recycle/page")
    public R<PageResult<SysPostDO>> recyclePage(PageQuery pageQuery,
                                          @RequestParam(value = "postName", required = false) String postName) {
        Page<SysPostDO> page = postService.getRecyclePage(pageQuery, postName);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        postService.restorePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:edit")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/restore")
    public R<Boolean> restoreByPost(@RequestParam("id") Long id) {
        postService.restorePost(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:post:remove")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        postService.deletePostPermanent(id);
        return R.ok(true);
    }
}
