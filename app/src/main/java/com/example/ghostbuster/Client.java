package com.example.ghostbuster;

import androidx.core.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import kotlin.Triple;

public class Client {

    private static final int PORT = 4999;
    private static DatagramSocket socket;
    private static byte[] buffer = new byte[256];
    public static Pair<float[], Boolean> fetchData() {
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

                // Parse and return the gyro data
                String[] pieces = received.split(",");
                float[] gyro = new float[] {   Float.parseFloat(pieces[0]),
                        Float.parseFloat(pieces[1]),
                        Float.parseFloat(pieces[2])};
                boolean fire = "1".equals(pieces[3]);

                return new Pair<float[], Boolean>(gyro, fire);
            }
        } catch (SocketException e)
        {
            e.printStackTrace();
        }

        // Return default value if no server data
        return new Pair<float[], Boolean>(new float[] {0, 0, 0}, false);
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
