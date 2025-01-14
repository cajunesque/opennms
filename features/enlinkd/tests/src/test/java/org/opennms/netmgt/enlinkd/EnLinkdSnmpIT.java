/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.enlinkd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.snmp.annotations.JUnitSnmpAgent;
import org.opennms.core.test.snmp.annotations.JUnitSnmpAgents;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.LldpUtils.LldpChassisIdSubType;
import org.opennms.core.utils.LldpUtils.LldpPortIdSubType;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.enlinkd.model.BridgeElement;
import org.opennms.netmgt.enlinkd.model.BridgeStpLink;
import org.opennms.netmgt.enlinkd.model.IpNetToMedia;
import org.opennms.netmgt.enlinkd.model.IsIsElement;
import org.opennms.netmgt.enlinkd.model.IsIsLink;
import org.opennms.netmgt.enlinkd.model.LldpElement;
import org.opennms.netmgt.enlinkd.model.LldpLink;
import org.opennms.netmgt.enlinkd.model.OspfElement;
import org.opennms.netmgt.enlinkd.model.OspfLink;
import org.opennms.netmgt.enlinkd.model.BridgeElement.BridgeDot1dBaseType;
import org.opennms.netmgt.enlinkd.model.BridgeElement.BridgeDot1dStpProtocolSpecification;
import org.opennms.netmgt.enlinkd.model.BridgeStpLink.BridgeDot1dStpPortEnable;
import org.opennms.netmgt.enlinkd.model.BridgeStpLink.BridgeDot1dStpPortState;
import org.opennms.netmgt.enlinkd.model.IpNetToMedia.IpNetToMediaType;
import org.opennms.netmgt.enlinkd.model.IsIsElement.IsisAdminState;
import org.opennms.netmgt.enlinkd.model.IsIsLink.IsisISAdjNeighSysType;
import org.opennms.netmgt.enlinkd.model.IsIsLink.IsisISAdjState;
import org.opennms.netmgt.enlinkd.model.OspfElement.Status;
import org.opennms.netmgt.enlinkd.model.OspfElement.TruthValue;
import org.opennms.netmgt.enlinkd.service.api.BridgeForwardingTableEntry;
import org.opennms.netmgt.enlinkd.service.api.BridgeForwardingTableEntry.BridgeDot1qTpFdbStatus;
import org.opennms.netmgt.enlinkd.snmp.*;
import org.opennms.netmgt.enlinkd.snmp.Dot1dBasePortTableTracker.Dot1dBasePortRow;
import org.opennms.netmgt.nb.NmsNetworkBuilder;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.proxy.LocationAwareSnmpClient;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.*;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations= {
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-mockDao.xml",
        "classpath:/META-INF/opennms/applicationContext-proxy-snmp.xml"
})
@JUnitConfigurationEnvironment
public class EnLinkdSnmpIT extends NmsNetworkBuilder implements InitializingBean {
    
    @Autowired
    LocationAwareSnmpClient m_client;
    private final static Logger LOG = LoggerFactory.getLogger(EnLinkdSnmpIT.class);
    
    @Override
    public void afterPropertiesSet() {
    }

    @Before
    public void setUp() throws Exception {
        Properties p = new Properties();
        p.setProperty("log4j.logger.org.opennms.mock.snmp", "WARN");
        p.setProperty("log4j.logger.org.opennms.netmgt.snmp", "WARN");
        p.setProperty("log4j.logger.org.springframework","WARN");
        p.setProperty("log4j.logger.com.mchange.v2.resourcepool", "WARN");
        MockLogAppender.setupLogging(p);
    }

    @Test
    public void testInSameNetwork() throws Exception {
        assertTrue(InetAddressUtils.inSameNetwork(InetAddress.getByName("192.168.0.1"),
                InetAddress.getByName("192.168.0.2"), InetAddress.getByName("255.255.255.252")));
        assertFalse(InetAddressUtils.inSameNetwork(InetAddress.getByName("192.168.0.1"),
                InetAddress.getByName("192.168.0.5"), InetAddress.getByName("255.255.255.252")));
        assertTrue(InetAddressUtils.inSameNetwork(InetAddress.getByName("10.10.0.1"),
                InetAddress.getByName("10.168.0.5"), InetAddress.getByName("255.0.0.0")));
    }
    
    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = RPict001_IP, port = 161, resource = RPict001_SNMP_RESOURCE)
    })
    public void testCdpInterfaceGetter() throws Exception {
        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(RPict001_IP));
        CdpInterfacePortNameGetter get = new CdpInterfacePortNameGetter(config,m_client,null,0);

        assertEquals("FastEthernet0", get.getInterfaceNameFromCiscoCdpMib(1).toDisplayString());
        assertEquals("FastEthernet1", get.getInterfaceNameFromCiscoCdpMib(2).toDisplayString());
        assertEquals("FastEthernet2", get.getInterfaceNameFromCiscoCdpMib(3).toDisplayString());
        assertEquals("FastEthernet3", get.getInterfaceNameFromCiscoCdpMib(4).toDisplayString());
        assertEquals("FastEthernet4", get.getInterfaceNameFromCiscoCdpMib(5).toDisplayString());
        assertEquals("Tunnel0", get.getInterfaceNameFromCiscoCdpMib(9).toDisplayString());
        assertEquals("Tunnel3", get.getInterfaceNameFromCiscoCdpMib(10).toDisplayString());

        assertEquals("FastEthernet0", get.getInterfaceNameFromMib2(1).toDisplayString());
        assertEquals("FastEthernet1", get.getInterfaceNameFromMib2(2).toDisplayString());
        assertEquals("FastEthernet2", get.getInterfaceNameFromMib2(3).toDisplayString());
        assertEquals("FastEthernet3", get.getInterfaceNameFromMib2(4).toDisplayString());
        assertEquals("FastEthernet4", get.getInterfaceNameFromMib2(5).toDisplayString());
        assertEquals("Tunnel0", get.getInterfaceNameFromMib2(9).toDisplayString());
        assertEquals("Tunnel3", get.getInterfaceNameFromMib2(10).toDisplayString());

    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = RPict001_IP, port = 161, resource = RPict001_SNMP_RESOURCE)
    })
    public void testCdpGlobalGroupCollection() throws Exception {
        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(RPict001_IP));

        String trackerName = "cdpGlobalGroup";

        final CdpGlobalGroupTracker cdpGlobalGroup = new CdpGlobalGroupTracker();

        try {
            m_client.walk(config,cdpGlobalGroup)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: Cdp Linkd collection interrupted, exiting",e);
            return;
        }

        assertEquals("r-ro-suce-pict-001.infra.u-ssi.net",cdpGlobalGroup.getCdpDeviceId());
        assertEquals(1,cdpGlobalGroup.getCdpGlobalRun().intValue());
        assertNull(cdpGlobalGroup.getCdpGlobalDeviceFormat());

    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = CISCO_WS_C2948_IP, port = 161, resource = CISCO_WS_C2948_SNMP_RESOURCE )
    })
    public void testCdpGlobalGroupCollectionWithGlobalIdFormat() throws Exception {
        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(CISCO_WS_C2948_IP));

        String trackerName = "cdpGlobalGroup";

        final CdpGlobalGroupTracker cdpGlobalGroup = new CdpGlobalGroupTracker();

        try {
            m_client.walk(config,cdpGlobalGroup)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: Cdp Linkd collection interrupted, exiting",e);
            return;
        }

        assertEquals("JAB043408B7",cdpGlobalGroup.getCdpDeviceId());
        assertEquals(1,cdpGlobalGroup.getCdpGlobalRun().intValue());
        assertEquals(3,cdpGlobalGroup.getCdpGlobalDeviceFormat().intValue());

    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = RPict001_IP, port = 161, resource = RPict001_SNMP_RESOURCE)
    })
    public void testCdpCacheTableCollection() throws Exception {
        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(RPict001_IP));

        class CdpCacheTableTrackerTester extends CdpCacheTableTracker {
            int count = 0;
            public int count() {
                return count;
            }
        }
        final CdpCacheTableTrackerTester cdpCacheTableTracker = new CdpCacheTableTrackerTester() {

            public void processCdpCacheRow(final CdpCacheRow row) {
                count++;
            }
            
        };

        String trackerName = "cdpCacheTable";

        try {
            m_client.walk(config,cdpCacheTableTracker)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: Cdp Linkd collection interrupted, exiting",e);
            return;
        }
        
        assertEquals(14, cdpCacheTableTracker.count());
    }

    
    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH1_IP, port=161, resource="classpath:/linkd/nms17216/switch1-walk.txt")
    })
    public void testOspfGeneralGroupWalk() throws Exception {
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH1_IP));
    	String trackerName = "ospfGeneralGroup";

        final OspfGeneralGroupTracker ospfGeneralGroup = new OspfGeneralGroupTracker();

        try {
            m_client.walk(config,ospfGeneralGroup)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }
        
        OspfElement ospfElement = ospfGeneralGroup.getOspfElement();
        assertEquals(InetAddress.getByName("192.168.100.246"), ospfElement.getOspfRouterId());
        assertNull(ospfElement.getOspfRouterIdNetmask());
        assertNull(ospfElement.getOspfRouterIdIfindex());
        assertEquals(Status.enabled, ospfElement.getOspfAdminStat());
        assertEquals(2, ospfElement.getOspfVersionNumber().intValue());
        assertEquals(TruthValue.FALSE, ospfElement.getOspfBdrRtrStatus());
        assertEquals(TruthValue.FALSE, ospfElement.getOspfASBdrRtrStatus());

        final OspfIpAddrTableGetter ipAddrTableGetter = new OspfIpAddrTableGetter(config,m_client,null,0);

        OspfElement ospfElementN = ipAddrTableGetter.get(ospfElement);
        assertEquals(InetAddress.getByName("192.168.100.246"), ospfElementN.getOspfRouterId());
        assertEquals(InetAddress.getByName("255.255.255.252"), ospfElementN.getOspfRouterIdNetmask());
        assertEquals(10101, ospfElementN.getOspfRouterIdIfindex().intValue());
        assertEquals(Status.enabled, ospfElementN.getOspfAdminStat());
        assertEquals(2, ospfElementN.getOspfVersionNumber().intValue());
        assertEquals(TruthValue.FALSE, ospfElementN.getOspfBdrRtrStatus());
        assertEquals(TruthValue.FALSE, ospfElementN.getOspfASBdrRtrStatus());

    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH1_IP, port=161, resource="classpath:/linkd/nms17216/switch1-walk.txt")
    })
    public void testOspfNbrTableWalk() throws Exception {
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH1_IP));
        String trackerName = "ospfNbrTable";
        OspfNbrTableTracker ospfNbrTableTracker = new OspfNbrTableTracker() {

        	public void processOspfNbrRow(final OspfNbrRow row) {
        		OspfLink link = row.getOspfLink();
        		try {
					assertEquals(InetAddress.getByName("192.168.100.249"), link.getOspfRemRouterId());
	        		assertEquals(InetAddress.getByName("192.168.100.245"), link.getOspfRemIpAddr());
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
        		assertEquals(0, link.getOspfRemAddressLessIndex().intValue());
        	}
        };

        try {
            m_client.walk(config,ospfNbrTableTracker)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
        }
    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH1_IP, port=161, resource="classpath:/linkd/nms17216/switch1-walk.txt")
    })
    public void testOspfIfTableWalk() throws Exception {
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH1_IP));
        String trackerName = "ospfIfTable";
        final List<OspfLink> links = new ArrayList<>();
        OspfIfTableTracker ospfIfTableTracker = new OspfIfTableTracker() {

        	public void processOspfIfRow(final OspfIfRow row) {
        		links.add(row.getOspfLink());
         	}
        };

        try {
            m_client.walk(config,ospfIfTableTracker)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }
        
        final OspfIpAddrTableGetter ipAddrTableGetter = new OspfIpAddrTableGetter(config,m_client,null,0);
        for (OspfLink link: links) {
                link = ipAddrTableGetter.get(link);
			assertEquals(0, link.getOspfAddressLessIndex().intValue());
			if (link.getOspfIpAddr().equals(InetAddress.getByName("192.168.100.246"))) {
				assertEquals(10101, link.getOspfIfIndex().intValue());
				assertEquals(InetAddress.getByName("255.255.255.252"), link.getOspfIpMask());
			} else if (link.getOspfIpAddr().equals(InetAddress.getByName("172.16.10.1"))){
				assertEquals(10, link.getOspfIfIndex().intValue());
				assertEquals(InetAddress.getByName("255.255.255.0"), link.getOspfIpMask());
			} else if (link.getOspfIpAddr().equals(InetAddress.getByName("172.16.20.1"))){
				assertEquals(20, link.getOspfIfIndex().intValue());
				assertEquals(InetAddress.getByName("255.255.255.0"), link.getOspfIpMask());
			} else if (link.getOspfIpAddr().equals(InetAddress.getByName("172.16.30.1"))){
				assertEquals(30, link.getOspfIfIndex().intValue());
				assertEquals(InetAddress.getByName("255.255.255.0"), link.getOspfIpMask());
			} else if (link.getOspfIpAddr().equals(InetAddress.getByName("172.16.40.1"))){
				assertEquals(40, link.getOspfIfIndex().intValue());
				assertEquals(InetAddress.getByName("255.255.255.0"), link.getOspfIpMask());
			} else {
                fail();
			}

        }
    }

    /**
     * This test is designed to test the issues in bug NMS-6921.
     * 
     * @see "https://issues.opennms.org/browse/NMS-6912"
     */
    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=DW_IP, port=161, resource=DW_SNMP_RESOURCE)
    })
    public void testLldpDragonWaveLocalGroupWalk() throws Exception {

        String trackerName = "lldpLocalGroup";
        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DW_IP));
                LldpLocalGroupTracker lldpLocalGroup = new LldpLocalGroupTracker();

        try {
            m_client.walk(config,lldpLocalGroup)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        LldpElement eiA = lldpLocalGroup.getLldpElement();
        System.err.println("local chassis type: " + LldpChassisIdSubType.getTypeString(eiA.getLldpChassisIdSubType().getValue()));
        System.err.println("local chassis id: " + eiA.getLldpChassisId());
        System.err.println("local sysname: " + eiA.getLldpSysname());
        
        assertEquals("cf", eiA.getLldpChassisId());
        assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_CHASSISCOMPONENT, eiA.getLldpChassisIdSubType());
        assertEquals("NuDesign", eiA.getLldpSysname());
    }

    /**
     * This test is designed to test the issues in bug NMS-6921.
     * 
     * @see "https://issues.opennms.org/browse/NMS-6912"

     */
    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=DW_IP, port=161, resource=DW_SNMP_RESOURCE)
    })
    public void testLldpDragonWaveLldpLocGetter() throws Exception {

        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DW_IP));
                
        final LldpLocPortGetter lldpLocPort = new LldpLocPortGetter(config,m_client,null,0);
        
                LldpLink link = new LldpLink();
                link.setLldpLocalPortNum(1);
                link = lldpLocPort.getLldpLink(link);
                assertEquals(1, link.getLldpLocalPortNum().intValue());
                assertEquals("cf", link.getLldpPortId());
                assertEquals("NuDesign", link.getLldpPortDescr());
                assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACEALIAS, link.getLldpPortIdSubType());
    }

    @Test
    @JUnitSnmpAgent(host=DW_IP, port=161, resource=DW_SNMP_RESOURCE)
    public void testLldpDragonWaveRemTableWalk() throws Exception {

        final SnmpAgentConfig config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DW_IP));
        LldpRemTableTracker lldpRemTable = new LldpRemTableTracker() {

            public void processLldpRemRow(final LldpRemRow row) {

                System.err.println("----------lldp rem----------------");
                System.err.println("columns number in the row: "
                        + row.getColumnCount());

                assertEquals(6, row.getColumnCount());
                LldpLink link = row.getLldpLink();

                assertEquals(1, row.getLldpRemLocalPortNum().intValue());
                System.err.println("local port number: "
                        + row.getLldpRemLocalPortNum());

                assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_CHASSISCOMPONENT,
                             link.getLldpRemChassisIdSubType());
                System.err.println("remote chassis type: "
                        + LldpChassisIdSubType.getTypeString(link.getLldpRemChassisIdSubType().getValue()));

                assertEquals("cf", link.getLldpRemChassisId());
                System.err.println("remote chassis: "
                        + link.getLldpRemChassisId());

                assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACEALIAS,
                             link.getLldpRemPortIdSubType());
                System.err.println("remote port type: "
                        + LldpPortIdSubType.getTypeString(link.getLldpRemPortIdSubType().getValue()));


                assertEquals("cf", link.getLldpRemPortId());
                System.err.println("remote port id: "
                        + link.getLldpRemPortId());

                assertEquals("NuDesign", link.getLldpRemPortDescr());
                System.err.println("remote port descr: "
                        + link.getLldpRemPortDescr());
                

                assertEquals("NuDesign", link.getLldpRemSysname());
                System.err.println("remote sysname: "
                        + link.getLldpRemSysname());

            }
        };
        String trackerName = "lldpRemTable";

        try {
            m_client.walk(config,lldpRemTable)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            fail();
        }

    }

    /**
     * This test is designed to test the issues in bug NMS-13593.
     *
     * @see "https://issues.opennms.org/browse/NMS-13593"

     *
     */
    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=ZHBGO1Zsr001_IP, port=161, resource=ZHBGO1Zsr001_RESOURCE),
            @JUnitSnmpAgent(host=ZHBGO1Zsr002_IP, port=161, resource=ZHBGO1Zsr002_RESOURCE)
    })
    public void testTimeTetraLldpWalk() throws Exception {
        String trackerName01 = "lldpLocalGroup01";
        SnmpAgentConfig  config01 = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(ZHBGO1Zsr001_IP));
        String trackerName02 = "lldpLocalGroup02";
        SnmpAgentConfig  config02 = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(ZHBGO1Zsr002_IP));
        LldpLocalGroupTracker lldpLocalGroup01 = new LldpLocalGroupTracker();
        LldpLocalGroupTracker lldpLocalGroup02 = new LldpLocalGroupTracker();
        final List<TimeTetraLldpLink> links01 = new ArrayList<>();
        final List<TimeTetraLldpLink> links02 = new ArrayList<>();

        try {
            m_client.walk(config01,lldpLocalGroup01)
                    .withDescription(trackerName01)
                    .withLocation(null)
                    .execute()
                    .get();
            m_client.walk(config02,lldpLocalGroup02)
                    .withDescription(trackerName02)
                    .withLocation(null)
                    .execute()
                    .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        LldpElement lldpElement01 = lldpLocalGroup01.getLldpElement();
        LldpElement lldpElement02 = lldpLocalGroup02.getLldpElement();
        LOG.warn("01 local chassis type: " + LldpChassisIdSubType.getTypeString(lldpElement01.getLldpChassisIdSubType().getValue()));
        LOG.warn("01 local chassis id: " + lldpElement01.getLldpChassisId());
        LOG.warn("01 local sysname: " + lldpElement01.getLldpSysname());
        LOG.warn("02 local chassis type: " + LldpChassisIdSubType.getTypeString(lldpElement02.getLldpChassisIdSubType().getValue()));
        LOG.warn("02 local chassis id: " + lldpElement02.getLldpChassisId());
        LOG.warn("02 local sysname: " + lldpElement02.getLldpSysname());

        assertEquals(ZHBGO1Zsr001_LLDP_ID, lldpElement01.getLldpChassisId());
        assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_MACADDRESS, lldpElement01.getLldpChassisIdSubType());
        assertEquals(ZHBGO1Zsr001_NAME, lldpElement01.getLldpSysname());
        assertEquals(ZHBGO1Zsr002_LLDP_ID, lldpElement02.getLldpChassisId());
        assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_MACADDRESS, lldpElement02.getLldpChassisIdSubType());
        assertEquals(ZHBGO1Zsr002_NAME, lldpElement02.getLldpSysname());

        LldpRemTableTracker lldpRemTable01 = new LldpRemTableTracker() {

            public void processLldpRemRow(final LldpRemRow row) {
                fail();
            }
        };

        LldpRemTableTracker lldpRemTable02 = new LldpRemTableTracker() {

            public void processLldpRemRow(final LldpRemRow row) {
                fail();
            }
        };

        try {
            m_client.walk(config01,
                            lldpRemTable01)
                    .withDescription("lldpRemTable01")
                    .withLocation(null)
                    .execute()
                    .get();
            m_client.walk(config02,
                            lldpRemTable02)
                    .withDescription("lldpRemTable02")
                    .withLocation(null)
                    .execute()
                    .get();
        } catch (final InterruptedException e) {
            fail();
        }

        TimeTetraLldpRemTableTracker timetetralldpRemTable01
                = new TimeTetraLldpRemTableTracker() {

            public void processLldpRemRow(final LldpRemRow row) {
                assertEquals(6, row.getColumnCount());
                TimeTetraLldpLink timeTetraLldpLinklink = row.getLldpLink();
                assertNotNull(timeTetraLldpLinklink.getLldpLink());
                assertNotNull(timeTetraLldpLinklink.getTmnxLldpRemLocalDestMACAddress());
                LldpLink link = timeTetraLldpLinklink.getLldpLink();
                assertNotNull(link.getLldpLocalPortNum());
                assertNotNull(link.getLldpPortIfindex());
                links01.add(timeTetraLldpLinklink);
                assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_MACADDRESS, link.getLldpRemChassisIdSubType());
            }
        };
        TimeTetraLldpRemTableTracker timetetralldpRemTable02
                = new TimeTetraLldpRemTableTracker() {

            public void processLldpRemRow(final LldpRemRow row) {

                assertEquals(6, row.getColumnCount());
                TimeTetraLldpLink timeTetraLldpLinklink = row.getLldpLink();
                assertNotNull(timeTetraLldpLinklink.getLldpLink());
                assertNotNull(timeTetraLldpLinklink.getTmnxLldpRemLocalDestMACAddress());
                LldpLink link = timeTetraLldpLinklink.getLldpLink();
                assertNotNull(link.getLldpLocalPortNum());
                assertNotNull(link.getLldpPortIfindex());
                links02.add(timeTetraLldpLinklink);
                assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_MACADDRESS, link.getLldpRemChassisIdSubType());
            }
        };

        try {
            m_client.walk(config01,
                            timetetralldpRemTable01)
                    .withDescription("timetetralldpRemTable01")
                    .withLocation(null)
                    .execute()
                    .get();
            m_client.walk(config02,
                            timetetralldpRemTable02)
                    .withDescription("timetetralldpRemTable02")
                    .withLocation(null)
                    .execute()
                    .get();
        } catch (final InterruptedException e) {
            fail();
        }

        assertEquals(3,links01.size());
        assertEquals(4,links02.size());

        final LldpLocPortGetter lldpLocPort01 = new LldpLocPortGetter(config01,
                m_client,
                null,0);

        final LldpLocPortGetter lldpLocPort02 = new LldpLocPortGetter(config02,
                m_client,
                null,1);

        for (TimeTetraLldpLink timeTetraLldpLink01: links01) {
            LldpLink link01 = timeTetraLldpLink01.getLldpLink();
            assertNull(link01.getLldpPortId());
            assertNull(link01.getLldpPortIdSubType());
            assertNull(link01.getLldpPortDescr());

            LldpLink updated = lldpLocPort01.getLldpLink(link01);
            assertEquals("\"Not Found On lldpLocPortTable\"",updated.getLldpPortId());
            assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACEALIAS, updated.getLldpPortIdSubType());
            assertEquals("",updated.getLldpPortDescr());
        }

        for (TimeTetraLldpLink timeTetraLldpLink02: links02) {
            LldpLink link02 = timeTetraLldpLink02.getLldpLink();
            assertNull(link02.getLldpPortId());
            assertNull(link02.getLldpPortIdSubType());
            assertNull(link02.getLldpPortDescr());

            LldpLink updated = lldpLocPort02.getLldpLink(link02);
            assertEquals("\"Not Found On lldpLocPortTable\"",updated.getLldpPortId());
            assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACEALIAS, updated.getLldpPortIdSubType());
            assertEquals("",updated.getLldpPortDescr());
        }

        final TimeTetraLldpLocPortGetter ttlldpLocPort01 = new TimeTetraLldpLocPortGetter(config01,
                m_client,
                null,0);

        final TimeTetraLldpLocPortGetter ttlldpLocPort02 = new TimeTetraLldpLocPortGetter(config02,
                m_client,
                null,1);

        for (TimeTetraLldpLink timeTetraLldpLink01: links01) {
            LldpLink link01 = timeTetraLldpLink01.getLldpLink();
            assertEquals("\"Not Found On lldpLocPortTable\"",link01.getLldpPortId());
            assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACEALIAS, link01.getLldpPortIdSubType());
            assertEquals("",link01.getLldpPortDescr());
            assertEquals(1,timeTetraLldpLink01.getTmnxLldpRemLocalDestMACAddress().intValue());

            LldpLink updated = ttlldpLocPort01.getLldpLink(timeTetraLldpLink01);
            assertNotEquals("\"Not Found On lldpLocPortTable\"",updated.getLldpPortId());
            assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_LOCAL, updated.getLldpPortIdSubType());
            assertNotEquals("",updated.getLldpPortDescr());
            LOG.warn("01 {} ifindex {}",updated.getLldpLocalPortNum(),updated.getLldpPortIfindex());
            LOG.warn("01 {} portid {}",updated.getLldpLocalPortNum(),updated.getLldpPortId());
            LOG.warn("01 {} port subtype {}",updated.getLldpLocalPortNum(),updated.getLldpPortIdSubType());
            LOG.warn("01 {} portdescr {}",updated.getLldpLocalPortNum(),updated.getLldpPortDescr());
            LOG.warn("01 {} rem chassisId {}",updated.getLldpLocalPortNum(),updated.getLldpRemChassisId());
            LOG.warn("01 {} rem chassisId subtype {}",updated.getLldpLocalPortNum(),updated.getLldpRemChassisIdSubType());
            LOG.warn("01 {} rem sysname {}",updated.getLldpLocalPortNum(),updated.getLldpRemSysname());
            LOG.warn("01 {} rem portid {}",updated.getLldpLocalPortNum(),updated.getLldpRemPortId());
            LOG.warn("01 {} rem port subtype {}",updated.getLldpLocalPortNum(),updated.getLldpRemPortIdSubType());
            LOG.warn("01 {} rem portdescr {}",updated.getLldpLocalPortNum(),updated.getLldpRemPortDescr());
        }

        for (TimeTetraLldpLink timeTetraLldpLink02: links02) {
            LldpLink link02 = timeTetraLldpLink02.getLldpLink();
            assertEquals("\"Not Found On lldpLocPortTable\"",link02.getLldpPortId());
            assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACEALIAS, link02.getLldpPortIdSubType());
            assertEquals("",link02.getLldpPortDescr());
            assertEquals(1,timeTetraLldpLink02.getTmnxLldpRemLocalDestMACAddress().intValue());

            LldpLink updated = ttlldpLocPort02.getLldpLink(timeTetraLldpLink02);
            assertNotEquals("\"Not Found On lldpLocPortTable\"",updated.getLldpPortId());
            assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_LOCAL, updated.getLldpPortIdSubType());
            assertNotEquals("",updated.getLldpPortDescr());
            LOG.warn("02 {} ifindex {}",updated.getLldpLocalPortNum(),updated.getLldpPortIfindex());
            LOG.warn("02 {} portid {}",updated.getLldpLocalPortNum(),updated.getLldpPortId());
            LOG.warn("02 {} port subtype {}",updated.getLldpLocalPortNum(),updated.getLldpPortIdSubType());
            LOG.warn("02 {} portdescr {}",updated.getLldpLocalPortNum(),updated.getLldpPortDescr());
            LOG.warn("02 {} rem chassisId {}",updated.getLldpLocalPortNum(),updated.getLldpRemChassisId());
            LOG.warn("02 {} rem chassisId subtype {}",updated.getLldpLocalPortNum(),updated.getLldpRemChassisIdSubType());
            LOG.warn("02 {} rem sysname {}",updated.getLldpLocalPortNum(),updated.getLldpRemSysname());
            LOG.warn("02 {} rem portid {}",updated.getLldpLocalPortNum(),updated.getLldpRemPortId());
            LOG.warn("02 {} rem port subtype {}",updated.getLldpLocalPortNum(),updated.getLldpRemPortIdSubType());
            LOG.warn("02 {} rem portdescr {}",updated.getLldpLocalPortNum(),updated.getLldpRemPortDescr());

        }


    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH1_IP, port=161, resource="classpath:/linkd/nms17216/switch1-walk.txt")
    })
    public void testLldpLocalGroupWalk() throws Exception {

    	String trackerName = "lldpLocalGroup";
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH1_IP));
    	        LldpLocalGroupTracker lldpLocalGroup = new LldpLocalGroupTracker();

        try {
            m_client.walk(config,lldpLocalGroup)
            .withDescription(trackerName)
            .withLocation(null)
            .execute()
            .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

		LldpElement eiA = lldpLocalGroup.getLldpElement();
		System.err.println("local chassis type: " + LldpChassisIdSubType.getTypeString(eiA.getLldpChassisIdSubType().getValue()));
		System.err.println("local chassis id: " + eiA.getLldpChassisId());
		System.err.println("local sysname: " + eiA.getLldpSysname());
		
		assertEquals("0016c8bd4d80", eiA.getLldpChassisId());
		assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_MACADDRESS, eiA.getLldpChassisIdSubType());
		assertEquals("Switch1", eiA.getLldpSysname());
    }
    

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH1_IP, port=161, resource="classpath:/linkd/nms17216/switch1-walk.txt")
    })
    public void testLldpLocGetter() throws Exception {

    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH1_IP));
		
    	final LldpLocPortGetter lldpLocPort = new LldpLocPortGetter(config,m_client,null,0);
		LldpLink link = new LldpLink();
		link.setLldpLocalPortNum(9);
		link = lldpLocPort.getLldpLink(link);
		assertEquals(9, link.getLldpLocalPortNum().intValue());
		assertEquals("Gi0/9", link.getLldpPortId());
		assertEquals("GigabitEthernet0/9", link.getLldpPortDescr());
		assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACENAME, link.getLldpPortIdSubType());
		
                link = new LldpLink();
                link.setLldpLocalPortNum(10);
	        link = lldpLocPort.getLldpLink(link);
                assertEquals(10, link.getLldpLocalPortNum().intValue());
                assertEquals("Gi0/10", link.getLldpPortId());
                assertEquals("GigabitEthernet0/10", link.getLldpPortDescr());
                assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACENAME, link.getLldpPortIdSubType());

    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH2_IP, port=161, resource="classpath:/linkd/nms17216/switch2-walk.txt")
    })
    public void test2LldpLocGetter() throws Exception {

        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH2_IP));
                
        final LldpLocPortGetter lldpLocPort = new LldpLocPortGetter(config,m_client,null,0);
        LldpLink link = new LldpLink();
        link.setLldpLocalPortNum(1);
        link = lldpLocPort.getLldpLink(link);
                assertEquals(1, link.getLldpLocalPortNum().intValue());
                assertEquals("Gi0/1", link.getLldpPortId());
                assertEquals("GigabitEthernet0/1", link.getLldpPortDescr());
                assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACENAME, link.getLldpPortIdSubType());
                
                link = new LldpLink();
                link.setLldpLocalPortNum(2);
                link = lldpLocPort.getLldpLink(link);
                assertEquals(2, link.getLldpLocalPortNum().intValue());
                assertEquals("Gi0/2", link.getLldpPortId());
                assertEquals("GigabitEthernet0/2", link.getLldpPortDescr());
                assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACENAME, link.getLldpPortIdSubType());

    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH2_IP, port=161, resource="classpath:/linkd/nms17216/switch2-walk.txt")
    })
    public void test3LldpRemoteTableWalk() throws Exception {

        SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH2_IP));
        final List<LldpLink> links = new ArrayList<>();
                
        LldpRemTableTracker lldpRemTable = new LldpRemTableTracker() {

            public void processLldpRemRow(final LldpRemRow row) {
                    links.add(row.getLldpLink());
            }
        };
        try {
            m_client.walk(config,
                          lldpRemTable)
                          .withDescription("lldpRemTable")
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (ExecutionException e) {
            // pass
            LOG.error("run: collection failed, exiting",e);
            return;
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }
        final LldpLocPortGetter lldpLocPort = new LldpLocPortGetter(config,
                                                                    m_client,
                                                                    null,0);

        for (LldpLink link : links) {
            assertNotNull(link);
            assertNotNull(link.getLldpLocalPortNum());
            assertNull(link.getLldpPortId());
            assertNull(link.getLldpPortIdSubType());
            assertNull(link.getLldpPortDescr());
            
            LldpLink updated = lldpLocPort.getLldpLink(link);
            assertNotNull(updated.getLldpPortId());
            assertEquals(5, updated.getLldpPortIdSubType().getValue().intValue());
            assertNotNull(updated.getLldpPortDescr());
        }
    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=SWITCH1_IP, port=161, resource="classpath:/linkd/nms17216/switch1-walk.txt")
    })
    public void testLldpRemTableWalk() throws Exception {
		
        final SnmpAgentConfig config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SWITCH1_IP));
        LldpRemTableTracker lldpRemTable = new LldpRemTableTracker() {
            
        	public void processLldpRemRow(final LldpRemRow row) {
        		
        		
        		System.err.println("----------lldp rem----------------");
        		System.err.println("columns number in the row: " + row.getColumnCount());

        		assertEquals(6, row.getColumnCount());
        		LldpLink link = row.getLldpLink();

        		System.err.println("local port number: " + row.getLldpRemLocalPortNum());
        		System.err.println("remote chassis: " + link.getLldpRemChassisId());
        		System.err.println("remote chassis type: " + LldpChassisIdSubType.getTypeString(link.getLldpRemChassisIdSubType().getValue()));
        		assertEquals(LldpChassisIdSubType.LLDP_CHASSISID_SUBTYPE_MACADDRESS, link.getLldpRemChassisIdSubType());

        		System.err.println("remote port id: " + link.getLldpRemPortId());
        		System.err.println("remote port type: " + LldpPortIdSubType.getTypeString(link.getLldpRemPortIdSubType().getValue()));
        		assertEquals(LldpPortIdSubType.LLDP_PORTID_SUBTYPE_INTERFACENAME, link.getLldpRemPortIdSubType());
            }
        };

        try {
            m_client.walk(config,
                          lldpRemTable)
                          .withDescription("lldpRemTable")
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            fail();
        }
        
    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = SIEGFRIE_IP, port = 161, resource = SIEGFRIE_SNMP_RESOURCE)
    })
    public void testIsisSysObjectWalk() throws Exception {

    	String trackerName = "isisSysObject";
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SIEGFRIE_IP));
        IsisSysObjectGroupTracker tracker = new IsisSysObjectGroupTracker();

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

		IsIsElement eiA = tracker.getIsisElement();
		System.err.println("Is-Is Sys Id: " + eiA.getIsisSysID());
		System.err.println("Is-Is Sys Admin State: " + IsisAdminState.getTypeString(eiA.getIsisSysAdminState().getValue()));
		
		assertEquals(SIEGFRIE_ISIS_SYS_ID, eiA.getIsisSysID());
		assertEquals(IsisAdminState.on, eiA.getIsisSysAdminState());
    }
    

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = SIEGFRIE_IP, port = 161, resource = SIEGFRIE_SNMP_RESOURCE)
    })
    public void testIsisISAdjTableWalk() throws Exception {

    	final List<IsIsLink> links = new ArrayList<>();
    	String trackerName = "isisISAdjTable";
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SIEGFRIE_IP));
        IsisISAdjTableTracker tracker = new IsisISAdjTableTracker() {
        	public void processIsisAdjRow(final IsIsAdjRow row) {
        		assertEquals(5, row.getColumnCount());
        		links.add(row.getIsisLink());
            }
        };

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(2, links.size());

        for (final IsIsLink link: links) {
    		assertEquals(1,link.getIsisISAdjIndex().intValue());
    		assertEquals(IsisISAdjState.up, link.getIsisISAdjState());
    		assertEquals(IsisISAdjNeighSysType.l1_IntermediateSystem, link.getIsisISAdjNeighSysType());
    		assertEquals(0, link.getIsisISAdjNbrExtendedCircID().intValue());
    		if (link.getIsisCircIndex() == 533) {
    			assertEquals("001f12accbf0", link.getIsisISAdjNeighSNPAAddress());
    			assertEquals("000110255062",link.getIsisISAdjNeighSysID());
    		} else if (link.getIsisCircIndex() == 552) {
    			assertEquals("0021590e47c2", link.getIsisISAdjNeighSNPAAddress());
    			assertEquals("000110088500",link.getIsisISAdjNeighSysID());
    		} else {
                fail();
    		}

        }
    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host = SIEGFRIE_IP, port = 161, resource = SIEGFRIE_SNMP_RESOURCE)
    })
    public void testIsisCircTableWalk() throws Exception {

    	final List<IsIsLink> links = new ArrayList<>();
    	String trackerName = "isisCircTable";
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(SIEGFRIE_IP));
        IsisCircTableTracker tracker = new IsisCircTableTracker() {
        	public void processIsisCircRow(final IsIsCircRow row) {
        		assertEquals(2, row.getColumnCount());
        		links.add(row.getIsisLink());
            }
        };

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(12, links.size());

        for (final IsIsLink link: links) {
    		if (link.getIsisCircIndex() == 533) {
    			assertEquals(533, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 552) {
    			assertEquals(552, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 13) {
    			assertEquals(13, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.off, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 16) {
    			assertEquals(16, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 504) {
    			assertEquals(504, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 507) {
    			assertEquals(507, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 508) {
    			assertEquals(508, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
    		} else if (link.getIsisCircIndex() == 512) {
    			assertEquals(512, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
       		} else if (link.getIsisCircIndex() == 514) {
    			assertEquals(514, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
       		} else if (link.getIsisCircIndex() == 531) {
    			assertEquals(531, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
       		} else if (link.getIsisCircIndex() == 572) {
    			assertEquals(572, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
       		} else if (link.getIsisCircIndex() == 573) {
    			assertEquals(573, link.getIsisCircIfIndex().intValue());
            	assertEquals(IsisAdminState.on, link.getIsisCircAdminState());
     		} else {
                fail();
    		}

        }
    }
        
    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=MIKROTIK_IP, port=161, resource=MIKROTIK_SNMP_RESOURCE)
    })
    public void testIpNetToMediaTableWalk() throws Exception {

    	final List<IpNetToMedia> rows = new ArrayList<>();
    	String trackerName = "ipNetToMediaTable";
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(MIKROTIK_IP));
        IpNetToMediaTableTracker tracker = new IpNetToMediaTableTracker() {
        	public void processIpNetToMediaRow(final IpNetToMediaRow row) {
        		rows.add(row.getIpNetToMedia());
            }
        };

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(6, rows.size());

        for (final IpNetToMedia row: rows) {
    		assertEquals(IpNetToMediaType.IPNETTOMEDIA_TYPE_DYNAMIC,row.getIpNetToMediaType());
            switch (row.getPhysAddress()) {
                case "00901a4222f8":
                    assertEquals(InetAddressUtils.addr("10.129.16.1"), row.getNetAddress());
                    assertEquals(1, row.getSourceIfIndex().intValue());
                    break;
                case "0013c8f1d242":
                    assertEquals(InetAddressUtils.addr("10.129.16.164"), row.getNetAddress());
                    assertEquals(1, row.getSourceIfIndex().intValue());
                    break;
                case "f0728c99994d":
                    assertEquals(InetAddressUtils.addr("192.168.0.13"), row.getNetAddress());
                    assertEquals(2, row.getSourceIfIndex().intValue());
                    break;
                case "0015999f07ef":
                    assertEquals(InetAddressUtils.addr("192.168.0.14"), row.getNetAddress());
                    assertEquals(2, row.getSourceIfIndex().intValue());
                    break;
                case "60334b0817a8":
                    assertEquals(InetAddressUtils.addr("192.168.0.16"), row.getNetAddress());
                    assertEquals(2, row.getSourceIfIndex().intValue());
                    break;
                case "001b63cda9fd":
                    assertEquals(InetAddressUtils.addr("192.168.0.17"), row.getNetAddress());
                    assertEquals(2, row.getSourceIfIndex().intValue());
                    break;
                default:
                    fail();
                    break;
            }
        }        
        
    }

    @Test
    @JUnitSnmpAgents(value={
        @JUnitSnmpAgent(host=DLINK1_IP, port=161, resource=DLINK1_SNMP_RESOURCE),
    })
    public void testDot1dBaseWalk() throws Exception {

    	String trackerName = "dot1dbase";
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DLINK1_IP));
        Dot1dBaseTracker tracker = new Dot1dBaseTracker();

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

    	final BridgeElement bridge =tracker.getBridgeElement();
    	assertEquals("001e58a32fcd", bridge.getBaseBridgeAddress());
    	assertEquals(26, bridge.getBaseNumPorts().intValue());
    	assertEquals(BridgeDot1dBaseType.DOT1DBASETYPE_TRANSPARENT_ONLY,bridge.getBaseType());
    	assertEquals(BridgeDot1dStpProtocolSpecification.DOT1D_STP_PROTOCOL_SPECIFICATION_IEEE8021D,bridge.getStpProtocolSpecification());
    	assertEquals(32768,bridge.getStpPriority().intValue());
    	assertEquals("0000000000000000",bridge.getStpDesignatedRoot());
    	assertEquals(0, bridge.getStpRootCost().intValue());
    	assertEquals(0, bridge.getStpRootPort().intValue());
    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=DLINK1_IP, port=161, resource=DLINK1_SNMP_RESOURCE),
    })
    public void testDot1dBasePortTableWalk() throws Exception {

    	String trackerName = "dot1dbasePortTable";
    	final List<Dot1dBasePortRow> rows = new ArrayList<>();
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DLINK1_IP));
        Dot1dBasePortTableTracker tracker = new Dot1dBasePortTableTracker() {
            @Override
        	public void processDot1dBasePortRow(final Dot1dBasePortRow row) {
            	rows.add(row);
            }
        };


        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(26, rows.size());
        for (Dot1dBasePortRow row: rows) {
        	assertEquals(row.getBaseBridgePort().intValue(), row.getBaseBridgePortIfindex().intValue());
        }
    }

    @Test
    @JUnitSnmpAgents(value={
        @JUnitSnmpAgent(host=DLINK1_IP, port=161, resource=DLINK1_SNMP_RESOURCE),
    })
    public void testDot1dStpPortTableWalk() throws Exception {

    	String trackerName = "dot1dbaseStpTable";
    	final List<BridgeStpLink> links = new ArrayList<>();
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DLINK1_IP));
        Dot1dStpPortTableTracker tracker = new Dot1dStpPortTableTracker() {
            @Override
        	public void processDot1dStpPortRow(final Dot1dStpPortRow row) {
            	links.add(row.getLink());
            }
        };

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(26, links.size());
        int i = 0;
        for (BridgeStpLink link: links) {
        	assertEquals(++i, link.getStpPort().intValue());
        	assertEquals(128, link.getStpPortPriority().intValue());
        	if (link.getStpPort() <= 6 || link.getStpPort() == 24 )
        		assertEquals(BridgeDot1dStpPortState.DOT1D_STP_PORT_STATUS_FORWARDING, link.getStpPortState());
        	else
        		assertEquals(BridgeDot1dStpPortState.DOT1D_STP_PORT_STATUS_DISABLED, link.getStpPortState());
        	assertEquals(BridgeDot1dStpPortEnable.DOT1D_STP_PORT_ENABLED, link.getStpPortEnable());
        	assertEquals(2000000,link.getStpPortPathCost().intValue());
        	assertEquals("0000000000000000",link.getDesignatedRoot());
        	assertEquals(0,link.getDesignatedCost().intValue());
        	assertEquals("0000000000000000",link.getDesignatedBridge());
        	assertEquals("0000",link.getDesignatedPort());
        }
    }

    @Test
    @JUnitSnmpAgents(value={
        @JUnitSnmpAgent(host=DLINK1_IP, port=161, resource=DLINK1_SNMP_RESOURCE),
    })
    public void testDot1dTpFdbTableWalk() throws Exception {

    	String trackerName = "dot1dTpFdbTable";
    	final List<BridgeForwardingTableEntry> links = new ArrayList<>();
    	SnmpAgentConfig  config = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DLINK1_IP));
        Dot1dTpFdbTableTracker tracker = new Dot1dTpFdbTableTracker() {
            @Override
        	public void processDot1dTpFdbRow(final Dot1dTpFdbRow row) {
            	links.add(row.getLink());
            }
        };

        try {
            m_client.walk(config,
                          tracker)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(17, links.size());
        for (BridgeForwardingTableEntry link: links) {
        	assertEquals(BridgeDot1qTpFdbStatus.DOT1D_TP_FDB_STATUS_LEARNED, link.getBridgeDot1qTpFdbStatus());
        	System.out.println(link.getMacAddress());
            switch (link.getMacAddress()) {
                case "000c29dcc076":
                case "f07d68711f89":
                case "f07d6876c565":
                    assertEquals(24, link.getBridgePort().intValue());
                    break;
                case "000ffeb10d1e":
                case "000ffeb10e26":
                case "001a4b802790":
                case "001d6004acbc":
                case "001e58865d0f":
                case "0021913b5108":
                case "002401ad3416":
                case "00248c4c8bd0":
                case "0024d608693e":
                case "1caff737cc33":
                case "1caff7443339":
                case "1cbdb9b56160":
                case "5cd998667abb":
                case "e0cb4e3e7fc0":
                    assertEquals(6, link.getBridgePort().intValue());
                    break;
                default:
                    fail();
                    break;
            }
        }
    }

    @Test
    @JUnitSnmpAgents(value={
            @JUnitSnmpAgent(host=DLINK1_IP, port=161, resource=DLINK1_SNMP_RESOURCE),
            @JUnitSnmpAgent(host=DLINK2_IP, port=161, resource=DLINK2_SNMP_RESOURCE)
    })
    public void testDot1qTpFdbTableWalk() throws Exception {

    	String trackerName = "dot1qTpFdbTable";
    	final Map<String,Integer> macs1 = new HashMap<>();
    	final Map<String,Integer> macs2 = new HashMap<>();
    	SnmpAgentConfig  config1 = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DLINK1_IP));
        Dot1qTpFdbTableTracker tracker1 = new Dot1qTpFdbTableTracker() {
            @Override
        	public void processDot1qTpFdbRow(final Dot1qTpFdbRow row) {
            	macs1.put(row.getDot1qTpFdbAddress(),row.getDot1qTpFdbPort());
            }
        };

        try {
            m_client.walk(config1,
                          tracker1)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        SnmpAgentConfig  config2 = SnmpPeerFactory.getInstance().getAgentConfig(InetAddress.getByName(DLINK2_IP));
        Dot1qTpFdbTableTracker tracker2 = new Dot1qTpFdbTableTracker() {
            @Override
        	public void processDot1qTpFdbRow(final Dot1qTpFdbRow row) {
            	macs2.put(row.getDot1qTpFdbAddress(),row.getDot1qTpFdbPort());
            }
        };

        try {
            m_client.walk(config2,
                          tracker2)
                          .withDescription(trackerName)
                          .withLocation(null)
                          .execute()
                          .get();
        } catch (final InterruptedException e) {
            LOG.error("run: collection interrupted, exiting",e);
            return;
        }

        assertEquals(59, macs1.size());
        assertEquals(979, macs2.size());

       for (Entry<String,Integer> entry: macs1.entrySet()) {
        	if (macs2.containsKey(entry.getKey())) {
                System.out.println("-----------mac on 1 learned on 2 port-----------------");
        		System.out.println("Mac: " + entry.getKey());
        		System.out.println("learned on PortOn1: " + entry.getValue());
        		System.out.println("learned on PortOn2: " + macs2.get(entry.getKey()));
        	} else {
                System.out.println("-----------mac found on 1 not learned on 2 port-----------------");
        		System.out.println("Mac: " + entry.getKey());
        		System.out.println("learned on PortOn1: " + entry.getValue());
        	}
        }

        for (Entry<String,Integer> entry: macs2.entrySet()) {
        	if (macs1.containsKey(entry.getKey())) {
           	    System.out.println("-----------mac on 2 learned on 1 port-----------------");
        	    System.out.println("Mac: " + entry.getKey());
        		System.out.println("learned on PortOn2: " + entry.getValue());
        		System.out.println("learned on PortOn1: " + macs1.get(entry.getKey()));
        	}
        }

        
    }

}
