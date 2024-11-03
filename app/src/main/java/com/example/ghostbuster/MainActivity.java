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
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    FragmentManager supportFragMgr;
    private ArFragment arFragment;
    private AnchorNode firstAnchorNode;
    private AnchorNode secondAnchorNode;
    private AnchorNode lineNode;

    private TextView label;

    private boolean pointsSet = false;

    float xpos = 1f;

    private float[] gyroData;
    private boolean isFiring;

    private boolean fetchingData;
    private ExecutorService threadPool;
    private Future<Pair<float[], Boolean>> fetchTask;

    private AnchorNode currentLineNode;  // Keep track of the current line node





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

        // Set up data to regularly sync the gyro


        //Button btn = findViewById(R.id.button);
        //EditText txt = findViewById(R.id.editTextText);

        //btn.setOnClickListener(r -> txt.setText(doThing()));


                supportFragMgr = getSupportFragmentManager();
                arFragment = (ArFragment) supportFragMgr.findFragmentById(R.id.arFragment);

        if (arFragment.getArSceneView() != null) {
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if (!pointsSet) {
                    setPointsInARSpace(new Vector3(0, 0, -1), new Vector3(0.5f, 0, -1.5f));
                    pointsSet = true; // Ensure this only runs once
                }
            });
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        label.setText(Float.toString(getGunData().first[0]));

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (arFragment.getArSceneView() != null && arFragment.getArSceneView().getScene() != null) {
                xpos  += 1e-2f;
                xpos = (float) (xpos % 1.2);
                //setPointsInARSpace(new Vector3(0, 0, 0), new Vector3(xpos, 0, 1));
                //updateEndPosition(new Vector3(xpos,0,1));
                handler.post(Update);
                firstAnchorNode = createAnchorAtPosition(new Vector3(0,0,0));
                drawLineBetweenPoints(firstAnchorNode,new Vector3(xpos,0,1));

            }
        });
    }


    private void setPointsInARSpace(Vector3 firstPoint, Vector3 secondPoint) {
        // Create an anchor at each specified point
        firstAnchorNode = createAnchorAtPosition(firstPoint);
        secondAnchorNode = createAnchorAtPosition(secondPoint);

        // Draw line between the two points
        //drawLineBetweenPoints(firstAnchorNode, secondAnchorNode);
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

    private void updateEndPosition(Vector3 newPosition) {

        secondAnchorNode.setParent(null);
        secondAnchorNode = createAnchorAtPosition(newPosition);
        // Update the end point position
        //secondAnchorNode.setWorldPosition(newPosition);

        // Redraw the line
        //drawLineBetweenPoints(firstAnchorNode, secondAnchorNode);
    }


    private void drawLineBetweenPoints(AnchorNode start, Vector3 endPosition) {
        // Set or update the position of the start node if it hasn't been set already
        if (firstAnchorNode == null) {
            firstAnchorNode = start;
        }

        // Remove previous endNode and lineNode if they exist
        if (secondAnchorNode != null) {
            secondAnchorNode.setParent(null);  // Detach to delete the previous end node
        }
        if (lineNode != null) {
            lineNode.setParent(null);  // Detach to delete the previous line node
        }

        // Create and position a new endNode at the updated end position
        secondAnchorNode = new AnchorNode();
        secondAnchorNode.setWorldPosition(endPosition);
        secondAnchorNode.setParent(arFragment.getArSceneView().getScene());

        // Calculate the midpoint, direction, and length for the line
        Vector3 startPoint = firstAnchorNode.getWorldPosition();
        Vector3 midPoint = Vector3.add(startPoint, endPosition).scaled(0.5f);
        Vector3 direction = Vector3.subtract(endPosition, startPoint);
        float lineLength = direction.length();
        Vector3 normalizedDirection = direction.normalized();

        // Create the line renderable and attach it to a new lineNode
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> {
                    ModelRenderable lineRenderable = ShapeFactory.makeCylinder(0.005f, lineLength, new Vector3(0, 0, 0), material);

                    // Create and position the line node
                    lineNode = new AnchorNode();
                    lineNode.setParent(arFragment.getArSceneView().getScene());
                    lineNode.setWorldPosition(midPoint);
                    lineNode.setRenderable(lineRenderable);

                    // Rotate the line to align with the direction vector
                    Vector3 upVector = new Vector3(0, 1, 0);  // Default cylinder orientation
                    Quaternion rotation = Quaternion.rotationBetweenVectors(upVector, normalizedDirection);
                    lineNode.setWorldRotation(rotation);
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Error creating line", Toast.LENGTH_SHORT).show();
                    return null;
                });
    }

    private void updateStartNode() {
        // Get the current position of the device
        Vector3 devicePosition = getCurrentDevicePosition();

        // Remove the previous startNode if it exists
        if (firstAnchorNode != null) {
            firstAnchorNode.setParent(null);  // Remove from the scene
        }

        // Create a new startNode at the device's current position
        firstAnchorNode = new AnchorNode();
        firstAnchorNode.setWorldPosition(devicePosition);
        firstAnchorNode.setParent(arFragment.getArSceneView().getScene());

        // Redraw the line with the new start position
        //if (secondAnchorNode != null) {
            //drawLineBetweenPoints(firstAnchorNode, secondAnchorNode.getWorldPosition());
        //}
    }

    private Vector3 getCurrentDevicePosition() {
        Pose pose = arFragment.getArSceneView().getArFrame().getCamera().getPose();
        return new Vector3(pose.tx(), pose.ty(), pose.tz());
    }

    private final Handler handler = new Handler();
    private final Runnable Update = new Runnable() {
        @Override
        public void run() {
            updateStartNode();  // Update the start position with the device's current position
            handler.postDelayed(this, 1000);  // Repeat every second

            Pair<float[], Boolean> gunData = getGunData();
            boolean isFiring = gunData.second;
            float[] gyroData = gunData.first;

            Vector3 pointing = new Vector3(gyroData[1], gyroData[2], -gyroData[0]);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(Update);  // Stop updating on pause
    }


}
