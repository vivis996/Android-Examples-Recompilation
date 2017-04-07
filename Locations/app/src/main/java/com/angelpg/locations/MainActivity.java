package com.angelpg.locations;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.security.Permission;

public class MainActivity extends AppCompatActivity {

    private Location currentBestLocation = null;
    private EditText latitud, longitud, resultado, etll;
    private GPSTracker gps;
    public int permissionCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitud = (EditText) findViewById(R.id.etLatitud);
        longitud = (EditText) findViewById(R.id.etLongitud);
        resultado = (EditText) findViewById(R.id.etResultado);
        latitud.setText("21.1398565");
        longitud.setText("-86.8522396");
        etll = (EditText) findViewById(R.id.etLL);

    }

    //Método que mide las distancias
    //usando el GPS del dispositivo
    public void checkDistance() {

        Location locationA = new Location("Walmart");

        locationA.setLatitude(Float.parseFloat(latitud.getText().toString()));
        locationA.setLongitude(Float.parseFloat(longitud.getText().toString()));

        Location locationB = new Location("UPQROO");

        gps = new GPSTracker(this);

        if (gps.canGetLocation) {
            if(gps.latitude != 0.0) {
                try{
                    Double Latitud = gps.getLatitude();
                    Double Longitud = gps.getLongitude();

                    locationB.setLatitude(gps.getLatitude());
                    locationB.setLongitude(gps.getLongitude());

                    Float distance = (locationA.distanceTo(locationB)) / 1000;

                    resultado.setText(distance.toString());
                    etll.setText(Latitud.toString() + ", " + Longitud.toString() + ", Permission: " + permissionCheck);
                }catch (Exception e) {
                    System.out.println(e);
                }
            } else {
                System.out.println("No se a activado el permiso");
            }
        } else {
            Toast.makeText(this, "Algo salió mal", Toast.LENGTH_SHORT).show();
            gps.showSettingsAlert();
        }
    }

    //Método que limpia campos.
    public void limpiar(View v) {
        latitud.setText("");
        longitud.setText("");
        resultado.setText("");
        etll.setText("");
        latitud.requestFocus();
    }

    //Método que precarga coordenadas
    //default del walmart nichupté
    public void cargarCordenadas(View v) {

        if(!latitud.getText().equals("") && !longitud.getText().equals(""))
        {
            latitud.setText("21.1398565");
            longitud.setText("-86.8522396");
        } else {
            Toast.makeText(this, "Los dos campos deben estar vacíos", Toast.LENGTH_SHORT).show();
        }
    }

    //Método que se ejecuta asociado
    //al botón "CHECAR DISTANCIA"
    public void onFirstClick(View v) {

        if (checkLocationPermission() == true) {
            if (!latitud.getText().equals("") || !longitud.getText().equals("")) {
                if (isNumeric(latitud.getText().toString()) == true
                        && isNumeric(longitud.getText().toString()) == true) {
                    checkDistance();
                } else {
                    Toast.makeText(this, "ERROR EN: Datos / Faltan o Formato Incorrecto.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    //Método para pedir el permiso
    //AQUÍ ESTÁ LA MAGIA
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // Si el usuario cancela el dialogo de permiso, el array permanee vacío.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "GRACIAS! :D", Toast.LENGTH_SHORT).show();
                    if (!latitud.getText().equals("") && !longitud.getText().equals("")) {
                        if (isNumeric(latitud.getText().toString()) == true
                                && isNumeric(longitud.getText().toString()) == true) {
                            checkDistance();
                        } else {
                            Toast.makeText(this, "Sólo se aceptan valores numéricos.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Faltan datos", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // permiso negado, la aplicacion no funcionará
                    // la funcionalidad depende de este permiso.
                    Toast.makeText(this, "Aplicación no funcionará. Lo sentimos.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            // otros 'casos' cotinuar por si se
            // se necesitan otros permisos
            // o sea 'case 2:' etcétera.
        }
    }

    //Con esto verificamos si el permiso ya
    //se ha dado para que funcione nuestro
    //método onFirstClick
    public boolean checkLocationPermission() {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    //Para asegurar que lo que se
    //ingresa son números, decimales
    //o negativos.
    public static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

}
