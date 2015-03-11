/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.event.output.adapter.websocket.local;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapter;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapterConfiguration;
import org.wso2.carbon.event.output.adapter.core.exception.OutputEventAdapterException;
import org.wso2.carbon.event.output.adapter.core.exception.TestConnectionNotSupportedException;
import org.wso2.carbon.event.output.adapter.websocket.local.internal.WebsocketLocalOutputCallbackRegisterServiceInternal;
import org.wso2.carbon.event.output.adapter.websocket.local.internal.ds.WebsocketLocalEventAdaptorServiceInternalValueHolder;
import org.wso2.carbon.event.output.adapter.websocket.local.internal.util.WebsocketLocalEventAdapterConstants;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WebsocketLocalEventAdapter implements OutputEventAdapter {

    private static final Log log = LogFactory.getLog(WebsocketLocalEventAdapter.class);
    private OutputEventAdapterConfiguration eventAdapterConfiguration;
    private Map<String, String> globalProperties;

    private int tenantID;

    public WebsocketLocalEventAdapter(OutputEventAdapterConfiguration eventAdapterConfiguration, Map<String, String> globalProperties) {
        this.eventAdapterConfiguration = eventAdapterConfiguration;
        this.globalProperties = globalProperties;
        this.tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    }


    @Override
    public void init() throws OutputEventAdapterException {
        //Nothing to initialize.
    }

    @Override
    public void testConnect() throws TestConnectionNotSupportedException {
        //Not applicable.
    }

    @Override
    public void connect() {
        //Nothing to do.
    }

    @Override
    public void publish(Object message, Map<String, String> dynamicProperties) {
        String topic = eventAdapterConfiguration.getStaticProperties().get(WebsocketLocalEventAdapterConstants.ADAPTER_TOPIC);
        WebsocketLocalOutputCallbackRegisterServiceInternal websocketLocalOutputCallbackRegisterServiceInternal = WebsocketLocalEventAdaptorServiceInternalValueHolder.getWebsocketLocalOutputCallbackRegisterServiceInternal();
        CopyOnWriteArrayList<Session> sessions = websocketLocalOutputCallbackRegisterServiceInternal.getSessions(tenantID, eventAdapterConfiguration.getName(), topic);
        if (sessions != null){
            if (message instanceof Object[]) {
                //TODO: should these be sent one by one or as a whole? My suggestion is that we define a new schema and convert Object[] into it,
                // and send the converted string as one message to the receiving party, so they can adopt the schema and parse the message. Saves expensive communication overhead.
                for (Object object : (Object[])message){
                    for (Session session : sessions){
                        synchronized (session){
                            session.getAsyncRemote().sendText(message.toString());  //this method call was synchronized to fix CEP-996
                        }
                    }
                }
            } else {
                for (Session session : sessions){
                    synchronized (session){
                        session.getAsyncRemote().sendText(message.toString());  //this method call was synchronized to fix CEP-996
                    }
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Dropping the message: '"+message+"', since no clients have being registered to receive events from websocket-local adapter: '"
                        +eventAdapterConfiguration.getName()+"', for tenant ID: "+tenantID+", for topic: "+topic);
            }
        }
    }

    @Override
    public void disconnect() {
        //Nothing to do.
    }

    @Override
    public void destroy() {
        //Nothing to be destroyed.
    }
}
