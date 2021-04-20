package com.huobi.snake.response.market;

import lombok.Data;

/**
 * 用来组装返回的'最近价格'的对象
 */
@Data
public class BNMarketTradeResponse {
    /**
     * "symbol": "BTCUSD_200626",  // 交易对
     * "ps": "BTCUSD",             // 标的交易对
     * "price": "9647.8",          // 价格
     * "time": 1591257246176       // 时间
     */

    private String symbol;
    private String ps;
    private String price;
    private Long time;

    public BNMarketTradeResponse(String symbol, String ps, String price, Long time) {
        this.symbol = symbol;
        this.ps = ps;
        this.price = price;
        this.time = time;
    }

}
