package gg;

import java.util.Arrays;
import java.util.Properties;

public class Util {
    static Properties getProperties(String[] args) {
        if(args.length!=1){
            System.err.println("Property file is missing");
            System.exit(-1);
        }

        System.out.println("Start. Parameters: " + Arrays.asList(args));
        return Main.getProperties(args[0]);
    }
}
