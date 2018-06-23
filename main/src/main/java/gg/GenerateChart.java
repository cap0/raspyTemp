package gg;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

class GenerateChart {

    private static final String BREW_NAME = "brewName";
    private static final String BREW_DAY_DATE = "brewDayDate";

    void generateChart(StatisticalInfo statisticalInfo, String outputFilePath, Properties p) throws Exception {
        Path dashboardTemplateFile = Paths.get(getClass().getClassLoader().getResource("chart_template.html").toURI());
        Charset charset = StandardCharsets.UTF_8;

        String dashboardSource = new String(Files.readAllBytes(dashboardTemplateFile), charset);

        String brewName = p.getProperty(BREW_NAME);
        String brewDate = p.getProperty(BREW_DAY_DATE);

        dashboardSource = dashboardSource.replaceAll(BREW_NAME, brewName);
        dashboardSource = dashboardSource.replaceAll(BREW_DAY_DATE, brewDate);
        dashboardSource = dashboardSource.replaceAll("XXX", statisticalInfo.temperatures.toString());
        Path outputFile = Paths.get(outputFilePath);

        Files.write(outputFile, dashboardSource.getBytes(charset));
    }
}
