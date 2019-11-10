This README file explains what the configuration-dashboard-server application is and how to run it in the local server.

TITLE: Configuration Dashboard Server

SUMMARY: This is the backend server application for the configuration-dashboard react application which exposes services for CRUD operations to be performed on each configuration.


-- PREREQUISITES --

1. Java 1.8 or higher
2. Play 2.6.x
3. SBT 1.x
4. Mongo 4.x.x


-- LIBRARY DEPENDENCIES --

1. org.mongodb  ->  mongo-java-driver-3.8.1
2. com.google.code.gson  ->  gson.2.2.4
3. ehcache-2.10.4


-- RUNNING THE APPLICATION --

Run `sbt run` from the root folder configuration-dashboard-server to have the application running. For the services to function, the mongo daemon should be started and the mongo instance should be running in localhost on port 27017


-- APPLICATION.CONF EXPLAINED --

1. CORS Filter enabled
2. Cache name is present.
3. Details to connect to Mongo DB instance is present.


-- AVAILABLE SERVICES AND ENDPOINTS --

=================================================================================================================================================================================================================
|S.NO.|	METHOD	|			RELATIVE URL			|   			SERVICE DETAILS    																															|
=================================================================================================================================================================================================================
|  1. |	 GET	|	 /find/:configId                |   To find the configuration JSON for the given configId. Triggered from UI on page load, on clicking add or update 										|
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|  2. |  GET	|	 /findAll						|   To find all the configurations to be listed in the dashboard. Returns a array of configId and configName values. Triggered from UI on clicking view		|
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  3. |  POST	|	 /save 							|   To save a configuration JSON with configId and configName on clicking Add button 																		|
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  4. |  POST	|	 /update                        |   To update a configuration JSON with configId and configName on clicking Add button 																		|
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  5. |  GET	|	 /delete/:configId              |   To delete a configuration for a given configId. Triggered from UI on clicking Delete 																	|
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  6. |  POST	|	 /updateReadOnlyProperties      |   To configure readonly properties for given keys in payload. Sample JSON is given below. Same endpoint is used for both saving and updating readonly keys| 													|
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  7. |  GET	|	 /getReadOnlyProperties         |   To retrieve the currently configured readonly properties 																								|			|
----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|  8. |  GET	|	 /getValueOfKey/:configId/:key  |   To retrieve the value for a given configId and key 																										|
=================================================================================================================================================================================================================


-- READNONLY PROPERTIES EXPLAINED --

Sample Payload: 

{
  "property": "readOnly",
  "keys": ["_id", "$oid", "configId", "dbSettings,#,host"]
}


Example Configuration: 

{
	"configuration": {
		"configId": 1,
		"configName": "Default",
		"appSettings": {
			"domain": "http://localhost",
			"port": 8080,
			"token": "A123",
			"logging": {
				"logLevel": "Debug"
			},
			"logoRelativeUrl": "/public/images/logo/logo.png",
			"applicationName": "My First Application",
			"version": "1.0.0"
		},
		"dbSettings": [{
				"dbVariant": "oracle",
				"host": "localhost",
				"port": 1521,
				"schema": "configDashboard",
				"username": "orcl",
				"password": "1234",
				"queryString": "jdbc:oracle:thin:@localhost:1521:orcl"
			},
			{
				"dbVariant": "mysql",
				"host": "127.0.0.1",
				"port": 3306,
				"schema": "configDashboard",
				"username": "root",
				"password": "12345",
				"queryString": "server=127.0.0.1;uid=root;pwd=12345;database=configDashboard"
			}
		]
	}
}

The following examples are with reference to the above mentioned example configuration.

The keyPath is represented using comma-separated keys. To represent `applicationName`, the keyPath is appSettings,applicationName
To make `applicationName` read-only, the keys array in payload should contain "appSettings,applicationName" value.

To represent a key present in an array, # is used to represent all indexes of array. To represent `queryString`, the keyPath is dbSettings,#,queryString
To make `queryString` of all indexes of dbSettings array read-only, the keys array in payload should contain "dbSettings,#,queryString" value.
