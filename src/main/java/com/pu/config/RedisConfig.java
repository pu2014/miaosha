package com.pu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Component;

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
}
