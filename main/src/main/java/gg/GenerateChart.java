package gg;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class GenerateChart {

    void generateChart(StatisticalInfo statisticalInfo, String outputFilePath) throws Exception {
        Path dashboardTemplateFile = Paths.get(getClass().getClassLoader().getResource("chart_template.html").toURI());
        Charset charset = StandardCharsets.UTF_8;

        String dashboardSource = new String(Files.readAllBytes(dashboardTemplateFile), charset);
        dashboardSource = dashboardSource.replaceAll("XXX", statisticalInfo.fixedRows.toString());
        Path outputFile = Paths.get(outputFilePath);

        Files.write(outputFile, dashboardSource.getBytes(charset));
    }
}
