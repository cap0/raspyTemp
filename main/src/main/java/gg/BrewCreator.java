package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoField;

public class BrewCreator {
    private static final Logger logger = LogManager.getLogger(BrewCreator.class);

    public void create(String name) {
        logger.info("hold your seat, we are starting a new journey");
        name = name.replaceAll("\\s+", "");
        logger.info("name: " + name);

        int month = LocalDate.now().get(ChronoField.MONTH_OF_YEAR);
        int year = LocalDate.now().get(ChronoField.YEAR);

        String folderName = year + month + name;

        try {
            Files.createDirectories(Paths.get(folderName));
        } catch (IOException e) {
            logger.error("unable to create dir", e);
            return;
        }


    }

}
