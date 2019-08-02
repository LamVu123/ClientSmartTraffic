package com.example.smartTraffic.modules.ShockPointModule;

import com.example.smartTraffic.common.MySocketFactory;
import com.example.smartTraffic.entity.RoadEntity;
import com.example.smartTraffic.entity.ShockPointEntity;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ShockPointGetter {

    private ArrayList<ShockPointEntity> shockPoints;
    private String road;
    private Socket mSocket = MySocketFactory.getInstance().getMySocket();
    ShockPointGetterListener listener;

    private final String ON_GET_POINTS_EVENT = "points";


    public ShockPointGetter(String road, ShockPointGetterListener listener) {
        shockPoints = new ArrayList<>();
        this.road = road;
        this.listener = listener;
    }

    public void execute() {
        getShockPointsStart();
    }

    public void terminate(){
        if(mSocket.hasListeners(ON_GET_POINTS_EVENT)){
            mSocket.off(ON_GET_POINTS_EVENT);
        }
        if(mSocket.connected()){
            mSocket.disconnect();
        }
    }

    public void getShockPointsStart() {
//        if(!mSocket.connected()){
//            mSocket.connect();
//        }
//        mSocket.emit("road", "test");
//        mSocket.on("ON_GET_POINTS_EVENT", onMessage_Results);
        shockPoints = createSomeDummyPoints();
    }

    private Emitter.Listener onMessage_Results = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];

                    try {
                        JSONObject road = data.getJSONObject("road");
                        int id = road.getInt("id");
                        String name = road.getString("name");
                        RoadEntity roadEntity = new RoadEntity(id, name);
                        JSONArray points = (JSONArray) data.getJSONArray("points");
                        for (int i = 0; i < points.length(); i++) {
                            JSONObject point = (JSONObject) points.get(i);
                            int pointId = point.getInt("id");
                            double pointLat = point.getDouble("latitude");
                            double pointLng = point.getDouble("longitude");
                            ShockPointEntity shockPoint = new ShockPointEntity(pointId, pointLat, pointLng, roadEntity);
                            shockPoints.add(shockPoint);
                        }
                        listener.onShockPointGetterSuccess(shockPoints);
                    } catch (JSONException e) {
                        return;
                    }
                }
            }).start();
        }
    };

    private ArrayList<ShockPointEntity> createSomeDummyPoints() {
        ShockPointEntity shockingPoint1 = new ShockPointEntity(1, 21.016189, 105.554189);
        ShockPointEntity shockingPoint2 = new ShockPointEntity(2, 21.0129657, 105.5278376);
        ShockPointEntity shockingPoint3 = new ShockPointEntity(3, 21.010091, 105.523932);
        ShockPointEntity shockingPoint4 = new ShockPointEntity(4, 21.022830, 105.544446);

        ArrayList<ShockPointEntity> shockingPoints = new ArrayList<>();
        shockingPoints.add(shockingPoint3);
        shockingPoints.add(shockingPoint2);
        shockingPoints.add(shockingPoint4);
        shockingPoints.add(shockingPoint1);
        return shockingPoints;
    }

    public ArrayList<ShockPointEntity> getShockPoints() {
        return shockPoints;
    }

    public void setShockPoints(ArrayList<ShockPointEntity> shockPoints) {
        this.shockPoints = shockPoints;
    }
}
