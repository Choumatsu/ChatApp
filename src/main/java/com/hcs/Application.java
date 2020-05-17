package com.hcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;


@SpringBootApplication
@MapperScan("com.hcs.mapper")//扫描mybatis mapper路径
@ComponentScan(basePackages = {"com.hcs","org.n3r.idworker"})
public class Application {

	@Bean
	public SpringUtil getSpringUtil(){
		return new SpringUtil();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class,args);
	}
	

} 
