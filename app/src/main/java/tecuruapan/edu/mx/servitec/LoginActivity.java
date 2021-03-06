package tecuruapan.edu.mx.servitec;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import lib.CentralDeConexiones;
import lib.ServicioSocial;

public class LoginActivity extends AppCompatActivity {
    public final static String TAG ="LoginActivity";
    EditText numControl;
    EditText contrasena;
    Button connect;
    ProgressBar progressBar;
    TextView registrarseTV;
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);
        registrarseTV = (TextView) findViewById(R.id.registrarse_tv);
        connect = (Button) findViewById(R.id.ConectarBtn);
        numControl = (EditText) findViewById(R.id.NumControl);
        contrasena = (EditText) findViewById(R.id.Contrasena);
        progressBar = (ProgressBar) findViewById(R.id.login_progress);
        progressBar.setVisibility(View.INVISIBLE);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noControl = numControl.getText().toString();
                String contra = contrasena.getText().toString();

                conectar(noControl, contra);

            }
        });
        setSupportActionBar(toolbar);

        registrarseTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostrarDialogo();
            }
        });
        if(tenemosPermiso() == false)
            pedirPermiso();

    }

    public void conectar(String user, String pass) {
        CentralDeConexiones.miServicioSocial = new ServicioSocial(user, pass);
        new LoginTask ().execute();
    }

    private void mostrarDialogo() {
        final EditText noControl = new EditText(this);
        noControl.setHint("Número de control");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(15,15,15,0);
        noControl.setLayoutParams(lp);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Registro")
                .setPositiveButton("Registrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new IntentoRegistroAsyncTask ().execute(noControl.getText().toString());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .setCancelable(false)
                .setView(noControl)
                .create();
        dialogo.show();
    }

    class IntentoRegistroAsyncTask extends AsyncTask<String, Void, Void>{
        String tituloRespuesta = "";
        String respuesta = "";
        @Override
        protected Void doInBackground(String... strings) {
            tituloRespuesta = ServicioSocial.intentarRegistro(strings[0]);
            respuesta = ServicioSocial.ultimoMensaje();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle(tituloRespuesta)
                    .setMessage(respuesta)
                    .setPositiveButton("Regresar", null)
                    .create()
                    .show();
        }
    }

    class LoginTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            connect.setEnabled(false);
        }

        String res;
        @Override
        protected Void doInBackground(Void... voids) {
            res = CentralDeConexiones.miServicioSocial.iniciarSesion();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.INVISIBLE);
            connect.setEnabled(true);
            Toast.makeText(LoginActivity.this, res, Toast.LENGTH_SHORT).show();
            if(CentralDeConexiones.miServicioSocial.sesionIniciada) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private boolean tenemosPermiso(){
        return this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void pedirPermiso (){
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "Permiso concedido" );
            }else {
                Log.d(TAG, "Permiso denegado" );
                Toast.makeText(this, "Es necesario el permiso de escritura para descargar archivos", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

}
