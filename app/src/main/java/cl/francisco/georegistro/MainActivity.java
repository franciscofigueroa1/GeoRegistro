package cl.francisco.georegistro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnRegistrar, btnListado, btnMapa;
    private TextView tvEstadoConexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets barras = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            barras.left,
                            barras.top,
                            barras.right,
                            barras.bottom);

                    return insets;
                });

        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnListado = findViewById(R.id.btnListado);
        btnMapa = findViewById(R.id.btnMapa);
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);

        btnRegistrar.setOnClickListener(v -> abrirPantalla(new Intent(this, RegistroActivity.class)));
        btnListado.setOnClickListener(v -> abrirPantalla(new Intent(this, ListadoActivity.class)));
        btnMapa.setOnClickListener(v -> abrirPantalla(new Intent(this, MapaActivity.class)));
    }

    private void abrirPantalla(Intent pantalla) {
        FirebaseAuth sesion = FirebaseAuth.getInstance();

        if (sesion.getCurrentUser() != null) {
            tvEstadoConexion.setText("");
            startActivity(pantalla);
            return;
        }

        activarBotones(false);
        tvEstadoConexion.setText(R.string.conexion_iniciando);

        sesion.signInAnonymously().addOnCompleteListener(this, tarea -> {
            activarBotones(true);
            if (tarea.isSuccessful()) {
                tvEstadoConexion.setText("");
                startActivity(pantalla);
            } else {
                tvEstadoConexion.setText(R.string.error_conexion);
            }
        });
    }

    private void activarBotones(boolean activos) {
        btnRegistrar.setEnabled(activos);
        btnListado.setEnabled(activos);
        btnMapa.setEnabled(activos);
    }
}
