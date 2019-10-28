package gg;

import gg.util.PropertyUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;

public class TelegramNotifier {
    private static final Logger logger = LogManager.getLogger(TelegramNotifier.class);

    private static String TELEGRAM_URI_TEMPLATE = "https://api.telegram.org/bot{0}/sendMessage?chat_id=@{1}&text=";
    private final String telegramUri;

    public TelegramNotifier(Properties p){ //TODO connect with HttpServer
        String telegramBotApiKey = PropertyUtil.getTelegramBotApiKey(p);
        String telegramChannelName = PropertyUtil.getTelegramChannelName(p);
        telegramUri = buildUri(telegramBotApiKey, telegramChannelName);
    }

    public TelegramNotifier(String telegramBotApiKey, String telegramChannelName) {
        telegramUri = buildUri(telegramBotApiKey, telegramChannelName);
    }

    public static void main(String... args){
        TelegramNotifier telegramNotifier = new TelegramNotifier(args[0], args[1]);
        telegramNotifier.sendNotify("this is my");
    }

    private String buildUri(String telegramBotApiKey, String telegramChannelName) {
        return MessageFormat.format(TELEGRAM_URI_TEMPLATE, telegramBotApiKey, telegramChannelName);
    }

    public void sendNotify(String message){
        HttpURLConnection con = null;
        try {
            URL url = new URL(telegramUri+message);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            String responseMessage = con.getResponseMessage();
            logger.info(responseMessage);
            //TODO timeouts, print response
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
