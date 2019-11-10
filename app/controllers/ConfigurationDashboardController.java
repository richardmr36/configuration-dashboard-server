package controllers;

import java.util.regex.Pattern;

import javax.inject.Inject;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.bson.json.JsonParseException;
import play.mvc.Controller;
import play.mvc.Result;
import com.typesafe.config.Config;

public class ConfigurationDashboardController {

    private static final String CONFIG_ID = "configId";
    private static final String CONFIG_NAME = "configName";

    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final String EMPTY_STRING = "";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String COMMA = ",";
    private static final int INTERNAL_SERVER_ERROR_CODE = 500;
    private static final String PROPERTY = "property";
    private static final String READ_ONLY = "readOnly";

    CacheManager manager;

    private String cacheName;

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    private String dbName;
    private String dbCollection;
    private String dbHost;
    private int dbPort;

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbCollection() {
        return dbCollection;
    }

    public void setDbCollection(String dbCollection) {
        this.dbCollection = dbCollection;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    @Inject
    public ConfigurationDashboardController() {
        String defaultConfigId = "c1";
        ConfigParseOptions options = ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF);
        Config appConfig = ConfigFactory.defaultApplication(options);
        setCacheName(appConfig.getString("cacheName"));

        ConfigObject dataSource =  appConfig.getObject("datasource");
        setDbName(getValueWithoutDoubleQuotes(dataSource.get("dbname").render()));
        setDbCollection(getValueWithoutDoubleQuotes(dataSource.get("dbcollection").render()));
        setDbHost(getValueWithoutDoubleQuotes(dataSource.get("host").render()));
        setDbPort(Integer.parseInt(getValueWithoutDoubleQuotes(dataSource.get("port").render())));

        String configFactoryJson = new Gson().toJson(appConfig);
        String configIdJson = "\"configId\": \""+defaultConfigId+"\",\"configName\": \"Default Configuration\",";

        StringBuilder json = new StringBuilder();
        json.append(configFactoryJson);
        json.insert(json.indexOf("\"object\""), configIdJson);

        DBCollection collection = getDBCollection();

        BasicDBObject searchQuery = new BasicDBObject().append(CONFIG_ID, defaultConfigId);

        DBObject configuration = collection.find(searchQuery).one();

        try {
            DBObject parse = (DBObject) JSON.parse(json.toString());
            if(configuration != null)
                collection.update(searchQuery, parse);
            else
                collection.insert(parse);
        } catch (JsonParseException e) {
            System.out.println(e.getMessage());
        }

        Cache cache = getCache();
        Element newJsonElement = new Element(defaultConfigId, json.toString());
        cache.put(newJsonElement);
    }

    public Cache initCache() {
        manager = CacheManager.create();
        manager.addCache(getCacheName());
        Cache cache = manager.getCache(getCacheName());
        CacheConfiguration config = cache.getCacheConfiguration();
        config.setEternal(true);

        return cache;
    }

    public Cache getCache() {
        Cache cache = CacheManager.getInstance().getCache(getCacheName());
        if (cache != null)
            return cache;

        return initCache();
    }

    public DBCollection getDBCollection() {
        Mongo mongo = new Mongo(getDbHost(), getDbPort());
        DB db = mongo.getDB(getDbName());
        DBCollection collection = db.getCollection(getDbCollection());
        return collection;
    }

    public Result save() {
        DBCollection collection = getDBCollection();
        JsonNode json = Controller.request().body().asJson();

        Result result = validateJson(json);
        if (result.status() == INTERNAL_SERVER_ERROR_CODE)
            return result;

        try {
            DBObject parse = (DBObject) JSON.parse(json.toString());
            collection.insert(parse);
        } catch (JsonParseException e) {
            System.out.println(e.getMessage());
        }

        Cache cache = getCache();
        String key = getValueWithoutDoubleQuotes(json.get(CONFIG_ID).toString());
        Element newJsonElement = new Element(key, json.toString());
        cache.put(newJsonElement);

        return ok(json).as(CONTENT_TYPE_JSON);
    }

    private Result validateJson(JsonNode json) {
        JsonNode configIdNode = json.get(CONFIG_ID);
        JsonNode configNameNode = json.get(CONFIG_NAME);

        if (configIdNode == null)
            return internalServerError("configId is not present");

        if (configNameNode == null)
            return internalServerError("configName is not present");

        if (EMPTY_STRING.equals(getValueWithoutDoubleQuotes(configIdNode.toString())))
            return internalServerError("configId cannot be empty");

        if (JsonNodeType.NUMBER.compareTo(configIdNode.getNodeType()) == 0)
            ((ObjectNode) json).put(CONFIG_ID, configIdNode.toString());
        else if (JsonNodeType.STRING.compareTo(configIdNode.getNodeType()) != 0)
            return internalServerError("Invalid type of configId");

        if (JsonNodeType.STRING.compareTo(configNameNode.getNodeType()) != 0)
            return internalServerError("Invalid type of configName");

        return ok();
    }

    private String getValueWithoutDoubleQuotes(String value) {
        return value.replaceAll(DOUBLE_QUOTE, EMPTY_STRING);
    }

    public Result update() {
        DBCollection collection = getDBCollection();
        JsonNode json = Controller.request().body().asJson();

        Result result = validateJson(json);
        if (result.status() == INTERNAL_SERVER_ERROR_CODE)
            return result;

        String key = getValueWithoutDoubleQuotes(json.get(CONFIG_ID).toString());

        try {
            BasicDBObject searchQuery = new BasicDBObject().append(CONFIG_ID, key);
            DBObject parse = (DBObject) JSON.parse(json.toString());
            collection.update(searchQuery, parse);
        } catch (JsonParseException e) {
            System.out.println(e.getMessage());
        }

        Cache cache = getCache();
        Element newJsonElement = new Element(key, json.toString());
        cache.put(newJsonElement);

        return ok(json).as(CONTENT_TYPE_JSON);
    }

    public Result find(String configId) {
        Cache cache = getCache();
        Element jsonElement = cache.get(configId);
        if (jsonElement != null) {
            return ok(jsonElement.getObjectValue().toString()).as(CONTENT_TYPE_JSON);
        }

        BasicDBObject searchQuery = new BasicDBObject().append(CONFIG_ID, configId);
        DBCollection collection = getDBCollection();

        DBObject configuration = collection.find(searchQuery).one();
        if (configuration == null)
            return ok("{\"message\":\"No configuration present with configId: " + configId + "\"}").as(CONTENT_TYPE_JSON);

        String jsonString = configuration.toString();

        Element newJsonElement = new Element(configId, jsonString);
        cache.put(newJsonElement);

        return ok(jsonString).as(CONTENT_TYPE_JSON);
    }

    public Result findAll() {
        DBCollection collection = getDBCollection();
        DBCursor csr = collection.find();
        StringBuilder json = new StringBuilder();

        for (DBObject dbObject : csr) {
            if(dbObject.get(CONFIG_ID) == null)
                continue;

            json.append("{\"").append(CONFIG_ID).append("\": ");
            json.append("\"" + dbObject.get(CONFIG_ID) + "\",");
            json.append("\"").append(CONFIG_NAME).append("\": ");
            json.append("\"" + dbObject.get(CONFIG_NAME) + "\"}");
            json.append(COMMA);
        }

        if (json.length() == 0)
            return ok("{\"message\":\"No configurations available!\"}").as(CONTENT_TYPE_JSON);

        return ok("[" + json.toString().substring(0,json.length()-1) + "]").as(CONTENT_TYPE_JSON);
    }

    public Result delete(String configId) {
        BasicDBObject searchQuery = new BasicDBObject().append(CONFIG_ID, configId);
        DBCollection collection = getDBCollection();
        DBObject configuration = collection.find(searchQuery).one();
        if (configuration == null)
            return ok("{\"message\":\"No configuration present with configId: " + configId + "\"}").as(CONTENT_TYPE_JSON);

        collection.remove(configuration);

        Cache cache = getCache();
        Element jsonElement = cache.get(configId);
        if (jsonElement != null)
            cache.remove(configId);

        return ok("{\"message\":\"Deleted configuration with configId: " + configId + "\"}").as(CONTENT_TYPE_JSON);
    }

    public Result updateReadOnlyProperties() {
        DBCollection collection = getDBCollection();
        JsonNode json = Controller.request().body().asJson();

        BasicDBObject searchQuery = new BasicDBObject().append(PROPERTY, READ_ONLY);

        DBObject configuration = collection.find(searchQuery).one();
        if(configuration != null)
            collection.remove(configuration);

        try {
            DBObject parse = (DBObject) JSON.parse(json.toString());
            collection.insert(parse);
        } catch (JsonParseException e) {
            System.out.println(e.getMessage());
        }

        Cache cache = getCache();
        Element newJsonElement = new Element(READ_ONLY, json.toString());
        cache.put(newJsonElement);

        return ok(json).as(CONTENT_TYPE_JSON);
    }

    public Result getReadOnlyProperties() {
        BasicDBObject searchQuery = new BasicDBObject().append(PROPERTY, READ_ONLY);
        DBCollection collection = getDBCollection();

        DBObject configuration = collection.find(searchQuery).one();
        if (configuration == null)
            return ok("{\"message\":\"No read-only properties present\"}").as(CONTENT_TYPE_JSON);

        String jsonString = configuration.toString();

        Cache cache = getCache();
        Element newJsonElement = new Element(READ_ONLY, jsonString);
        cache.put(newJsonElement);

        return ok(jsonString).as(CONTENT_TYPE_JSON);
    }

    public Result getValueOfKey(String configId, String key) {
        BasicDBObject searchQuery = new BasicDBObject().append(CONFIG_ID, configId);
        DBCollection collection = getDBCollection();

        DBObject configuration = collection.find(searchQuery).one();
        if (configuration == null)
            return ok("{\"message\":\"No configuration present with configId: " + key + "\"}").as(CONTENT_TYPE_JSON);

        String[] keyPath = {key};
        if(key.indexOf(COMMA) > 0)
            keyPath = key.split(COMMA);

        Pattern numberPattern = Pattern.compile("^(\\d+)$");

        Object currentObject = configuration;

        for(String currentKey: keyPath) {
            if(numberPattern.matcher(currentKey).matches()) {
                int index = Integer.parseInt(currentKey);
                if(currentObject instanceof BasicDBObject)
                    return internalServerError("{\"message\":\"Invalid key path - Array not present\"}").as(CONTENT_TYPE_JSON);

                try {
                    currentObject = ((BasicDBList) currentObject).toArray()[index];
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    return internalServerError("{\"message\":\"Invalid key path - Given index is more than the array size\"}").as(CONTENT_TYPE_JSON);
                }

                continue;
            }

            if(currentObject instanceof BasicDBList)
                return internalServerError("{\"message\":\"Invalid key path\"}").as(CONTENT_TYPE_JSON);

            currentObject = ((BasicDBObject)currentObject).get(currentKey);

            if(currentObject == null)
                return internalServerError("{\"message\":\"Invalid key path\"}").as(CONTENT_TYPE_JSON);
        }

        String json = currentObject.toString();
        if(json.startsWith("{"))
            return ok("{\"value\":" + currentObject.toString() + "}").as(CONTENT_TYPE_JSON);
        return ok("{\"value\":\"" + currentObject.toString() + "\"}").as(CONTENT_TYPE_JSON);
    }
}
