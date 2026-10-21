package cl.francisco.georegistro;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private TextView tvEstadoMapa;
    private String incidenciaId;
    private DocumentSnapshot incidencia;
    private ListenerRegistration escuchaMapa;
    private boolean mapaActivado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        tvEstadoMapa = findViewById(R.id.tvEstadoMapa);
        incidenciaId = getIntent().getStringExtra("incidenciaId");
        Button btnActualizarMapa = findViewById(R.id.btnActualizarMapa);
        btnActualizarMapa.setOnClickListener(v -> cargarIncidencia());
        btnActualizarMapa.setEnabled(false);
        findViewById(R.id.btnVolverMapa).setOnClickListener(v -> finish());

        if (!BuildConfig.MAPA_CONFIGURADO) {
            tvEstadoMapa.setText(R.string.mapa_sin_configuracion);
            return;
        }
        int servicios = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (servicios != ConnectionResult.SUCCESS) {
            tvEstadoMapa.setText(R.string.mapa_servicios_google);
            return;
        }

        mapaActivado = true;
        btnActualizarMapa.setEnabled(true);
        findViewById(R.id.contenedorMapa).setVisibility(View.VISIBLE);
        SupportMapFragment fragmento = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.contenedorMapa);
        if (fragmento == null) {
            fragmento = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedorMapa, fragmento).commit();
        }
        fragmento.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        cargarIncidencia();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mostrarMarcador();
    }

    private void cargarIncidencia() {
        if (!mapaActivado) return;
        if (escuchaMapa != null) {
            escuchaMapa.remove();
            escuchaMapa = null;
        }
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            tvEstadoMapa.setText(R.string.error_sesion);
            return;
        }
        tvEstadoMapa.setText(R.string.mapa_cargando);
        CollectionReference registros = FirebaseFirestore.getInstance()
                .collection("usuarios").document(usuario.getUid()).collection("incidencias");

        if (incidenciaId != null) {
            escuchaMapa = registros.document(incidenciaId)
                    .addSnapshotListener(MetadataChanges.INCLUDE, (documento, error) ->
                            recibirIncidencia(documento, error,
                                    documento != null && documento.getMetadata().isFromCache()));
        } else {
            escuchaMapa = registros.orderBy("fecha", Query.Direction.DESCENDING).limit(1)
                    .addSnapshotListener(MetadataChanges.INCLUDE, (resultado, error) -> {
                        DocumentSnapshot documento = resultado == null || resultado.isEmpty()
                                ? null : resultado.getDocuments().get(0);
                        recibirIncidencia(documento, error,
                                resultado != null && resultado.getMetadata().isFromCache());
                    });
        }
    }

    private void recibirIncidencia(DocumentSnapshot documento, FirebaseFirestoreException error,
                                   boolean desdeCache) {
        if (error != null) {
            incidencia = null;
            if (mapa != null) mapa.clear();
            tvEstadoMapa.setText(R.string.mapa_error);
            return;
        }
        incidencia = documento;
        if (documento == null || !documento.exists()) {
            if (mapa != null) mapa.clear();
            if (desdeCache) {
                tvEstadoMapa.setText(R.string.mapa_sin_datos_locales);
            } else {
                tvEstadoMapa.setText(incidenciaId == null
                        ? R.string.mapa_vacio : R.string.mapa_incidencia_ausente);
            }
            return;
        }
        mostrarMarcador();
    }

    private void mostrarMarcador() {
        if (mapa == null || incidencia == null || !incidencia.exists()) return;
        Object latitud = incidencia.get("latitud");
        Object longitud = incidencia.get("longitud");
        Object titulo = incidencia.get("titulo");
        Object categoria = incidencia.get("categoria");
        if (!(latitud instanceof Number) || !(longitud instanceof Number) || !(titulo instanceof String)) {
            mapa.clear();
            tvEstadoMapa.setText(R.string.mapa_coordenadas_invalidas);
            return;
        }
        double lat = ((Number) latitud).doubleValue();
        double lon = ((Number) longitud).doubleValue();
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            mapa.clear();
            tvEstadoMapa.setText(R.string.mapa_coordenadas_invalidas);
            return;
        }

        mapa.clear();
        LatLng posicion = new LatLng(lat, lon);
        MarkerOptions opciones = new MarkerOptions().position(posicion).title((String) titulo);
        if (categoria instanceof String) opciones.snippet((String) categoria);
        Marker marcador = mapa.addMarker(opciones);
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16));
        if (marcador != null) marcador.showInfoWindow();

        if (incidencia.getMetadata().hasPendingWrites()) {
            tvEstadoMapa.setText(R.string.mapa_pendiente);
        } else if (incidencia.getMetadata().isFromCache()) {
            tvEstadoMapa.setText(R.string.mapa_cache);
        } else {
            tvEstadoMapa.setText(R.string.mapa_listo);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escuchaMapa != null) {
            escuchaMapa.remove();
            escuchaMapa = null;
        }
    }
}
