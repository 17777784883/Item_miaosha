package com.ren.service;

import com.ren.error.BusinessException;
import com.ren.service.model.ItemModel;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Ren
 */

@Repository
public interface ItemService {

    // 创建商品

    ItemModel createItem(ItemModel itemModel) throws Exception;

    // 商品列表浏览
    List<ItemModel> listItem();


    // 商品详情浏览
    ItemModel getItemById(Integer id);


    //item 及 promo model缓存模型
    ItemModel getItemByIdInCache(Integer id);


    // 库存扣减
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    //库存回补
    boolean increaseStock(Integer itemId,Integer amount) throws BusinessException;


    // 异步更新库存
    boolean asyncDecreaseStock(Integer itemId,Integer amount);



    //商品销量增加
    void increaseSales(Integer itemId, Integer amount) throws BusinessException;


    //初始化库存流水
    String initStockLog(Integer itemId,Integer amount);



}
