package gg.LCD;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LCDMock implements ILCD {

    private static final Logger logger = LogManager.getLogger(LCDMock.class);

    @Override
    public void print(String row0, String row1) {
        logger.info(row0 +"\n" + row1);

    }
}
