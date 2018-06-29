package gg;

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
    void generateChart(StatisticalInfo statisticalInfo, String outputFilePath, Properties p) throws Exception {
        String dashboardSource = getHtmlPageTemplate();
        dashboardSource = addPropertiesInfoToTemplate(p, dashboardSource);
        dashboardSource = addDataToTemplate(statisticalInfo, dashboardSource);

        Path outputFile = Paths.get(outputFilePath);
        Files.write(outputFile, dashboardSource.getBytes(StandardCharsets.UTF_8));
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
