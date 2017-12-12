package mclovin.lowlevelchat;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


public class LocalDirectChatActivity extends AppCompatActivity
{
    final Context context = this;
    static EditText chat_log;
    static MyClientSocket myClientSocket;
    static Handler handler;
    static boolean weAreTheClient = true;

    @SuppressLint({"HandlerLeak", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_direct_chat);

        //innit UI components

        String[] ipAndName = getIntent().getStringExtra("ipAndName").split("\n");
        final String name = ipAndName[0], ipAndPort = ipAndName[1];

        MyTextWatcher myTextWatcher = new MyTextWatcher(ipAndPort);

        ((TextView)findViewById(R.id.title_text_view)).setText(name + " - " + ipAndPort);

        final SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String log = sharedPreferences.getString("chatLog" + ipAndPort, "");

        chat_log = (EditText)findViewById(R.id.log_box);
        chat_log.setKeyListener(null); //prevent editing the chat log box
        chat_log.setText(log);
        chat_log.addTextChangedListener(myTextWatcher);

        final EditText chat_box = (EditText)findViewById(R.id.chat_box);

        Button send_butt = (Button)findViewById(R.id.send_butt);
        final Context context = this;
        send_butt.setOnClickListener(new View.OnClickListener()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v)
            {
                //trim and get the typed message
                //make sure it's not empty
                //add '\n'
                //start the send thread and pass the essential parameters
                //wipe the EditText control
                String msgToSend = chat_box.getText().toString().trim();
                if (!msgToSend.equals(""))
                {
                    msgToSend += '\n';
                    try
                    {
                        myClientSocket.sendMsg(msgToSend);
                        try
                        {
                            chat_log.setText(chat_log.getText() + "You: " + msgToSend);
                        }
                        catch (Exception e)
                        {
                            new AlertDialog.Builder(context)
                                    .setTitle("Error")
                                    .setMessage("Message sent but not posted in the UI")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialog, int which){}})
                                    .create().show();
                        }
                    }
                    catch (Exception e)
                    {
                        new AlertDialog.Builder(context)
                                .setTitle("Error")
                                .setMessage("Message not sent")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialog, int which){}})
                                .create().show();
                    }
                    chat_box.setText("");
                }
            }
        });

        Button clear_butt = (Button)findViewById(R.id.clear_butt);
        clear_butt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                new AlertDialog.Builder(v.getContext())
                        .setMessage("Are you sure fam?")
                        .setCancelable(true)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialog, int which){}})
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                chat_log.setText("");
                                sharedPreferences.edit().putString("chatLog" + ipAndPort, "").apply();
                            }
                        })
                        .create().show();
            }
        });


        //connectOrKeepTrying client class
        myClientSocket = new MyClientSocket(ipAndPort);

        //handler to post stuff to the UI
        handler = new Handler(){
            @Override
            public void handleMessage(Message inputMessage)
            {
                String messageString = (String)inputMessage.getData().get("error");
                new AlertDialog.Builder(context).setMessage(messageString)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialog, int which){}})
                        .create().show();
            }
        };
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        System.out.println("///on restart baby!");
        myClientSocket.connectOrKeepTrying(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        System.out.println("///on resume baby!");
        myClientSocket.connectOrKeepTrying(true);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        try
        {
            myClientSocket.pullSocket();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try
        {
            myClientSocket.pullSocket();
        }
        catch (Exception e)
        {
            Log.i("/// Error(onDestroy): ", e.toString());
            Bundle data = new Bundle();
            data.putString("error", e.toString());
            Message msg = handler.obtainMessage();
            msg.setData(data);
            handler.sendMessage(msg);
        }
    }

    class MyTextWatcher implements TextWatcher
    {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String ip;

        MyTextWatcher(String ip) { this.ip = ip; }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {

        }

        @Override
        public void afterTextChanged(Editable s)
        {
            editor.putString("chatLog" + ip, chat_log.getText().toString());
            editor.apply();
        }
    }

    public static class MyBroadcastReceiver extends BroadcastReceiver
    {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(final Context context, Intent intent)
        {
            String recvdMsg = intent.getStringExtra("recvdMsg");
            boolean instrmAndSockClosed = intent.getBooleanExtra("instrmAndSockClosed", true);
            if (recvdMsg != null)
                chat_log.setText(chat_log.getText() + recvdMsg);
            if (instrmAndSockClosed)
                myClientSocket.connectOrKeepTrying(true);
        }
    }
}

class MyClientSocket //one instance only for the activity
{
    static Socket socket;
    private OutputStream outputStream;
    private String ip;
    private int port;
    private String ipAndPort;

    MyClientSocket(String ipAndPort)
    {
        this.ipAndPort = ipAndPort;
        connectOrKeepTrying(false);
    }

    void connectOrKeepTrying(boolean closeFirst)
    {
        try
        {
            outputStream.close();
            socket.close();
        }
        catch (Exception ignore) {}

        String[] temp = ipAndPort.split(":");
        ip = temp[0];
        this.port = Integer.parseInt(temp[1]);

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                long start = System.currentTimeMillis();
                while (true)
                {
                    if (System.currentTimeMillis() - start > 1000)
                    {
                        try
                        {
                            socket = new Socket(ip, port);
                            outputStream = socket.getOutputStream();
                            LocalDirectChatActivity.weAreTheClient = true;
                            break;
                        } catch (Exception ignore)
                        {
                            //System.out.println("///connectionkeeptrying -> run" + e.toString());
                        }
                        start = System.currentTimeMillis();
                    }
                }
            }
        });
        thread.start();
    }

    void sendMsg(String msg) throws Exception
    {
        //just to make sure
        if (msg.charAt(msg.length()-1) != '\n')
            msg += '\n';

        msg = MainActivity.myName + "--+_)-=" + msg;
        outputStream.write(msg.getBytes());
        outputStream.flush();
        //update ui in a callback in onCreate()
    }

    void pullSocket() throws Exception
    {
        socket.close();
        outputStream.close();
    }
}
