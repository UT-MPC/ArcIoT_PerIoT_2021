package edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public final class HueBridgeHttpService extends HttpService {
    private static String TAG = "PhilipsHueBridgeHttpService";

    private PhilipsHueBridgeRestApi api;

    public HueBridgeHttpService(String baseUrl) {
        super();
        api = getInstance(baseUrl).create(PhilipsHueBridgeRestApi.class);
    }

    public String createUser(String username) {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setDevicetype(String.format("%s", username));

        String returnVal = null;
        Response<List<CreateUserResponse>> response;
        try {
            response = api.createUser(createUserRequest).execute();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        List<CreateUserResponse> responseBody = response.body();

        if (responseBody != null) {
            for (CreateUserResponse createUserResponse : responseBody) {
                try {
                    CreateUserResponse.Success success = createUserResponse.getSuccess();
                    returnVal = success.getUsername();
                    Log.i(TAG, String.format("Create User succeed. Token: %s", success.getUsername()));
                } catch (NullPointerException e) {
                    CreateUserResponse.Error error = createUserResponse.getError();
                    Log.w(TAG, String.format("Create User failed. Reason: %s", error.getDescription()));
                }
            }
            return returnVal;
        }
        else {
            return null;
        }
    }

    public String getConfig(String user) {
        Response<MainResponse.Config> response;
        try {
            response = api.getConfig(user).execute();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        MainResponse.Config responseBody = response.body();

        if (responseBody != null) {
            Map<String, MainResponse.Config.User> whitelist = responseBody.getWhitelist();

            if (whitelist != null) {
                String name = whitelist.get(user).getName();
                if (name != null){
                    return name;
                }
            }
            else {
                return null;
            }
        }
        return null;
    }



    public List<HueLight> getLights(String user) {
        Response<Map<String, MainResponse.Light>> response = null;
        try {
            response = api.getLights(user).execute();
        }
        catch (IOException | RuntimeException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        Map<String, MainResponse.Light> responseBody = null;
        try {
            if (response != null) {
                responseBody = response.body();
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            return null;
        }

        if (responseBody != null) {
            List<HueLight> lights = new ArrayList<>();
            for (Map.Entry<String, MainResponse.Light> entry : responseBody.entrySet()) {
                HueLight light = new HueLight();

                MainResponse.Light entryValue = entry.getValue();

                light.setName(entryValue.name);

                light.setUuid(entryValue.uniqueid);

                light.setAccessName(entry.getKey());

                HueLightState newState = new HueLightState();
                newState.setHue(entryValue.state.hue);
                newState.setBrightness(entryValue.state.bri);
                newState.setOn(entryValue.state.on);

                light.setState(newState);

                lights.add(light);
            }

            return lights;
        }
        else {
            return null;
        }
    }

    public void putThingState(String user, HueLight light, HueLightState lightState) {
        try {
//            Log.d("???", "Before sending light command " + System.currentTimeMillis());
            api.putLightState(user, light.getAccessName(), toPutLightStateBody(lightState)).execute();
            //TODO: add latency measurement.
//            Log.d("???", "After sending light command " + System.currentTimeMillis());

        }
        catch (IOException e) {
            Log.e(TAG, String.format("%s", "putThingState is unsuccessful"));
        }
    }

    private HashMap<String, Object> toPutLightStateBody(HueLightState lightState) {
        HashMap<String, Object> body = new HashMap<>();

            body.put("on", lightState.isOn());
            if (lightState.getBrightness() >= 0) {
                int bri = lightState.getBrightness();
                body.put("bri", bri);
            }
            if (lightState.getHue() >= 0) {
                body.put("hue", lightState.getHue());
            }
            if (lightState.getSaturation() >= 0) {
                int sat = lightState.getSaturation();
                body.put("sat", sat);
            }

        return body;
    }

    protected interface PhilipsHueBridgeRestApi {
        @POST("api")
        @Headers({
                "Content-Type: application/json"
        })
        Call<List<CreateUserResponse>> createUser(@Body CreateUserRequest createUserRequest);

        @GET("api/{user}/config")
        Call<MainResponse.Config> getConfig(@Path("user") String userId);

        @GET("api/{user}/lights")
        Call<Map<String, MainResponse.Light>> getLights(@Path("user") String userId);

        // Lights Capabilities
        @GET("api/{user}/lights/{lightId}/state")
        Call<List<Object>> getLightState(@Path("user") String userId, @Path("lightId") int lightId);

        @PUT("api/{user}/lights/{lightId}")
        @Headers({
                "Content-Type: application/json"
        })
        Call<List<Object>> putLight(@Path("user") String userId, @Path("lightId") String lightId, @Body HashMap<String, Object> light);

        @PUT("api/{user}/lights/{lightId}/state")
        @Headers({
                "Content-Type: application/json"
        })
        Call<List<Object>> putLightState(@Path("user") String userId, @Path("lightId") String lightId, @Body HashMap<String, Object> putLightStateBody);
    }

    private class CreateUserRequest {
        @SerializedName("devicetype") @Expose private String devicetype;

        public String getDevicetype(){
            return devicetype;
        }
        public void setDevicetype(String devicetype){
            this.devicetype = devicetype;
        }
    }

    private class CreateUserResponse {
        @SerializedName("success") @Expose private Success success;

        private Success getSuccess() {
            return success;
        }

        public class Success {
            @SerializedName("username") @Expose private String username;

            public String getUsername() {
                return username;
            }
        }

        @SerializedName("error") @Expose private Error error;

        public Error getError() {
            return error;
        }

        public class Error {
            @SerializedName("type") @Expose private String type;
            @SerializedName("address") @Expose private String address;
            @SerializedName("description") @Expose private String description;

            public String getType() {
                return type;
            }
            public String getAddress() {
                return address;
            }
            public String getDescription() {
                return description;
            }
        }
    }

    private class MainResponse {
        @SerializedName("lights") @Expose private Map<String, Light> lights;

        private class Light {
            @SerializedName("state") @Expose private State state;

            private class State {
                @SerializedName("on") @Expose private boolean on;
                @SerializedName("bri") @Expose private int bri;
                @SerializedName("hue") @Expose private int hue;
                @SerializedName("sat") @Expose private int sat;
                @SerializedName("effect") @Expose private String effect;
                @SerializedName("xy") @Expose private List<Double> xy;
                @SerializedName("ct") @Expose private int ct;
                @SerializedName("alert") @Expose private String alert;
                @SerializedName("colormode") @Expose private String colormode;
                @SerializedName("mode") @Expose private String mode;
                @SerializedName("reachable") @Expose private boolean reachable;
            }

            @SerializedName("swupdate") @Expose private Swupdate swupdate;

            private class Swupdate {
                @SerializedName("state") @Expose private String state;
                @SerializedName("lastinstall") @Expose private String lastinstall;

                public String getState() {
                    return state;
                }
                public String getLastinstall() {
                    return lastinstall;
                }
            }

            @SerializedName("type") @Expose private String type;
            @SerializedName("name") @Expose private String name;
            @SerializedName("modelid") @Expose private String modelid;
            @SerializedName("manufacturername") @Expose private String manufacturername;
            @SerializedName("productname") @Expose private String productname;
            @SerializedName("capabilities") @Expose private Capabilities capabilities;

            private class Capabilities {
                @SerializedName("certified") @Expose private boolean certified;
                @SerializedName("control") @Expose private Control control;

                private class Control {
                    @SerializedName("mindimlevel") @Expose private int mindimlevel;
                    @SerializedName("maxlumen") @Expose private int maxlumen;
                    @SerializedName("colorgamuttype") @Expose private String colorgamuttype;
                    @SerializedName("colorgamut") @Expose private List<List<Double>> colorgamut;
                    @SerializedName("ct") @Expose private Ct ct;

                    private class Ct {
                        @SerializedName("min") @Expose private int min;
                        @SerializedName("max") @Expose private int max;

                        public int getMin() {
                            return min;
                        }
                        public int getMax() {
                            return max;
                        }
                    }

                    public int getMindimlevel() {
                        return mindimlevel;
                    }
                    public int getMaxlumen() {
                        return maxlumen;
                    }
                    public String getColorgamuttype() {
                        return colorgamuttype;
                    }
                    public List<List<Double>> getColorgamut() {
                        return colorgamut;
                    }
                    public Ct getCt() {
                        return ct;
                    }
                }

                @SerializedName("streaming") @Expose private Streaming streaming;

                private class Streaming {
                    @SerializedName("renderer") @Expose private boolean renderer;
                    @SerializedName("proxy") @Expose private boolean proxy;

                    public boolean isRenderer() {
                        return renderer;
                    }
                    public boolean isProxy() {
                        return proxy;
                    }
                }

                public boolean isCertified() {
                    return certified;
                }
                public Control getControl() {
                    return control;
                }
                public Streaming getStreaming() {
                    return streaming;
                }
            }

            @SerializedName("config") @Expose private Config config;

            private class Config {
                @SerializedName("archetype") @Expose private String archetype;
                @SerializedName("function") @Expose private String function;



                @SerializedName("direction") @Expose private String direction;

                public String getArchetype() {
                    return archetype;
                }
                public String getFunction() {
                    return function;
                }
                public String getDirection() {
                    return direction;
                }
            }

            @SerializedName("uniqueid") @Expose private String uniqueid;
            @SerializedName("swversion") @Expose private String swversion;
        }

        @SerializedName("groups") @Expose private Map<String, Object> groups;
        @SerializedName("config") @Expose private Config config;

        private class Config {
            @SerializedName("name") @Expose private String name;
            @SerializedName("zigbeechannel") @Expose private int zigbeechannel;
            @SerializedName("bridgeid") @Expose private String bridgeid;
            @SerializedName("mac") @Expose private String mac;
            @SerializedName("dhcp") @Expose private boolean dhcp;
            @SerializedName("ipaddress") @Expose private String ipaddress;
            @SerializedName("netmask") @Expose private String netmask;
            @SerializedName("gateway") @Expose private String gateway;
            @SerializedName("proxyaddress") @Expose private String proxyaddress;
            @SerializedName("proxyport") @Expose private int proxyport;
            @SerializedName("UTC") @Expose private String UTC;
            @SerializedName("localtime") @Expose private String localtime;
            @SerializedName("timezone") @Expose private String timezone;
            @SerializedName("modelid") @Expose private String modelid;
            @SerializedName("datastoreversion") @Expose private String datastoreversion;
            @SerializedName("swversion") @Expose private String swversion;
            @SerializedName("apiversion") @Expose private String apiversion;
            @SerializedName("swupdate") @Expose private Swupdate swupdate;

            public String getName() {
                return name;
            }
            public int getZigbeechannel() {
                return zigbeechannel;
            }
            public String getBridgeid() {
                return bridgeid;
            }
            public String getMac() {
                return mac;
            }
            public boolean isDhcp() {
                return dhcp;
            }
            public String getIpaddress() {
                return ipaddress;
            }
            public String getNetmask() {
                return netmask;
            }
            public String getGateway() {
                return gateway;
            }
            public String getProxyaddress() {
                return proxyaddress;
            }
            public int getProxyport() {
                return proxyport;
            }
            public String getUTC() {
                return UTC;
            }
            public String getLocaltime() {
                return localtime;
            }
            public String getTimezone() {
                return timezone;
            }
            public String getModelid() {
                return modelid;
            }
            public String getDatastoreversion() {
                return datastoreversion;
            }
            public String getSwversion() {
                return swversion;
            }
            public String getApiversion() {
                return apiversion;
            }
            public Swupdate getSwupdate() {
                return swupdate;
            }
            public Swupdate2 getSwupdate2() {
                return swupdate2;
            }
            public boolean isLinkbutton() {
                return linkbutton;
            }
            public boolean isPortalservices() {
                return portalservices;
            }
            public String getPortalconnection() {
                return portalconnection;
            }
            public PortalState getPortalstate() {
                return portalstate;
            }
            public InternetServices getInternetservices() {
                return internetservices;
            }
            public boolean isFactorynew() {
                return factorynew;
            }
            public String getReplacesbridgeid() {
                return replacesbridgeid;
            }
            public Backup getBackup() {
                return backup;
            }
            public String getStarterkitid() {
                return starterkitid;
            }

            private class Swupdate {
                @SerializedName("updatestate") @Expose private int updatestate;
                @SerializedName("checkforupdate") @Expose private boolean checkforupdate;
                @SerializedName("devicetypes") @Expose private DeviceTypes devicetypes;

                private class DeviceTypes {
                    @SerializedName("bridge") @Expose private boolean bridge;
                    @SerializedName("lights") @Expose private List<String> lights;
                    @SerializedName("sensors") @Expose private List<String> sensors;

                    public boolean isBridge() {
                        return bridge;
                    }
                    public List<String> getLights() {
                        return lights;
                    }
                    public List<String> getSensors() {
                        return sensors;
                    }
                }

                @SerializedName("url") @Expose private String url;
                @SerializedName("text") @Expose private String text;
                @SerializedName("notify") @Expose private boolean notify;

                public int getUpdatestate() {
                    return updatestate;
                }
                public boolean isCheckforupdate() {
                    return checkforupdate;
                }
                public DeviceTypes getDevicetypes() {
                    return devicetypes;
                }
                public String getUrl() {
                    return url;
                }
                public String getText() {
                    return text;
                }
                public boolean isNotify() {
                    return notify;
                }
            }

            @SerializedName("swupdate2") @Expose private Swupdate2 swupdate2;

            private class Swupdate2 {
                @SerializedName("checkforupdate") @Expose private boolean checkforupdate;
                @SerializedName("lastchange") @Expose private String lastchange;
                @SerializedName("bridge") @Expose private Bridge bridge;

                public boolean isCheckforupdate() {
                    return checkforupdate;
                }
                public String getLastchange() {
                    return lastchange;
                }
                public Bridge getBridge() {
                    return bridge;
                }
                public String getState() {
                    return state;
                }
                public Autoinstall getAutoinstall() {
                    return autoinstall;
                }

                private class Bridge {
                    @SerializedName("updatetime") @Expose private String updatetime;
                    @SerializedName("on") @Expose private boolean on;

                    public String getUpdatetime() {
                        return updatetime;
                    }
                    public boolean isOn() {
                        return on;
                    }
                }

                @SerializedName("state") @Expose private String state;
                @SerializedName("autoinstall") @Expose private Autoinstall autoinstall;

                private class Autoinstall {
                    @SerializedName("updatetime") @Expose private String updatetime;
                    @SerializedName("on") @Expose private boolean on;

                    public String getUpdatetime() {
                        return updatetime;
                    }
                    public boolean isOn() {
                        return on;
                    }
                }
            }

            @SerializedName("linkbutton") @Expose private boolean linkbutton;
            @SerializedName("portalservices") @Expose private boolean portalservices;
            @SerializedName("portalconnection") @Expose private String portalconnection;
            @SerializedName("portalstate") @Expose private PortalState portalstate;

            private class PortalState {
                @SerializedName("signedon") @Expose private boolean signedon;
                @SerializedName("incoming") @Expose private boolean incoming;
                @SerializedName("outgoing") @Expose private boolean outgoing;
                @SerializedName("communication") @Expose private String communication;

                public boolean isSignedon() {
                    return signedon;
                }
                public boolean isIncoming() {
                    return incoming;
                }
                public boolean isOutgoing() {
                    return outgoing;
                }
                public String getCommunication() {
                    return communication;
                }
            }

            @SerializedName("internetservices") @Expose private InternetServices internetservices;

            private class InternetServices {
                @SerializedName("internet") @Expose private String internet;
                @SerializedName("remoteaccess") @Expose private String remoteaccess;
                @SerializedName("time") @Expose private String time;
                @SerializedName("swupdate") @Expose private String swupdate;

                public String getInternet() {
                    return internet;
                }
                public String getRemoteaccess() {
                    return remoteaccess;
                }
                public String getTime() {
                    return time;
                }
                public String getSwupdate() {
                    return swupdate;
                }
            }

            @SerializedName("factorynew") @Expose private boolean factorynew;
            @SerializedName("replacesbridgeid") @Expose private String replacesbridgeid;
            @SerializedName("backup") @Expose private Backup backup;

            private class Backup {
                @SerializedName("status") @Expose private String status;
                @SerializedName("errorcode") @Expose private int errorcode;

                public String getStatus() {
                    return status;
                }
                public int getErrorcode() {
                    return errorcode;
                }
            }

            @SerializedName("starterkitid") @Expose private String starterkitid;
            @SerializedName("whitelist") @Expose private Map<String, User> whitelist;

            public Map<String, User> getWhitelist() {
                return whitelist;
            }

            private class User {
                @SerializedName("last use date") @Expose private String lastUseDate;
                @SerializedName("create date") @Expose private String createDate;
                @SerializedName("name") @Expose private String name;

                public String getLastUseDate() {
                    return lastUseDate;
                }

                public String getCreateDate() {
                    return createDate;
                }

                public String getName() {
                    return name;
                }
            }
        }

        @SerializedName("schedules") @Expose private Object schedules;
        @SerializedName("scenes") @Expose private Object scenes;
        @SerializedName("rules") @Expose private Object rules;
        @SerializedName("sensors") @Expose private Object sensors;
        @SerializedName("resourcelinks") @Expose private Object resourcelinks;

        public Map<String, Light> getLights() {
            return lights;
        }
        public Map<String, Object> getGroups() {
            return groups;
        }
        public Config getConfig() {
            return config;
        }
        public Object getSchedules() {
            return schedules;
        }
        public Object getScenes() {
            return scenes;
        }
        public Object getRules() {
            return rules;
        }
        public Object getSensors() {
            return sensors;
        }
        public Object getResourcelinks() {
            return resourcelinks;
        }
    }

    private class ErrorResponse {
        @SerializedName("error") @Expose private Error error;

        public Error getError() {
            return error;
        }

        private class Error {
            @SerializedName("type") @Expose private String type;
            @SerializedName("address") @Expose private String address;
            @SerializedName("description") @Expose private String description;

            public String getType() {
                return type;
            }

            public String getAddress() {
                return address;
            }

            public String getDescription() {
                return description;
            }
        }
    }
}
