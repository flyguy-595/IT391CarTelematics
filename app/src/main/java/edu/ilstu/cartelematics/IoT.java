package edu.ilstu.cartelematics;
import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import java.text.DecimalFormat;

public class IoT {

    private static AWSConnection connection;
    private static String[] data = {"", "", "", "", "", "", ""};
    private static DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public static void Connect(Context activityContext){
        connection = AWSConnection.newInstance(activityContext);
        connection.AWSConnect();
    }

    public static void setData(String[] AWSData){
        data = AWSData;
        double fuelLevel = Double.parseDouble(data[1].substring(0, data[1].length()-8));
        data[1] = "Fuel Level: " + decimalFormat.format(fuelLevel) + "%";
        double rpm = Double.parseDouble(data[2].substring(0, data[2].length()-23));
        data[2] = "RPMs: " +  rpm;
        data[3] = "Speed: " + data[3];
        double throttlePos = Double.parseDouble(data[4].substring(0, data[4].length()-8));
        data[4] = "Throttle Pos: " + decimalFormat.format(throttlePos) + "%";
        data[5] = "Engine Time: " + data[5].substring(0, data[5].length()-8) + "s";
    }

    public static String getMph(){
        return data[3];
    }

    public static String[] getData(){
        return data;
    }
}
