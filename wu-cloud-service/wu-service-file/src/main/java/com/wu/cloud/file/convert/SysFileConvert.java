package com.wu.cloud.file.convert;

import com.wu.cloud.file.entity.SysFileDO;
import com.wu.cloud.file.vo.FileRespVO;

/**
 * 文件对象转换工具类
 */
public class SysFileConvert {

    public static FileRespVO convertToVO(SysFileDO fileDO) {
        if (fileDO == null) {
            return null;
        }
        FileRespVO vo = new FileRespVO();
        vo.setId(fileDO.getId());
        vo.setOriginalName(fileDO.getOriginalName());
        vo.setFileName(fileDO.getFileName());
        vo.setUrl(fileDO.getUrl());
        vo.setFileSize(fileDO.getFileSize());
        vo.setFileType(fileDO.getFileType());
        vo.setCreateTime(fileDO.getCreateTime());
        return vo;
    }
}
