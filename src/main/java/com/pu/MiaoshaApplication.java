package com.pu;

import com.pu.dao.UserMapper;
import com.pu.domain.User;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestController集成了@Controller 和@ResponseBody
 * SpringBootApplication:集成EnableAutoConfiguration和ComponentScan
 */
@SpringBootApplication(scanBasePackages = {"com.pu"})
@RestController
@MapperScan("com.pu.dao")
public class MiaoshaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiaoshaApplication.class, args);
    }

}
