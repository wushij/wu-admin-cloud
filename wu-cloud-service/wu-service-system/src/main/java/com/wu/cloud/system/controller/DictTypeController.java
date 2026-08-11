package com.wu.cloud.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysDictDataDO;
import com.wu.cloud.system.entity.SysDictTypeDO;
import com.wu.cloud.system.mapper.DictDataMapper;
import com.wu.cloud.system.mapper.DictTypeMapper;
import com.wu.cloud.system.service.SysRecycleService;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典类型（兼容 /dict-type 与网关 rewrite 后的 /dict/type）
 */
@RestController
@RequestMapping({"/api/system/dict-type", "/api/system/dict/type"})
public class DictTypeController {

    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private SysRecycleService sysRecycleService;

    @RequiresPermissions("system:dict:list")
    @GetMapping("/page")
    public R<PageResult<SysDictTypeDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysDictTypeDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictName)) {
            wrapper.like(SysDictTypeDO::getName, dictName.trim());
        }
        if (StringUtils.hasText(dictType)) {
            wrapper.like(SysDictTypeDO::getType, dictType.trim());
        }
        if (status != null) {
            wrapper.eq(SysDictTypeDO::getStatus, status);
        }
        wrapper.orderByDesc(SysDictTypeDO::getId);
        Page<SysDictTypeDO> page = dictTypeMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        for (SysDictTypeDO item : page.getRecords()) {
            Long count = dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictDataDO>()
                    .eq(SysDictDataDO::getDictType, item.getType()));
            item.setDataCount(count == null ? 0 : count.intValue());
        }
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:dict:list")
    @GetMapping("/list")
    public R<List<SysDictTypeDO>> list() {
        return R.ok(dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getStatus, 1)
                .orderByAsc(SysDictTypeDO::getId)));
    }

    @RequiresPermissions("system:dict:list")
    @GetMapping("/{id}")
    public R<SysDictTypeDO> detail(@PathVariable("id") Long id) {
        SysDictTypeDO type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        return R.ok(type);
    }

    @RequiresPermissions("system:dict:list")
    @GetMapping("/{id}/export")
    public R<Map<String, Object>> export(@PathVariable("id") Long id) {
        SysDictTypeDO type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        List<SysDictDataDO> data = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, type.getType())
                .orderByAsc(SysDictDataDO::getSort));
        Map<String, Object> body = new HashMap<>(2);
        body.put("dictType", type);
        body.put("dictDataList", data);
        return R.ok(body);
    }

    @RequiresPermissions("system:dict:add")
    @Log(title = "字典类型", businessType = Log.BusinessType.INSERT)
    @PostMapping
    public R<Boolean> create(@RequestBody SysDictTypeDO dictTypeDO) {
        validateType(dictTypeDO);
        assertTypeUnique(dictTypeDO.getType(), null);
        if (dictTypeDO.getStatus() == null) {
            dictTypeDO.setStatus(1);
        }
        dictTypeMapper.insert(dictTypeDO);
        return R.ok(true);
    }

    /** 兼容旧路径 */
    @RequiresPermissions("system:dict:add")
    @Log(title = "字典类型", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Boolean> createCompat(@RequestBody SysDictTypeDO dictTypeDO) {
        return create(dictTypeDO);
    }

    @RequiresPermissions("system:dict:edit")
    @Log(title = "字典类型", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> update(@RequestBody SysDictTypeDO dictTypeDO) {
        if (dictTypeDO.getId() == null) {
            throw new BusinessException("字典类型ID不能为空");
        }
        validateType(dictTypeDO);
        assertTypeUnique(dictTypeDO.getType(), dictTypeDO.getId());
        SysDictTypeDO old = dictTypeMapper.selectById(dictTypeDO.getId());
        if (old == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        String oldType = old.getType();
        dictTypeMapper.updateById(dictTypeDO);
        if (StringUtils.hasText(dictTypeDO.getType()) && !dictTypeDO.getType().equals(oldType)) {
            dictDataMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysDictDataDO>()
                    .eq(SysDictDataDO::getDictType, oldType)
                    .set(SysDictDataDO::getDictType, dictTypeDO.getType()));
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:edit")
    @Log(title = "字典类型", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/update")
    public R<Boolean> updateCompat(@RequestBody SysDictTypeDO dictTypeDO) {
        return update(dictTypeDO);
    }

    @RequiresPermissions("system:dict:remove")
    @Log(title = "字典类型", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(@PathVariable("id") Long id) {
        SysDictTypeDO type = dictTypeMapper.selectById(id);
        if (type == null) {
            return R.ok(true);
        }
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, type.getType()));
        dictTypeMapper.deleteById(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:remove")
    @Log(title = "字典类型", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    public R<Boolean> deleteCompat(@RequestParam("id") Long id) {
        return delete(id);
    }

    @RequiresPermissions("system:dict:add")
    @Log(title = "字典类型", businessType = Log.BusinessType.INSERT)
    @PostMapping("/{id}/copy")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> copy(@PathVariable("id") Long id) {
        SysDictTypeDO source = dictTypeMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        String newType = source.getType() + "_copy";
        int i = 1;
        while (dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getType, newType)) > 0) {
            newType = source.getType() + "_copy" + (i++);
        }
        SysDictTypeDO target = new SysDictTypeDO();
        target.setName(source.getName() + "（副本）");
        target.setType(newType);
        target.setStatus(source.getStatus());
        target.setRemark(source.getRemark());
        dictTypeMapper.insert(target);

        List<SysDictDataDO> dataList = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, source.getType())
                .orderByAsc(SysDictDataDO::getSort));
        for (SysDictDataDO data : dataList) {
            SysDictDataDO copy = new SysDictDataDO();
            copy.setSort(data.getSort());
            copy.setLabel(data.getLabel());
            copy.setValue(data.getValue());
            copy.setDictType(newType);
            copy.setStatus(data.getStatus());
            copy.setColorType(data.getColorType());
            copy.setCssClass(data.getCssClass());
            copy.setIsDefault(data.getIsDefault());
            copy.setRemark(data.getRemark());
            dictDataMapper.insert(copy);
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:list")
    @GetMapping("/recycle/page")
    public R<PageResult<SysDictTypeDO>> recyclePage(PageQuery pageQuery,
                                                    @RequestParam(required = false) String dictName,
                                                    @RequestParam(required = false) String dictType) {
        String keyword = StringUtils.hasText(dictName) ? dictName
                : (StringUtils.hasText(dictType) ? dictType : null);
        Page<SysDictTypeDO> page = sysRecycleService.getDictTypeRecyclePage(pageQuery, keyword);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:dict:edit")
    @Log(title = "字典类型", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        if (!sysRecycleService.restoreDictType(id)) {
            throw new BusinessException(404, "回收站字典类型不存在");
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dict:remove")
    @Log(title = "字典类型", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        if (!sysRecycleService.purgeDictType(id)) {
            throw new BusinessException(404, "回收站字典类型不存在");
        }
        return R.ok(true);
    }

    private void validateType(SysDictTypeDO dictTypeDO) {
        if (dictTypeDO == null || !StringUtils.hasText(dictTypeDO.getName())
                || !StringUtils.hasText(dictTypeDO.getType())) {
            throw new BusinessException("字典名称与类型编码不能为空");
        }
    }

    private void assertTypeUnique(String type, Long excludeId) {
        LambdaQueryWrapper<SysDictTypeDO> wrapper = new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getType, type);
        if (excludeId != null) {
            wrapper.ne(SysDictTypeDO::getId, excludeId);
        }
        if (dictTypeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("字典类型编码已存在");
        }
    }
}
