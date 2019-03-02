package gg;

import com.pi4j.io.gpio.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GPIOController implements IGPIOController{

    private static final Logger logger = LogManager.getLogger(Controller.class);

    private final GpioController gpio;
    private final GpioPinDigitalOutput fridgePin;
    private final GpioPinDigitalOutput beltPin;

    public GPIOController(){
        gpio = GpioFactory.getInstance();

        fridgePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "fridgePin", PinState.HIGH);
        beltPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "beltPin", PinState.HIGH);

        //TODO se non li faccio cosa succede?
        fridgePin.setShutdownOptions(true, PinState.LOW);
        beltPin.setShutdownOptions(true, PinState.LOW);
    }

    @Override
    public void startFridge() {
        logger.info("request to start fridge");

        if (fridgePin.isLow()) {
            logger.info("fridge pin is low, nope");
        } else{
            logger.info("setting fridge pin to low");
            fridgePin.low();
        }
    }

    @Override
    public void stopFridge() {
        logger.info("request to stop fridge");

        if (fridgePin.isHigh()) {
            logger.info("fridge pin is high, nope");
        }else {
            logger.info("setting fridge pin to high");
            fridgePin.high();
        }
    }

    @Override
    public void startBelt() {
        logger.info("request to start belt");

        if (beltPin.isLow()) {
            logger.info("belt pin is low, nope");
        }else {
            logger.info("setting belt pin to low");
            beltPin.low();
        }
    }

    @Override
    public void stopBelt() {
        logger.info("request to stop belt");

        if (beltPin.isHigh()) {
            logger.info("belt pin is high, nope");
        }else {
            logger.info("setting belt pin to high");
            beltPin.high();
        }    }

    @Override
    public void stop() {
        stopBelt();
        stopFridge();
    }
}
