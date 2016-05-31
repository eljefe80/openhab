/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.almondplus.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.almondplus.AlmondPlusClientBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.TypeParser;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.items.DimmerItem;


/**
 * This class is responsible for parsing the binding configuration.
 *
 * in:  almondplus:"<[<device>:<index>]"
 * out: almondplus:">[<device>:<index>:<value>]"
 *
 * almondplus="<[1:'some text']" - for String Items
 * almondplus=">[ON:1:'some text'], >[OFF:1:'some other command']"
 *
 * @author elakito
 * @since 1.6.0
 */
public class AlmondPlusClientGenericBindingProvider extends AbstractGenericBindingProvider implements AlmondPlusClientBindingProvider {
    private static final Logger logger = LoggerFactory.getLogger(AlmondPlusClientGenericBindingProvider.class);

    protected static final Command MULTILEVEL_IN_COMMAND_KEY = StringType.valueOf("INM");
    protected static final Command BINARY_IN_COMMAND_KEY = StringType.valueOf("INB");
    protected static final Command ON_COMMAND_KEY = StringType.valueOf("ON");
    protected static final Command OFF_COMMAND_KEY = StringType.valueOf("OFF");
    protected static final Command PERCENT_COMMAND_KEY = StringType.valueOf("PERCENT");
    protected static final Command INCREASE_COMMAND_KEY = StringType.valueOf("INCREASE");
    protected static final Command DECREASE_COMMAND_KEY = StringType.valueOf("DECREASE");
    protected static final Command CHANGED_COMMAND_KEY = StringType.valueOf("CHANGED");
    protected static final Command WILDCARD_COMMAND_KEY = StringType.valueOf("*");

    /** {@link Pattern} which matches a binding configuration part */
    private static final Pattern BASE_CONFIG_PATTERN = Pattern.compile("([<|>]\\[.*?\\])*");
    private static final Pattern CONFIG_PATTERN = Pattern.compile("([<|>])(\\[.*?\\])");
    private static final Pattern COMMAND_CONFIG_PATTERN = Pattern.compile("\\[(.*?):(.*?):(.*?):(.*?)\\]");
    private static final Pattern IN_CONFIG_PATTERN = Pattern.compile("\\[(.*?):(.*?):(.*?)\\]");

    /**
     * {@inheritDoc}
     */
    public String getBindingType() {
        return "almondplus";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
        logger.warn("validateItemType");
        //if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
        //      throw new BindingConfigParseException("item '" + item.getName()
        //                      + "' is of type '" + item.getClass().getSimpleName()
        //                      + "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
        //}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
        super.processBindingConfiguration(context, item, bindingConfig);
        logger.warn("processBindingConfiguration");
        AlmondPlusClientBindingConfig  config = new AlmondPlusClientBindingConfig();
/*        AlmondPlusClientBindingConfigElement configElement = new AlmondPlusClientBindingConfigElement("0", "0");
        Command command = IN_COMMAND_KEY;
        config.put(command, configElement);
	addBindingConfig(item,config);
*/
        if (bindingConfig != null) {
            config = parseBindingConfig(item, bindingConfig);
            addBindingConfig(item, config);
        }
        else {
            logger.warn("bindingConfig is NULL (item=" + item + ") -> process bindingConfig aborted!");
        }
    }

    protected AlmondPlusClientBindingConfig parseBindingConfig(Item item, String bindingConfig) throws BindingConfigParseException {

        logger.warn("processBindingConfig");
        AlmondPlusClientBindingConfig config = new AlmondPlusClientBindingConfig();
        config.itemType = item.getClass();

        Matcher matcher = BASE_CONFIG_PATTERN.matcher(bindingConfig);

        if (!matcher.matches()){
            throw new BindingConfigParseException("bindingConfig '" + bindingConfig + "' doesn't contain a valid binding configuration");
        }
        matcher.reset();
        while (matcher.find()) {
            String configPart = matcher.group(1);
            if (StringUtils.isNotBlank(configPart)) {
                parseBindingConfig(config, item, configPart);
            }
        }
        return config;
    }

    protected void parseBindingConfig(AlmondPlusClientBindingConfig config, Item item, String bindingConfig) 
        throws BindingConfigParseException {

        Matcher matcher = CONFIG_PATTERN.matcher(bindingConfig);
//        Matcher matcher2 = OUT_CONFIG_PATTERN.matcher(bindingConfig);
        if (!matcher.matches()) {
            throw new BindingConfigParseException(getBindingType() +
                                                  " binding configuration ("+bindingConfig+") must consist of three or four parts [config=" + matcher + "]");
        } else {
            String directionStr = matcher.group(1);
        logger.warn("parseBindingConfig" + matcher);
            if(directionStr.equals(">")) {
		parseCommandConfig(config, item, matcher.group(2));	
	    } else if (directionStr.equals("<")) {
		logger.debug("Matching "+matcher.group(2));
		String match2 = matcher.group(2);
                Matcher matcher2 = IN_CONFIG_PATTERN.matcher(match2);
        if (!matcher2.matches()) {
            throw new BindingConfigParseException(getBindingType() +
                                                  " binding configuration ("+bindingConfig+") must consist of three or four parts [config=" + matcher + "]");
	} else {
		logger.debug("Matching "+matcher2);
                String type = matcher2.group(1);
                String device = matcher2.group(2);
                String index = matcher2.group(3);
                AlmondPlusClientBindingConfigElement configElement = new AlmondPlusClientBindingConfigElement(type,device, index);
                Command command;
		if (type.equals("SWITCH MULTILEVEL")){
		  command = MULTILEVEL_IN_COMMAND_KEY;
		} else {
                  command = BINARY_IN_COMMAND_KEY;
                }
                config.put(command, configElement);
        }
            }
        }
    }
    protected void parseCommandConfig(AlmondPlusClientBindingConfig config, Item item, String bindingConfig) 
        throws BindingConfigParseException {
	   logger.debug("parseCommandConfig: "+bindingConfig);
           Matcher matcher = COMMAND_CONFIG_PATTERN.matcher(bindingConfig);
        if (!matcher.matches()) {
            throw new BindingConfigParseException(getBindingType() +
                                                  " binding configuration ("+bindingConfig+") must consist of three or four parts [config=" + matcher + "]");
        } else {
            String type = matcher.group(1);
            String commandStr = matcher.group(2);
            String device = matcher.group(3);
            String index = matcher.group(4);
            if (logger.isDebugEnabled()) {
                    logger.debug("adding a binding for commandStr={}, device={}, index={}",
                    commandStr, device, index);
            }
            AlmondPlusClientBindingConfigElement configElement = new AlmondPlusClientBindingConfigElement(type,device, index);
            config.put(createCommandFromString(item,commandStr), configElement);
         }
        }
    
    class AlmondPlusClientBindingConfig extends HashMap<Command, AlmondPlusClientBindingConfigElement>implements BindingConfig {
        private static final long serialVersionUID = -108946006112637386L;
        Class<? extends Item> itemType;
    }
    
    static class AlmondPlusClientBindingConfigElement implements BindingConfig {
        private String device;
        private String index;
        private String value;
	private String type;
        
        public AlmondPlusClientBindingConfigElement(String type,String device, String index) {
	    logger.debug("Creating binding: {} {} {}",type,device,index);
            this.type = type;
            this.device = device;
            this.index = index;
        }

        
        public String getDevice() {
            return device;
        }
        public String getType() {
            return type;
        }
        
        public String getIndex() {
            return index;
        }
        public String getValue() {
            return value;
        }

        public void setValue(String newValue) {
            value = newValue;
        }
        
        @Override
        public String toString() {
            return "AlmondPlusClientBindingConfigElement [device=" + device + ", index=" + index + ", value="+ value + "]";
        }
        
    }
    
    private static Command createCommandFromString(Item item, String commandAsString) throws BindingConfigParseException {
	logger.warn(item+"commandAsString: "+commandAsString);
        Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandAsString);

        if (command == null) {
            throw new BindingConfigParseException("couldn't create Command from '" + commandAsString + "' ");
        } 
        
        return command;
    }
    
    @Override
    public String getDevice(String itemName, Command command) {
        logger.warn("getDevice:"+itemName+", "+command );
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
	if (StringUtils.isNumeric(command.toString())) {
           command = MULTILEVEL_IN_COMMAND_KEY;
        }
        logger.warn("getDevice: "+config+" : "+config.get(command)+" : "+config.get(command).getDevice());
        return config != null && config.get(command) != null ? config.get(command).getDevice() : null;
    }
/*
    @Override
    public String getDevice(String itemName) {
        logger.warn("getDevice");
        return getDevice(itemName, IN_COMMAND_KEY);
    }
*/
    @Override
    public String getIndex(String itemName, Command command) {
        logger.warn("getIndex");
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
	if (StringUtils.isNumeric(command.toString())) {
           command = MULTILEVEL_IN_COMMAND_KEY;
        }
        return config != null && config.get(command) != null ? config.get(command).getIndex() : null;
    }
/*
    @Override
    public String getIndex(String itemName) {
        logger.warn("getIndex");
        return getIndex(itemName, IN_COMMAND_KEY);
    }
*/
/*
        @Override
        public String getValue(String itemName) {
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
            return config != null && config.get(IN_COMMAND_KEY) != null ? config.get(IN_COMMAND_KEY).getValue() : null;
        }
*/
        @Override
        public String getType(String itemName,Command command) {
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
	if (StringUtils.isNumeric(command.toString())) {
          command = MULTILEVEL_IN_COMMAND_KEY;
        }
        return config != null && config.get(command) != null ? config.get(command).getType() : null;
        }
/*
        @Override
        public String getType(String itemName) {
        logger.warn("getType");
        return getType(itemName, IN_COMMAND_KEY);
        }
*/
        @Override
        public void setValue(String itemName,String newValue, Command command) {
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
           if ((config != null) && (config.get(command) != null)) { config.get(command).setValue(newValue); }
        }
/*    @Override
    public String getValue(String itemName, Command command) {
        logger.warn("getValue");
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
        return config != null && config.get(command) != null ? config.get(command).getValue() : null;
    }

    @Override
    public String getValue(String itemName) {
        logger.warn("getValue");
        return getValue(itemName, IN_COMMAND_KEY);
    }
*/
    @Override
    public List<String> getItemNames(Command command) {
        logger.warn("getItemNames");
        List<String> bindings = new ArrayList<String>();
        for (String itemName : bindingConfigs.keySet()) {
            AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
            if (config.containsKey(command)) {
            logger.warn("getItemNames:"+itemName+" command:"+command);
                bindings.add(itemName);
            }
        }
        return bindings;
    }

    @Override
    public Class<? extends Item> getItemType(String itemName) {
        logger.warn("getItemType");
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
        return config != null ? config.itemType : null;
    }

    @Override
    public List<Command> getCommands(String itemName) {
        logger.warn("getCommands");
	
        List<Command> commands = new ArrayList<Command>();
        AlmondPlusClientBindingConfig config = (AlmondPlusClientBindingConfig) bindingConfigs.get(itemName);
        commands.addAll(config.keySet());
        return commands;
    }
}
