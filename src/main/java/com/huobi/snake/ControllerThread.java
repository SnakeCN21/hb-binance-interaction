package com.huobi.snake;

import com.huobi.snake.constants.Constants;
import com.huobi.snake.service.TopService;
import com.huobi.snake.service.hb.BNBLatestPriceServiceImpl;
import com.huobi.snake.service.hb.BTCLatestPriceServiceImpl;
import com.huobi.snake.service.bn.BNBNBLatestPriceServiceImpl;
import com.huobi.snake.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class ControllerThread extends Thread {
    private static Logger logger = LoggerFactory.getLogger(ControllerThread.class);

    private TopService topService;

    private static Constants cons = new Constants();
    private static Utils utils = new Utils();

    public ControllerThread(TopService topService) {
        this.topService = topService;
    }

    @Override
    public void run() {
        // 设置 LocalDateTime 的精度到秒
        LocalDateTime now = LocalDateTime.of(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth(),
                LocalDateTime.now().getHour(),
                LocalDateTime.now().getMinute(),
                LocalDateTime.now().getSecond());

        topService.getLatestPrice(now);
    }

    public static void main(String args[]) {
        String hbBtcSwitch = utils.getPropValues("hb_btc_switch");
        String hbBnbSwitch = utils.getPropValues("hb_bnb_switch");

        String bnBnbSwitch = utils.getPropValues("bn_bnb_switch");

        if (hbBtcSwitch.equals(cons.SWITCH_ON)) {
            BTCLatestPriceServiceImpl btc = new BTCLatestPriceServiceImpl();

            ControllerThread btcThread = new ControllerThread(btc);

            logger.debug("HB.BTCLatestPriceServiceImpl.getLatestPrice() 开始执行....");
            btcThread.start();
        }

        if (hbBnbSwitch.equals(cons.SWITCH_ON)) {
            BNBLatestPriceServiceImpl bnb = new BNBLatestPriceServiceImpl();

            ControllerThread btcThread = new ControllerThread(bnb);

            logger.debug("HB.BNBLatestPriceServiceImpl.getLatestPrice() 开始执行....");
            btcThread.start();
        }

        if (bnBnbSwitch.equals(cons.SWITCH_ON)) {
            BNBNBLatestPriceServiceImpl bnBNB = new BNBNBLatestPriceServiceImpl();

            ControllerThread bnBNBThread = new ControllerThread(bnBNB);

            logger.debug("BN.BNBLatestPriceServiceImpl.getLatestPrice() 开始执行....");
            bnBNBThread.start();
        }
    }

}
