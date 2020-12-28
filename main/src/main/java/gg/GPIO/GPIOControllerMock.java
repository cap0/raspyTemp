package gg.GPIO;

import gg.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GPIOControllerMock implements IGPIOController {

    private static final Logger logger = LogManager.getLogger(GPIOControllerMock.class);
    private Status s;

    @Override
    public void startFridge() {
        logger.info("startFridge");
        s = Status.cold;
    }

    @Override
    public void stopFridge() {
        logger.info("stopFridge");
        s = Status.ferm;
    }

    @Override
    public void startBelt() {
        logger.info("startBelt");
        s = Status.warm;
    }

    @Override
    public void stopBelt() {
        logger.info("stopBelt");
        s = Status.ferm;
    }

    @Override
    public void stop() {
        logger.info("stop");
        s = Status.ferm;
    }

    @Override
    public Status getStatus() {
        return s;
    }
}
