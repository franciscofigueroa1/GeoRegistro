package cl.francisco.georegistro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

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

        Button btnRegistrar = findViewById(R.id.btnRegistrar);
        Button btnListado = findViewById(R.id.btnListado);
        Button btnMapa = findViewById(R.id.btnMapa);

        btnRegistrar.setOnClickListener(v -> {
            Intent pantalla = new Intent(
                    MainActivity.this, RegistroActivity.class);
            startActivity(pantalla);
        });

        btnListado.setOnClickListener(v -> {
            Intent pantalla = new Intent(
                    MainActivity.this, ListadoActivity.class);
            startActivity(pantalla);
        });

        btnMapa.setOnClickListener(v -> {
            Intent pantalla = new Intent(
                    MainActivity.this, MapaActivity.class);
            startActivity(pantalla);
        });
    }
}