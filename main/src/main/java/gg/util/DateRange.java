package gg.util;

import java.time.LocalDateTime;

public class DateRange {
    public final LocalDateTime sd;
    public final LocalDateTime ed;

    public DateRange(LocalDateTime sd, LocalDateTime ed) {
        this.sd = sd;
        this.ed = ed;
    }
}
