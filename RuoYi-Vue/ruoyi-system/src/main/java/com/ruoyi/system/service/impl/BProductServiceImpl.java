package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Objects;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.ShopProduct;
import com.ruoyi.system.domain.dto.BShopProductPageView;
import com.ruoyi.system.domain.dto.BShopProductQuery;
import com.ruoyi.system.domain.dto.BShopProductSoldOutRequest;
import com.ruoyi.system.domain.dto.BShopProductView;
import com.ruoyi.system.domain.dto.StockAdjustRequest;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.service.IBProductService;
import com.ruoyi.system.service.IProductCenterService;

/** 门店员工端商品管理服务实现。 */
@Service
public class BProductServiceImpl implements IBProductService
{
    private static final Logger log = LoggerFactory.getLogger(BProductServiceImpl.class);

    @Autowired private ProductCenterMapper productCenterMapper;
    @Autowired private IProductCenterService productCenterService;

    @Override
    public List<Category> listCategories(Long shopId)
    {
        requireShop(shopId);
        return productCenterMapper.selectBShopProductCategories(shopId);
    }

    @Override
    public BShopProductPageView listProducts(Long shopId, BShopProductQuery query)
    {
        requireShop(shopId);
        BShopProductQuery effectiveQuery = query == null ? new BShopProductQuery() : query;
        effectiveQuery.setShopId(shopId);
        effectiveQuery.normalize();
        PageHelper.startPage(effectiveQuery.getPageNum(), effectiveQuery.getPageSize());
        List<BShopProductView> rows = productCenterMapper.selectBShopProductList(effectiveQuery);
        PageInfo<BShopProductView> pageInfo = new PageInfo<>(rows);
        rows.forEach(this::fillProductDesc);
        return new BShopProductPageView(rows, pageInfo.getTotal(), effectiveQuery.getPageNum(), effectiveQuery.getPageSize());
    }

    @Override
    public void updateStatus(Long shopId, Long shopProductId, Integer status, Long operatorId)
    {
        ShopProduct shopProduct = requireShopProductInShop(shopId, shopProductId);
        if (status == null || status < 0 || status > 1)
        {
            throw new ServiceException("商品状态不正确", HttpStatus.BAD_REQUEST);
        }
        if (Objects.equals(shopProduct.getStatus(), status))
        {
            log.debug("员工端门店商品状态幂等返回 shopId={} shopProductId={} operatorId={} status={}",
                    shopId, shopProductId, operatorId, status);
            return;
        }
        int rows = productCenterMapper.updateShopProductStatus(shopProductId, status);
        if (rows == 0)
        {
            throw new ServiceException("商品状态已变更，请刷新后重试", HttpStatus.CONFLICT);
        }
        log.info("员工端门店商品状态更新 shopId={} shopProductId={} operatorId={} status={}",
                shopId, shopProductId, operatorId, status);
    }

    @Override
    public void setSoldOut(Long shopId, Long shopProductId, BShopProductSoldOutRequest request, Long operatorId)
    {
        ShopProduct shopProduct = requireShopProductInShop(shopId, shopProductId);
        if (request == null || request.getSoldOut() == null)
        {
            throw new ServiceException("售空状态不能为空", HttpStatus.BAD_REQUEST);
        }
        int targetStock = Boolean.TRUE.equals(request.getSoldOut()) ? 0 : -1;
        StockAdjustRequest stockRequest = new StockAdjustRequest();
        stockRequest.setRequestId(request.getRequestId());
        stockRequest.setStock(targetStock);
        stockRequest.setReason(request.getReason());
        productCenterService.adjustStock(shopProductId, stockRequest);
        log.info("员工端门店商品售空状态更新 shopId={} shopProductId={} operatorId={} soldOut={} requestId={}",
                shopId, shopProductId, operatorId, request.getSoldOut(), request.getRequestId());
    }

    private void requireShop(Long shopId)
    {
        if (shopId == null || productCenterMapper.countShopById(shopId) == 0)
        {
            throw new ServiceException("门店不存在或已删除", HttpStatus.NOT_FOUND);
        }
    }

    private ShopProduct requireShopProductInShop(Long shopId, Long shopProductId)
    {
        if (shopId == null || shopProductId == null)
        {
            throw new ServiceException("门店商品参数不能为空", HttpStatus.BAD_REQUEST);
        }
        ShopProduct shopProduct = productCenterMapper.selectShopProductById(shopProductId);
        if (shopProduct == null)
        {
            throw new ServiceException("门店商品不存在", HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(shopProduct.getShopId(), shopId))
        {
            throw new ServiceException("商品不属于当前门店", HttpStatus.FORBIDDEN);
        }
        return shopProduct;
    }

    private void fillProductDesc(BShopProductView view)
    {
        view.setStatusDesc(Integer.valueOf(1).equals(view.getStatus()) ? "销售中" : "已停售");
        Integer stock = view.getStock();
        view.setSoldOut(stock != null && stock == 0);
        if (stock == null)
        {
            view.setStockDesc("--");
        }
        else if (stock == 0)
        {
            view.setStockDesc("已售空");
        }
        else
        {
            view.setStockDesc("有货");
        }
    }
}
