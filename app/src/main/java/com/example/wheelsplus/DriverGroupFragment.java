package com.example.wheelsplus;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.model.LatLng;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import model.Grupo;
import model.Vehiculo;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DriverGroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DriverGroupFragment extends Fragment {

    /**
     * View
     */
    View root;

    /**
     * Map/Location
     */
    Geocoder geocoder;

    /**
     * Screen elements (to inflate)
     */
    Button buttonTimeDriver, buttonCreateGroup;
    TextInputEditText editGroupNameDriver, editGroupDestinationDriver, editGroupOriginDriver, editGroupFeeDriver;
    TextView tvSelectedTime, tvPlacaVehiculo;
    ImageButton buttonChooseVehicle;

    /**
     * Firebase
     */
    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference myRef;

    /**
     * Utils
     */
    public static final String FB_GROUPS_PATH = "groups/";
    public static final String FB_DRIVERS_PATH = "drivers/";
    boolean time, date = false;
    Calendar calendar = Calendar.getInstance();
    Vehiculo vehiculo = null;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public DriverGroupFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DriverGroupFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DriverGroupFragment newInstance(String param1, String param2) {
        DriverGroupFragment fragment = new DriverGroupFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_driver_group, container, false);

        buttonTimeDriver = root.findViewById(R.id.buttonTimeDriver);
        buttonCreateGroup = root.findViewById(R.id.buttonCreateGroup);
        editGroupNameDriver = root.findViewById(R.id.editGroupNameDriver);
        editGroupDestinationDriver = root.findViewById(R.id.editGroupDestinationDriver);
        editGroupOriginDriver = root.findViewById(R.id.editGroupOriginDriver);
        editGroupFeeDriver = root.findViewById(R.id.editGroupFeeDriver);
        tvSelectedTime = root.findViewById(R.id.tvSelectedTimeDriver);
        buttonChooseVehicle = root.findViewById(R.id.buttonChooseVehicle);
        tvPlacaVehiculo = root.findViewById(R.id.tvPlacaVehiculo);

        geocoder = new Geocoder(getActivity().getBaseContext());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        if(getArguments() != null) {
            vehiculo = getArguments().getParcelable("carSelected");
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(vehiculo != null){
            tvPlacaVehiculo.setText(vehiculo.getPlaca());
        }

        buttonChooseVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replaceFragment(new SelectVehicleFragment());
            }
        });

        buttonTimeDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm");
                TimePickerDialog timePickerDialog = new TimePickerDialog(view.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        calendar.set(Calendar.SECOND, 0);
                        tvSelectedTime.setText(sdf.format(calendar.getTime()));
                        time = true;
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
                timePickerDialog.updateTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
                timePickerDialog.show();
                DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.YEAR, year);
                        date = true;
                    }
                }, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
                datePickerDialog.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });

        buttonCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean flag = true;
                if(TextUtils.isEmpty(editGroupNameDriver.getText())){
                    editGroupNameDriver.setError("Este campo no puede estar vacío");
                    flag = false;
                }
                if(TextUtils.isEmpty(editGroupDestinationDriver.getText())){
                    editGroupDestinationDriver.setError("Este campo no puede estar vacío");
                    flag = false;
                }
                if(TextUtils.isEmpty(editGroupOriginDriver.getText())){
                    editGroupOriginDriver.setError("Este campo no puede estar vacío");
                    flag = false;
                }
                if(TextUtils.isEmpty(editGroupFeeDriver.getText())){
                    editGroupFeeDriver.setError("Este campo no puede estar vacío");
                    flag = false;
                }
                if(!time || !date){
                    Toast.makeText(view.getContext(), "Hora de acuerdo requerida", Toast.LENGTH_SHORT).show();
                    flag = false;
                }
                if(vehiculo == null){
                    Toast.makeText(view.getContext(), "Recuerda que debes escoger un vehiculo para salir", Toast.LENGTH_SHORT).show();
                    flag = false;
                }
                if(flag){
                    String groupName = editGroupNameDriver.getText().toString();
                    LatLng latLngDestination = searchLatLngAddress(editGroupDestinationDriver.getText().toString());
                    LatLng latLngOrigin = searchLatLngAddress(editGroupOriginDriver.getText().toString());
                    double fee = Double.parseDouble(editGroupFeeDriver.getText().toString());
                    long timestamp = calendar.getTimeInMillis();
                    if(latLngDestination != null && latLngOrigin != null) {
                        String key = myRef.push().getKey();
                        Grupo grupo = new Grupo(key, groupName, auth.getCurrentUser().getUid(), fee, vehiculo.getCapacidad(), latLngOrigin.lat, latLngOrigin.lng, latLngDestination.lat, latLngDestination.lng, timestamp, vehiculo.getIdVehiculo());
                        myRef.child(FB_GROUPS_PATH + key).setValue(grupo).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    myRef.child(FB_DRIVERS_PATH + auth.getCurrentUser().getUid()).child(FB_GROUPS_PATH + key).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(view.getContext(), "Grupo " + groupName + " creado correctamente", Toast.LENGTH_SHORT).show();
                                                tvSelectedTime.setText("");
                                                tvPlacaVehiculo.setText("");
                                                editGroupDestinationDriver.setText("");
                                                editGroupFeeDriver.setText("");
                                                editGroupNameDriver.setText("");
                                                editGroupOriginDriver.setText("");
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }else{
                        if(latLngDestination == null){
                            editGroupDestinationDriver.setError("Destino no encontrado, intente de nuevo");
                        }
                        if(latLngOrigin == null){
                            editGroupOriginDriver.setError("Origen no encontrado, intente de nuevo");
                        }
                    }
                }
            }
        });
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.driver_nav_host_fragment, fragment);
        fragmentTransaction.commit();
    }

    public LatLng searchLatLngAddress(String address){
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 2);
            if (addresses != null && !addresses.isEmpty()) {
                Address addressResult = addresses.get(0);
                return new LatLng(addressResult.getLatitude(), addressResult.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}