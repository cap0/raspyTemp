package gg.TemperatureSetting;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ITemperatureSettingsSourceHandler {
    List<String> readLineSettings();
    void backupAndWriteFile(Set<TemperatureRangeSetting> v) throws IOException;
}
