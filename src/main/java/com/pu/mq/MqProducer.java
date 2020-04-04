package com.pu.mq;

import com.alibaba.fastjson.JSON;
import com.pu.dao.StockLogMapper;
import com.pu.domain.Item;
import com.pu.domain.StockLog;
import com.pu.error.BusinessException;
import com.pu.service.IOrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 * Created By @Author my on @Date 2020/4/3 10:26
 */
@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private StockLogMapper stockLogMapper;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;
    @PostConstruct
    public void init() throws MQClientException {
        //mqproducer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object args) {
                Map<String, Object> argsMap = (Map<String, Object>) args;
                //真正需要完成的环节 创建订单
                try {
                    orderService.createOrder((Integer)argsMap.get("userId"), (Integer)argsMap.get("itemId"), (Integer)argsMap.get("producer"), (Integer)argsMap.get("amount"),(String)argsMap.get("stockLogId"));
                } catch (BusinessException e) {
                    e.printStackTrace();
                    //回滚
                    //无论接收到消息没有
                    StockLog stockLog = stockLogMapper.selectByPrimaryKey((String)argsMap.get("stockLogId"));
                    stockLog.setStatus(3);
                    stockLogMapper.updateByPrimaryKeySelective(stockLog);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            /**
             * 1:下单过程成功，发送LocalTransactionState.COMMIT_MESSAGE过程中失败
             * 2：下单失败，LocalTransactionState.ROLLBACK_MESSAGE;消息发送过程中失败
             * 3：下单过程还没有结束，回调函数就执行了
             * @param msg
             * @return
             */

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                //根据是否扣减库存成功，来判断是否要返回COMMIT，ROLLBACK还是继续UNKNOWN
                String jsonString = new String(msg.getBody());
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId = (String) map.get("stockLogId");
                //无论接收到消息没有
                StockLog stockLog = stockLogMapper.selectByPrimaryKey(stockLogId);
                if(stockLog == null || stockLog.getStatus().intValue() == 1){
                    return LocalTransactionState.UNKNOW; //下次再来重试
                }
                if(stockLog.getStatus().intValue() == 2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }
        });

    }

    public boolean transactionAsynReduceStock(Integer itemId, Integer userId, Integer promoId, Integer amount, String stockLogId){

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("itemId", itemId);
        argsMap.put("amount", amount);
        argsMap.put("userId", userId);
        argsMap.put("promoId", promoId);
        argsMap.put("stockLogId", stockLogId);
        Message message = new Message(topicName, "increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        TransactionSendResult sendResult = null;
        try {
            sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);

        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        if(sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE){
            return false;
        }else if(sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 同步库存扣减消息
     * @param itemId
     * @param amount
     * @return
     */
    public boolean asyncReduceStock(Integer itemId, Integer amount){
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        Message message = new Message(topicName, "increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }
}
