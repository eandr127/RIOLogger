package com.myshopify.owcr.riologger;

import java.io.IOException;
import java.util.logging.Level;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.networktables.TableEntryListener;


public class RobotLoggerLevelSetter {

    public static int TEAM_NUMBER = 2706;
    public static String LOGGER_TABLE = "logging-level";
    
    private static NetworkTable table;
    
    public static volatile Level currentLevel;
    
    public static void setUpRobotLoggingLevelSetter() {
        NetworkTableInstance.getDefault().startClient();
        NetworkTableInstance.getDefault().setServer("roboRIO-" + TEAM_NUMBER + "-FRC.local");
        
        table = NetworkTableInstance.getDefault().getTable(LOGGER_TABLE);
        table.getEntry("level").setDefaultValue(Level.ALL.intValue());
        
        table.addEntryListener("Value", new TableEntryListener() {

            @Override
            public void valueChanged(NetworkTable table, String key, NetworkTableEntry entry,
                            NetworkTableValue value, int flags) {
                try {
                    RIOLogger.write(table.getEntry(key).getRaw(new byte[0]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                table.getEntry("Value").setRaw(new byte[0]);
                
            }
            
        }, EntryListenerFlags.kUpdate);
        
        table.addEntryListener("save", new TableEntryListener() {

            @Override
            public void valueChanged(NetworkTable table, String key, NetworkTableEntry entry,
                            NetworkTableValue value, int flags) {
                if(table.getEntry("save").getBoolean(false)) {
                    table.getEntry("save").setBoolean(false);
                    ClientLogger.restart(table.getEntry("match").getString(""));
                }
                
            }
            
        }, EntryListenerFlags.kUpdate);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean wasConnected = false;
                
                while(!RIOLogger.getLogger().cleanup) {
                    if(NetworkTableInstance.getDefault().isConnected() && !wasConnected) {
                        setLevel(currentLevel);
                        wasConnected = true;
                    }
                    else if(!NetworkTableInstance.getDefault().isConnected()) {
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
            table.getEntry("level").setNumber(level.intValue());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void exit() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                NetworkTableInstance.getDefault().stopClient();
            }
            
        }).start();
    }
}
