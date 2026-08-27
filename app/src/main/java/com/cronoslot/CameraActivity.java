package com.cronoslot;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends ComponentActivity {
    private PreviewView preview;
    private LineOverlay overlay;
    private TextView info;
    private TextView lapsView;
    private Db db;

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private byte[] previous;
    private boolean armed = true;
    private boolean calibrationOnly = false;

    private long lastDetection = 0L;
    private long lastLapAt = 0L;
    private long raceStart = 0L;

    private final List<Double> lapTimes = new ArrayList<>();

    private long pilotId;
    private long carId;
    private long trackId;
    private String remote = "";

    private ToneGenerator tone;
    private android.content.SharedPreferences prefs;

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "CronoSlot necesita permiso de cámara para cronometrar.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new Db(this);
        pilotId = getIntent().getLongExtra("pilotId", 0L);
        carId = getIntent().getLongExtra("carId", 0L);
        trackId = getIntent().getLongExtra("trackId", 0L);
        remote = getIntent().getStringExtra("remote");
        if (remote == null) remote = "";
        calibrationOnly = getIntent().getBooleanExtra("calibrationOnly", false);

        prefs = getSharedPreferences("cronoslot_calibration", MODE_PRIVATE);
        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95);

        buildUi();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);

        preview = new PreviewView(this);
        preview.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(preview, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        overlay = new LineOverlay(this);
        overlay.lineX = prefs.getFloat("lineX_" + trackId, 0.50f);
        root.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(14, 14, 14, 14);
        panel.setBackgroundColor(0xBB000000);

        info = new TextView(this);
        info.setTextColor(Color.WHITE);
        info.setTextSize(20f);

        lapsView = new TextView(this);
        lapsView.setTextColor(Color.WHITE);
        lapsView.setTextSize(26f);
        lapsView.setText("Vueltas: 0");

        panel.addView(info);
        panel.addView(lapsView);

        if (calibrationOnly) {
            info.setText("Mueve la línea roja hasta el punto de paso.");
            panel.addView(button("GUARDAR CALIBRACIÓN", v -> {
                prefs.edit().putFloat("lineX_" + trackId, overlay.lineX).apply();
                Toast.makeText(this, "Calibración guardada.", Toast.LENGTH_SHORT).show();
                finish();
            }));
        } else {
            info.setText("Pasa el coche por la línea roja.");
            panel.addView(button("ARMAR DETECCIÓN", v -> {
                armed = true;
                info.setText("DETECCIÓN ACTIVA — pasa el coche por la línea roja");
            }));
            Button finish = button("🔴 TERMINAR CARRERA", v -> confirmFinish());
            panel.addView(finish);
        }

        FrameLayout.LayoutParams panelParams =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
        panelParams.gravity = Gravity.TOP;
        root.addView(panel, panelParams);

        setContentView(root);
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(listener);
        return b;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview previewUseCase = new Preview.Builder().build();
                previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyze);

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        analysis
                );

                mainHandler.post(() -> {
                    if (!calibrationOnly) {
                        info.setText("CÁMARA ACTIVA — pasa el coche por la línea roja");
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> info.setText(
                        "No se pudo iniciar la cámara: " + e.getClass().getSimpleName()));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyze(@NonNull ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            int width = image.getWidth();
            int height = image.getHeight();

            int x = (int) (overlay.lineX * width);
            x = Math.max(10, Math.min(width - 11, x));

            double motion = 0.0;
            int count = 0;

            if (previous != null && previous.length == data.length) {
                for (int y = (int) (height * 0.30);
                     y < (int) (height * 0.70);
                     y += 4) {
                    for (int xx = Math.max(0, x - 12);
                         xx <= Math.min(width - 1, x + 12);
                         xx += 4) {

                        int index = y * width + xx;
                        if (index >= 0 && index < data.length) {
                            motion += Math.abs(
                                    (data[index] & 0xFF) - (previous[index] & 0xFF));
                            count++;
                        }
                    }
                }
            }

            previous = data;

            double score = count == 0 ? 0.0 : motion / count;
            long now = SystemClock.elapsedRealtime();

            if (!calibrationOnly
                    && armed
                    && score > 22.0
                    && now - lastDetection > 250L) {

                lastDetection = now;
                armed = false;

                final long detectionTime = now;

                runOnUiThread(() -> {
                    if (lastLapAt == 0L) {
                        raceStart = detectionTime;
                        info.setText("CRONÓMETRO INICIADO");
                    } else {
                        double seconds = (detectionTime - lastLapAt) / 1000.0;
                        lapTimes.add(seconds);

                        lapsView.setText(
                                "Vuelta " + lapTimes.size() + " · " +
                                        String.format(Locale.getDefault(), "%.3f s", seconds));

                        recordTone(seconds);
                    }

                    lastLapAt = detectionTime;
                });

            } else if (!armed && score < 9.0) {
                armed = true;
            }

        } catch (Exception ignored) {
        } finally {
            image.close();
        }
    }

    private void recordTone(double seconds) {
        Double bestTrack = db.bestTrack(trackId);
        Double bestCombo = db.bestCombo(trackId, pilotId, carId);

        boolean newTrack = bestTrack == null || seconds < bestTrack;
        boolean newCombo = bestCombo == null || seconds < bestCombo;

        if (newCombo) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140);

            if (newTrack) {
                mainHandler.postDelayed(
                        () -> tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140),
                        190
                );
                info.setText("🏆 DOBLE RÉCORD");
            } else {
                info.setText("🏎️ RÉCORD DE COCHE");
            }
        } else if (newTrack) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140);
            info.setText("🏆 RÉCORD DE PISTA");
        }
    }

    private void confirmFinish() {
        new AlertDialog.Builder(this)
                .setTitle("¿Realmente quieres terminar la sesión?")
                .setMessage("Se guardarán todos los tiempos registrados.")
                .setNegativeButton("NO, CONTINUAR", null)
                .setPositiveButton("SÍ, TERMINAR", (dialog, which) -> askNotes())
                .show();
    }

    private void askNotes() {
        final EditText input = new EditText(this);
        input.setHint("Notas de la sesión (opcional)");

        new AlertDialog.Builder(this)
                .setTitle("Notas de la sesión")
                .setMessage("Puedes anotar neumáticos u otras condiciones de esta prueba.")
                .setView(input)
                .setNeutralButton("OMITIR", (dialog, which) -> save(""))
                .setPositiveButton("GUARDAR SESIÓN",
                        (dialog, which) -> save(input.getText().toString()))
                .show();
    }

    private void save(String notes) {
        if (lapTimes.isEmpty()) {
            Toast.makeText(this, "No hay vueltas registradas.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.addSession(
                pilotId,
                carId,
                trackId,
                remote,
                raceStart,
                notes,
                lapTimes
        );

        Toast.makeText(this, "Sesión guardada.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        cameraExecutor.shutdown();
        if (tone != null) tone.release();
        super.onDestroy();
    }

    static class LineOverlay extends View {
        float lineX = 0.50f;

        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);

        LineOverlay(Context context) {
            super(context);
            line.setColor(Color.RED);
            line.setStrokeWidth(7f);
            handle.setColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float x = getWidth() * lineX;
            canvas.drawLine(x, 0f, x, getHeight(), line);
            canvas.drawCircle(x, getHeight() / 2f, 18f, handle);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {

                lineX = Math.max(
                        0.03f,
                        Math.min(0.97f, event.getX() / Math.max(1f, getWidth()))
                );
                invalidate();
                return true;
            }
            return true;
        }
    }
}
