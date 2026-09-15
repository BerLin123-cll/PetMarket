package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.UploadFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传文件记录数据访问层
 */
@Mapper
public interface UploadFileMapper extends BaseMapper<UploadFile> {
}
