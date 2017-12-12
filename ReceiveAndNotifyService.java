package mclovin.lowlevelchat;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

//send a broadcast every second to keep alive
//TODO: it doesn't work when the screen is off

public class ReceiveAndNotifyService extends Service
{
    static int PORT = 4444;
    ServerSocket serverSocket;
    ArrayList<Socket> connectedClientSockets;
    ArrayList<Thread> connectedClientThreadPool;

    //debug
    void logi(String log_txt) { Log.i("///", log_txt); }

    @Override
    public void onCreate()
    {
        super.onCreate();

        connectedClientSockets = new ArrayList<>();
        connectedClientThreadPool = new ArrayList<>();

        try
        {
            serverSocket = new ServerSocket(PORT);
        }
        catch (IOException e) { logi(e.toString()); }

        connectedClientSockets = new ArrayList<>();
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                mainLoop();
            }
        });
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        startService(new Intent(this, ReceiveAndNotifyService.class));
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void pullServerSocket() throws IOException //determine how are sockets identified...do i have to pull every socket in the pool?
    {
        serverSocket.close();
    }

    public void mainLoop()
    {
        Socket tempSocket;
        String ipAndPort;
        String[] ipAndPortSplitTemp;
        String tempIp;
        Thread tempThread;

        while (true)
        {
            try
            {
                tempSocket = serverSocket.accept();//stuck here untill new connection come in
                if (MyClientSocket.socket != null && tempSocket.getInetAddress().equals(MyClientSocket.socket.getInetAddress()) && tempSocket.getLocalPort() == MyClientSocket.socket.getLocalPort())
                    tempSocket = MyClientSocket.socket;
                connectedClientSockets.add(tempSocket);

                tempThread = new Thread(new MyClientThread(this, tempSocket));
                tempThread.start();
                connectedClientThreadPool.add(tempThread);
            }
            catch (IOException e)
            {
                Log.i("/// Error(mainLoop):", e.toString());
                sendBroadcast(new Intent(this, LocalDirectChatActivity.class).putExtra("error", e.toString()));
            }
        }
    }

    class MyClientThread implements Runnable //this class handles one client (each client has instance in the thread pool)
    {
        Context context;
        Socket socket;
        InputStream inputStream;

        MyClientThread(Context context, Socket socket)
        {
            this.context = context;
            this.socket = socket;

            init();
        }

        void init()
        {
            try
            {
                inputStream = socket.getInputStream();
            }
            catch (IOException e)
            {
                System.out.println("/// Error (MyClientThread->init):" + e.toString());
                sendBroadcast(new Intent(context, LocalDirectChatActivity.class).putExtra("error", e.toString()));
            }
        }

        @Override
        public void run()
        {
            while (true)
            {
                //get the received string character by character and put it in temp
                String msgTemp = "";
                String nameTemp;
                String[] msgSplitTemp;
                int i;

                try
                {
                    while ((i = inputStream.read()) != -1)
                    {
                        if ((char) i == '\n') //and DOESN'T has more characters afterwards
                        {
                            msgTemp += '\n';
                            msgSplitTemp = msgTemp.split("--");
                            nameTemp = msgSplitTemp[0];
                            msgTemp = (msgSplitTemp[1].split("-="))[1];
                            sendBroadcast(new Intent(context, LocalDirectChatActivity.MyBroadcastReceiver.class).putExtra("recvdMsg", nameTemp + ": " + msgTemp));
                            msgTemp = "";
                            continue;
                        }
                        msgTemp += (char) i;
                    }
                }
                catch (Exception e)
                {
                    System.out.println("/// Error1(MyClientThread->run()):" + e.toString());
                }
            }
        }
    }
}










        /*logi("constructor started bih");
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                FileOutputStream fileOutputStream = null;
                long[] intervals = {2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 5000, 5000, 5000, 3600000};
                try
                {
                     fileOutputStream = new FileOutputStream("storage/emulated/0/txt3.txt");
                }
                catch (Exception e)
                {
                    logi(e.toString());
                }

                long start = System.currentTimeMillis();
                int x = 0;
                while (true)
                {
                    if (System.currentTimeMillis() - start > 2000)
                    {
                        try
                        {
                            fileOutputStream.write((String.valueOf(x++) + '\n').getBytes());
                            fileOutputStream.flush();
                        }
                        catch (Exception e)
                        {
                            logi(e.toString());
                        }

                        start = System.currentTimeMillis();
                    }
                }
            }
        });
        thread.start();*/
