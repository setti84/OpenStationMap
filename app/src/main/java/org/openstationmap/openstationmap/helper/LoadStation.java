package org.openstationmap.openstationmap.helper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openstationmap.openstationmap.MainActivity;
import org.openstationmap.openstationmap.R;
import org.openstationmap.openstationmap.objects.BahnColor;
import org.openstationmap.openstationmap.objects.SncfColor;
import org.openstationmap.openstationmap.objects.Station;

/**
 * Created by sebastian on 10/9/17.
 */

public class LoadStation extends AsyncTask <String, Void, Station>{

    private GoogleMap mMap;
    private String fileExtension;
    private String folderName;
    private ProgressDialog ringProgressDialog;
    private MainActivity main;
    private GeoJsonLayer layer;

    public LoadStation(MainActivity maini, GoogleMap mMapi, String fileExtensioni, String folderNamei, GeoJsonLayer layeri){
        main = maini;
        mMap = mMapi;
        fileExtension = fileExtensioni;
        folderName = folderNamei;
        layer = layeri;
    }




    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        ringProgressDialog = new ProgressDialog(main);
        ringProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        ringProgressDialog.setTitle("read Station data");
        ringProgressDialog.setMessage("Please Wait");
        ringProgressDialog.setIndeterminate(false);
        ringProgressDialog.show();
    }

    @Override
    protected Station doInBackground(String... name) {

        String stationName = name[0];
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + folderName + "/" + stationName + fileExtension;
        JsonParser jsonParser = new JsonParser();
        JsonObject parseResult;
        JSONObject jsonStation;
        HashMap<Integer, JSONObject> geoJsonLayer = new HashMap<>();
        HashMap<Integer, GeoJsonLayer> googleMapsLayer = new HashMap<>();

        int singleLevel;
        Station stat;
        String[] elements;
        LatLngBounds bounds = null;
        Log.v("Log2", String.valueOf("download triggered5"));
        try {
            parseResult = jsonParser.parse(new FileReader(dir)).getAsJsonObject();
            jsonStation = new JSONObject(parseResult.toString());

            bounds = new LatLngBounds(new LatLng((Double) jsonStation.getJSONArray("bbox").get(1),(Double) jsonStation.getJSONArray("bbox").get(0)),
                    new LatLng((Double) jsonStation.getJSONArray("bbox").get(3),(Double) jsonStation.getJSONArray("bbox").get(2)));

            for(int i = 0 ; i<jsonStation.getJSONArray("features").length() ;i++ ){

                elements = jsonStation.getJSONArray("features").getJSONObject(i).getJSONObject("properties").getString("level").split(";", -1);
                for( String element : elements ) {
                    JSONObject temporary;
                    JSONObject temporary2;

                    if( element.isEmpty() ) {
                        continue;
                    }
                    singleLevel = Integer.parseInt(element);
                    if(!geoJsonLayer.containsKey(singleLevel)){
                        geoJsonLayer.put(singleLevel, createJSON());
                    }
                    temporary = jsonStation.getJSONArray("features").getJSONObject(i);
                    temporary2 = geoJsonLayer.get(singleLevel);
                    temporary2.getJSONArray("features").put(temporary);
                }
            }
            //Log.e("Log ", String.valueOf(zeit));
        } catch (JSONException | FileNotFoundException e) {
            e.printStackTrace();
        }

        for (Object o : geoJsonLayer.entrySet()) {
            Map.Entry me = (Map.Entry) o;
            googleMapsLayer.put((Integer) me.getKey(), new GeoJsonLayer(mMap, (JSONObject) me.getValue()));
        }

        setStyleBahn(googleMapsLayer);
        //setStyleSncf(googleMapsLayer);

        stat = new Station(stationName, googleMapsLayer, bounds);

        return stat;
    }



    protected void onPostExecute(Station stat) {
        super.onPostExecute(stat);
        ringProgressDialog.cancel();

        // choose first selected level, if 0 or 1 is not available pick something else
        int level = 0;
        for (Object key : stat.getData().keySet()){
             level = (Integer) key;
        }
        if (stat.getData().keySet().contains(1)) {
            level = 1;
        }
        if (stat.getData().keySet().contains(0)) {
            level = 0;
        }
        if(layer != null){
            layer.removeLayerFromMap();
        }

        layer = (GeoJsonLayer) stat.getData().get(level);
        layer.setOnFeatureClickListener(new Layer.OnFeatureClickListener() {
            @Override
            public void onFeatureClick(Feature feature) {
                Log.i("GeoJsonClick", "Feature clicked: " + String.valueOf(feature));

            }
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stat.getBounds().getCenter(), 18));
        main.setLayer(layer);
        main.setStation(stat);
        layer.addLayerToMap();
        main.LevelList(stat);
    }

    private JSONObject createJSON(){

        JSONObject featureCollection = new JSONObject();
        JSONArray features = new JSONArray();

        try {
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", features);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return featureCollection;
    }

    public static void setStyleBahn(HashMap<Integer, GeoJsonLayer> googleMapsLayer) {

        GeoJsonPolygonStyle roomStyle = new GeoJsonPolygonStyle();
        roomStyle.setFillColor(BahnColor.room.getColor());
        roomStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonPolygonStyle corridorStyle = new GeoJsonPolygonStyle();
        corridorStyle.setFillColor(BahnColor.corridor.getColor());
        corridorStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonPolygonStyle facilitiesStyle = new GeoJsonPolygonStyle();
        facilitiesStyle.setFillColor(BahnColor.facilities.getColor());
        facilitiesStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonPolygonStyle serviceStyle = new GeoJsonPolygonStyle();
        serviceStyle.setFillColor(BahnColor.service.getColor());
        serviceStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonPolygonStyle eleStyle = new GeoJsonPolygonStyle();
        eleStyle.setFillColor(BahnColor.elevator.getColor());
        eleStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonPolygonStyle noAccessStyle = new GeoJsonPolygonStyle();
        noAccessStyle.setFillColor(BahnColor.roomNoAccess.getColor());
        noAccessStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonPolygonStyle platfStyle = new GeoJsonPolygonStyle();
        platfStyle.setFillColor(BahnColor.platform.getColor());
        platfStyle.setStrokeColor(BahnColor.roomStroke.getColor());

        GeoJsonLineStringStyle footwayStyle = new GeoJsonLineStringStyle();
        footwayStyle.setColor(BahnColor.footway.getColor());
        footwayStyle.setWidth(1.5f);

        GeoJsonLineStringStyle stepsStyle = new GeoJsonLineStringStyle();
        stepsStyle.setColor(BahnColor.steps.getColor());
        stepsStyle.setWidth(3f);

        GeoJsonLineStringStyle doorStyle = new GeoJsonLineStringStyle();
        doorStyle.setColor(BahnColor.door.getColor());
        doorStyle.setWidth(5f);

        GeoJsonPointStyle elePoint = new GeoJsonPointStyle();
        elePoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.aufzug));
        elePoint.setAnchor(0.5f,0.5f);

        GeoJsonPointStyle atmPoint = new GeoJsonPointStyle();
        atmPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ec_atm));

        GeoJsonPointStyle entrancePoint = new GeoJsonPointStyle();
        entrancePoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance));

        GeoJsonPointStyle fahrkartenentwerterPoint = new GeoJsonPointStyle();
        fahrkartenentwerterPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.fahrkartenentwerter));

        GeoJsonPointStyle infoPoint = new GeoJsonPointStyle();
        infoPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.info));

        GeoJsonPointStyle schliessfachPoint = new GeoJsonPointStyle();
        schliessfachPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.schliessfach));

        GeoJsonPointStyle wartebereichPoint = new GeoJsonPointStyle();
        wartebereichPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.wartebereich));

        GeoJsonPointStyle wcPoint = new GeoJsonPointStyle();
        wcPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.wc));

        GeoJsonPointStyle entranceNumb1 = new GeoJsonPointStyle();
        entranceNumb1.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance1));

        GeoJsonPointStyle entranceNumb2 = new GeoJsonPointStyle();
        entranceNumb2.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance2));

        GeoJsonPointStyle entranceNumb3 = new GeoJsonPointStyle();
        entranceNumb3.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance3));

        GeoJsonPointStyle entranceNumb4 = new GeoJsonPointStyle();
        entranceNumb4.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance4));

        GeoJsonPointStyle entranceNumb5 = new GeoJsonPointStyle();
        entranceNumb5.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance5));

        GeoJsonPointStyle entranceNumb6 = new GeoJsonPointStyle();
        entranceNumb6.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance6));

        GeoJsonPointStyle entranceNumb7 = new GeoJsonPointStyle();
        entranceNumb7.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance7));

        GeoJsonPointStyle entranceNumb8 = new GeoJsonPointStyle();
        entranceNumb8.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance8));

        GeoJsonPointStyle entranceNumb9 = new GeoJsonPointStyle();
        entranceNumb9.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.entrance9));

        for(GeoJsonLayer layer : googleMapsLayer.values()){
            // deactivate default Style

            GeoJsonPointStyle defPoint = layer.getDefaultPointStyle();
            GeoJsonLineStringStyle defLine = layer.getDefaultLineStringStyle();
            GeoJsonPolygonStyle defPoly = layer.getDefaultPolygonStyle();

            defPoint.setVisible(false);
            defLine.setVisible(false);
            defPoly.setVisible(false);


            for(GeoJsonFeature feature : layer.getFeatures()){
                String geometry = feature.getGeometry().getGeometryType();

                switch (geometry) {
                    case "Point":
                        feature.setPointStyle(defPoint);

                        if (feature.hasProperty("room")) {
                            if (feature.getProperty("room").equals("toilets")) {
                                feature.setPointStyle(wcPoint);
                            }
                        }

                        if (feature.hasProperty("highway")) {
                            if (feature.getProperty("highway").equals("elevator")) {
                                feature.setPointStyle(elePoint);
                            }
                        }
                        if (feature.hasProperty("amenity")) {
                            if (feature.getProperty("amenity").equals("atm")) {
                                feature.setPointStyle(atmPoint);
                            } else if (feature.getProperty("amenity").equals("luggage_locker")) {
                                feature.setPointStyle(schliessfachPoint);
                            } else if (feature.getProperty("amenity").equals("bench")) {
                                feature.setPointStyle(wartebereichPoint);
                            }
                        }
                        if (feature.hasProperty("public_transport")) {
                            if (feature.getProperty("public_transport").equals("service_point") ||
                                    feature.getProperty("public_transport").equals("service_center")) {
                                feature.setPointStyle(infoPoint);
                            }
                        }
                        if (feature.hasProperty("entrance")) {
                            feature.setPointStyle(entrancePoint);
                            if(feature.hasProperty("ref")){
                                if(feature.getProperty("ref").equals("1")){feature.setPointStyle(entranceNumb1);}
                                else if (feature.getProperty("ref").equals("2")){feature.setPointStyle(entranceNumb2);}
                                else if (feature.getProperty("ref").equals("3")){feature.setPointStyle(entranceNumb3);}
                                else if (feature.getProperty("ref").equals("4")){feature.setPointStyle(entranceNumb4);}
                                else if (feature.getProperty("ref").equals("5")){feature.setPointStyle(entranceNumb5);}
                                else if (feature.getProperty("ref").equals("6")){feature.setPointStyle(entranceNumb6);}
                                else if (feature.getProperty("ref").equals("7")){feature.setPointStyle(entranceNumb7);}
                                else if (feature.getProperty("ref").equals("8")){feature.setPointStyle(entranceNumb8);}
                                else if (feature.getProperty("ref").equals("9")){feature.setPointStyle(entranceNumb9);}
                            }

                        }
                        if (feature.hasProperty("vending")) {
                            if (feature.getProperty("vending").equals("ticket_validator")) {
                                feature.setPointStyle(fahrkartenentwerterPoint);
                            }
                        }

                        //Log.e("Log ", String.valueOf(feature.getGeometry().getGeometryType()));

                        break;
                    case "LineString":
                        feature.setLineStringStyle(defLine);

                        if (feature.hasProperty("highway")) {
                            if (feature.getProperty("highway").equals("footway")) {
                                feature.setLineStringStyle(footwayStyle);
                            } else if (feature.getProperty("highway").equals("steps")) {
                                feature.setLineStringStyle(stepsStyle);
                            }

                        }
                        if (feature.hasProperty("door")) {
                            feature.setLineStringStyle(doorStyle);
                        }
                        break;
                    case "Polygon":
                        feature.setPolygonStyle(defPoly);

                        if (feature.hasProperty("indoor")) {
                            if (feature.getProperty("indoor").equals("room")) {
                                feature.setPolygonStyle(roomStyle);
                            } else if (feature.getProperty("indoor").equals("yes")) {
                                feature.setPolygonStyle(roomStyle);
                            } else if (feature.getProperty("indoor").equals("corridor")) {
                                //Log.e("Log ", String.valueOf(feature));
                                feature.setPolygonStyle(corridorStyle);
                            }
                        }

                        if (feature.hasProperty("public_transport")) {
                            if (feature.getProperty("public_transport").equals("waiting_room")) {
                                feature.setPolygonStyle(facilitiesStyle);
                            } else if (feature.getProperty("public_transport").equals("service_center")) {
                                feature.setPolygonStyle(serviceStyle);
                            } else if (feature.getProperty("public_transport").equals("service_point")) {
                                feature.setPolygonStyle(serviceStyle);
                            } else if (feature.getProperty("public_transport").equals("platform")) {
                                feature.setPolygonStyle(platfStyle);
                            }

                        }
                        if (feature.hasProperty("amenity")) {
                            if (feature.getProperty("amenity").equals("toilets")) {
                                feature.setPolygonStyle(facilitiesStyle);
                            } else if (feature.getProperty("amenity").equals("police")) {
                                feature.setPolygonStyle(facilitiesStyle);
                            } else if (feature.getProperty("amenity").equals("luggage_locker")) {
                                feature.setPolygonStyle(serviceStyle);
                            }

                        }
                        if (feature.hasProperty("room")) {
                            if (feature.getProperty("room").equals("toilets")) {
                                feature.setPolygonStyle(facilitiesStyle);
                            }

                        }
                        if (feature.hasProperty("shop")) {
                            if (feature.getProperty("shop").equals("ticket")) {
                                feature.setPolygonStyle(serviceStyle);
                            }
                        }
                        if (feature.hasProperty("highway")) {
                            if (feature.getProperty("highway").equals("elevator")) {
                                feature.setPolygonStyle(eleStyle);
                            }
                            if (feature.getProperty("highway").equals("platform")) {
                                feature.setPolygonStyle(platfStyle);
                            }
                        }
                        if (feature.hasProperty("stairwell")) {
                            if (feature.getProperty("stairwell").equals("elevator") ||
                                    feature.getProperty("stairwell").equals("flight_of_stairs") ||
                                    feature.getProperty("stairwell").equals("yes")) {
                                feature.setPolygonStyle(eleStyle);
                            } else if (feature.getProperty("stairwell").equals("stair_landing")) {
                                feature.setPolygonStyle(corridorStyle);
                            }
                        }
                        if (feature.hasProperty("access")) {
                            if (feature.getProperty("access").equals("no") ||
                                    feature.getProperty("access").equals("private")) {
                                feature.setPolygonStyle(noAccessStyle);
                            }
                        }
                        if (feature.hasProperty("building_part")) {
                            if (feature.getProperty("building_part").equals("elevator")) {

                                feature.setPolygonStyle(eleStyle);
                            }
                        }
                        if (feature.hasProperty("railway")) {
                            if (feature.getProperty("railway").equals("platform")) {
                                feature.setPolygonStyle(platfStyle);
                            }
                        }
                        break;
                }
            }
        }
    }

    public static void setStyleSncf(HashMap<Integer, GeoJsonLayer> googleMapsLayer) {



        GeoJsonPolygonStyle corridorStyle = new GeoJsonPolygonStyle();
        corridorStyle.setFillColor(SncfColor.corridor.getColor());
        corridorStyle.setStrokeColor(SncfColor.corridor.getColor());

        GeoJsonPolygonStyle commericalStyle = new GeoJsonPolygonStyle();
        commericalStyle.setFillColor(SncfColor.commercial.getColor());
        commericalStyle.setStrokeColor(SncfColor.commercial.getColor());

        GeoJsonPolygonStyle infrastStyle = new GeoJsonPolygonStyle();
        infrastStyle.setFillColor(SncfColor.infrastructure.getColor());
        infrastStyle.setStrokeColor(SncfColor.infrastructure.getColor());

        GeoJsonPolygonStyle emergenStyle = new GeoJsonPolygonStyle();
        emergenStyle.setFillColor(SncfColor.emergency.getColor());
        emergenStyle.setStrokeColor(SncfColor.emergency.getColor());

        GeoJsonPolygonStyle serviceStyle = new GeoJsonPolygonStyle();
        serviceStyle.setFillColor(SncfColor.service.getColor());
        serviceStyle.setStrokeColor(SncfColor.service.getColor());

        GeoJsonLineStringStyle stairsStyle = new GeoJsonLineStringStyle();
        stairsStyle.setColor(SncfColor.stairs.getColor());
        stairsStyle.setWidth(3f);

        GeoJsonLineStringStyle footwayStyle = new GeoJsonLineStringStyle();
        stairsStyle.setColor(SncfColor.stairs.getColor());
        stairsStyle.setWidth(1.5f);

        GeoJsonPointStyle cashPoint = new GeoJsonPointStyle();
        cashPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cashmaschine));
        cashPoint.setAnchor(0.5f,0.5f);

        GeoJsonPointStyle coffeePoint = new GeoJsonPointStyle();
        coffeePoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.coffee));
        coffeePoint.setAnchor(0.5f,0.5f);

        GeoJsonPointStyle restaurantPoint = new GeoJsonPointStyle();
        restaurantPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant));
        restaurantPoint.setAnchor(0.5f,0.5f);

        GeoJsonPointStyle sandwichPoint = new GeoJsonPointStyle();
        sandwichPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.sandwich));
        sandwichPoint.setAnchor(0.5f,0.5f);

        GeoJsonPointStyle ticketPoint = new GeoJsonPointStyle();
        ticketPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ticket));
        ticketPoint.setAnchor(0.5f,0.5f);

        GeoJsonPointStyle welcomePoint = new GeoJsonPointStyle();
        welcomePoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.welcome));
        welcomePoint.setAnchor(0.5f,0.5f);

        for(GeoJsonLayer layer : googleMapsLayer.values()) {

            // deactivate default Style
            GeoJsonPointStyle defPoint = layer.getDefaultPointStyle();
            GeoJsonLineStringStyle defLine = layer.getDefaultLineStringStyle();
            GeoJsonPolygonStyle defPoly = layer.getDefaultPolygonStyle();

            defPoint.setVisible(false);
            defLine.setVisible(false);
            defPoly.setVisible(false);


            for(GeoJsonFeature feature : layer.getFeatures()) {
                String geometry = feature.getGeometry().getGeometryType();

                switch (geometry) {
                    case "Point":
                        feature.setPointStyle(defPoint);
                        if (feature.hasProperty("amenity")) {
                            if (feature.getProperty("amenity").equals("atm")) {
                                feature.setPointStyle(cashPoint);
                            }
                            else if (feature.getProperty("amenity").equals("restaurant")) {
                                feature.setPointStyle(restaurantPoint);
                            }
                            else if (feature.getProperty("amenity").equals("fast_food")) {
                                feature.setPointStyle(sandwichPoint);
                            }
                            else if (feature.getProperty("amenity").equals("cafe")) {
                                feature.setPointStyle(coffeePoint);
                            }
                            else if (feature.getProperty("amenity").equals("vending_machine") ) {
                                feature.setPointStyle(ticketPoint);
                            }
                        }
                        if (feature.hasProperty("indoor")) {
                            if (feature.getProperty("indoor").equals("restaurant")) {
                                feature.setPointStyle(restaurantPoint);
                            }
                        }
                        if (feature.hasProperty("shop")) {
                            if (feature.getProperty("shop").equals("coffee")) {
                                feature.setPointStyle(coffeePoint);
                            }
                        }
                        if (feature.hasProperty("tourism")) {
                            if (feature.getProperty("tourism").equals("information")) {
                                feature.setPointStyle(welcomePoint);
                            }
                        }


                        break;
                    case "LineString":
                        feature.setLineStringStyle(defLine);
                        if (feature.hasProperty("highway")) {
                            if (feature.getProperty("highway").equals("steps")) {
                                feature.setLineStringStyle(stairsStyle);
                            }
                        }
                        if (feature.hasProperty("highway")) {
                            if (feature.getProperty("highway").equals("footway")) {
                                feature.setLineStringStyle(footwayStyle);
                            }
                        }
                        break;
                    case "Polygon":
                        feature.setPolygonStyle(defPoly);
                        if (feature.hasProperty("indoor")) {
                            if (feature.getProperty("indoor").equals("corridor")||
                                feature.getProperty("indoor").equals("area")) {
                                feature.setPolygonStyle(corridorStyle);
                            }
                            if (feature.getProperty("indoor").equals("room")) {
                                feature.setPolygonStyle(commericalStyle);
                            }
                        }
                        if (feature.hasProperty("room")) {
                            if (feature.getProperty("room").equals("corridor")) {
                                feature.setPolygonStyle(corridorStyle);
                            }
                            else if (feature.getProperty("room").equals("shop") ||
                                     feature.getProperty("room").equals("restaurant")) {
                                feature.setPolygonStyle(commericalStyle);
                            }
                            else if (feature.getProperty("room").equals("toilets")) {
                                feature.setPolygonStyle(infrastStyle);
                            }
                        }
                        if (feature.hasProperty("building_part")) {
                            if (feature.getProperty("building_part").equals("floor")) {
                                feature.setPolygonStyle(corridorStyle);
                            }
                        }
                        if (feature.hasProperty("amenity")) {
                            if (feature.getProperty("amenity").equals("fast_food") ||
                                feature.getProperty("amenity").equals("pharmacy") ||
                                feature.getProperty("amenity").equals("cafe") ||
                                feature.getProperty("amenity").equals("restaurant")) {
                                feature.setPolygonStyle(commericalStyle);
                            }
                            else if(feature.getProperty("amenity").equals("toilets")){
                                feature.setPolygonStyle(infrastStyle);
                            }
                            else if(feature.getProperty("amenity").equals("police")){
                                feature.setPolygonStyle(emergenStyle);
                            }
                        }
                        if (feature.hasProperty("shop")) {
                            feature.setPolygonStyle(commericalStyle);
                        }
                        if (feature.hasProperty("stairwell")) {
                            if (feature.getProperty("stairwell").equals("flight_of_stairs") ||
                                    feature.getProperty("stairwell").equals("yes")||
                                    feature.getProperty("stairwell").equals("stairlanding")) {
                                feature.setPolygonStyle(infrastStyle);
                            }
                        }
                        if (feature.hasProperty("emergency")) {
                            feature.setPolygonStyle(emergenStyle);
                        }
                        if (feature.hasProperty("public_transport")) {
                            if (feature.getProperty("public_transport").equals("shop") ||
                                feature.getProperty("public_transport").equals("waiting_room") ||
                                feature.getProperty("public_transport").equals("service_center") ||
                                feature.getProperty("public_transport").equals("service_point")    ) {
                                feature.setPolygonStyle(serviceStyle);
                            }
                        }


                        break;
                }
            }
        }


    }
}