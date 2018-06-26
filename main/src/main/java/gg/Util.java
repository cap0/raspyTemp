package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

class Util {
    private static final Logger logger = LogManager.getLogger(Util.class);

    static Properties getProperties(String[] args) {
        if(args.length!=1){
            logger.error("Property file is missing");
            System.exit(-1);
        }

        logger.info("Start. Parameters: " + Arrays.asList(args));
        return getProperties(args[0]);
    }

    private static Properties getProperties(String arg) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(arg));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        p.list(System.out);

        return p;
    }
}
