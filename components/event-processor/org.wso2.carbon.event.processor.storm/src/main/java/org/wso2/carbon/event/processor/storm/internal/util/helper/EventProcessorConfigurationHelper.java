/**
 * Copyright (c) 2005 - 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.event.processor.storm.internal.util.helper;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.wso2.carbon.event.processor.storm.ExecutionPlanConfiguration;
import org.wso2.carbon.event.processor.storm.StreamConfiguration;
import org.wso2.carbon.event.processor.storm.exception.ExecutionPlanConfigurationException;
import org.wso2.carbon.event.processor.storm.internal.util.StormProcessorConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EventProcessorConfigurationHelper {

    public static final String SIDDHI_STREAM_REGEX = "[a-zA-Z0-9_]+";
    public static final String DATABRIDGE_STREAM_REGEX = "[a-zA-Z0-9_\\.]+";
    public static final String STREAM_VER_REGEX = "([0-9]*)\\.([0-9]*)\\.([0-9]*)";

    public static ExecutionPlanConfiguration fromOM(OMElement executionPlanConfigElement) {
        ExecutionPlanConfiguration executionPlanConfiguration = new ExecutionPlanConfiguration();
        executionPlanConfiguration.setName(executionPlanConfigElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME)));


        Iterator descIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_DESC));
        if (descIterator.hasNext()) {
            OMElement descriptionElement = (OMElement) descIterator.next();
            executionPlanConfiguration.setDescription(descriptionElement.getText());
        }

        Iterator siddhiConfigIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_SIDDHI_CONFIG));
        while (siddhiConfigIterator.hasNext()) {
            Iterator siddhiConfigPropertyIterator = ((OMElement) siddhiConfigIterator.next()).getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_PROPERTY));
            while (siddhiConfigPropertyIterator.hasNext()) {
                OMElement configPropertyElement = (OMElement) siddhiConfigPropertyIterator.next();
                executionPlanConfiguration.addSiddhiConfigurationProperty(configPropertyElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME)), configPropertyElement.getText());
            }
        }

        Iterator allImportedStreamsIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_IMP_STREAMS));
        while (allImportedStreamsIterator.hasNext()) {
            Iterator importedStreamIterator = ((OMElement) allImportedStreamsIterator.next()).getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_STREAM));
            while (importedStreamIterator.hasNext()) {
                OMElement importedStream = (OMElement) importedStreamIterator.next();
                String version = importedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_VERSION));
                StreamConfiguration streamConfiguration;
                if (version != null) {
                    streamConfiguration = new StreamConfiguration(importedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME)), version);
                } else {
                    streamConfiguration = new StreamConfiguration(importedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME)));
                }
                OMAttribute as = importedStream.getAttribute(new QName(StormProcessorConstants.EP_ATTR_AS));
                if (as != null && as.getAttributeValue() != null && as.getAttributeValue().trim().length() > 0) {
                    streamConfiguration.setSiddhiStreamName(as.getAttributeValue());
                }

                executionPlanConfiguration.addImportedStream(streamConfiguration); // todo validate
            }
        }

        Iterator allExportedStreamsIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_EXP_STREAMS));
        while (allExportedStreamsIterator.hasNext()) {
            Iterator exportedStreamIterator = ((OMElement) allExportedStreamsIterator.next()).getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_STREAM));
            while (exportedStreamIterator.hasNext()) {
                OMElement exportedStream = (OMElement) exportedStreamIterator.next();
                StreamConfiguration streamConfiguration = new StreamConfiguration(exportedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME)), exportedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_VERSION)), exportedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_VALUEOF)));
                executionPlanConfiguration.addExportedStream(streamConfiguration);
                OMAttribute valueOf = exportedStream.getAttribute(new QName(StormProcessorConstants.EP_ATTR_VALUEOF));
                if (valueOf != null && valueOf.getAttributeValue() != null && valueOf.getAttributeValue().trim().length() > 0) {
                    streamConfiguration.setSiddhiStreamName(valueOf.getAttributeValue());
                }
            }
        }

        Iterator queryIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_QUERIES));
        if (queryIterator.hasNext()) {
            executionPlanConfiguration.setQueryExpressions(((OMElement) queryIterator.next()).getText());
        }

        if (executionPlanConfigElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_STATISTICS)) != null && executionPlanConfigElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_STATISTICS)).equals(StormProcessorConstants.EP_ENABLE)) {
            executionPlanConfiguration.setStatisticsEnabled(true);
        }

        if (executionPlanConfigElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_TRACING)) != null && executionPlanConfigElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_TRACING)).equals(StormProcessorConstants.EP_ENABLE)) {
            executionPlanConfiguration.setTracingEnabled(true);
        }
        return executionPlanConfiguration;
    }

    public static OMElement toOM(ExecutionPlanConfiguration executionPlanConfiguration) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement executionPlan = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_ROOT_ELEMENT));
        executionPlan.declareDefaultNamespace(StormProcessorConstants.EP_CONF_NS);
        executionPlan.addAttribute(StormProcessorConstants.EP_ATTR_NAME, executionPlanConfiguration.getName(), null);

        OMElement description = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_DESC));
        description.setNamespace(executionPlan.getDefaultNamespace());
        description.setText(executionPlanConfiguration.getDescription());
        executionPlan.addChild(description);

        OMElement siddhiConfiguration = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_SIDDHI_CONFIG));
        siddhiConfiguration.setNamespace(executionPlan.getDefaultNamespace());
        for (Map.Entry<String, String> entry : executionPlanConfiguration.getSiddhiConfigurationProperties().entrySet()) {
            OMElement propertyElement = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_PROPERTY));
            propertyElement.setNamespace(executionPlan.getDefaultNamespace());
            propertyElement.addAttribute(StormProcessorConstants.EP_ATTR_NAME, entry.getKey(), null);
            propertyElement.setText(entry.getValue());
            siddhiConfiguration.addChild(propertyElement);
        }
        executionPlan.addChild(siddhiConfiguration);

        OMElement importedStreams = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_IMP_STREAMS));
        importedStreams.setNamespace(executionPlan.getDefaultNamespace());
        for (StreamConfiguration stream : executionPlanConfiguration.getImportedStreams()) {
            OMElement streamElement = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_STREAM));
            streamElement.setNamespace(executionPlan.getDefaultNamespace());
            streamElement.addAttribute(StormProcessorConstants.EP_ATTR_NAME, stream.getName(), null);
            if (stream.getSiddhiStreamName() != null) {
                streamElement.addAttribute(StormProcessorConstants.EP_ATTR_AS, stream.getSiddhiStreamName(), null);
            }
            if (stream.getVersion() != null) {
                streamElement.addAttribute(StormProcessorConstants.EP_ATTR_VERSION, stream.getVersion(), null);
            }

            importedStreams.addChild(streamElement);
        }
        executionPlan.addChild(importedStreams);

        OMElement queries = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_QUERIES));
        queries.setNamespace(executionPlan.getDefaultNamespace());
        factory.createOMText(queries, executionPlanConfiguration.getQueryExpressions(),
                XMLStreamReader.CDATA);
        executionPlan.addChild(queries);

        OMElement exportedStreams = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_EXP_STREAMS));
        exportedStreams.setNamespace(executionPlan.getDefaultNamespace());
        for (StreamConfiguration stream : executionPlanConfiguration.getExportedStreams()) {
            OMElement streamElement = factory.createOMElement(new QName(StormProcessorConstants.EP_ELE_STREAM));
            streamElement.setNamespace(executionPlan.getDefaultNamespace());
            streamElement.addAttribute(StormProcessorConstants.EP_ATTR_NAME, stream.getName(), null);
            if (stream.getSiddhiStreamName() != null) {
                streamElement.addAttribute(StormProcessorConstants.EP_ATTR_VALUEOF, stream.getSiddhiStreamName(), null);
            }
            if (stream.getVersion() != null) {
                streamElement.addAttribute(StormProcessorConstants.EP_ATTR_VERSION, stream.getVersion(), null);
            }

            exportedStreams.addChild(streamElement);
        }
        executionPlan.addChild(exportedStreams);

        if (executionPlanConfiguration.isStatisticsEnabled()) {
            executionPlan.addAttribute(StormProcessorConstants.EP_ATTR_STATISTICS, StormProcessorConstants.EP_ENABLE, null);
        } else {
            executionPlan.addAttribute(StormProcessorConstants.EP_ATTR_STATISTICS, StormProcessorConstants.EP_DISABLE, null);
        }

        if (executionPlanConfiguration.isTracingEnabled()) {
            executionPlan.addAttribute(StormProcessorConstants.EP_ATTR_TRACING, StormProcessorConstants.EP_ENABLE, null);
        } else {
            executionPlan.addAttribute(StormProcessorConstants.EP_ATTR_TRACING, StormProcessorConstants.EP_DISABLE, null);
        }

        return executionPlan;
    }

    public static void validateExecutionPlanConfiguration(OMElement executionPlanConfigElement) throws ExecutionPlanConfigurationException {
        if (!executionPlanConfigElement.getQName().getLocalPart().equals(StormProcessorConstants.EP_ELE_ROOT_ELEMENT)) {
            throw new ExecutionPlanConfigurationException("Invalid root element expected:" + StormProcessorConstants.EP_ELE_ROOT_ELEMENT + " found:" + executionPlanConfigElement.getQName().getLocalPart());
        }

        String name;
        try {
            name = executionPlanConfigElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME));
        } catch (Exception e) {
            throw new ExecutionPlanConfigurationException("Execution plan name can't be null.");
        }
        if (name == null) {
            throw new ExecutionPlanConfigurationException("Execution plan name can't be null.");
        } else if (name.trim().contains(" ")) {
            throw new ExecutionPlanConfigurationException("Execution plan name can't have spaces.");
        }

        Iterator descIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_DESC));
        if (!descIterator.hasNext()) {
            throw new ExecutionPlanConfigurationException("No description available:" + name);
        }

        HashMap<String, String> siddhiConfigParams = new HashMap<String, String>();
        Iterator siddhiConfigIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_SIDDHI_CONFIG));
        while (siddhiConfigIterator.hasNext()) {
            Iterator siddhiConfigPropertyIterator = ((OMElement) siddhiConfigIterator.next()).getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_PROPERTY));
            while (siddhiConfigPropertyIterator.hasNext()) {
                OMElement configPropertyElement = (OMElement) siddhiConfigPropertyIterator.next();
                siddhiConfigParams.put(configPropertyElement.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME)), configPropertyElement.getText());
            }
        }

        int siddhiSnapshotTime;
        try {
            siddhiSnapshotTime = Integer.parseInt(siddhiConfigParams.get(StormProcessorConstants.SIDDHI_SNAPSHOT_INTERVAL).trim());
            if (siddhiSnapshotTime < 0) {
                throw new ExecutionPlanConfigurationException("Invalid Siddhi snapshot time interval in execution plan:" + name);
            }
            // TODO enable when distributed processing is available.
//            Boolean.parseBoolean(siddhiConfigParams.get(StormProcessorConstants.SIDDHI_DISTRIBUTED_PROCESSING).trim());
        } catch (NumberFormatException e) {
            throw new ExecutionPlanConfigurationException("Invalid Siddhi snapshot time interval specified in execution plan : " + name);
        }

        Pattern siddhiStreamNamePattern = Pattern.compile(SIDDHI_STREAM_REGEX);
        Pattern databridgeStreamNamePattern = Pattern.compile(DATABRIDGE_STREAM_REGEX);
        Pattern streamVersionPattern = Pattern.compile(STREAM_VER_REGEX);
        Iterator allImportedStreamsIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_IMP_STREAMS));
        while (allImportedStreamsIterator.hasNext()) {
            Iterator importedStreamIterator = ((OMElement) allImportedStreamsIterator.next()).getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_STREAM));
            while (importedStreamIterator.hasNext()) {
                OMElement importedStream = (OMElement) importedStreamIterator.next();
                String version = importedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_VERSION));
                if (version != null && version.trim().length() > 0) {
                    Matcher m = streamVersionPattern.matcher(version.trim());
                    if (!m.matches()) {
                        throw new ExecutionPlanConfigurationException("Invalid stream version [" + version + "] in execution plan: " + name);
                    }
                }
                String streamName = importedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME));
                if (streamName == null || streamName.length() < 1 ||
                        (!databridgeStreamNamePattern.matcher(streamName.trim()).matches())) {
                    throw new ExecutionPlanConfigurationException("Invalid imported stream name[" + streamName + "] in execution plan:" + name);
                }

                OMAttribute as = importedStream.getAttribute(new QName(StormProcessorConstants.EP_ATTR_AS));
                if (as != null && as.getAttributeValue() != null) {
                    if (!siddhiStreamNamePattern.matcher(as.getAttributeValue().trim()).matches()) {
                        throw new ExecutionPlanConfigurationException("Invalid imported stream name as [" + streamName + "] in execution plan:" + name);
                    }
                }
            }
        }

        Iterator allExportedStreamsIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_EXP_STREAMS));
        while (allExportedStreamsIterator.hasNext()) {
            Iterator exportedStreamIterator = ((OMElement) allExportedStreamsIterator.next()).getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_STREAM));
            while (exportedStreamIterator.hasNext()) {
                OMElement exportedStream = (OMElement) exportedStreamIterator.next();

                OMAttribute valueOf = exportedStream.getAttribute(new QName(StormProcessorConstants.EP_ATTR_VALUEOF));
                if (valueOf == null || valueOf.getAttributeValue() == null ||
                        valueOf.getAttributeValue().trim().length() < 1 ||
                        (!siddhiStreamNamePattern.matcher(valueOf.getAttributeValue().trim()).matches())) {
                    throw new ExecutionPlanConfigurationException("Invalid exported stream valueOf [" + valueOf.getAttributeValue() + "] in execution plan:" + name);

                }

                String version = exportedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_VERSION));
                if (version != null && version.trim().length() > 0) {
                    if (!streamVersionPattern.matcher(version.trim()).matches()) {
                        throw new ExecutionPlanConfigurationException("Invalid stream version [" + version + "] in execution plan: " + name);
                    }
                }

                String streamName = exportedStream.getAttributeValue(new QName(StormProcessorConstants.EP_ATTR_NAME));
                if (streamName != null && streamName.length() > 0) {
                    if (!databridgeStreamNamePattern.matcher(streamName.trim()).matches()) {
                        throw new ExecutionPlanConfigurationException("Invalid exported stream name[" + streamName + "] in execution plan:" + name);
                    }
                }

            }
        }

        Iterator queryIterator = executionPlanConfigElement.getChildrenWithName(new QName(StormProcessorConstants.EP_CONF_NS, StormProcessorConstants.EP_ELE_QUERIES));
        if (queryIterator.hasNext()) {
            String query = ((OMElement) queryIterator.next()).getText();
            if (query == null || query.trim().length() < 1)
                throw new ExecutionPlanConfigurationException("Invalid execution plan with no queries: " + name);
        } else {
            throw new ExecutionPlanConfigurationException("Invalid execution plan with no queries: " + name);

        }
    }

}
