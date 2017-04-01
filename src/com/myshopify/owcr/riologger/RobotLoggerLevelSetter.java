package com.myshopify.owcr.riologger;

import java.io.IOException;
import java.util.logging.Level;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

public class RobotLoggerLevelSetter {

    public static int TEAM_NUMBER;
    public static String LOGGER_TABLE = "logging-level";
    
    private static NetworkTable table;
    
    public static volatile Level currentLevel;
    
    public static void setUpRobotLoggingLevelSetter() {
        NetworkTable.setClientMode();
        NetworkTable.setIPAddress("roboRIO-" + TEAM_NUMBER + "-FRC.local");
        table = NetworkTable.getTable(LOGGER_TABLE);
        table.setDefaultNumber("level", Level.ALL.intValue());
        
        table.addTableListener(new ITableListener() {
            @Override
            public void valueChanged(ITable source, String key, Object value, boolean isNew) {
                
                if(key.equals("Value")) {
                    try {
                        RIOLogger.write(source.getRaw(key, new byte[0]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    source.putRaw("Value", new byte[0]);
                }
            }
        });
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean wasConnected = false;
                
                while(!RIOLogger.getLogger().cleanup) {
                    if(table.isConnected() && !wasConnected) {
                        setLevel(currentLevel);
                        wasConnected = true;
                    }
                    else if(!table.isConnected()) {
                        wasConnected = false;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    
    public static void setLevel(Level level) {
        try {
            currentLevel = level;
            table.putNumber("level", level.intValue());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
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
