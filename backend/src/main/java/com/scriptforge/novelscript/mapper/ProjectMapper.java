package com.scriptforge.novelscript.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptforge.novelscript.entity.persistence.ProjectRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<ProjectRecord> {
}
