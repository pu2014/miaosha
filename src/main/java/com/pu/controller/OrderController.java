package com.pu.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.pu.response.ReturnType;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.mq.MqProducer;
import com.pu.service.IItemService;
import com.pu.service.IOrderService;
import com.pu.service.IPromoService;
import com.pu.service.model.UserModel;
import com.pu.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 23:52
 */
@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends baseController {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private IItemService itemService;

    @Autowired
    private IPromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init(){
        //开启20个线程大小的线程池
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(300);
    }
    //生成验证码
    @ResponseBody
    @RequestMapping(value="/generateverifycode",method = {RequestMethod.GET,RequestMethod.POST})
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常,不能生成验证码");
        }
        //获取用户的登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息已经过期，不能生成验证码");
        }
        //创建文件输出流对象
        Map<String,Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_" + userModel.getId(), map.get("code"));
        redisTemplate.expire("verify_code_" + userModel.getId(), 3, TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());
    }


    @ResponseBody
    @RequestMapping(value="/generatetoken",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType generateToken(@RequestParam(name="itemId")Integer itemId,
                                    @RequestParam(name="promoId")Integer promoId,
                                    @RequestParam(name="verifyCode")String verifyCode) throws BusinessException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常");
        }
        //获取用户的登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息已经过期");
        }
        //获取验证码的有效性
        String inRedisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if(StringUtils.isEmpty(inRedisVerifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "请求非法");
        }
        if(!inRedisVerifyCode.equalsIgnoreCase(verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "验证码错误");
        }
        //获取秒杀访问令牌
        String secondKillToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());

        if(StringUtils.isEmpty(secondKillToken)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }
        return ReturnType.create(secondKillToken);
    }
    //封装下单请求
    @ResponseBody
    @RequestMapping(value="/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                  @RequestParam(name="amount")Integer amount,
                                  @RequestParam(name="promoId",required = false)Integer promoId,
                                  @RequestParam(name="promoToken",required = false)String promoToken) throws BusinessException {

        if(!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EmBusinessError.RETELIMIT_ERROR);
        }
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常");
        }
        //获取用户的登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息已经过期");
        }
        //校验秒杀令牌是否正确
        if(StringUtils.isEmpty(promoToken)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户不具有秒杀令牌");
        }else {
            if (!StringUtils.isEmpty(promoToken)) {
                String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userId_" + userModel.getId() + "_itemId_" + itemId);
                if (inRedisPromoToken == null || !StringUtils.equals(promoToken, inRedisPromoToken)) {
                    throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
                }
            }
        }
        //基于cookie的方式
        /*
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(isLogin == null || !isLogin.booleanValue()){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常");
        }
        UserModel loginUser = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");
        */


        //由mq事务接管
        //OrderModel order = orderService.createOrder(userModel.getId(), itemId, promoId, amount);

        //前置到秒杀令牌接口
        /*
        redisTemplate.opsForValue().get("promo_item_stock_invalid_" + itemId);
        if(redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        */
        //同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用来队列泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入库存流水init状态  用于追踪库存扣减异步消息
                String stockLogId = itemService.initStockLog(itemId, amount);
                //完成对应的下单事务型消息
                boolean createOrderResult = mqProducer.transactionAsynReduceStock(itemId, userModel.getId(), promoId, amount, stockLogId);
                if (!createOrderResult) {
                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        return ReturnType.create(null);
    }
}
