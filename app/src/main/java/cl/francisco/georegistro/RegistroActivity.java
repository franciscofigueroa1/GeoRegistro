package cl.francisco.georegistro;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity implements SensorEventListener {

    private EditText etTitulo, etDescripcion;
    private Spinner spCategoria;
    private TextView tvLatitud, tvLongitud, tvEstadoRegistro;
    private Button btnUbicacion, btnGuardar;
    private Date fecha;
    private Double latitud, longitud;
    private boolean ubicacionAproximada;
    private boolean permisoSolicitado;
    private boolean guardadoConfirmado;
    private String documentoPendiente;
    private CancellationTokenSource peticionUbicacion;
    private ListenerRegistration escuchaGuardado;
    private SensorManager sensores;
    private Sensor acelerometro;
    private TextView tvSensor;
    private float sensorX, sensorY, sensorZ;
    private boolean lecturaDisponible;

    private final ActivityResultLauncher<String[]> permisosUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                if (tienePermisoUbicacion()) {
                    obtenerUbicacion();
                } else {
                    boolean bloqueado = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                            && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION);
                    tvEstadoRegistro.setText(bloqueado
                            ? R.string.ubicacion_ajustes : R.string.ubicacion_denegada);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        spCategoria = findViewById(R.id.spCategoria);
        tvLatitud = findViewById(R.id.tvLatitud);
        tvLongitud = findViewById(R.id.tvLongitud);
        tvEstadoRegistro = findViewById(R.id.tvEstadoRegistro);
        btnUbicacion = findViewById(R.id.btnUbicacion);
        btnGuardar = findViewById(R.id.btnGuardar);
        tvSensor = findViewById(R.id.tvSensor);
        sensores = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensores != null) {
            acelerometro = sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        fecha = new Date();
        if (savedInstanceState != null) {
            fecha = new Date(savedInstanceState.getLong("fecha"));
            permisoSolicitado = savedInstanceState.getBoolean("permisoSolicitado");
            documentoPendiente = savedInstanceState.getString("documentoPendiente");
            lecturaDisponible = savedInstanceState.getBoolean("lecturaDisponible");
            if (lecturaDisponible) {
                sensorX = savedInstanceState.getFloat("sensorX");
                sensorY = savedInstanceState.getFloat("sensorY");
                sensorZ = savedInstanceState.getFloat("sensorZ");
                mostrarSensor();
            }
            if (savedInstanceState.containsKey("latitud")) {
                latitud = savedInstanceState.getDouble("latitud");
                longitud = savedInstanceState.getDouble("longitud");
                ubicacionAproximada = savedInstanceState.getBoolean("ubicacionAproximada");
                mostrarCoordenadas();
            }
        }

        TextView tvFecha = findViewById(R.id.tvFecha);
        String fechaTexto = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(fecha);
        tvFecha.setText(getString(R.string.fecha_valor, fechaTexto));

        btnUbicacion.setOnClickListener(v -> solicitarUbicacion());
        btnGuardar.setOnClickListener(v -> guardarIncidencia());
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        if (documentoPendiente != null) {
            activarFormulario(false);
            tvEstadoRegistro.setText(R.string.guardado_pendiente);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        escucharGuardado();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (documentoPendiente == null) {
            lecturaDisponible = false;
            tvSensor.setText(acelerometro == null
                    ? R.string.sensor_ausente : R.string.sensor_esperando);
        } else if (!lecturaDisponible) {
            tvSensor.setText(acelerometro == null
                    ? R.string.sensor_ausente : R.string.sensor_sin_lectura);
        }
        if (acelerometro != null && sensores != null) {
            boolean registrado = sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
            if (!registrado && documentoPendiente == null) {
                tvSensor.setText(R.string.sensor_sin_lectura);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent evento) {
        if (documentoPendiente != null || evento.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        sensorX = evento.values[0];
        sensorY = evento.values[1];
        sensorZ = evento.values[2];
        lecturaDisponible = true;
        mostrarSensor();
    }

    private void mostrarSensor() {
        tvSensor.setText(getString(R.string.sensor_valores,
                (double) sensorX, (double) sensorY, (double) sensorZ));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precision) {
    }

    @Override
    protected void onPause() {
        if (sensores != null) {
            sensores.unregisterListener(this);
        }
        super.onPause();
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarUbicacion() {
        if (tienePermisoUbicacion()) {
            obtenerUbicacion();
            return;
        }

        if (permisoSolicitado
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            tvEstadoRegistro.setText(R.string.ubicacion_ajustes);
            return;
        }

        permisoSolicitado = true;
        permisosUbicacion.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void obtenerUbicacion() {
        boolean precisa = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean aproximada = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!precisa && !aproximada) {
            tvEstadoRegistro.setText(R.string.ubicacion_denegada);
            return;
        }

        LocationManager ubicaciones = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ubicaciones == null || !ubicaciones.isLocationEnabled()) {
            tvEstadoRegistro.setText(R.string.ubicacion_desactivada);
            return;
        }

        latitud = null;
        longitud = null;
        tvLatitud.setText(R.string.latitud_pendiente);
        tvLongitud.setText(R.string.longitud_pendiente);
        btnUbicacion.setEnabled(false);
        tvEstadoRegistro.setText(R.string.ubicacion_buscando);

        CurrentLocationRequest solicitud = new CurrentLocationRequest.Builder()
                .setPriority(precisa ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .setDurationMillis(15000)
                .build();
        CancellationTokenSource peticion = new CancellationTokenSource();
        peticionUbicacion = peticion;

        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .getCurrentLocation(solicitud, peticion.getToken())
                    .addOnSuccessListener(ubicacion -> {
                        if (isDestroyed() || peticionUbicacion != peticion) return;
                        peticionUbicacion = null;
                        btnUbicacion.setEnabled(true);
                        if (ubicacion == null) {
                            tvEstadoRegistro.setText(R.string.ubicacion_no_disponible);
                            return;
                        }
                        latitud = ubicacion.getLatitude();
                        longitud = ubicacion.getLongitude();
                        ubicacionAproximada = !precisa;
                        mostrarCoordenadas();
                        tvEstadoRegistro.setText(ubicacionAproximada
                                ? R.string.ubicacion_aproximada : R.string.ubicacion_lista);
                    })
                    .addOnFailureListener(error -> {
                        if (isDestroyed() || peticionUbicacion != peticion) return;
                        peticionUbicacion = null;
                        btnUbicacion.setEnabled(true);
                        tvEstadoRegistro.setText(R.string.ubicacion_no_disponible);
                    });
        } catch (SecurityException error) {
            peticionUbicacion = null;
            btnUbicacion.setEnabled(true);
            tvEstadoRegistro.setText(R.string.ubicacion_denegada);
        }
    }

    private void mostrarCoordenadas() {
        tvLatitud.setText(getString(R.string.latitud_valor, latitud));
        tvLongitud.setText(getString(R.string.longitud_valor, longitud));
    }

    private void guardarIncidencia() {
        if (documentoPendiente != null) return;
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        if (titulo.isEmpty()) {
            etTitulo.setError(getString(R.string.error_titulo));
            etTitulo.requestFocus();
            return;
        }
        if (descripcion.isEmpty()) {
            etDescripcion.setError(getString(R.string.error_descripcion));
            etDescripcion.requestFocus();
            return;
        }
        if (spCategoria.getSelectedItemPosition() == 0) {
            tvEstadoRegistro.setText(R.string.error_categoria);
            return;
        }
        if (latitud == null || longitud == null || latitud < -90 || latitud > 90
                || longitud < -180 || longitud > 180) {
            tvEstadoRegistro.setText(R.string.error_coordenadas);
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            tvEstadoRegistro.setText(R.string.error_sesion);
            return;
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("titulo", titulo);
        datos.put("descripcion", descripcion);
        datos.put("categoria", spCategoria.getSelectedItem().toString());
        datos.put("fecha", new Timestamp(fecha));
        datos.put("latitud", latitud);
        datos.put("longitud", longitud);
        datos.put("ubicacionAproximada", ubicacionAproximada);
        datos.put("sensorDisponible", acelerometro != null);
        datos.put("lecturaSensorDisponible", lecturaDisponible);
        if (lecturaDisponible) {
            Map<String, Object> datoSensor = new HashMap<>();
            datoSensor.put("x", (double) sensorX);
            datoSensor.put("y", (double) sensorY);
            datoSensor.put("z", (double) sensorZ);
            datos.put("datoSensor", datoSensor);
        }

        DocumentReference documento = FirebaseFirestore.getInstance()
                .collection("usuarios").document(usuario.getUid())
                .collection("incidencias").document();
        documentoPendiente = documento.getId();
        activarFormulario(false);
        tvEstadoRegistro.setText(R.string.guardando);

        documento.set(datos).addOnFailureListener(error -> {
            if (isDestroyed()) return;
            if (escuchaGuardado != null) {
                escuchaGuardado.remove();
                escuchaGuardado = null;
            }
            documentoPendiente = null;
            activarFormulario(true);
            tvEstadoRegistro.setText(R.string.error_guardado);
        });
        escucharGuardado();
    }

    private void escucharGuardado() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (documentoPendiente == null || usuario == null || escuchaGuardado != null) return;

        escuchaGuardado = FirebaseFirestore.getInstance()
                .collection("usuarios").document(usuario.getUid())
                .collection("incidencias").document(documentoPendiente)
                .addSnapshotListener(MetadataChanges.INCLUDE, (documento, error) -> {
                    if (isFinishing() || guardadoConfirmado) return;
                    if (error != null) {
                        tvEstadoRegistro.setText(R.string.error_confirmacion);
                        return;
                    }
                    if (documento == null) return;
                    if (!documento.exists()) {
                        if (!documento.getMetadata().isFromCache()
                                && !documento.getMetadata().hasPendingWrites()) {
                            documentoPendiente = null;
                            activarFormulario(true);
                            tvEstadoRegistro.setText(R.string.error_guardado);
                            if (escuchaGuardado != null) {
                                escuchaGuardado.remove();
                                escuchaGuardado = null;
                            }
                        }
                        return;
                    }
                    if (documento.getMetadata().hasPendingWrites() || documento.getMetadata().isFromCache()) {
                        tvEstadoRegistro.setText(R.string.guardado_pendiente);
                    } else {
                        guardadoConfirmado = true;
                        Toast.makeText(this, R.string.guardado_correcto, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void activarFormulario(boolean activo) {
        etTitulo.setEnabled(activo);
        etDescripcion.setEnabled(activo);
        spCategoria.setEnabled(activo);
        btnUbicacion.setEnabled(activo);
        btnGuardar.setEnabled(activo);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putLong("fecha", fecha.getTime());
        estado.putBoolean("permisoSolicitado", permisoSolicitado);
        estado.putString("documentoPendiente", documentoPendiente);
        estado.putBoolean("lecturaDisponible", lecturaDisponible);
        if (lecturaDisponible) {
            estado.putFloat("sensorX", sensorX);
            estado.putFloat("sensorY", sensorY);
            estado.putFloat("sensorZ", sensorZ);
        }
        if (latitud != null && longitud != null) {
            estado.putDouble("latitud", latitud);
            estado.putDouble("longitud", longitud);
            estado.putBoolean("ubicacionAproximada", ubicacionAproximada);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (peticionUbicacion != null) {
            peticionUbicacion.cancel();
            peticionUbicacion = null;
            btnUbicacion.setEnabled(documentoPendiente == null);
            tvEstadoRegistro.setText(R.string.ubicacion_cancelada);
        }
        if (escuchaGuardado != null) {
            escuchaGuardado.remove();
            escuchaGuardado = null;
        }
    }
}
