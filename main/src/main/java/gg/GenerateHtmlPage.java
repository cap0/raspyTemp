package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Scanner;

import static gg.Util.toJsDate;


class GenerateHtmlPage {
    private static final Logger logger = LogManager.getLogger(Orchestrator.class);

    void generateChart(StatisticalInfo statisticalInfo, String outputFilePath, Properties p) {
        String dashboardSource = getHtmlPageTemplate();
        dashboardSource = addPropertiesInfoToTemplate(p, dashboardSource);
        dashboardSource = addDataToTemplate(statisticalInfo, dashboardSource);

        Path outputFile = Paths.get(outputFilePath);
        try {
            Files.write(outputFile, dashboardSource.getBytes(StandardCharsets.UTF_8));
            logger.info("chart generated");
        } catch (IOException e) {
            logger.error("Cannot write html page file on disk", e);
        }
    }

    private String addDataToTemplate(StatisticalInfo statisticalInfo, String dashboardSource) {
        dashboardSource = dashboardSource.replaceAll("param.startDate", toJsDate(statisticalInfo.startDate));
        dashboardSource = dashboardSource.replaceAll("param.endDate",  toJsDate(statisticalInfo.endDate));
        dashboardSource = dashboardSource.replaceAll("param.now",  LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dashboardSource = dashboardSource.replaceAll("XXX", statisticalInfo.temperatures.toString());
        return dashboardSource;
    }

    private String addPropertiesInfoToTemplate(Properties p, String dashboardSource) {
        Enumeration<String> propertyNames = (Enumeration<String>) p.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = propertyNames.nextElement();
            if(propertyName.startsWith("info.")) {
                dashboardSource = dashboardSource.replaceAll(propertyName, p.getProperty(propertyName));
            }
        }
        return dashboardSource;
    }

    private String getHtmlPageTemplate() {
        InputStream is = getClass().getResourceAsStream("/chart_template.html") ;
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
