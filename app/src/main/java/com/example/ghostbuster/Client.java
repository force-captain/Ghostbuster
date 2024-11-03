package com.example.ghostbuster;

import android.os.AsyncTask;
import android.util.Pair;

import java.io.Console;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FetchDataTask  {

    static final String SERVER_IP = "10.252.93.103";
    static final int PORT = 4999;
    static DatagramSocket socket;
    static byte[] buffer = new byte[256];
    public static String doInBackground() {
        String received = "";
        try{
            socket = new DatagramSocket(PORT);
            boolean receivedData = false;
            while (!receivedData) {
                try {
                    DatagramPacket dpRec = new DatagramPacket(buffer, buffer.length);
                    socket.receive(dpRec);

                    received = new String(dpRec.getData(), 0, dpRec.getLength());
                    receivedData = true;
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                socket.close();
                String[] pieces = received.split(",");
                return pieces[0];
            }
        } catch (SocketException e)
        {
            e.printStackTrace();
        }
        return "";
    }


    /*
    static final String SERVER_IP = "10.252.93.103";
    static final int PORT = 4999;
    static DatagramSocket socket;
    static byte[] buffer = new byte[256];

    public static String getData()//Pair<float[], Boolean> getData()
    {
        String received = "";
        try {
            socket = new DatagramSocket(PORT);
            boolean receivedData = false;

            socket.close();
            String[] pieces = received.split(",");
            return pieces[0];
        } catch (SocketException e)
        {
            e.printStackTrace();
        }
        return "";//return new Pair<float[], Boolean>(new float[]{}, false);
    }
*/

}
