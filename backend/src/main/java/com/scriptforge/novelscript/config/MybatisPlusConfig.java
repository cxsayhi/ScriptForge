package com.scriptforge.novelscript.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.scriptforge.novelscript.mapper")
public class MybatisPlusConfig {
}
