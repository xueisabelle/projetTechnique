package com.example.helloindoor;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.estimote.indoorsdk.EstimoteCloudCredentials;
import com.estimote.indoorsdk.IndoorLocationManagerBuilder;
import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener;
import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager;
import com.estimote.indoorsdk_module.cloud.CloudCallback;
import com.estimote.indoorsdk_module.cloud.EstimoteCloudException;
import com.estimote.indoorsdk_module.cloud.IndoorCloudManager;
import com.estimote.indoorsdk_module.cloud.IndoorCloudManagerFactory;
import com.estimote.indoorsdk_module.cloud.Location;
import com.estimote.indoorsdk_module.cloud.LocationPosition;
import com.estimote.indoorsdk_module.cloud.LocationWall;
import com.estimote.indoorsdk_module.view.IndoorLocationView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final double DOT_STEP = 0.5d;

    private static final EstimoteCloudCredentials estimoteCredentials =
            new EstimoteCloudCredentials("-salon-isabelle-kxa",
                    "844ed4f78a49624eadb9c7e28fb41021");

    private ScanningIndoorLocationManager indoorLocationManager;

    //method

    public double sign(DoublePoint p1, DoublePoint p2, DoublePoint p3)
    {
        return (p1.getX() - p3.getX()) * (p2.getY() - p3.getY()) - (p2.getX() - p3.getX()) * (p1.getY() - p3.getY());
    }

    public boolean pointInTriangle(DoublePoint pt, DoublePoint v1, DoublePoint v2, DoublePoint v3)
    {
        boolean b1, b2, b3;

        b1 = sign(pt, v1, v2) < 0.0d;
        //Log.d("isDotInTriangle", String.valueOf(b1));
        b2 = sign(pt, v2, v3) < 0.0d;
        //Log.d("isDotInTriangle", String.valueOf(b2));
        b3 = sign(pt, v3, v1) < 0.0d;
      //  Log.d("isDotInTriangle", String.valueOf(b3));

        return ((b1 == b2) && (b2 == b3));
    }

    // afficher sur l'application mobile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IndoorCloudManager cloudManager = new IndoorCloudManagerFactory().create(this,
                estimoteCredentials);

        cloudManager.getLocation("salon-ix", new CloudCallback<Location>() {
            @Override
            public void success(Location location) {
                for (LocationWall locationWall : location.getWalls()) {
                    //Log.d("walls","Position du 1er angle de mur " + locationWall.getX1() + " " + locationWall.getY1());
                   // Log.d("walls","Position du 2eme angle de mur " + locationWall.getX2() + " " + locationWall.getY2());
                }

                indoorLocationManager =
                        new IndoorLocationManagerBuilder(getApplicationContext(),
                                location,
                                estimoteCredentials)
                                .withDefaultScanner()
                                .build();

                final IndoorLocationView indoorView = findViewById(R.id.indoor_view);
                indoorView.setLocation(location);

                final DoublePoint triangle1Point1 = new DoublePoint(0.5, 5.3);
                final DoublePoint triangle1Point2= new DoublePoint(0, 0.1);
                final DoublePoint triangle1Point3 = new DoublePoint(2.9, 0);

                final DoublePoint triangle2Point1 = new DoublePoint(0.5, 5.4);
                final DoublePoint triangle2Point2 = new DoublePoint(3.3, 5.6);
                final DoublePoint triangle2Point3 = new DoublePoint(2.8, 0);

                /* Test point à remettre dans la nouvelle salle

                List<LocationPosition> p = new ArrayList<>();
                for(double i=0d; i<3.4d; i+=0.1d) {
                    for (double j = 0d; j < 5.6d; j += 0.2d) {


                        p.add(new LocationPosition(i, j, 0d));
                    }

                }
                indoorView.setCustomPoints(p);
                */


                List<LocationPosition> dots = new ArrayList<>();
                for (double i = 0d; i < 5.8d; i+=DOT_STEP) {
                    for (double j = 0d; j<3.3d; j+=DOT_STEP) {

                        if (pointInTriangle(new DoublePoint(j, i), triangle1Point1, triangle1Point2, triangle1Point3)
                                || pointInTriangle(new DoublePoint(j, i), triangle2Point1, triangle2Point2, triangle2Point3)) {
                            LocationPosition current = new LocationPosition(j, i, 0d);
                            dots.add(current);
                        }
                    }
                }

                indoorView.setCustomPoints(dots);
                
                List<List<Node>> adj = definirListAdjacents(dots);
                callDijkstra(adj);


                indoorLocationManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
                    @Override
                    public void onPositionUpdate(LocationPosition position) {
                        // here, we update the IndoorLocationView with the current position,
                        // but you can use the position for anything you want
                        Log.w("appli", "Position :" + position.getX() + " " + position.getY());
                        indoorView.updatePosition(position);
                    }

                    @Override
                    public void onPositionOutsideLocation() {

                        indoorView.hidePosition();
                        Log.w("appli", "je suis en dehors !!!");
                    }
                });
                indoorLocationManager.startPositioning();
            }

            @Override
            public void failure(EstimoteCloudException e) {
                // oops!
            }
        });
    }

    public List<List<Node>> definirListAdjacents(List<LocationPosition> dots) {

        List<List<Node>> result = new ArrayList<>();

        for (LocationPosition current : dots) {

            List<Node> adjacents = new ArrayList<>();

            double x1, y1, x2, y2, x3, y3, x4, y4;
            x1 = current.getX() + DOT_STEP;
            y1 = current.getY();

            x2 = current.getX();
            y2 = current.getY() - DOT_STEP;

            x3 = current.getX() - DOT_STEP;
            y3 = current.getY();

            x4 = current.getX();
            y4 = current.getY() + DOT_STEP;

            for (LocationPosition potentielAdjacent : dots) {
                if (potentielAdjacent.getX() == x1 && potentielAdjacent.getY() == y1) {
                    adjacents.add(new Node(dots.indexOf(potentielAdjacent) ,Node.NODE_COST));
                    continue;
                }
                if (potentielAdjacent.getX() == x2 && potentielAdjacent.getY() == y2) {
                    adjacents.add(new Node(dots.indexOf(potentielAdjacent) ,Node.NODE_COST));
                    continue;
                }
                if (potentielAdjacent.getX() == x3 && potentielAdjacent.getY() == y3) {
                    adjacents.add(new Node(dots.indexOf(potentielAdjacent) ,Node.NODE_COST));
                    continue;
                }
                if (potentielAdjacent.getX() == x4 && potentielAdjacent.getY() == y4) {
                    adjacents.add(new Node(dots.indexOf(potentielAdjacent) ,Node.NODE_COST));
                    continue;
                }
            }

            // On ajoute le noeud lui même
            adjacents.add(new Node(dots.indexOf(current), Node.NODE_COST));

            //On ajoute cette liste de noeuds adjacents à la grande liste
            result.add(adjacents);


        }

        return result;

    }

    public void callDijkstra(List<List<Node> > adj) {
        {
            int V = 5;
            int source = 0;

            // Adjacency list representation of the
            // connected edges
            //List<List<Node> > adj = new ArrayList<List<Node> >();

            // Initialize list for every node


            /*for (int i = 0; i < V; i++) {
                List<Node> item = new ArrayList<Node>();
                adj.add(item);
            }*/

            /*
            adj.get(0).add(new Node(1, 9));
            adj.get(0).add(new Node(2, 6));
            adj.get(0).add(new Node(3, 5));
            adj.get(0).add(new Node(4, 3));

            adj.get(2).add(new Node(1, 2));
            adj.get(2).add(new Node(3, 4));*/



            // Calculate the single source shortest path
            Dijkstra dijkstra = new Dijkstra(adj.size());
            dijkstra.dijkstra(adj, source);

            // Print the shortest path to all the nodes
            // from the source node
            System.out.println("The shorted path from node :");
            for (int i = 0; i < dijkstra.dist.length; i++)
                Log.i("paths",source + " to " + i + " is "
                        + dijkstra.dist[i]);
        }
    }


}
