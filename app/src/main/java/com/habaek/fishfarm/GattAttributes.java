package com.habaek.fishfarm;

import java.util.HashMap;

/**
 * Created by 박용주 on 2015-09-01.
 */
public class GattAttributes {
    /*
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    //public static String HM_10_CONF = "0000ffe0-0000-1000-8000-00805f9b34fb";
    //public static String HM_RX_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String Nordic_Service = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String RX_DATA = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String TX_DATA = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    static {
        // Sample Services.
        attributes.put("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", "Nordic_UART");
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(RX_DATA,"RX data");
        attributes.put(TX_DATA,"TX data");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
*/

/*
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String BOT = "BOT";
    public static String BOT_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String RX_DATA = "RX_DATA";
    public static String RX_DATA_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static String TX_DATA = "TX_DATA";
    public static String TX_DATA_UUID = "0000fff2-0000-1000-8000-00805f9b34fb";
    static {
        attributes.put(RX_DATA_UUID, RX_DATA);
        attributes.put(TX_DATA_UUID, TX_DATA);
        attributes.put(BOT_UUID, BOT);

        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
*/

    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String BoT_Service = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String RX_DATA = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static String TX_DATA = "0000fff2-0000-1000-8000-00805f9b34fb";
    static {
        attributes.put(RX_DATA, "RX_DATA");
        attributes.put(TX_DATA, "TX_DATA");
        attributes.put(BoT_Service, "BoT_Service");

        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}