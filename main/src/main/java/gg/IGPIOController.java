package gg;

import gg.util.Status;

public interface IGPIOController {
    void startFridge();

    void stopFridge();

    void startBelt();

    void stopBelt();

    void stop();

    Status getStatus();
}
