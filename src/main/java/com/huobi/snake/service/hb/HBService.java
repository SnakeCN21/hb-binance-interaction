package com.huobi.snake.service.hb;

import com.huobi.api.service.market.MarketAPIServiceImpl;
import com.huobi.snake.service.TopService;

public interface HBService extends TopService {

    /**
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，获取最新合约价格
     */
    @Override
    void getLatestPrice();

    /**
     * 根据合约标识获取'BTC交割合约'最新价格，并最终写入到数据库中
     *
     * @param huobiAPIService - 调用的API service
     * @param contractType    - 合约标识
     * @return
     */
    void getMarketTrade(MarketAPIServiceImpl huobiAPIService, String contractType);

    /**
     * 将获得的数据进行组装，并插入到数据表中
     *
     * @param contractType - 合约标识
     * @param price        - 合约对应的价格
     * @param time         - 获取价格的时间
     */
    void insertLatestPriceTbl(String contractType, String price, String time);
}
