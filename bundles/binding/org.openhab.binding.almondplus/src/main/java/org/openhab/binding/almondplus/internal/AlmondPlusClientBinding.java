/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.almondplus.internal;

import static org.openhab.binding.almondplus.internal.AlmondPlusClientGenericBindingProvider.CHANGED_COMMAND_KEY;
import static org.openhab.binding.almondplus.internal.AlmondPlusClientGenericBindingProvider.BINARY_IN_COMMAND_KEY;
import static org.openhab.binding.almondplus.internal.AlmondPlusClientGenericBindingProvider.MULTILEVEL_IN_COMMAND_KEY;
import static org.openhab.binding.almondplus.internal.AlmondPlusClientGenericBindingProvider.INCREASE_COMMAND_KEY;

import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
//import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import java.util.Random;
import java.io.StringWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.openhab.binding.almondplus.AlmondPlusClientBindingProvider;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
//import org.openhab.core.transform.TransformationHelper;
//import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 *
 * @author elakito
 * @since 1.6.0
 */
public class AlmondPlusClientBinding extends AbstractActiveBinding<AlmondPlusClientBindingProvider> implements ManagedService {

    private static final Logger logger =
        LoggerFactory.getLogger(AlmondPlusClientBinding.class);

    /** RegEx to extract a parse a function String <code>'(.*?)\((.*)\)'</code> */
    private static final Pattern EXTRACT_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\)");
    protected static final String SWITCH_MULTILEVEL = "SWITCH MULTILEVEL";
    protected static final String SWITCH_BINARY = "SWITCH BINARY";
	// flag to use the reply of the remote end to update the status of the Item receving the data
	private static boolean updateWithResponse = true;

    /**
     * the refresh interval which is used to check if the inbound connection is open
     * (optional, defaults to 60000ms)
     */
    private long refreshInterval = 60000;
    private String almondURI;
    private AlmondPlusClientManager clientManager;

    public AlmondPlusClientBinding() {
        logger.debug("AlmondPlusClientBinding()");
        this.clientManager = new AlmondPlusClientManager(this);
    }

    public void activate() {
        logger.debug("activate");
        super.activate();
        clientManager.init();
        setProperlyConfigured(true);
    }

    public void deactivate() {
        // deallocate resources here that are no longer needed and
        // should be reset when activating this binding again
        logger.debug("deactivate");
        clientManager.release();
    }

    @Override
    public void bindingChanged(BindingProvider provider, String itemName) {
	logger.debug("bindingChanged");
        super.bindingChanged(provider, itemName);
        AlmondPlusClientBindingProvider bindingProvider = (AlmondPlusClientBindingProvider)provider;
        if (bindingProvider.getItemType(itemName) != null) {
            // added
            initializeItem(itemName, bindingProvider);
        } else {
            // removed
            releaseItem(itemName);
        }
    }


    /**
     * @{inheritDoc}
     */
    @Override
    protected long getRefreshInterval() {
        logger.debug("getRefreshInterval");
        return refreshInterval;
    }

    /**
     * @{inheritDoc}
     */
    protected String getAlmondURI() {
        logger.debug("getAlmondURI");
        return almondURI;
    }
    /**
     * @{inheritDoc}
     */
    protected void setAlmondURI(String URI) {
        logger.debug("setAlmondURI");
        almondURI = URI;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected String getName() {
        logger.debug("getName");
        return "AlmondPlusClient Refresh Service";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void execute() {
        // currently, we use this execute method to keep the inbound connections alive.
        logger.debug("execute() method is called!");
        for (AlmondPlusClientBindingProvider provider : providers) {
//            for (String itemName : provider.getItemNames(IN_COMMAND_KEY)) {
//                String device = provider.getDevice(itemName, IN_COMMAND_KEY);
//                String index = provider.getIndex(itemName, IN_COMMAND_KEY);
//                logger.debug("Check websocket connection for item={} for device={}", itemName, device);
                AlmondPlusClient wc = clientManager.getAlmondPlusClient(almondURI);
                if (wc != null) {
                    try {
//		        String transformedMessage = jsonifyMessage(device,index,null,"GET");
        		try {
            			wc.ensureConnected();
            		//	wc.sendTextMessage(transformedMessage);
        		} catch (Exception e) {
           			logger.error("Unable to send message:"+e.getMessage());
        		}
                    } catch (Exception e) {
                        logger.error("Failed to keep the connection open", e);
                    }
                } else {
                    logger.warn("Unable to find the almondplus client");
                }
//            }
        }
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveCommand(String itemName, Command command) {
        // the code being executed when a command was sent on the openHAB
        // event bus goes here. This method is only called if one of the
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveCommand() is called!");
        writeToWebsocket(itemName, command, command);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveUpdate(String itemName, State newState) {
        // the code being executed when a state was sent on the openHAB
        // event bus goes here. This method is only called if one of the
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveUpdate() is called!");
/*        AlmondPlusClientBindingProvider provider = findFirstMatchingBindingProvider(itemName, null, newState);
	if (!provider.getValue(itemName).equals(newState)){
           writeToWebsocket(itemName, CHANGED_COMMAND_KEY, newState);
	}
*/
    }

    private void writeToWebsocket(String itemName, Command command, Type newState) {
        AlmondPlusClientBindingProvider provider = findFirstMatchingBindingProvider(itemName, command);
	if (newState != null){
            logger.debug("writeToWebsocket: item={}, command={}, type={}", itemName, command, newState.toString());
	}else{
            logger.debug("writeToWebsocket: item={}, command={}", itemName, command);
	}
        if (provider == null) {
            logger.trace("doesn't find matching binding provider [itemName={}, command={}]", itemName, command);
            return;
        }
        String device = provider.getDevice(itemName, command);
	String index = provider.getIndex(itemName, command);
	String type;
	   logger.debug("Command is {} {}",command, StringUtils.isNumeric(command.toString()));
        if (StringUtils.isNumeric(command.toString())){
           type = provider.getType(itemName, command);
//           type = provider.getType(itemName,INCREASE_COMMAND_KEY);
	}else{
	   logger.debug("Command is {}",command);
           type = provider.getType(itemName,command);
        }
//	String value = provider.getValue(itemName,command);
        String transformedMessage;
        if (newState == null) {
          transformedMessage = jsonifyMessage(type,device,index,null,"GET");
	} else {
          transformedMessage = jsonifyMessage(type,device,index,newState.toString(),"SET");
	}
	logger.debug("writeToWebsocket: {}",transformedMessage);
        AlmondPlusClient wc = clientManager.getAlmondPlusClient(almondURI);
        try {
            wc.ensureConnected();
            wc.sendTextMessage(transformedMessage);
        } catch (Exception e) {
            logger.error("Unable to send message "+transformedMessage+" to " + device + ":"+e.getMessage());
        }
    }

    //TODO this is for testing for now
    void postToBus(String itemName, String message, AlmondPlusClientBindingProvider provider) {
	logger.debug("postToBus: {} {} {}",itemName,message,provider);
        for (Command command : provider.getCommands(itemName)) {
          if (command.equals(MULTILEVEL_IN_COMMAND_KEY) || command.equals(BINARY_IN_COMMAND_KEY)) {
            String transformedMessage = deJsonifyMessage(provider.getDevice(itemName,command),provider.getIndex(itemName,command), message);
            if (transformedMessage != "") {
              Class<? extends Item> itemType = provider.getItemType(itemName);
              if (itemType != null) {
	         logger.debug("creating a state from: {} {} {}", itemName, transformedMessage, itemType);
              } else {
	         logger.debug("creating a state from: {} {}", itemName, transformedMessage);
              }
              State state = createState(itemType, transformedMessage);
	      provider.setValue(itemName,transformedMessage,command);
	      logger.debug("publishing: {} {} {}", itemName, state, transformedMessage);
              if (state != null) {
                eventPublisher.postUpdate(itemName, state);
              }
            }
          }
        }
    }

    private State createState(Class<? extends Item> itemType, String message) {
        try {
            if (itemType.isAssignableFrom(NumberItem.class)) {
                return DecimalType.valueOf(message);
            } else if (itemType.isAssignableFrom(ContactItem.class)) {
                return OpenClosedType.valueOf(message);
            } else if (itemType.isAssignableFrom(SwitchItem.class)) {
                return (Boolean.valueOf(message)) ? OnOffType.valueOf("ON") : OnOffType.valueOf("OFF");
            } else if (itemType.isAssignableFrom(DimmerItem.class)) {
                if (StringUtils.isNumeric(message)) {
                   return new PercentType(Integer.valueOf(message));
                } else {
                   return (Boolean.valueOf(message)) ? OnOffType.valueOf("ON") : OnOffType.valueOf("OFF");
                }
            } else if (itemType.isAssignableFrom(RollershutterItem.class)) {
                return PercentType.valueOf(message);
            } else if (itemType.isAssignableFrom(DateTimeItem.class)) {
                return DateTimeType.valueOf(message);
            } else {
                return StringType.valueOf(message);
            }
        } catch (Exception e) {
            logger.debug("Couldn't create state of type '{}' for value '{}' error: '{}'", itemType, message, e.getMessage());
            return StringType.valueOf(message);
        }
    }

    private AlmondPlusClientBindingProvider findFirstMatchingBindingProvider(String itemName, Command command) {
        AlmondPlusClientBindingProvider firstMatchingProvider = null;
        for (AlmondPlusClientBindingProvider provider : providers) {
            String device = provider.getDevice(itemName,command);
            if (device != null) {
                firstMatchingProvider = provider;
                break;
            }
        }

        return firstMatchingProvider;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public void updated(Dictionary<String, ?> config) throws ConfigurationException {
        logger.debug("Configuration updated with provided {}", config != null);
        if (config != null) {

            // to override the default refresh interval one has to add a
            // parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
            String refreshIntervalString = (String) config.get("refresh");
            if (StringUtils.isNotBlank(refreshIntervalString)) {
                refreshInterval = Long.parseLong(refreshIntervalString);
            }

            String almondURI = (String) config.get("uri");
            setAlmondURI(almondURI);
            // read further config parameters here ...
            setProperlyConfigured(true);
        }
        for (AlmondPlusClientBindingProvider provider : this.providers) {
            for (String itemName : provider.getItemNames()) {
                initializeItem(itemName, provider);
            }
        }
    }

    private void initializeItem(String itemName, AlmondPlusClientBindingProvider provider) {
        logger.debug("initialize item={}", itemName);
        for (Command command : provider.getCommands(itemName)) {
            //String url = provider.getURL(itemName, command);
            clientManager.add(itemName, almondURI, MULTILEVEL_IN_COMMAND_KEY.equals(command) || BINARY_IN_COMMAND_KEY.equals(command) ? provider : null);
            //REVISIT may connect the websocket for the inbound here instead of doing it in the execute method as of now
            //
            if (MULTILEVEL_IN_COMMAND_KEY.equals(command) || BINARY_IN_COMMAND_KEY.equals(command)) {
             String device = provider.getDevice(itemName,command);
             String index = provider.getIndex(itemName,command);
             String type = provider.getType(itemName, command);
	     String transformedMessage = jsonifyMessage(type,device,index,null ,"GET");
             AlmondPlusClient wc = clientManager.getAlmondPlusClient(almondURI);
             try {
             	wc.ensureConnected();
             	wc.sendTextMessage(transformedMessage);
             } catch (Exception e) {
            	logger.error("Unable to send message:"+e.getMessage());
            }
           }
        }
    }

    private void releaseItem(String itemName) {
        logger.debug("release item={}", itemName);
        clientManager.remove(itemName);
    }

    protected String deJsonifyMessage(String device,String index, String message) {
	String ret = "";

     try {
       JSONParser parser = new JSONParser();
       JSONObject obj = (JSONObject)parser.parse(message.replaceAll(".ommand.ype","commandtype"));
       String val = (String)obj.get("commandtype");
       logger.debug("	Looking for: {} {} {} {}", val, device, index, message);
       JSONObject data;

       if (obj.containsKey("data") && ((obj.containsKey("success") ^ true) || Boolean.valueOf((String)obj.get("success")))) {
	 data  = (JSONObject)obj.get("data");
	 if (!data.containsKey("devid") && !data.containsKey("index") && val.equals("SensorUpdate")){
           logger.debug("Found SensorUpdate");
           Set keys = data.keySet();
           Iterator key = keys.iterator();
	   String next = (String)key.next();
           logger.debug("Device Key {}",next);
	   if (!device.equals(next)){
              return "";
	   }
           data = (JSONObject)data.get(next);
	   keys = data.keySet();
           key = keys.iterator();
	   next = (String)key.next();
           logger.debug("Index Key {}",next);
	   if (!index.equals(next)){
              return "";
	   }
           data = (JSONObject)data.get(next);
           logger.debug("Found SensorUpdate");
           ret = (String)data.get("value");
           logger.debug("Found SensorUpdate");
           if (StringUtils.isNumeric(ret)){
             ret = Integer.toString(Integer.valueOf(ret) * 100/255);
           }
           logger.debug("Found SensorUpdate");
           logger.debug("Device {}:{} was successfully set to {}",(String)data.get("devid"),
           (String)data.get("index"),ret);
           return ret;
	 }
	 if (device.equals((String)data.get("devid")) && index.equals((String)data.get("index"))){
	   switch (val) {
             case "getdeviceindex":
	       ret = (String)data.get("value");
               if (StringUtils.isNumeric(ret)){
                 ret = Integer.toString(Integer.valueOf(ret) * 100/255);
               }
	       break;
             case "setdeviceindex":
               logger.debug("Device {}:{} was successfully set to {}",(String)data.get("devid"),
                  (String)data.get("index"),(String)data.get("value"));
	     break;
	     default:
	       logger.debug("No actionable command was received in JSON:"+message);
           }
	 }
       }
     }catch(ParseException pe){
	logger.warn("Could not parse JSON String. Error: {}",pe.toString());
     }

     return ret;
    }

    protected String jsonifyMessage(String type,String device, String index, String value, String dir) {
        logger.debug("jsonifying device {} using index {} with value {}", device, index, value);
        String ret = "";
	JSONObject obj = new JSONObject();
	Random rand = new Random();
	obj.put("mii",rand.nextInt(65536)+1);
	if (dir.equals("GET")) {
           obj.put("cmd","getdeviceindex");
	}else if (dir.equals("SET")) {
           obj.put("cmd","setdeviceindex");
	}
	obj.put("devid",device);
	obj.put("index",index);
        if (value != null) {
	  obj.put("value",getValueFromMessage(type, value));
	}
    StringWriter out = new StringWriter();
      try {
       obj.writeJSONString(out);
      }catch(IOException io){
         System.out.println(io);
      }

      ret = out.toString();

        return ret;
    }

    protected String[] splitTransformationConfig(String index) {
        Matcher matcher = EXTRACT_FUNCTION_PATTERN.matcher(index);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("given transformation function '" + index + "' does not follow the expected pattern '<function>(<pattern>)'");
        }
        matcher.reset();

        matcher.find();
        String type = matcher.group(1);
        String pattern = matcher.group(2);

        return new String[] { type, pattern };
    }

    private String getValueFromMessage(String type, String value){
	String ret = "";
	logger.debug("getValueFromMessage type: {} value: {}", type, value);
	 if (StringUtils.isNumeric(value)){
	  switch(type) {
	   case SWITCH_MULTILEVEL:
	     ret = Integer.toString(Integer.valueOf(value) * 255/100);
	     break;
           default:
	     ret = value;
	     break;
	  }
	} else {

          switch (value) {
	    case "OFF": 
		ret = type.equals(SWITCH_MULTILEVEL) ? "0":"false";
		break;
	    case "ON":
		ret = type.equals(SWITCH_MULTILEVEL)? "100" : "true";
		break;
	    default:
		ret = value;
		break;
		}
	}
	return ret;
    }
}
