package com.huobi.snake.service;

import java.time.LocalDateTime;

public interface TopService {
    /**
     * 主控制器，每隔一段时间依次调用 getMarketTrade()，获取最新合约价格
     */
    void getLatestPrice(LocalDateTime startDT);
}
