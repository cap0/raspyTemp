package gg;

import java.time.LocalDateTime;

class DateRange {
    final LocalDateTime sd;
    final LocalDateTime ed;

    DateRange(LocalDateTime sd, LocalDateTime ed) {
        this.sd = sd;
        this.ed = ed;
    }
}
