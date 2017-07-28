package com.asiainfo.ocmanager.dacp;

import com.asiainfo.ocmanager.dacp.model.DBDistribution;
import com.asiainfo.ocmanager.dacp.model.DBRegister;

import com.asiainfo.ocmanager.dacp.model.Team;
import com.asiainfo.ocmanager.dacp.service.TeamWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;

/**
 * Created by YANLSH
 * Created on 2017/7/3
 */
public class dacpForResourceUtil {

    public static Log logger = LogFactory.getLog(dacpForResourceUtil.class);

    private static Map<String, List> mapInfo;
    private static List dbRegisterList;
    private static List dbDistributionList;
    private static String databasename = "";
    private static String username = "";
    private static String password = "";
    private static String uri = "";
    private static String url = "";
    private static String team_code = "";
    private static String thriftUri = "";
    private static String thriftUrl = "";
    public static Map<String, List> getResult(String tenantId) {
        Team team = TeamWrapper.getTeamFromTenant(tenantId);
        team_code = team.getteam_code();
        try {
            String resourceJson = DacpQuery.GetData(tenantId);
            logger.info("call DF tenant instance resource: \r\n"+resourceJson);
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(resourceJson);
            JsonArray array = object.get("items").getAsJsonArray();
            mapInfo = new HashMap<>();
            dbRegisterList = new ArrayList<>();
            dbDistributionList = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JsonObject subObject = array.get(i).getAsJsonObject();
                JsonObject specJsonObj = subObject.get("spec").getAsJsonObject();
                JsonObject statusJsonObj = subObject.get("status").getAsJsonObject();
                String phase = statusJsonObj.get("phase").getAsString();
                String instance_id = specJsonObj.get("instance_id").getAsString();
                if (!"Failure".equals(phase)) {
                    JsonObject provisioningJsonObj = specJsonObj.get("provisioning").getAsJsonObject();
                    String backingservice_name = provisioningJsonObj.get("backingservice_name").getAsString();//dbname
                    String driveTypeStr = provisioningJsonObj.get("backingservice_name").getAsString().toLowerCase();
                    String driverclassname = DriverTypeEnum.getDriverTypeEnum(driveTypeStr);

                    boolean hadoopflag = isHadoopflag(backingservice_name.toLowerCase());
                    if(hadoopflag){
                        if(!"Unbound".equals(phase)){
                            if(!backingservice_name.toLowerCase().equals("hive")) continue;
                            if(specJsonObj.get("binding").isJsonArray()){
                                /*JsonArray bindingJsonArray = specJsonObj.get("binding").getAsJsonArray();
                                JsonObject bindObj = bindingJsonArray.get(0).getAsJsonObject();*///no sort
                                JsonArray bindingJsonArray = specJsonObj.get("binding").getAsJsonArray();

                                String sortByUserBindingStr = sortByUsername(bindingJsonArray.toString());

                                JsonObject bindObj = (JsonObject) parser.parse(sortByUserBindingStr);//have sort

                                if(bindObj != null){
                                    JsonObject credentialJsonObj = bindObj.get("credentials").getAsJsonObject();
                                    assignForDBInfo(credentialJsonObj,backingservice_name);
                                }

                            }
                            DBEntityAssign(instance_id,backingservice_name,driverclassname);
                        }
                    }else{
                        boolean flag = provisioningJsonObj.get("credentials").isJsonObject();
                        if (flag) {
                            if(backingservice_name.toLowerCase().equals("neo4j")||
                                    backingservice_name.toLowerCase().equals("mongodb")||
                                    backingservice_name.toLowerCase().equals("rabbitmq")) continue;
                            JsonObject credentialsJsonObj = provisioningJsonObj.get("credentials").getAsJsonObject();
                            assignForDBInfo(credentialsJsonObj,backingservice_name);
                        }
                        DBEntityAssign(instance_id,backingservice_name,driverclassname);
                    }
                }
            }
            mapInfo.put("database", dbRegisterList);
            mapInfo.put("transdatabase", dbDistributionList);

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("DacpforResourceUtil Exception " + e.getMessage());
        }
        return mapInfo;
    }


    /*初始化数据注册与分配实例，并分别加入队列*/
    private static void DBEntityAssign(String instance_id, String backingservice_name, String driverclassname) {
        /*数据库分配*/
        String state = "on";
        String remark = "";//remark

        DBRegister dbRegister = new DBRegister();
        dbRegister.setXmlid("hive_"+instance_id);
        dbRegister.setDbname("hive_"+databasename);
        dbRegister.setCnname("hive_"+databasename);
        dbRegister.setDriverclassname(driverclassname);
        dbRegister.setUrl(url);
        dbRegister.setUsername(username);
        dbRegister.setPassword(password);
        dbRegister.setRemark(remark);
        dbRegister.setAlias(backingservice_name.toLowerCase());

        DBDistribution dbDistribution = new DBDistribution();
        dbDistribution.setDbname("hive_"+databasename);
        dbDistribution.setCnname("hive_"+databasename);
        dbDistribution.setDriverclassname(driverclassname);
        dbDistribution.setUrl(url);
        dbDistribution.setUsername(username);
        dbDistribution.setPassword(password);
        dbDistribution.setState(state);
        dbDistribution.setTeam_code(team_code);
        dbDistribution.setDbtype(backingservice_name.toLowerCase());

        dbRegisterList.add(dbRegister);
        dbDistributionList.add(dbDistribution);
        if(backingservice_name.toLowerCase().equals("hive")){
            DBRegister dbRegister_sparksql = new DBRegister();
            dbRegister_sparksql.setXmlid("spark_"+instance_id);
            dbRegister_sparksql.setDbname("spark_"+databasename);
            dbRegister_sparksql.setCnname("spark_"+databasename);
            dbRegister_sparksql.setDriverclassname(driverclassname);
            dbRegister_sparksql.setUrl(thriftUrl);
            dbRegister_sparksql.setUsername(username);
            dbRegister_sparksql.setPassword(password);
            dbRegister_sparksql.setRemark(remark);
            dbRegister_sparksql.setAlias(backingservice_name.toLowerCase());

            DBDistribution dbDistribution_sparksql = new DBDistribution();
            dbDistribution_sparksql.setDbname("spark_"+databasename);
            dbDistribution_sparksql.setCnname("spark_"+databasename);
            dbDistribution_sparksql.setDriverclassname(driverclassname);
            dbDistribution_sparksql.setUrl(thriftUrl);
            dbDistribution_sparksql.setUsername(username);
            dbDistribution_sparksql.setPassword(password);
            dbDistribution_sparksql.setState(state);
            dbDistribution_sparksql.setTeam_code(team_code);
            dbDistribution_sparksql.setDbtype(backingservice_name.toLowerCase());


            dbRegisterList.add(dbRegister_sparksql);
            dbDistributionList.add(dbDistribution_sparksql);
        }
    }

    /*数据注册与分配实例，并分别加入队列*/
    private static void assignForDBInfo(JsonObject credentialsJsonObj,String backingservice_name) {
        if (credentialsJsonObj.get("username") != null) {
            username = credentialsJsonObj.get("username").getAsString();//username
        }
        if (credentialsJsonObj.get("password") != null) {
            password = credentialsJsonObj.get("password").getAsString();//password
        }
        if (credentialsJsonObj.has("uri")) {
            uri = credentialsJsonObj.get("uri").getAsString();//uri
        }
        String host = credentialsJsonObj.get("host").getAsString();//host
        String port = credentialsJsonObj.get("port").getAsString();//port
        if(DbTypeEnum.getDbFlagEnum(backingservice_name.toLowerCase()).equals("true")){
            if (credentialsJsonObj.get("Hive database") != null) {
                String hiveDatabase = credentialsJsonObj.get("Hive database").getAsString();
                databasename=hiveDatabase.substring(0,hiveDatabase.indexOf(":"));
            }
        }else{
            if (credentialsJsonObj.get("name") != null) {
                databasename = credentialsJsonObj.get("name").getAsString();
            }
        }
        if(credentialsJsonObj.has("thriftUri")){
            thriftUri = credentialsJsonObj.get("thriftUri").getAsString();
        }
        thriftUrl = DBUrlEnum.getDBUrlEnum(backingservice_name.toLowerCase(), thriftUri, host, port, databasename,username);//thriftUrl
        url = DBUrlEnum.getDBUrlEnum(backingservice_name.toLowerCase(), uri, host, port, databasename,username);//url
    }

    private static boolean isHadoopflag(String backingservice_name) {
        String dbflag = DbTypeEnum.getDbFlagEnum(backingservice_name);
        if(dbflag.equals("true")){
            return true;
        }else{
            return false;
        }
    }

    private static String sortByUsername (String bindingJsonArray){
        JSONArray jsonArr = null;
        JSONArray sortedJsonArray = new JSONArray();
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        String bindObjStr="";
        try {
            jsonArr = new JSONArray(bindingJsonArray);
            for (int i = 0; i < jsonArr.length(); i++) {
                jsonValues.add(jsonArr.getJSONObject(i));
            }
            Collections.sort(jsonValues, new Comparator<JSONObject>() {
                private static final String KEY_NAME = "bind_hadoop_user";

                public int compare(JSONObject a, JSONObject b) {
                    String valA = new String();
                    String valB = new String();
                    try {
                        valA = (String) a.get(KEY_NAME);
                        valB = (String) b.get(KEY_NAME);
                    } catch (JSONException e) {
                        logger.info("DacpforResourceUtil Collections sort compare" + e.getMessage());
                    }
                    return valA.compareTo(valB);
                }
            });
            for (int i = 0; i < jsonArr.length(); i++) {
                sortedJsonArray.put(jsonValues.get(i));
            }
            bindObjStr = sortedJsonArray.get(0).toString();
        } catch (JSONException e) {
            logger.info("DacpforResourceUtil sortByUsername" + e.getMessage());
        }
        return bindObjStr;
    }
}