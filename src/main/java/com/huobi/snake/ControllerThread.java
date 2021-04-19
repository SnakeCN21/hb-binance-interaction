package com.huobi.snake;

import com.huobi.snake.constants.Constants;
import com.huobi.snake.service.BNBLatestPriceServiceImpl;
import com.huobi.snake.service.BTCLatestPriceServiceImpl;
import com.huobi.snake.service.LatestPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerThread extends Thread {
    private static Logger logger = LoggerFactory.getLogger(ControllerThread.class);

    private LatestPriceService latestPriceService;

    private static Constants cons = new Constants();

    public ControllerThread(LatestPriceService latestPriceService){
        this.latestPriceService = latestPriceService;
    }

    @Override
    public void run() {
        latestPriceService.getLatestPrice();
    }

    public static void main(String args[]) {
        String btcSwitch = cons.getPropValues("btc_switch");
        String bnbSwitch = cons.getPropValues("bnb_switch");

        if (btcSwitch.equals(cons.SWITCH_ON)) {
            BTCLatestPriceServiceImpl btc = new BTCLatestPriceServiceImpl();

            ControllerThread btcThread = new ControllerThread(btc);

            logger.debug("BTCLatestPriceServiceImpl.getBTCLatestPrice() 开始执行....");
            btcThread.start();
        }

        if (bnbSwitch.equals(cons.SWITCH_ON)) {
            BNBLatestPriceServiceImpl bnb = new BNBLatestPriceServiceImpl();

            ControllerThread btcThread = new ControllerThread(bnb);

            logger.debug("BNBLatestPriceServiceImpl.getLatestPrice() 开始执行....");
            btcThread.start();
        }
    }

}
