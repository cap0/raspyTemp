package gg;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GenerateChart {

    public static void main(String [] args) throws IOException {
        generateChart("[new Date(2008, 1 ,5 ,3, 4, 1,), 11, 4],\n" +
                "                [new Date(2008, 1 ,6, 3 , 6), 2, 1]");
    }

    public static void generateChart(String replacement) throws IOException {
        Path path = Paths.get("/Users/gabriele.gattari/raspyTemp/main/src/main/resources/chart_template.html");
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll("XXX", replacement);
        Path out = Paths.get("/Users/gabriele.gattari/raspyTemp/main/src/main/resources/chart_generated.html");

        Files.write(out, content.getBytes(charset));
    }
}
