/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.almondplus;

import java.util.List;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

/**
 * @author elakito
 * @since 1.6.0
 */
public interface AlmondPlusClientBindingProvider extends BindingProvider {
    String getDevice(String itemName, Command command);
//    String getDevice(String itemName);
    String getIndex(String itemName, Command command);
//    String getIndex(String itemName);
    String getType(String itemName, Command command);
//    String getType(String itemName);
    void setValue(String itemName,String value, Command command);
//    String getValue(String itemName);
    List<String> getItemNames(Command command);
    Class<? extends Item> getItemType(String itemName);
    List<Command> getCommands(String itemName);
}
