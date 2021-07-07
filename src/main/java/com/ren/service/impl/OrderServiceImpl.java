package com.ren.service.impl;

import com.ren.dao.OrderDOMapper;
import com.ren.dao.SequenceDOMapper;
import com.ren.dao.StockLogDOMapper;
import com.ren.dataObject.OrderDO;
import com.ren.dataObject.SequenceDO;
import com.ren.dataObject.StockLogDO;
import com.ren.error.BusinessException;
import com.ren.error.EmBusinessError;
import com.ren.service.ItemService;
import com.ren.service.OrderService;
import com.ren.service.UserService;
import com.ren.service.model.ItemModel;
import com.ren.service.model.OrderModel;
import com.ren.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Ren
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    @Lazy(true)
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;


    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount,String stockLogId) throws BusinessException {


        //1. 校验下单状态，下单的商品是否存在  用户是否合法  购买数量是否正确
        //ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }


        /**System.out.println(userId);

        //UserModel userModel = userService.getUserById(userId);
        UserModel userModel = userService.getUserByIdInCache(userId);
        System.out.println(userModel.toString());
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");
        }*/

        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不正确");
        }

        // 校验活动信息
        /*if (promoId != null) {
            //(1) 校验对应活动是否存在这个使用商品
            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
                //(2) 校验活动是否在进行中
            } else if (itemModel.getPromoModel().getStatus().intValue() != 2) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信尚未开始");
            }
        }*/


        //2. 落单减库存(提交订单减库存)，支付减库存(无法保证支付成功后 对应的库存还有，可能会超卖)
        System.out.println(itemId + " --- " + amount);


        // 减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH, "库存不足");
        }


        // 3. 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setPromoId(promoId);

        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        //生成交易流水号，订单号
        orderModel.setId(generateOrderNo());
        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        // 使商品销量增加
        itemService.increaseSales(itemId, amount);

        // 设置库存流水状态为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if(stockLogDO == null){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        /*
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            // 在最近的一个transfactional 标签成功commit后，该方法执行
            public void afterCommit(){
                // 异步更新库存
                boolean mqResult = itemService.asyncDecreaseStock(itemId,amount);
                if(!mqResult){
                    try {
                        itemService.increaseStock(itemId,amount);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                    }
                    try {
                        throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });*/


        // 4. 返回前端

        return orderModel;
    }


    /**
     * 数据类型转换
     *
     * @param orderModel
     * @return
     */
    private OrderDO convertFromOrderModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }

    /**
     * 生成订单号的方法
     * <p>
     * 下面这个注解的意思是 只要执行了这段代码块  外部的事务无论成功与否  对应的sql都被提交掉
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNo() {
        // 订单号16位
        StringBuilder stringBuilder = new StringBuilder(16);

        // 前8位为时间信息  年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        // 中间6位为自增序列
        // 获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKey(sequenceDO);

        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        // 最后两位为分库分表位
        stringBuilder.append("00");

        return stringBuilder.toString();

    }
}
