package com.example.ghostbuster;

import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.schemas.lull.Quat;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    FragmentManager supportFragMgr;
    private ArFragment arFragment;
    private AnchorNode originNode;
    private AnchorNode ghostAnchor;
    private Node lineNode;
    private boolean loaded;
    private boolean lineDb;

    private ModelRenderable ghostRenderable;


    private Random rnd;

    private TextView label;

    private boolean pointsSet = false;

    float xpos = 1f;

    private float[] gyroData;
    private boolean wasFiring;
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
                    wasFiring = isFiring;
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


        loaded = false;

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

        if (arFragment.getArSceneView() != null) {
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if (!pointsSet) {
                    pointsSet = true; // Ensure this only runs once
                }
            });
        }

        // Get the ghost model







    }



    @Override
    protected void onResume() {
        super.onResume();

        originNode = createAnchorAtPosition(new Vector3(0,0.5f,-2.5f));

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        //drawLineBetweenPoints(firstAnchorNode, secondAnchorNode);
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (arFragment.getArSceneView() != null && arFragment.getArSceneView().getScene() != null) {
                onUpdate(frameTime);
            }
        });

        rnd = new Random();


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
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.DKGRAY))
                .thenAccept(material -> {
                    ModelRenderable sphereRenderable = ShapeFactory.makeSphere(0.66f, anchorNode.getWorldPosition(), material);
                    anchorNode.setRenderable(sphereRenderable);
                });
    }

    // Credit to mickod, code taken from https://github.com/mickod/LineView
    private void drawLineToPoint(AnchorNode p1, Vector3 endv)
    {
        removeLine(lineNode);
        Vector3 startv = p1.getWorldPosition();

        Vector3 difference = Vector3.subtract(startv, endv);
        Vector3 direction = difference.normalized();
        Quaternion rotation = Quaternion.lookRotation(direction, Vector3.up());

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 255, 0))
                .thenAccept(
                        material -> {
                            ModelRenderable model = ShapeFactory.makeCube(
                                    new Vector3(.02f, .02f, difference.length()), Vector3.zero(), material
                            );
                            lineNode = new Node();
                            lineNode.setParent(p1);
                            lineNode.setRenderable(model);
                            lineNode.setWorldPosition(Vector3.add(startv, endv).scaled(.5f));
                            lineNode.setWorldRotation(rotation);
                        }
                );

    }

    private AnchorNode moveRenderable(AnchorNode aNode, Pose nPose)
    {
        if (aNode == null) return null;
        arFragment.getArSceneView().getScene().removeChild(aNode);
        Frame frame = arFragment.getArSceneView().getArFrame();
        Session sess = arFragment.getArSceneView().getSession();
        Anchor newAnchor = sess.createAnchor(nPose.extractTranslation());
        AnchorNode newANode = new AnchorNode(newAnchor);
        newANode.setRenderable(ghostRenderable);
        newANode.setParent(arFragment.getArSceneView().getScene());
        return newANode;
    }
    private void removeLine(Node lNode)
    {
        if (lNode != null)
        {
            arFragment.getArSceneView().getScene().removeChild(lNode);
            lNode.setParent(null);
            lNode.setRenderable(null);
            lNode = null;
        }
    }

    private void addGhost(Renderable render, Anchor anch)
    {

    }
    private void onUpdate(FrameTime frameTime)
    {
        Pair<float[], Boolean> gunData = getGunData();
        boolean isFiring = gunData.second;
        float[] gyroData = gunData.first;

        Vector3 pointing = new Vector3((float)Math.cos(gyroData[1]), (float)Math.cos(gyroData[2]), -(float)Math.cos(gyroData[0]));

        framesElapsedInMoveCycle += 1;

        if (framesElapsedInMoveCycle > 100)
        {
            if (!loaded)
            {
                Frame frame = arFragment.getArSceneView().getArFrame();
                Session sess = arFragment.getArSceneView().getSession();
                Anchor gAnc = sess.createAnchor(
                        frame.getCamera().getPose()
                                .compose(Pose.makeTranslation(0, 0, -1f))
                                .extractTranslation());

                //drawLineToPoint(originNode, ghostAnchor.getWorldPosition());


                ModelRenderable.builder()
                        .setSource(this, R.raw.teapot)
                        .build()
                        .thenAccept(renderable -> ghostRenderable = renderable)
                        .exceptionally(
                                throwable -> {
                                    Toast toast =
                                            Toast.makeText(this, "Unable to load ghost renderable", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    return null;
                                });
                ghostAnchor = new AnchorNode(gAnc);
                TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                node.setRenderable(ghostRenderable);
                node.setParent(ghostAnchor);
                arFragment.getArSceneView().getScene().addChild(ghostAnchor);
                node.select();

                loaded = true;
            }
            else {
                float deltaX = (rnd.nextFloat() - 0.5f) * frameTime.getDeltaSeconds();
                float deltaY = (rnd.nextFloat() - 0.5f) * frameTime.getDeltaSeconds();
                float deltaZ = (float)Math.sin((double)framesElapsedInMoveCycle/360);

                Anchor oldA = ghostAnchor.getAnchor();
                Pose oldP = oldA.getPose();
                Pose newP = oldP.compose(Pose.makeTranslation(deltaX, deltaY, deltaZ));
                ghostAnchor = moveRenderable(ghostAnchor, newP);
            }

        }


        if(isFiring && !wasFiring) {
            lineDb = true;
            drawLineToPoint(originNode, pointing.scaled(10f));
            label.setText("FIRING");
        }
        else {
            removeLine(lineNode);
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
