package gg;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

class Util {
    static Properties getProperties(String[] args) {
        if(args.length!=1){
            System.err.println("Property file is missing");
            System.exit(-1);
        }

        System.out.println("Start. Parameters: " + Arrays.asList(args));
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
