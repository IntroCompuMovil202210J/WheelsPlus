package boot;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.wheelsplus.ChatActivity;
import com.example.wheelsplus.MainActivity;
import com.example.wheelsplus.NavActivity;
import com.example.wheelsplus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import model.Chat;
import model.Grupo;
import model.Mensaje;
import model.Usuario;

public class BackgroundBootService extends Service {

    public static final String CHANNEL_ID = "WheelsPlus";
    public static final String FB_USERS_PATH = "users/";
    public static final String FB_GROUPS_PATH = "groups/";
    public static final String FB_CHATS_PATH = "chats/";
    public static final String FB_MESSAGES_PATH = "messages/";


    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference myRef, myRefChat;
    ChildEventListener vel, velC;

    String uuid;
    Map<String, String> mapGroup = new HashMap<>();
    ArrayList<Mensaje> arrayMessage = new ArrayList<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(myRef != null){
            myRef.removeEventListener(vel);
            myRefChat.removeEventListener(velC);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent intent1;
        if(auth.getCurrentUser() != null){
            intent1 = new Intent(this, NavActivity.class);
        }else{
            intent1 = new Intent(this, MainActivity.class);
        }

        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startForeground(2, buildComplexNotification("WheelsPlus", "Connected", R.drawable.logo_wheels, intent1));
        if(auth.getCurrentUser() != null) {
            myRef = database.getReference(FB_USERS_PATH + auth.getCurrentUser().getUid()).child("groups");
            vel = myRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    myRef = database.getReference(FB_GROUPS_PATH + snapshot.getKey());
                    myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (task.isSuccessful()) {
                                Grupo grupo = task.getResult().getValue(Grupo.class);
                                mapGroup.put(grupo.getId_Grupo(), grupo.getNombreGrupo());
                            }
                        }
                    });
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    String grupoId = snapshot.getKey();
                    groupStarted(mapGroup.get(grupoId));
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    String grupoId = snapshot.getKey();
                    mapGroup.remove(grupoId);
                    groupRemoved(mapGroup.get(grupoId));
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            myRefChat = database.getReference(FB_USERS_PATH + auth.getCurrentUser().getUid()).child("chats");
            myRefChat.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if(task.isSuccessful()){
                        for(DataSnapshot single : task.getResult().getChildren()){
                            myRefChat = database.getReference(FB_CHATS_PATH + single.getKey()).child(FB_MESSAGES_PATH);
                            velC = myRefChat.limitToLast(1).addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                    Mensaje mensaje = snapshot.getValue(Mensaje.class);
                                    if(!mensaje.getIdEnvio().equals(auth.getCurrentUser().getUid())){
                                        myRefChat = database.getReference(FB_USERS_PATH + mensaje.getIdEnvio());
                                        myRefChat.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                                if(task.isSuccessful()) {
                                                    if (mensaje.getTipo().equals("TEXT")) {
                                                        chatNotification(single.getKey(),task.getResult().child("nombre").getValue(String.class) + " " + task.getResult().child("apellido").getValue(String.class), mensaje.getDato());
                                                    }else{
                                                        chatNotification(single.getKey(),task.getResult().child("nombre").getValue(String.class) + " " + task.getResult().child("apellido").getValue(String.class), "Foto");
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                }

                                @Override
                                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                                }

                                @Override
                                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });


                        }
                    }
                }
            });
        }

        return START_STICKY;
    }

    /*
    private void dataChanged(String username) {
        sendNotification("Taller3FOV", username + " actualizó su estado a disponible", R.drawable.ic_baseline_local_fire_department_24);
    }

    */

    private void groupRemoved(String groupname) {
        sendNotification("Wheelsplus", "El grupo " + groupname +" se ha eliminado", R.drawable.logo_wheels, new Intent(this, NavActivity.class));
    }

    private void groupStarted(String groupname) {
        sendNotification("Wheelsplus", "El grupo " + groupname +" ha iniciado", R.drawable.logo_wheels, new Intent(this, NavActivity.class));
    }

    private void chatNotification(String idChat, String nameOther, String dataMessage) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatKey", idChat);
        sendNotification(nameOther, dataMessage, R.drawable.logo_wheels, intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Notification buildComplexNotification(String title, String message, int icon, Intent intent){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mBuilder.setSmallIcon(icon);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(message);
        mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);

        return mBuilder.build();
    }

    private void sendNotification(String title, String message, int icon, Intent intent){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(7, buildComplexNotification(title, message, icon, intent));
    }


}