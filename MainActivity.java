package mclovin.lowlevelchat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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

        final SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        //sharedPreferences.edit().remove("contacts").putString("contacts", "").commit();
        myName = sharedPreferences.getString("myName", "user-7567");

        if (myName.equals("user-7567"))
        {
            setMyNameFromAnAlertDialog(sharedPreferences.edit());
        }

        listViewLocal = (ListView) findViewById(R.id.lst_vw_local);

        neighboors = new ArrayList<>();
        final String[] contacts = sharedPreferences.getString("contacts", "").split("-");
        //if (!(contacts.length > 1 && !contacts[0].equals("User")))
            Collections.addAll(neighboors, contacts);
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
        listViewLocal.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id)
            {
                new AlertDialog.Builder(context)
                        .setMessage("Delete?")
                        .setNegativeButton("NO", null)
                        .setPositiveButton("YES", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String allContacts = sharedPreferences.getString("contacts", "_not_Found_");
                                if (!allContacts.equals("_not_Found_"))
                                {
                                    String nameAndIpToDelete = ((String)myListViewAdapter.getItem(position));
                                    int nameAndIpStartIndex = allContacts.indexOf(nameAndIpToDelete); //this line only checks if the item has a name
                                    if (nameAndIpStartIndex == -1)
                                    {
                                        nameAndIpToDelete = nameAndIpToDelete.split("\n")[1];
                                        nameAndIpStartIndex = allContacts.indexOf(nameAndIpToDelete);
                                    }
                                    allContacts = allContacts.replace(nameAndIpToDelete + "-", "");
                                    sharedPreferences.edit().putString("contacts", allContacts).commit();
                                }
                                neighboors.remove(position);
                                myListViewAdapter.notifyDataSetChanged();
                            }
                        })
                        .create().show();
                return true;
            }
        });

        ((Button)findViewById(R.id.add_butt)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                View view = (View)getLayoutInflater().inflate(R.layout.just_an_edittext, null);
                final EditText nameEditText = (EditText)view.findViewById(R.id.name_editText);
                final EditText ipEditText = (EditText)view.findViewById(R.id.ip_editText);
                new AlertDialog.Builder(context)
                        .setTitle("enter ip")
                        .setView(view)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                new Thread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        final String tempStringName = nameEditText.getText().toString();
                                        final String tempStringIp = ipEditText.getText().toString();
                                        if (tempStringName == "")
                                            Toast.makeText(context, "Give it a name", Toast.LENGTH_SHORT).show();
                                        if (tempStringIp == "")
                                            Toast.makeText(context, "You didn't tell me what the IP is", Toast.LENGTH_SHORT).show();
                                        try
                                        {
                                            boolean available = InetAddress.getByName(tempStringIp).isReachable(500); //just to check if the address is valid or not
                                            neighboors.add(tempStringIp + ":4444");
                                            runOnUiThread(new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    myListViewAdapter.notifyDataSetChanged();
                                                    sharedPreferences.edit().putString("contacts", sharedPreferences.getString("contacts", "192.168.1.1") + tempStringIp + '-').apply();
                                                }
                                            });
                                            if (!available)
                                                Toast.makeText(context, "IP is added but it's not available in the moment", Toast.LENGTH_SHORT).show();
                                        }
                                        catch (Exception e)
                                        {
                                            //TODO:alert dialog
                                            System.out.println("///Error2(run()<-onClick()<-onClick of add_butt):" + "wrong ip");
                                        }
                                    }
                                }).start();
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
        (view.findViewById(R.id.ip_editText)).setVisibility(View.GONE);
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
