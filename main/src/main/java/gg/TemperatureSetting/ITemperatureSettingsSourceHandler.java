package gg.TemperatureSetting;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

public interface ITemperatureSettingsSourceHandler {
    Stream<String> readLineStream();
    void backupAndWriteFile(Set<TemperatureRangeSetting> v) throws IOException;
}
