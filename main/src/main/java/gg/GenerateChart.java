package gg;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

class GenerateChart {
    void generateChart(StatisticalInfo statisticalInfo, String outputFilePath, Properties p) throws Exception {
        Path dashboardTemplateFile = Paths.get(getClass().getClassLoader().getResource("chart_template.html").toURI());
        Charset charset = StandardCharsets.UTF_8;

        String dashboardSource = new String(Files.readAllBytes(dashboardTemplateFile), charset);

        Enumeration<String> propertyNames = (Enumeration<String>) p.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = propertyNames.nextElement();
            if(propertyName.startsWith("info.")) {
                dashboardSource = dashboardSource.replaceAll(propertyName, p.getProperty(propertyName));
            }
        }

        dashboardSource = dashboardSource.replaceAll("XXX", statisticalInfo.temperatures.toString());

        Path outputFile = Paths.get(outputFilePath);

        Files.write(outputFile, dashboardSource.getBytes(charset));
    }
}
