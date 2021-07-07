package com.ren.controller;

import com.ren.controller.viewObject.ItemVO;
import com.ren.response.CommonReturnType;
import com.ren.service.CacheService;
import com.ren.service.ItemService;
import com.ren.service.PromoService;
import com.ren.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Ren
 */
@Controller
@RequestMapping("/item")
//跨域请求中，不能做到session共享
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    //创建商品的controller
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "price") BigDecimal price,
                                       @RequestParam(name = "stock") Integer stock,
                                       @RequestParam(name = "imgUrl") String imgUrl) {
        //封装service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = null;
        try {
            itemModelForReturn = itemService.createItem(itemModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ItemVO itemVO = convertVOFromModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);

    }




    @RequestMapping(value = "/publishpromo", method = {RequestMethod.GET}/*, consumes = {CONTENT_TYPE_FORMED}*/)
    @ResponseBody
    public CommonReturnType publishpromo(@RequestParam(name = "id") Integer id) {
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }



    /**
     * 缓存流程：  先取本地缓存，若本地缓存没有则取redis缓存，redis缓存还没有则取数据库
     *
     */
    // 商品详情页浏览
    @RequestMapping(value = "/get", method = {RequestMethod.GET}/*, consumes = {CONTENT_TYPE_FORMED}*/)
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) {
        //ItemModel itemModel = null ;

        // 1. ------ 先取本地缓存
        ItemModel itemModel = (ItemModel) cacheService.getFromCommonCache("item_" + id);

        if (itemModel == null) {
            // 2.------- 根据商品的id到redis中获取
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);

            // 3.------- 若redis中不存在对应的itemModel ，则访问下游service
            if (itemModel == null) {
                itemModel = itemService.getItemById(id);
                //设置itemModel到redis内
                redisTemplate.opsForValue().set("item_" + id, itemModel);
                // 设置redis的失效时间,10分钟的失效时间
                redisTemplate.expire("item_" + id, 10, TimeUnit.MINUTES);
            }

            // 填充本地缓存
            cacheService.setCommonCache("item_"+id,itemModel);
        }




        ItemVO itemVO = convertVOFromModel(itemModel);
        return CommonReturnType.create(itemVO);
    }


    //商品列表页面浏览
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem() {
        List<ItemModel> itemModelList = itemService.listItem();
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = this.convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());

        return CommonReturnType.create(itemVOList);
    }


    private ItemVO convertVOFromModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if (itemModel.getPromoModel() != null) {
            // 有正在进行或即将开始的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());  //秒杀状态
            itemVO.setPromoId(itemModel.getPromoModel().getId());          // 秒杀Id
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().
                    toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))); // 秒杀时间
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());    // 秒杀金额
        } else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }

}