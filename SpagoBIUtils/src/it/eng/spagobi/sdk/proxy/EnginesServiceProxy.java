/* SpagoBI, the Open Source Business Intelligence suite

 * Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0, without the "Incompatible With Secondary Licenses" notice. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package it.eng.spagobi.sdk.proxy;

import java.rmi.Remote;

import org.apache.axis.client.Stub;
import org.apache.ws.security.handler.WSHandlerConstants;

import it.eng.spagobi.sdk.callbacks.ClientCredentialsHolder;

public class EnginesServiceProxy extends AbstractSDKServiceProxy implements it.eng.spagobi.sdk.engines.stub.EnginesService {
  private String _endpoint = null;
  private it.eng.spagobi.sdk.engines.stub.EnginesService enginesService = null;
  private ClientCredentialsHolder cch = null;
  
	public EnginesServiceProxy(String user, String pwd) {
		cch = new ClientCredentialsHolder(user, pwd);
		_initEnginesServiceProxy();
	}
  
  private void _initEnginesServiceProxy() {
	    try {
			it.eng.spagobi.sdk.engines.stub.EnginesServiceServiceLocator locator = new it.eng.spagobi.sdk.engines.stub.EnginesServiceServiceLocator();
			Remote remote = locator.getPort(it.eng.spagobi.sdk.engines.stub.EnginesService.class);
	        Stub axisPort = (Stub) remote;
	        axisPort._setProperty(WSHandlerConstants.USER, cch.getUsername());
	        axisPort._setProperty(WSHandlerConstants.PW_CALLBACK_REF, cch);
	        //axisPort.setTimeout(30000); //used in SpagoBIStudio
	        enginesService = (it.eng.spagobi.sdk.engines.stub.EnginesService) axisPort;
			
	      if (enginesService != null) {
	        if (_endpoint != null)
	          ((javax.xml.rpc.Stub)enginesService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
	        else
	          _endpoint = (String)((javax.xml.rpc.Stub)enginesService)._getProperty("javax.xml.rpc.service.endpoint.address");
	      }
	      
	    }
	    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (enginesService != null)
      ((javax.xml.rpc.Stub)enginesService)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public it.eng.spagobi.sdk.engines.stub.EnginesService getEnginesService() {
    if (enginesService == null)
      _initEnginesServiceProxy();
    return enginesService;
  }
  
  public it.eng.spagobi.sdk.engines.bo.SDKEngine[] getEngines() throws java.rmi.RemoteException, it.eng.spagobi.sdk.exceptions.NotAllowedOperationException{
    if (enginesService == null)
      _initEnginesServiceProxy();
    return enginesService.getEngines();
  }
  
  public it.eng.spagobi.sdk.engines.bo.SDKEngine getEngine(java.lang.Integer in0) throws java.rmi.RemoteException, it.eng.spagobi.sdk.exceptions.NotAllowedOperationException{
    if (enginesService == null)
      _initEnginesServiceProxy();
    return enginesService.getEngine(in0);
  }
  
  
}