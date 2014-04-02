/**

SpagoBI - The Business Intelligence Free Platform

Copyright (C) 2005-2010 Engineering Ingegneria Informatica S.p.A.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

**/
package it.eng.spagobi.dataset.cache.impl.sqldbcache.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import it.eng.qbe.dataset.QbeDataSet;
import it.eng.spago.configuration.ConfigSingleton;
import it.eng.spago.configuration.FileCreatorConfiguration;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.tools.dataset.cache.CacheException;
import it.eng.spagobi.tools.dataset.cache.CacheFactory;
import it.eng.spagobi.tools.dataset.cache.ICache;
import it.eng.spagobi.tools.dataset.cache.ICacheMetadata;
import it.eng.spagobi.tools.dataset.cache.impl.sqldbcache.CacheItem;
import it.eng.spagobi.tools.dataset.cache.impl.sqldbcache.SQLDBCache;
import it.eng.spagobi.tools.dataset.cache.impl.sqldbcache.SQLDBCacheConfiguration;
import it.eng.spagobi.dataset.cache.impl.sqldbcache.DataType;
import it.eng.spagobi.dataset.cache.test.FakeDatamartRetriever;
import it.eng.spagobi.dataset.cache.test.TestConstants;
import it.eng.spagobi.tenant.Tenant;
import it.eng.spagobi.tenant.TenantManager;
import it.eng.spagobi.tools.dataset.bo.FileDataSet;
import it.eng.spagobi.tools.dataset.bo.FlatDataSet;
import it.eng.spagobi.tools.dataset.bo.JDBCDataSet;
import it.eng.spagobi.tools.dataset.bo.ScriptDataSet;
import it.eng.spagobi.tools.dataset.common.datastore.DataStore;
import it.eng.spagobi.tools.dataset.common.datastore.Field;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.common.datastore.Record;
import it.eng.spagobi.tools.dataset.common.metadata.FieldMetadata;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData.FieldType;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.MetaData;
import it.eng.spagobi.tools.dataset.persist.PersistedTableManager;
import it.eng.spagobi.tools.datasource.bo.DataSource;
import junit.framework.TestCase;

/**
 * @author Marco Cortella (marco.cortella@eng.it)
 *
 */
public abstract class AbstractSQLDBCacheTest extends TestCase {
	protected static ICache cache = null;
	
	protected JDBCDataSet sqlDataset;
	protected QbeDataSet qbeDataset;
	protected FileDataSet fileDataset;
	protected FlatDataSet flatDataset;
	protected ScriptDataSet scriptDataset;
	protected DataSource dataSourceReading;
	protected DataSource dataSourceWriting;
	
	static private Logger logger = Logger.getLogger(AbstractSQLDBCacheTest.class);
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("AF_CONFIG_FILE", TestConstants.AF_CONFIG_FILE);
    	ConfigSingleton.setConfigurationCreation( new FileCreatorConfiguration( TestConstants.WEBCONTENT_PATH ) );

    	TenantManager.setTenant(new Tenant("SPAGOBI"));

		//Creating DataSources and DataSets
		this.createDataSources();
		this.createDatasets();
		
		if (cache == null){
			CacheFactory cacheFactory = new CacheFactory();
			
			//Set configuration parameters for the cache (in SpagoBI Server this is the sbi_config table)
			SQLDBCacheConfiguration cacheConfiguration = new SQLDBCacheConfiguration();
			//table prefix for tables created by the cache
			cacheConfiguration.setTableNamePrefix(TestConstants.CACHE_CONFIG_TABLE_PREFIX); 
			//Dimension of cache in bytes
			cacheConfiguration.setCacheSpaceAvailable(TestConstants.CACHE_CONFIG_CACHE_DIMENSION); 
			//percentage of the cache to clean (from 0 to 100)
			cacheConfiguration.setCachePercentageToClean(TestConstants.CACHE_CONFIG_PERCENTAGE_TO_CLEAN); 
			
			cacheConfiguration.setCacheDataSource(dataSourceWriting);
			//schema name used for correct cache dimension calculation
			cacheConfiguration.setSchema(TestConstants.CACHE_CONFIG_SCHEMA_NAME);
			
			DataType dataType = new DataType(); //class used for setting data type dimension properties
			cacheConfiguration.setObjectsTypeDimension(dataType.getProps());
			
			cache = cacheFactory.getCache(cacheConfiguration);
		}


	}
	
	//Test cases
	
	public void testCacheInit(){
		assertNotNull("Cache correctly initialized", cache );
	}
	
	public void testCacheDimension(){
		assertEquals(TestConstants.CACHE_CONFIG_CACHE_DIMENSION, cache.getMetadata().getAvailableMemory() );
		assertEquals(new Integer(100), cache.getMetadata().getAvailableMemoryAsPercentage() );
		assertEquals(new Integer(TestConstants.CACHE_CONFIG_PERCENTAGE_TO_CLEAN), cache.getMetadata().getCleaningQuota() );
		assertEquals(new Integer(0), cache.getMetadata().getNumberOfObjects() );
	}
	
	public void testCachePutJDBCDataSet(){
		IDataStore resultset;	
		
		sqlDataset.loadData();
		resultset = sqlDataset.getDataStore();
		cache.put(sqlDataset, resultset);
		assertNotNull(cache.get(sqlDataset.getSignature()));
		logger.debug("JDBCDataset inserted inside cache");
	}
	
	public void testCachePutFileDataSet(){
		IDataStore resultset;

		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();
		cache.put(fileDataset, resultset);
		assertNotNull(cache.get(fileDataset.getSignature()));
		logger.debug("FileDataSet inserted inside cache");
	}
	
	public void testCachePutQbeDataSet(){		
		IDataStore resultset;

		qbeDataset.loadData();
		resultset =	qbeDataset.getDataStore();
		cache.put(qbeDataset, resultset);
		assertNotNull(cache.get(qbeDataset.getSignature()));
		logger.debug("QbeDataSet inserted inside cache");
		
	}
	
	public void testCachePutFlatDataSet(){		
		IDataStore resultset;

		flatDataset.loadData();
		resultset =	flatDataset.getDataStore();
		cache.put(flatDataset, resultset);
		assertNotNull(cache.get(flatDataset.getSignature()));
		logger.debug("FlatDataSet inserted inside cache");
		
	}
	
	public void testCachePutScriptDataSet(){		
		IDataStore resultset;

		scriptDataset.loadData();
		resultset =	scriptDataset.getDataStore();
		cache.put(scriptDataset, resultset);
		assertNotNull(cache.get(scriptDataset.getSignature()));
		logger.debug("ScriptDataset inserted inside cache");
		
	}	
	
	public void testCacheGetJDBCDataSet(){
		String signature = sqlDataset.getSignature();
		IDataStore dataStore = cache.get(signature);
		assertNull(dataStore);
		if (dataStore == null){
			sqlDataset.loadData();
			dataStore =	sqlDataset.getDataStore();
			cache.put(sqlDataset, dataStore);
		}
		dataStore = cache.get(signature);
		assertNotNull(dataStore);
	}
	
	public void testCacheGetFileDataSet(){
		String signature = fileDataset.getSignature();
		IDataStore dataStore = cache.get(signature);
		assertNull(dataStore);
		if (dataStore == null){
			fileDataset.loadData();
			dataStore =	fileDataset.getDataStore();
			cache.put(fileDataset, dataStore);
		}
		dataStore = cache.get(signature);
		assertNotNull(dataStore);
	}
	
	public void testCacheGetQbeDataSet(){
		String signature = qbeDataset.getSignature();
		IDataStore dataStore = cache.get(signature);
		assertNull(dataStore);
		if (dataStore == null){
			qbeDataset.loadData();
			dataStore =	qbeDataset.getDataStore();
			cache.put(qbeDataset, dataStore);
		}
		dataStore = cache.get(signature);
		assertNotNull(dataStore);
	}	
	
	public void testCacheGetFlatDataSet(){
		String signature = flatDataset.getSignature();
		IDataStore dataStore = cache.get(signature);
		assertNull(dataStore);
		if (dataStore == null){
			flatDataset.loadData();
			dataStore =	flatDataset.getDataStore();
			cache.put(flatDataset, dataStore);
		}
		dataStore = cache.get(signature);
		assertNotNull(dataStore);
	}
	
	public void testCacheGetScriptDataSet(){
		String signature = scriptDataset.getSignature();
		IDataStore dataStore = cache.get(signature);
		assertNull(dataStore);
		if (dataStore == null){
			scriptDataset.loadData();
			dataStore =	scriptDataset.getDataStore();
			cache.put(scriptDataset, dataStore);
		}
		dataStore = cache.get(signature);
		assertNotNull(dataStore);
	}	
	
	
	public void testCacheDeleteCachedDataset(){
		IDataStore resultset;

		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();
		cache.put(fileDataset, resultset);
		logger.debug("FileDataSet inserted inside cache");
		String tableName = cache.getMetadata().getCacheItem(fileDataset.getSignature()).getTable();
		assertTrue(cache.delete(fileDataset.getSignature()));	
		assertNull("Dataset still present in cache registry",cache.get(fileDataset.getSignature()));
		IDataStore dataStore = null;
		//Check that the table is not present, if this fail with an exception  we know that the table isn't on DB
		try {
			dataStore = dataSourceWriting.executeStatement("SELECT * FROM "+tableName, 0, 0);

		} catch (Exception e){
			logger.debug("The table ["+tableName+"] not found on cache database");
			System.out.println("Delete successfull: The table ["+tableName+"] not found on cache database");

		}
		
		assertNull("Delete fail: Dataset is still present on cache ",dataStore);
		
	}
	
	//Trying to delete a dataset that is not in the cache
	public void testCacheDeleteUncachedDataset(){
		IDataStore resultset;

		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();
		
		assertFalse(cache.delete(fileDataset.getSignature()));	
		
	}
	
	public void testCacheDeleteAll(){
		testCachePutJDBCDataSet();
		testCachePutFileDataSet();
		cache.deleteAll();
		
		ICacheMetadata cacheMetadata = cache.getMetadata();
		BigDecimal cacheSpaceAvaiable = cacheMetadata.getAvailableMemory();
		boolean cacheCleaned = false;
		if (cacheSpaceAvaiable.compareTo(TestConstants.CACHE_CONFIG_CACHE_DIMENSION) == 0){
			cacheCleaned = true;
		}
		assertTrue("The cache wasn't cleaned correctly",cacheCleaned);
	}
	
	
	public void testCacheDatasetDimension(){
		ICacheMetadata cacheMetadata = cache.getMetadata();
		BigDecimal cacheSpaceAvaiable = cacheMetadata.getAvailableMemory();
		
		IDataStore resultset;

		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();
		
		BigDecimal estimatedDimension = cacheMetadata.getRequiredMemory(resultset);
		
		BigDecimal usedSpacePrevision = cacheSpaceAvaiable.subtract(estimatedDimension);
		
		boolean flag = false;
		if (usedSpacePrevision.compareTo(BigDecimal.ZERO) >= 0) {
			flag = true;
			cache.put(fileDataset, resultset);
			IDataStore cachedResultSet = cache.get(fileDataset.getSignature());
			if (cachedResultSet != null){
				flag = true;
			} else {
				flag = false;
			}
		} else {
			flag = true;
			logger.debug("Not enought space for inserting the dataset in cache");

		}
		assertTrue("Problem calculating space avaiable for inserting dataset in cache",flag);

	}
	
	public void testCacheHasSpaceForDataset(){
		IDataStore resultset;

		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();
		
		
		ICacheMetadata cacheMetadata = cache.getMetadata();
		if (cacheMetadata.hasEnoughMemoryForResultSet(resultset)){
			cache.put(fileDataset, resultset);
			IDataStore cachedResultSet = cache.get(fileDataset.getSignature());
			assertNotNull("Checking space for dataset is wrong",cachedResultSet);

		}
	}
	
	
	public void testCacheZeroSpaceAvaiable(){
		
		//Create a cache with space available equal to zero
		ICache cacheZero = createCacheZero();
		boolean exception = false;
		
		IDataStore resultset;	
		
		sqlDataset.loadData();
		resultset = sqlDataset.getDataStore();
		try {
			cacheZero.put(sqlDataset, resultset);

		}
		catch (CacheException ex) {
			logger.error("Dataset cannot be cached");
			exception = true;
		}		
		finally {
			assertTrue("Wrong behavior: Exception expected but not catched",exception );
			assertNull("Wrong behavior: dataset cached even if the space available is zero",cacheZero.get(sqlDataset.getSignature()));		
			cacheZero.deleteAll();
		}

	}
	
	public void testGetDimensionSpaceAvailable(){
		ICacheMetadata cacheMetadata = cache.getMetadata();
		BigDecimal cacheSpaceAvaiable = cacheMetadata.getAvailableMemory();
		assertNotNull("Error calculating avaiable cache space", cacheSpaceAvaiable);
		System.out.println(" >> Avaiable cache space: "+cacheSpaceAvaiable+" byte");
	}
	
	public void testGetDimensionSpaceUsed(){
		IDataStore resultset;

		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();		
		
		ICacheMetadata cacheMetadata = cache.getMetadata();
		BigDecimal estimatedDatasetDimension = cacheMetadata.getRequiredMemory(resultset);
		assertNotNull("Error calculating dimension of dataset", estimatedDatasetDimension);
		System.out.println(" >> Estimated dataset dimension: "+estimatedDatasetDimension+" byte");
	}
	
	public void testCountNumberOfObjects(){
		testCachePutJDBCDataSet();
		testCachePutFileDataSet();
		testCachePutQbeDataSet();
		
		ICacheMetadata cacheMetadata = cache.getMetadata();
		boolean result = false;
		if(cacheMetadata.getNumberOfObjects() == 3){
			result = true;
		}
		assertTrue(result);
	}
	
	public void testCacheCleaning(){
		ICache cacheCustom = createCache(791449);
		IDataStore resultset;

		//Insert first dataset
		qbeDataset.loadData();
		resultset =	qbeDataset.getDataStore();
		
		try {
			cacheCustom.put(qbeDataset, resultset);
		} finally {
			assertNotNull(cacheCustom.get(qbeDataset.getSignature()));
			logger.debug("QbeDataSet inserted inside cache");
		}

		ICacheMetadata cacheMetadata = cacheCustom.getMetadata();
		
		//Second dataset (too big for avaiable space)
		fileDataset.loadData();
		resultset =	fileDataset.getDataStore();		
		
		assertFalse(cacheMetadata.hasEnoughMemoryForResultSet(resultset));
		
		try{
			cacheCustom.put(fileDataset, resultset);

		} finally {
			assertNull("Dataset found in cache but there should not be",cacheCustom.get(fileDataset.getSignature()));
			
			cacheCustom.deleteAll();
		}
	}
	
	public void testSchemaName(){

		String schemaName =  TestConstants.CACHE_CONFIG_SCHEMA_NAME;
		
		IDataStore resultset;
		

		qbeDataset.loadData();
		resultset =	qbeDataset.getDataStore();
		String signature = qbeDataset.getSignature();
		cache.put(qbeDataset, resultset);
		logger.debug("QbeDataSet inserted inside cache");
		
		ICacheMetadata cacheMetadata = cache.getMetadata();
		CacheItem cacheItem = cache.getMetadata().getCacheItem(signature);
		String tableName = cacheItem.getTable();		        
        if (schemaName.isEmpty()){
			IDataStore dataStore = dataSourceWriting.executeStatement("SELECT * FROM "+tableName, 0, 0);

        } else {
			IDataStore dataStore = dataSourceWriting.executeStatement("SELECT * FROM "+schemaName+"."+tableName, 0, 0);
        }

	}
	
	public void testFakeDataset(){
		String schemaName =  TestConstants.CACHE_CONFIG_SCHEMA_NAME;

		//Create a fake dataStore
		DataStore dataStore = new DataStore();
		IMetaData metadata = new MetaData();
		IFieldMetaData fieldMetaData = new FieldMetadata();
		fieldMetaData.setAlias("test_column");
		fieldMetaData.setName("test_column");
		fieldMetaData.setType(String.class);
		fieldMetaData.setFieldType(FieldType.ATTRIBUTE);
		metadata.addFiedMeta(fieldMetaData);
		dataStore.setMetaData(metadata);
		Record record = new Record();
		Field field = new Field();
		field.setValue("try");
		record.appendField(field);
		dataStore.appendRecord(record);
		
		//persist the datastore as a table on db
		PersistedTableManager persistedTableManager = new PersistedTableManager();
		Random ran = new Random();
		int x = ran.nextInt(100);
		String tableName = "SbiTest"+x;
		persistedTableManager.setTableName(tableName);
		
		try {
			persistedTableManager.persistDataset(dataStore, dataSourceWriting);
		} catch (Exception e) {
			logger.error("Error persisting dataset");
		}
		
		//try to query the table using the SchemaName.TableName syntax if schemaName is valorized
        
        try {
            if (schemaName.isEmpty()){
    			dataSourceWriting.executeStatement("SELECT * FROM "+tableName, 0, 0);

            } else {
    			dataSourceWriting.executeStatement("SELECT * FROM "+schemaName+"."+tableName, 0, 0);
            }
        }
        finally {
            //Dropping table
            persistedTableManager.dropTableIfExists(dataSourceWriting,tableName);
    		
        }

		
		
	}
	
	
	//------------------------------------------------------------------------------
	
	
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		cache.deleteAll();
		//clean cache in memory and on db
	}
	
	/*
	* ----------------------------------------------------
	* Initialization Methods
	* ----------------------------------------------------
	*/
	
	public void createDataSources(){
		//Must be overridden by specific implementation
		//dataSourceReading = TestDataSourceFactory.createDataSource(TestConstants.DatabaseType.MYSQL, false);
		//dataSourceWriting = TestDataSourceFactory.createDataSource(TestConstants.DatabaseType.MYSQL, true);
		logger.error("Specific DataSource must be specified in specialized Test");
	}
	
	public JDBCDataSet createJDBCDataset(){
		//Create JDBCDataSet
		sqlDataset = new JDBCDataSet();
		sqlDataset.setQuery("select * from customer");
		sqlDataset.setQueryScript("");
		sqlDataset.setQueryScriptLanguage("");
		sqlDataset.setDataSource(dataSourceReading);
		sqlDataset.setLabel("test_jdbcDataset");
		return sqlDataset;
	}
	
	public FileDataSet createFileDataset(){
		fileDataset = new FileDataSet();

		try {
			fileDataset.setFileType("CSV");
			JSONObject jsonConf = new JSONObject();
			jsonConf.put("fileType", "CSV");
			jsonConf.put("fileName", "customers.csv");
			jsonConf.put("csvDelimiter", ",");
			jsonConf.put("csvDelimiter", ",");
			jsonConf.put("csvQuote", "\"");
			jsonConf.put("csvEncoding", "UTF-8");
			jsonConf.put("DS_SCOPE", "USER");	
			fileDataset.setDsMetadata("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><META version=\"1\"><COLUMNLIST><COLUMN alias=\"customer_id\" fieldType=\"ATTRIBUTE\" name=\"customer_id\" type=\"java.lang.Integer\"/><COLUMN alias=\"lname\" fieldType=\"ATTRIBUTE\" name=\"lname\" type=\"java.lang.String\"/><COLUMN alias=\"fname\" fieldType=\"ATTRIBUTE\" name=\"fname\" type=\"java.lang.String\"/><COLUMN alias=\"num_children_at_home\" fieldType=\"ATTRIBUTE\" name=\"num_children_at_home\" type=\"java.lang.Integer\"/></COLUMNLIST><DATASET><PROPERTY name=\"resultNumber\" value=\"49\"/> </DATASET></META>");			
			fileDataset.setConfiguration(jsonConf.toString());
			fileDataset.setResourcePath(TestConstants.RESOURCE_PATH);
			fileDataset.setFileName("customers.csv");
			fileDataset.setLabel("test_fileDataset");

		}
		catch(JSONException e){
			logger.error("JSONException when creating a FileDataset");
		}
		return fileDataset;


	}
	
	public QbeDataSet createQbeDataset(){
		qbeDataset = new QbeDataSet();
		qbeDataset.setJsonQuery("{\"catalogue\": {\"queries\": [{\"id\":\"q1390389018208\",\"distinct\":false,\"isNestedExpression\":false,\"fields\":[{\"alias\":\"Lname\",\"visible\":true,\"include\":true,\"type\":\"datamartField\",\"id\":\"it.eng.spagobi.meta.Customer:lname\",\"entity\":\"Customer\",\"field\":\"Lname\",\"longDescription\":\"Customer : Lname\",\"group\":\"\",\"funct\":\"NONE\",\"iconCls\":\"attribute\",\"nature\":\"attribute\"},{\"alias\":\"Fname\",\"visible\":true,\"include\":true,\"type\":\"datamartField\",\"id\":\"it.eng.spagobi.meta.Customer:fname\",\"entity\":\"Customer\",\"field\":\"Fname\",\"longDescription\":\"Customer : Fname\",\"group\":\"\",\"funct\":\"NONE\",\"iconCls\":\"attribute\",\"nature\":\"attribute\"},{\"alias\":\"City\",\"visible\":true,\"include\":true,\"type\":\"datamartField\",\"id\":\"it.eng.spagobi.meta.Customer:city\",\"entity\":\"Customer\",\"field\":\"City\",\"longDescription\":\"Customer : City\",\"group\":\"true\",\"funct\":\"NONE\",\"iconCls\":\"attribute\",\"nature\":\"attribute\"}],\"filters\":[],\"expression\":{},\"havings\":[],\"subqueries\":[]}]}, \t\"version\":7,\t\"generator\": \"SpagoBIMeta\" }\t");
		qbeDataset.setResourcePath(TestConstants.RESOURCE_PATH);
		qbeDataset.setDatamarts("MyModel41");
		qbeDataset.setDataSource(dataSourceReading);
		qbeDataset.setDataSourceForWriting(dataSourceWriting);
		Map params = new HashMap();
		FakeDatamartRetriever fakeDatamartRetriever = new FakeDatamartRetriever();
		fakeDatamartRetriever.setResourcePath(TestConstants.RESOURCE_PATH);
		params.put(SpagoBIConstants.DATAMART_RETRIEVER, fakeDatamartRetriever);
		qbeDataset.setParamsMap(params);
		qbeDataset.setLabel("test_qbeDataset");
		return qbeDataset;
	}
	
	public FlatDataSet createFlatDataset(){
		flatDataset = new FlatDataSet();
		flatDataset.setDataSource(dataSourceReading);
		flatDataset.setTableName("department"); //name of the table corresponding to the flat dataset (persisted dataset)
		flatDataset.setLabel("test_flatDataset");
		return flatDataset;
	}
	
	public ScriptDataSet createScriptDataSet(){
		scriptDataset=new ScriptDataSet();
		scriptDataset.setScriptLanguage("groovy");
		scriptDataset.setScript("returnValue(new Double(5).toString());\n");
		scriptDataset.setLabel("test_scriptDataset");
		return scriptDataset;
	}
	
	public void createDatasets() throws JSONException{
		createJDBCDataset();
		createFileDataset();
		createQbeDataset();
		createFlatDataset();
		createScriptDataSet();

	}
	
	public ICache createCacheZero(){
		//Create a cache with space available equal to zero
		return createCache(0);
	}
	
	public ICache createCache(int dimension ){
		//Create a cache with space available equal to dimension
		
		CacheFactory cacheFactory = new CacheFactory();
		
		//Set configuration parameters for the cache (in SpagoBI Server this is the sbi_config table)
		SQLDBCacheConfiguration cacheConfigurationCustom = new SQLDBCacheConfiguration();
		//table prefix for tables created by the cache
		cacheConfigurationCustom.setTableNamePrefix(TestConstants.CACHE_CONFIG_TABLE_PREFIX); 
		//Dimension of cache in bytes
		cacheConfigurationCustom.setCacheSpaceAvailable(new BigDecimal(dimension)); 
		//percentage of the cache to clean (from 0 to 100)
		cacheConfigurationCustom.setCachePercentageToClean(TestConstants.CACHE_CONFIG_PERCENTAGE_TO_CLEAN); 
		//schema name used for correct cache dimension calculation
		cacheConfigurationCustom.setSchema(TestConstants.CACHE_CONFIG_SCHEMA_NAME);
		cacheConfigurationCustom.setCacheDataSource(dataSourceWriting);
		
		DataType dataType = new DataType(); //class used for setting data type dimension properties
		cacheConfigurationCustom.setObjectsTypeDimension(dataType.getProps());
		
		ICache cacheCustom = cacheFactory.getCache(cacheConfigurationCustom);
		
		return cacheCustom;
	}
	
	

}
