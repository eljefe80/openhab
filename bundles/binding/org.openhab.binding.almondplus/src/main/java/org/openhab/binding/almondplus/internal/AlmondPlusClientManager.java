package org.openhab.binding.almondplus.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.openhab.binding.almondplus.AlmondPlusClientBindingProvider;

//REVISIT if this is a good way of managing the websockets used by all the configured items
class AlmondPlusClientManager {
    private AlmondPlusClientBinding clientBinding;
    // a map to store a client for the given url
    private Map<String, AlmondPlusClient> urlclients;
    // a map to store all clients associted for the given itemName
    private Map<String, List<AlmondPlusClient>> itemclients;
    private static Logger logger = LoggerFactory.getLogger(AlmondPlusClient.class);
    public AlmondPlusClientManager(AlmondPlusClientBinding clientBinding) {
        this.clientBinding = clientBinding;
    }

    /**
     * Add a websocket client for the specified url if there is previously no
     * websocket client with this url has been added for this item.
     * 
     * @param itemName
     * @param url
     * @param listener
     */
    public AlmondPlusClient add(String itemName, String url, AlmondPlusClientBindingProvider provider) {
        AlmondPlusClient wc = null;
        synchronized (this) {
            List<AlmondPlusClient> icls = itemclients.get(itemName);
            if (icls == null) {
                icls = new LinkedList<AlmondPlusClient>();
                itemclients.put(itemName, icls);
            }
            for (AlmondPlusClient iwc : icls) {
                if (url.equals(iwc.getURL())) {
                    wc = iwc;
                    break;
                }
            }
            if (wc == null) {
                wc = urlclients.get(url);
                if (wc == null) {
                    //TODO supply a custom configuration
                    wc = new AlmondPlusClient(url, null, this);
			logger.debug("adding a new websocket {}",url);
                    urlclients.put(url,  wc);
                }
                icls.add(wc);
            }
            // increment the reference count for the inbound item referencing this client
            wc.register(itemName, provider);
        }
        return wc;
    }

    /**
     * Remove the websocket clients previously assigned to the specified item
     * and close and remove them if they are no longer assigned to any other item.
     * @param itemName
     */
    public void remove(String itemName) {
        synchronized (this) {
            List<AlmondPlusClient> icls = itemclients.remove(itemName);
            if (icls != null) {
                for (AlmondPlusClient iwc : icls) {
                    iwc.unregister(itemName);
                    if (iwc.getReferenceCount() == 0) {
                        iwc.close();
                        urlclients.remove(iwc.getURL());
                    }
                }
            }
        }
    }
        
    public AlmondPlusClient getAlmondPlusClient(String URL) {
        return urlclients.get(URL);
    }
        
    public Collection<AlmondPlusClient> getAllAlmondPlusClients() {
        return urlclients.values();
    }

    public void postMessage(String itemName, String message, AlmondPlusClientBindingProvider provider) {
        clientBinding.postToBus(itemName, message, provider);
    }
        
    public void init() {
        urlclients = Collections.synchronizedMap(new HashMap<String, AlmondPlusClient>());       
        itemclients = new HashMap<String, List<AlmondPlusClient>>();     
    }
        
    public void release() {
        for (AlmondPlusClient wc : urlclients.values()) {
            wc.close();
        }
        urlclients = null;
        itemclients = null;
    }
}
