package cl.francisco.georegistro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class ListadoActivity extends AppCompatActivity {

    private final ArrayList<String> textos = new ArrayList<>();
    private final ArrayList<String> identificadores = new ArrayList<>();
    private ArrayAdapter<String> adaptador;
    private TextView tvEstadoListado;
    private ListenerRegistration escuchaListado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        tvEstadoListado = findViewById(R.id.tvEstadoListado);
        ListView lvIncidencias = findViewById(R.id.lvIncidencias);
        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos);
        lvIncidencias.setAdapter(adaptador);
        lvIncidencias.setOnItemClickListener((lista, vista, posicion, id) -> {
            Intent pantalla = new Intent(this, MapaActivity.class);
            pantalla.putExtra("incidenciaId", identificadores.get(posicion));
            startActivity(pantalla);
        });
        findViewById(R.id.btnActualizar).setOnClickListener(v -> cargarIncidencias());
        findViewById(R.id.btnVolverListado).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        cargarIncidencias();
    }

    private void cargarIncidencias() {
        if (escuchaListado != null) {
            escuchaListado.remove();
            escuchaListado = null;
        }
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            tvEstadoListado.setText(R.string.error_sesion);
            return;
        }
        tvEstadoListado.setText(R.string.listado_cargando);

        escuchaListado = FirebaseFirestore.getInstance()
                .collection("usuarios").document(usuario.getUid())
                .collection("incidencias").orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener(MetadataChanges.INCLUDE, (resultado, error) -> {
                    if (error != null) {
                        tvEstadoListado.setText(R.string.error_listado);
                        return;
                    }
                    if (resultado == null) return;

                    textos.clear();
                    identificadores.clear();
                    SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        Object titulo = documento.get("titulo");
                        Object descripcion = documento.get("descripcion");
                        Object categoria = documento.get("categoria");
                        Object fecha = documento.get("fecha");
                        Object latitud = documento.get("latitud");
                        Object longitud = documento.get("longitud");
                        if (!(titulo instanceof String) || !(descripcion instanceof String)
                                || !(categoria instanceof String) || !(fecha instanceof Timestamp)
                                || !(latitud instanceof Number) || !(longitud instanceof Number)) continue;

                        String texto = getString(R.string.incidencia_listado, titulo, categoria,
                                formato.format(((Timestamp) fecha).toDate()), descripcion,
                                ((Number) latitud).doubleValue(), ((Number) longitud).doubleValue());
                        textos.add(texto + "\n" + textoSensor(documento));
                        identificadores.add(documento.getId());
                    }
                    adaptador.notifyDataSetChanged();

                    if (resultado.getMetadata().hasPendingWrites()) {
                        tvEstadoListado.setText(R.string.listado_pendiente);
                    } else if (resultado.getMetadata().isFromCache()) {
                        tvEstadoListado.setText(R.string.listado_cache);
                    } else {
                        tvEstadoListado.setText(textos.isEmpty()
                                ? R.string.listado_vacio : R.string.listado_actualizado);
                    }
                });
    }

    private String textoSensor(DocumentSnapshot documento) {
        Object datoSensor = documento.get("datoSensor");
        if (Boolean.TRUE.equals(documento.get("lecturaSensorDisponible")) && datoSensor instanceof Map<?, ?>) {
            Map<?, ?> valores = (Map<?, ?>) datoSensor;
            Object x = valores.get("x");
            Object y = valores.get("y");
            Object z = valores.get("z");
            if (x instanceof Number && y instanceof Number && z instanceof Number) {
                return getString(R.string.sensor_listado,
                        ((Number) x).doubleValue(), ((Number) y).doubleValue(), ((Number) z).doubleValue());
            }
        }
        if (Boolean.FALSE.equals(documento.get("sensorDisponible"))) {
            return getString(R.string.sensor_listado_ausente);
        }
        if (Boolean.TRUE.equals(documento.get("sensorDisponible"))) {
            return getString(R.string.sensor_listado_sin_lectura);
        }
        return getString(R.string.sensor_listado_anterior);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escuchaListado != null) {
            escuchaListado.remove();
            escuchaListado = null;
        }
    }
}
