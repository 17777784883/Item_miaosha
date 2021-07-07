package com.ren.service;

import com.ren.error.BusinessException;
import com.ren.service.model.OrderModel;

/**
 * @author Ren
 */

public interface OrderService {
    // 1. 通过前端url上传过来的秒杀活动id，然后下单接口内校验对应id是否属于对应商品街活动已开始
    // 2. 直接在下单接口内判断对应的商品是否存在秒杀活动，若存在进行中的则以秒杀价格下单
    // 使用第一种方案
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount,String stockLogId) throws BusinessException;


}
