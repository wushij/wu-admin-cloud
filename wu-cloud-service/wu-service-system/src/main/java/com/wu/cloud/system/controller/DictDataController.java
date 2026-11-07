package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysDictDataDO;
import com.wu.cloud.system.mapper.DictDataMapper;
import com.wu.cloud.system.service.SysRecycleService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典数据（规范路径 /api/system/dict/data；P1 已去掉 /dict-data 兼容映射）
 */
@Tag(name = "字典数据")
@RestController
@RequestMapping("/api/system/dict/data")
public class DictDataController {

    private static final String DICT_CACHE_PREFIX = "system:dict:";

    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private SysRecycleService sysRecycleService;
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    /** 下拉：仅启用项 */
    @Operation(summary = "获取列表")
    @GetMapping("/type/{dictType}")
    public R<List<SysDictDataDO>> listByTypePath(@PathVariable("dictType") String dictType) {
        return R.ok(listEnabled(dictType));
    }

    @Operation(summary = "获取列表")
    @GetMapping("/list-by-type")
    public R<List<SysDictDataDO>> listByTypeQuery(@RequestParam("type") String type) {
        return R.ok(listEnabled(type));
    }

    /** 管理页：含停用项 */
    @RequiresPermissions("system:dict:list")
    @Operation(summary = "获取列表")
    @GetMapping("/manage/{dictType}")
    public R<List<SysDictDataDO>> listForManage(@PathVariable("dictType") String dictType) {
        return R.ok(dictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, dictType)
                .orderByAsc(SysDictDataDO::getSort)
                .orderByAsc(SysDictDataDO::getId)));
    }

    @Operation(summary = "批量操作")
    @GetMapping("/batch")
    public R<Map<String, List<SysDictDataDO>>> batch(@RequestParam("types") String types) {
        Map<String, List<SysDictDataDO>> result = new HashMap<>();
        if (!StringUtils.hasText(types)) {
            return R.ok(result);
        }
        List<String> typeList = Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        for (String type : typeList) {
            result.put(type, listEnabled(type));
        }
        return R.ok(result);
    }

    @RequiresPermissions("system:dict:update")
    @Operation(summary = "新增")
    @PostMapping("/refresh-cache")
    public R<Boolean> refreshCache() {
        Set<String> keys = redisTemplate.keys(DICT_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:create")
    @Log(title = "字典数据", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "上传工单附件")
    @PostMapping
    public R<Boolean> create(@RequestBody SysDictDataDO dictDataDO) {
        validateData(dictDataDO);
        if (dictDataDO.getStatus() == null) {
            dictDataDO.setStatus(1);
        }
        if (dictDataDO.getSort() == null) {
            dictDataDO.setSort(0);
        }
        dictDataMapper.insert(dictDataDO);
        evictCache(dictDataDO.getDictType());
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:create")
    @Log(title = "字典数据", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Boolean> createCompat(@RequestBody SysDictDataDO dictDataDO) {
        return create(dictDataDO);
    }

    @RequiresPermissions("system:dict:update")
    @Log(title = "字典数据", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@RequestBody SysDictDataDO dictDataDO) {
        if (dictDataDO.getId() == null) {
            throw new BusinessException("字典数据ID不能为空");
        }
        validateData(dictDataDO);
        dictDataMapper.updateById(dictDataDO);
        evictCache(dictDataDO.getDictType());
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:update")
    @Log(title = "字典数据", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    public R<Boolean> updateCompat(@RequestBody SysDictDataDO dictDataDO) {
        return update(dictDataDO);
    }

    @RequiresPermissions("system:dict:delete")
    @Log(title = "字典数据", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        SysDictDataDO data = dictDataMapper.selectById(id);
        dictDataMapper.deleteById(id);
        if (data != null) {
            evictCache(data.getDictType());
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:delete")
    @Log(title = "字典数据", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    public R<Boolean> deleteCompat(@RequestParam("id") Long id) {
        return delete(id);
    }

    @RequiresPermissions("system:dict:list")
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/recycle/page")
    public R<PageResult<SysDictDataDO>> recyclePage(PageQuery pageQuery,
                                                    @RequestParam(required = false) String dictType,
                                                    @RequestParam(required = false) String dictLabel) {
        Page<SysDictDataDO> page = sysRecycleService.getDictDataRecyclePage(pageQuery, dictType, dictLabel);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:dict:update")
    @Log(title = "字典数据", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        if (!sysRecycleService.restoreDictData(id)) {
            throw new BusinessException(404, "回收站字典数据不存在");
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:delete")
    @Log(title = "字典数据", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        if (!sysRecycleService.purgeDictData(id)) {
            throw new BusinessException(404, "回收站字典数据不存在");
        }
        return R.ok(true);
    }

    private List<SysDictDataDO> listEnabled(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, dictType)
                .eq(SysDictDataDO::getStatus, 1)
                .orderByAsc(SysDictDataDO::getSort)
                .orderByAsc(SysDictDataDO::getId));
    }

    private void validateData(SysDictDataDO data) {
        if (data == null || !StringUtils.hasText(data.getDictType())
                || !StringUtils.hasText(data.getLabel())
                || !StringUtils.hasText(data.getValue())) {
            throw new BusinessException("字典类型、标签与键值不能为空");
        }
    }

    private void evictCache(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return;
        }
        redisTemplate.delete(DICT_CACHE_PREFIX + dictType);
    }
}
