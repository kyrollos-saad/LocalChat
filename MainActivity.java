package mclovin.lowlevelchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.renderscript.RenderScript;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;


//TODO: descover devices in the local nerwork








public class MainActivity extends AppCompatActivity
{
    public static String myName = "zuzu";
    private ListView listViewLocal;
    MyListViewAdapter myListViewAdapter;
    ArrayList<String> neighboors;
    Context context = this;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService(new Intent(this, ReceiveAndNotifyService.class));

        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        myName = sharedPreferences.getString("myName", "user-7567");

        if (myName.equals("user-7567"))
        {
            setMyNameFromAnAlertDialog(sharedPreferences.edit());
        }

        listViewLocal = (ListView) findViewById(R.id.lst_vw_local);

        neighboors = new ArrayList<>();
        myListViewAdapter = new MyListViewAdapter(context, neighboors);

        listViewLocal.setAdapter(myListViewAdapter);
        listViewLocal.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String ipAndName = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), LocalDirectChatActivity.class);
                intent.putExtra("ipAndName", ipAndName);
                startActivity(intent);
            }
        });


        ((Button)findViewById(R.id.add_butt)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                View view = (View)getLayoutInflater().inflate(R.layout.just_an_edittext, null);
                final EditText nameEditText = (EditText)view.findViewById(R.id.name_editText);
                new AlertDialog.Builder(context)
                        .setTitle("enter ip")
                        .setView(view)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String tempStringIp = nameEditText.getText().toString();
                                if (true || tempStringIp.matches("\\d{3}.\\d{3}.\\d{1}.\\d{1}"))
                                {
                                    neighboors.add(tempStringIp + ":4444");
                                    myListViewAdapter.notifyDataSetChanged();
                                }
                                else
                                {
                                    new AlertDialog.Builder(context)
                                            .setTitle("Error")
                                            .setMessage("wrong format")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialog, int which){}})
                                            .create().show();
                                }
                            }
                        })
                        .create().show();
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        startService(new Intent(this, ReceiveAndNotifyService.class));
    }

    void logi(String log_txt) { Log.i("///", log_txt); }

    void setMyNameFromAnAlertDialog(SharedPreferences.Editor sharedPreferencesEditor)
    {
        final SharedPreferences.Editor sharedPreferencesEditorFinal = sharedPreferencesEditor;
        View view = (View)getLayoutInflater().inflate(R.layout.just_an_edittext, null);
        final EditText nameEditText = (EditText)view.findViewById(R.id.name_editText);
        new AlertDialog.Builder(this)
                .setTitle("Choose a username")
                .setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        myName = nameEditText.getText().toString();
                        sharedPreferencesEditorFinal.putString("myName", myName).apply();
                    }
                })
                .create().show();
    }
}
