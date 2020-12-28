package gg.GPIO;

import com.pi4j.io.gpio.*;
import gg.Controller;
import gg.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GPIOController implements IGPIOController{

    private static final Logger logger = LogManager.getLogger(Controller.class);

    private final GpioController gpio;
    private final GpioPinDigitalOutput fridgePin;
    private final GpioPinDigitalOutput beltPin;
    private Status status; //TODO volatile?

    public GPIOController() {
        gpio = GpioFactory.getInstance();

        fridgePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "fridgePin", PinState.HIGH);
        beltPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "beltPin", PinState.HIGH);

        gpio.setMode(PinMode.DIGITAL_OUTPUT, fridgePin, beltPin);

        fridgePin.setShutdownOptions(true, PinState.HIGH);
        beltPin.setShutdownOptions(true, PinState.HIGH);

        status = Status.ferm;
        shutdownHook();
    }

    @Override
    public void startFridge() {
        logger.debug("request to start fridge");
        beltPin.high();

        if (fridgePin.isLow()) {
            logger.debug("fridge pin is low, nope");
        } else {
            logger.info("setting fridge pin to low");
            fridgePin.low();
        }
        status = Status.cold;
    }

    @Override
    public void stopFridge() {
        logger.debug("request to stop fridge");

        if (fridgePin.isHigh()) {
            logger.debug("fridge pin is high, nope");
        } else {
            logger.info("setting fridge pin to high");
            fridgePin.high();
        }
        status = Status.ferm;
    }

    @Override
    public void startBelt() {
        logger.debug("request to start belt");
        fridgePin.high();

        if (beltPin.isLow()) {
            logger.debug("belt pin is low, nope");
        } else {
            logger.info("setting belt pin to low");
            beltPin.low();
        }
        status = Status.warm;
    }

    @Override
    public void stopBelt() {
        logger.debug("request to stop belt");

        if (beltPin.isHigh()) {
            logger.debug("belt pin is high, nope");
        } else {
            logger.info("setting belt pin to high");
            beltPin.high();
        }
        status = Status.ferm;
    }

    @Override
    public void stop() {
        stopBelt();
        stopFridge();
    }

    @Override
    public Status getStatus(){
        return status;
    }

    private void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(200);
                logger.info("Shutting down ...");

                gpio.shutdown();
                gpio.unprovisionPin(fridgePin, beltPin);

                logger.info("released GPIO");

            } catch (InterruptedException e) {
                logger.error(e);
            }
        }));
    }

    public static void main(String[] args) throws InterruptedException {
        logger.info("GPIO Start");
        GPIOController g = new GPIOController();

        g.startFridge();
        Thread.sleep(5000);
        g.stopFridge();
        Thread.sleep(5000);
        g.startBelt();
        Thread.sleep(5000);
        g.stopBelt();
    }
}
