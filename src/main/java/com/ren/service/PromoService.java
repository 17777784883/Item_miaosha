package com.ren.service;

import com.ren.service.model.PromoModel;
import org.springframework.stereotype.Repository;

/**
 * @author Ren
 */
@Repository
public interface PromoService {

    /**
     * 根据itemid获取即将进行的秒杀活动 或 正在进行的秒杀活动
      */
    PromoModel getPromoByItemId(Integer itemId);


    // 活动发布
    void publishPromo(Integer promoId);

    // 生成秒杀用的令牌
    String generateSecondKillToken(Integer PROMOiD,Integer itemId,Integer userId);

}
