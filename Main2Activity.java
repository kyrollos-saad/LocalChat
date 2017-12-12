package mclovin.lowlevelchat;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main2Activity extends AppCompatActivity
{
    EditText chat_log, chat_box;
    Button send_butt, clear_butt;
    ReceiveThread receiveThread;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        final String ipAndName = getIntent().getStringExtra("ipAndName");

        MyTextWatcher myTextWatcher = new MyTextWatcher(ipAndName);

        ((TextView)findViewById(R.id.title_text_view)).setText(ipAndName);

        final SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String log = sharedPreferences.getString("chatLog" + ipAndName.split("\n")[0].split("\n")[0], "");

        chat_log = (EditText)findViewById(R.id.log_box);
        chat_log.setKeyListener(null); //prevent editing the chat log box
        chat_log.setText(log);
        chat_log.addTextChangedListener(myTextWatcher);

        chat_box = (EditText)findViewById(R.id.chat_box);

        send_butt = (Button)findViewById(R.id.send_butt);
        final Context context = this;
        send_butt.setOnClickListener(new View.OnClickListener()
        {
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
                    Thread thread = new Thread(new SendThread(context, msgToSend, chat_log, ipAndName.split("\n")[1]));
                    thread.start();
                    chat_box.setText("");
                }
            }
        });

        clear_butt = (Button)findViewById(R.id.clear_butt);
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
                                sharedPreferences.edit().putString("chatLog" + ipAndName.split("\n")[0], "").apply();
                            }
                        })
                        .create().show();
            }
        });

        //this handler gets the received message from the server and put it in the chat log box
        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg)
            {
                chat_log.setText( chat_log.getText() + msg.getData().getString("recvdMsg") );
            }
        };

        receiveThread = new ReceiveThread(Main2Activity.this, handler, ipAndName.split("\n")[1]);
        Thread thread = new Thread(receiveThread);
        thread.start();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        try
        {
            receiveThread.stopSocket();
        }
        catch (Exception e)
        {
            Log.i("/// Error(onStop): ", e.toString());
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try
        {
            receiveThread.stopSocket();
        }
        catch (Exception e)
        {
            Log.i("/// Error(onDestroy): ", e.toString());
        }
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        try
        {
            receiveThread.restartSocket();
        }
        catch (Exception e)
        {
            Log.i("/// Error(onRestart): ", e.toString());
        }
    }

    class MyTextWatcher implements TextWatcher
    {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String ipAndName;

        public MyTextWatcher(String ipAndName) { this.ipAndName = ipAndName; }

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
            editor.putString("chatLog" + ipAndName.split("\n")[0], chat_log.getText().toString());
            editor.apply();
        }
    }
}

class SendThread implements Runnable
{
    Context context;
    String[] ipAndPort;
    String msgToSend;
    EditText chat_log;

    public SendThread(Context context, String msgToSend, EditText chat_log, String ipAndPort)
    {
        this.context = context;
        this.msgToSend = msgToSend;
        this.chat_log = chat_log;
        this.ipAndPort = ipAndPort.split(":");
    }

    @Override
    public void run()
    {
        Socket socket;
        OutputStream outputStream;

        try
        {
            socket = new Socket(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            outputStream = socket.getOutputStream();
            outputStream.write(msgToSend.getBytes());
            outputStream.flush();
            ((Activity)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    chat_log.setText(chat_log.getText() + "You: " + msgToSend);
                    chat_log.scrollBy(0, 1920);
                }
            });
        }
        catch (Exception e)
        {
            Log.i("/// Error:", e.toString());
            ((Activity)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    new AlertDialog.Builder(context).setMessage("Message not sent")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialog, int which){}})
                            .create().show();
                }
            });
        }
    }
}

class ReceiveThread implements Runnable
{
    ServerSocket serverSocket;
    Socket socket;
    OutputStream outputStream;
    InputStream inputStream;
    Handler handler;
    String ipAndPort;

    private Context context;

    ReceiveThread(Context context, Handler handler, String ipAndPort)
    {
        this.context = context;
        this.handler = handler;
        this.ipAndPort = ipAndPort;
    }

    public void restartSocket()
    {
        run();
    }

    public void stopSocket()
    {
        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            final IOException ex = e;
            ((Activity)context).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    new AlertDialog.Builder(context).setMessage("error closing the socket" + ex.toString()).create().show();
                }
            });
        }
    }

    @Override
    public void run()
    {
        try
        {
            String[] temp = ipAndPort.split(":");

            socket = new Socket(temp[0], Integer.parseInt(temp[1]));
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }
        catch (Exception e)
        {
            Log.i("/// Error", e.toString());
        }

        while (true)
        {
            try
            {
                //get the received string character by character and put it in temp
                String temp = "";
                int tmp;
                while (true)
                {
                    tmp = inputStream.read();
                    temp += (char)tmp;
                    if (tmp == '\n')
                        break;
                }
                //make a message object to hold the message instance of the handler
                //the message object takes data in a bundle object
                //use the handler to send the data
                Message msg = handler.obtainMessage();
                Bundle data = new Bundle();
                data.putString("recvdMsg", temp);
                msg.setData(data);
                handler.sendMessage(msg);
            }
            catch (Exception e)
            {
                Log.i("/// Error", e.toString());
            }
        }
    }
}
