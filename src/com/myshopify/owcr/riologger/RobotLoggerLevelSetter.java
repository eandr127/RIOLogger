package com.myshopify.owcr.riologger;

import java.util.logging.Level;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class RobotLoggerLevelSetter {

    public static int TEAM_NUMBER;
    public static String LOGGER_TABLE = "logging-level";
    
    private static NetworkTable table;
    
    public static void setUpRobotLoggingLevelSetter() {
        NetworkTable.setClientMode();
        NetworkTable.setIPAddress("roboRIO-" + TEAM_NUMBER + "-FRC.local");
        table = NetworkTable.getTable(LOGGER_TABLE);
        table.setDefaultNumber("level", Level.ALL.intValue());
    }
    
    public static void setLevel(Level level) {
        table.putNumber("level", level.intValue());
    }
    
    public static void exit() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                NetworkTable.shutdown();
            }
            
        }).start();
    }
}
