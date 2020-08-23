package ru.nsu.ccfit.khudyakov.network_lab_4.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigParser {

    private static final Properties p;
    private final static Logger LOGGER = Logger.getLogger(ConfigParser.class.getName());

    private static int width;
    private static int height;
    private static int foodStatic;
    private static int foodPerPlayer;
    private static int stateDelayMs;
    private static float deadFoodProb;
    private static int pingDelayMs;
    private static int nodeTimeoutMs;
    private static String name;

    static {
        try {
            InputStream resourceAsStream = ConfigParser.class.getClassLoader().getResourceAsStream("config.properties");
            p = new Properties();
            p.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException();
        }

        String value;

        value = p.getProperty("width");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            width = Integer.valueOf(value);
        }

        value = p.getProperty("height");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            height = Integer.valueOf(value);
        }

        value = p.getProperty("food_static");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            foodStatic = Integer.valueOf(value);
        }

        value = p.getProperty("food_per_player");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            foodPerPlayer = Integer.valueOf(value);
        }

        value = p.getProperty("state_delay_ms");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            stateDelayMs = Integer.valueOf(value);
        }

        value = p.getProperty("dead_food_prob");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            deadFoodProb = Float.valueOf(value);
        }

        value = p.getProperty("ping_delay_ms");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            pingDelayMs = Integer.valueOf(value);
        }


        value = p.getProperty("node_timeout_ms");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            nodeTimeoutMs = Integer.valueOf(value);
        }

        value = p.getProperty("name");
        if (value == null) {
            LOGGER.severe("Couldn't read value from properties file");
        } else {
            name = value;
        }
    }

    public ConfigParser() {
    }

    public static String getName() {
        return name;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int getFoodStatic() {
        return foodStatic;
    }

    public static int getFoodPerPlayer() {
        return foodPerPlayer;
    }

    public static int getStateDelayMs() {
        return stateDelayMs;
    }

    public static float getDeadFoodProb() {
        return deadFoodProb;
    }

    public static int getPingDelayMs() {
        return pingDelayMs;
    }

    public static int getNodeTimeoutMs() {
        return nodeTimeoutMs;
    }
}
