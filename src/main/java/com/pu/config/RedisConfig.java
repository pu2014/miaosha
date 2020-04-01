package com.pu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.pu.serializer.JsonDateTimeJsonDeserializer;
import com.pu.serializer.JsonDateTimeJsonSerializer;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Component;

import javax.validation.Validation;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/31 21:04
 */
@Component
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {
    /**
     * SameSite Cookie 应该是一种新的cookie属性值,我看到很多大型网站如百度都没有用到，
     * 他是防止 CSRF 攻击 具体可看 https://www.cnblogs.com/ziyunfei/p/5637945.html
     *
     * spring web 最新版默认生成为SameSite=Lax,奇怪的是用spring data Session redis 后 cookie新增了 SameSite这个字段,所以不能携带cookie进行跨域post访问
     * ————————————————
     * 版权声明：本文为CSDN博主「Boom_Man」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
     * 原文链接：https://blog.csdn.net/boom_man/article/details/84642040
     * @return
     */
    @Bean
    public CookieSerializer httpSessionIdResolver() {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        // 取消仅限同一站点设置
        cookieSerializer.setSameSite(null);
        return cookieSerializer;
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //解决key的序列化方式
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);

        //解决value的序列化方式
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(DateTime.class, new JsonDateTimeJsonSerializer());
        simpleModule.addDeserializer(DateTime.class, new JsonDateTimeJsonDeserializer());
        /**
        反序列化会得到LinkedHashMap类型，该类型不能强转为具体的对象类型，即抛出
        java.lang.ClassCastException: java.util.LinkedHashMap cannot be cast to com.galax.pojo.Account
        所以我们需要ObjectMapper.DefaultTyping.NON_FINAL配置，在序列化时记录对象类型，以便反序列化时得到对应的具体对象。
         */
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(simpleModule);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        return redisTemplate;
    }
}
