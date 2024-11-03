package com.example.ghostbuster;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.core.HitResult;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
//import com.google.ar.sceneform.renderables.ModelRenderable;
import com.google.ar.core.Plane;
import android.view.MotionEvent;  // For handling touch events
import android.net.Uri;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    FragmentManager supportFragMgr;
    private ArFragment arFragment;
    private AnchorNode originNode;
    private Node lineNode;
    private boolean lineDb;




    private TextView label;

    private boolean pointsSet = false;

    float xpos = 1f;

    private float[] gyroData;
    private boolean isFiring;


    private boolean ghostVisible;
    private Vector3 ghostPosition;
    private int framesElapsedInMoveCycle;

    private boolean fetchingData;
    private Future<Pair<float[], Boolean>> fetchTask;






    private Pair<float[], Boolean> getGunData() {

        // Check if already waiting for server response
        if (fetchingData) {
            // If the server has responded, get the response.
            if (fetchTask.isDone()) {
                fetchingData = false;
                try {
                    Pair<float[], Boolean> result = fetchTask.get();
                    gyroData = result.first;
                    isFiring = result.second;
                    return result;
                } catch (ExecutionException e)
                {
                    e.printStackTrace();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            return new Pair<float[], Boolean>(gyroData, isFiring);
        }

        // Set up a listener
        ExecutorService threadpool = Executors.newCachedThreadPool();
        fetchTask = threadpool.submit(Client::fetchData);
        fetchingData = true;
        return new Pair<float[], Boolean>(gyroData, isFiring);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fetchingData = false;
        label = findViewById(R.id.textView);

        gyroData = new float[] {0, 0, 0};
        isFiring = false;

        framesElapsedInMoveCycle = 0;
        // Set up data to regularly sync the gyro


        //Button btn = findViewById(R.id.button);
        //EditText txt = findViewById(R.id.editTextText);

        //btn.setOnClickListener(r -> txt.setText(doThing()));


                supportFragMgr = getSupportFragmentManager();
                arFragment = (ArFragment) supportFragMgr.findFragmentById(R.id.arFragment);
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            // Load your model here
            ModelRenderable.builder()
                    .setSource(this, Uri.parse("models/your_ghost_model.glb")) // Adjust the filename and format
                    .build()
                    .thenAccept(renderable -> addModelToScene(hitResult, renderable))
                    .exceptionally(
                            throwable -> {
                                Toast.makeText(this, "Unable to load model", Toast.LENGTH_SHORT).show();
                                return null;
                            });
        });

        if (arFragment.getArSceneView() != null) {
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if (!pointsSet) {
                    pointsSet = true; // Ensure this only runs once
                }
            });
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        originNode = createAnchorAtPosition(new Vector3(0,0,0));

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        //drawLineBetweenPoints(firstAnchorNode, secondAnchorNode);
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (arFragment.getArSceneView() != null && arFragment.getArSceneView().getScene() != null) {
                onUpdate(frameTime);
            }
        });

    }
    private void addModelToScene(HitResult hitResult, ModelRenderable renderable) {
        TransformableNode ghostNode = new TransformableNode(arFragment.getTransformationSystem());
        ghostNode.setParent(arFragment.getArSceneView().getScene());
        ghostNode.setRenderable(renderable);
        ghostNode.select();
        ghostNode.setWorldPosition(new Vector3(hitResult.getHitPose().tx(), hitResult.getHitPose().ty(), hitResult.getHitPose().tz()));
    }

    private AnchorNode createAnchorAtPosition(Vector3 position) {
        // Create an anchor node and manually set its world position
        AnchorNode anchorNode = new AnchorNode();
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        anchorNode.setWorldPosition(position);

        // Place a sphere as a visual marker at the anchor position
        placePoint(anchorNode);
        return anchorNode;
    }

    private void placePoint(AnchorNode anchorNode) {
        // Create a small sphere at the anchor point to represent it visually
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material -> {
                    ModelRenderable sphereRenderable = ShapeFactory.makeSphere(0.02f, new Vector3(0, 0, 0), material);
                    anchorNode.setRenderable(sphereRenderable);
                });
    }

    private void drawLineToPoint(AnchorNode p1, Vector3 endv)
    {
        Vector3 startv = p1.getWorldPosition();

        Vector3 difference = Vector3.subtract(startv, endv);
        Vector3 direction = difference.normalized();
        Quaternion rotation = Quaternion.lookRotation(direction, Vector3.up());

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(255, 0, 0))
                .thenAccept(
                        material -> {
                            ModelRenderable model = ShapeFactory.makeCube(
                                    new Vector3(.01f, .01f, difference.length()), Vector3.zero(), material
                            );
                            lineNode = new Node();
                            lineNode.setParent(p1);
                            lineNode.setRenderable(model);
                            lineNode.setWorldPosition(Vector3.add(startv, endv).scaled(.5f));
                            lineNode.setWorldRotation(rotation);
                        }
                );

        lineDb = false;
    }

    private void removeLine(Node lNode)
    {
        if (lNode != null)
        {
            lineDb = true;
            arFragment.getArSceneView().getScene().removeChild(lNode);
            lNode.setParent(null);
            lNode.setRenderable(null);
            lNode = null;
            lineDb = false;
        }
    }


    private void onUpdate(FrameTime frameTime)
    {
        xpos  += 1e-2f;
        xpos = (float) (xpos % 1.2);
        //setPointsInARSpace(new Vector3(0, 0, 0), new Vector3(xpos, 0, 1));
        //updateEndPosition(new Vector3(xpos,0,1));
        //handler.post(updateStartPos);




        Pair<float[], Boolean> gunData = getGunData();
        boolean isFiring = gunData.second;
        float[] gyroData = gunData.first;

        Vector3 pointing = new Vector3((float)Math.cos(gyroData[1]), (float)Math.cos(gyroData[2]), (float)Math.cos(gyroData[0]));

        framesElapsedInMoveCycle += 1;

        removeLine(lineNode);
        if(isFiring && !lineDb) {
            lineDb = true;
            drawLineToPoint(originNode, pointing.scaled(10f));
            label.setText("FIRING");
        }
        else {
            label.setText(Float.toString(gyroData[0]));
        }
    }
/*
    private final Handler handler = new Handler();
    private final Runnable updateStartPos = new Runnable() {
        @Override
        public void run() {
            updateStartNode();  // Update the start position with the device's current position

        }
    };
*/
    @Override
    protected void onPause() {
        super.onPause();
        //handler.removeCallbacks(updateStartPos);  // Stop updating on pause
    }


}
