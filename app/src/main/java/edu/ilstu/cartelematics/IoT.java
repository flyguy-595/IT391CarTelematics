package edu.ilstu.cartelematics;
import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

public class IoT {

    private static AWSConnection connection;
    private static String[] data = {"", "", "", "", "", "", "", "", ""};

    public static void Connect(Context activityContext){
        connection = AWSConnection.newInstance(activityContext);
        connection.AWSConnect();
    }

    public static void setData(String[] AWSData){
        data = AWSData;
    }

    public static String getMph(){
        return data[2];
    }

}
