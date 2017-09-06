package de.hpi.unicorn.application.rest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.dom.ElementImpl;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.espertech.esper.client.EventBean;

import de.hpi.unicorn.EventProcessingPlatformWebservice;
import de.hpi.unicorn.application.rest.EventQueryRestWebservice.RegisterQueryCall;
import de.hpi.unicorn.application.rest.EventQueryRestWebservice.SubscribeCall;
import de.hpi.unicorn.event.EapEvent;
import de.hpi.unicorn.eventbuffer.BufferManager;
import de.hpi.unicorn.eventbuffer.EventBuffer;
import de.hpi.unicorn.eventhandling.Broker;
import de.hpi.unicorn.exception.DuplicatedSchemaException;
import de.hpi.unicorn.exception.UnparsableException;
import de.hpi.unicorn.importer.xml.XMLParser;
import de.hpi.unicorn.query.LiveQueryListener;
import net.sf.json.JSONObject;

/**
 *
 */
public class EventBufferTest extends JerseyTest {
	private EventProcessingPlatformWebservice service;
	private String eventString;
	private String eventSchemaString;

	@After
	public void removeEventType() {
		try {
			service.unregisterEventType("TestEvent");
		} catch (Exception e) {
			// Do nothing
		}
	}

	@Before
	public void setup() {
		System.out.println("Setup....");

		service = new EventProcessingPlatformWebservice();
		eventString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> "
				+ "<cpoi xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:noNamespaceSchemaLocation=\"TestEvent.xsd\"> " + "<TestValue>1.0</TestValue> "
				+ "<Timestamp>2015-09-05T20:05:32</Timestamp> " + "</cpoi>";
		eventSchemaString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "                <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"TestEvent.xsd\"\n"
				+ "        targetNamespace=\"TestEvent.xsd\" elementFormDefault=\"qualified\">\n"
				+ "        <xs:element name=\"TestEvent\">\n" + "        <xs:complexType>\n" + "        <xs:sequence>\n"
				+ "        <xs:element name=\"TestValue\" type=\"xs:float\"\n"
				+ "        minOccurs=\"1\" maxOccurs=\"1\" />\n"
				+ "        <xs:element name=\"Timestamp\" type=\"xs:dateTime\" minOccurs=\"1\"\n"
				+ "        maxOccurs=\"1\" />\n" + "        </xs:sequence>\n" + "        </xs:complexType>\n"
				+ "        </xs:element>\n" + "        </xs:schema>";
		try {
			service.registerEventType(eventSchemaString, "TestEvent", "Timestamp");
		} catch (UnparsableException e) {
			e.printStackTrace();
		} catch (DuplicatedSchemaException e) {
			// Do nothing, schema already exsits
		}

		System.out.println("test setup finished.");

	}

	@Override
	protected Application configure() {
		return new ResourceConfig(EventQueryRestWebservice.class);
	}

	@Test
	public void testWorkflowUsingREST() throws ParserConfigurationException, SAXException, IOException {
		// register a query
		String url = "REST/BufferedEventQuery/";
		Response response = target(url).request().post(Entity.json("{\"eventQuery\":\"Select * from TestEvent\"}"));

		String queryId = response.readEntity(String.class);

		// subscribe
		url += queryId;
		JSONObject jsonPayload = new JSONObject();
		jsonPayload.put("postAddress", "localhost");
		jsonPayload.put("messageName", "my-message");
		jsonPayload.put("processInstanceId", "12345");
		response = target(url).request().post(Entity.json(jsonPayload.toString()));
		String subscriptionId = response.readEntity(String.class);

		// unsubscribe
		url = "REST/BufferedEventQuery/subscriptions/" + subscriptionId;
		target(url).request().delete();

		// delete the query
		url = "REST/BufferedEventQuery/" + queryId;
		target(url).request().delete();
	}

	@Test
	public void testRemoveQueryWhenNotificationsExist() {
		// register query
		RegisterQueryCall queryInformation = new EventQueryRestWebservice().new RegisterQueryCall();
		queryInformation.eventQuery = "select * from TestEvent";
		String queryId = service.registerBufferedQuery(queryInformation);

		// subscribe
		SubscribeCall subcall = new EventQueryRestWebservice().new SubscribeCall();
		subcall.postAddress = "some-address";
		subcall.messageName = "some-message";
		subcall.processInstanceId = "12345";
		String subId = service.addSubscription(subcall, queryId);

		// remove query
		BufferManager.removeBuffer(queryId);
	}

	/**
	 * Create a simple buffered query (no bufferPolicies provided). Send an
	 * event. Check that it is in the buffer. Send a second event, see that the
	 * new event is in the buffer. Subscribe to the buffer. (in console: events
	 * delivered) Unsubscribe. Remove query.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testSimpleBufferedQuery() throws InterruptedException {
		System.out.println("STARTING TEST: testSimpleBufferedQuery");
		// Create a simple buffered query (no bufferPolicies provided).
		RegisterQueryCall queryInformation = new EventQueryRestWebservice().new RegisterQueryCall();
		queryInformation.eventQuery = "select * from TestEvent";
		String queryId = service.registerBufferedQuery(queryInformation);

		// Send an event.
		helperSendEvent();
		// Check that the event is in the buffer.
		EventBuffer eb = BufferManager.getEventBuffer(queryId);
		assertTrue(eb.size() == 1);
		assertTrue(getTestValue(eb.read()).equals("1.0"));

		// Send a second event, see that the new event is in the buffer.
		eventString = eventString.replace("<TestValue>1.0</TestValue>", "<TestValue>2.0</TestValue>");
		helperSendEvent();
		assertTrue(eb.size() == 1);
		assertTrue(getTestValue(eb.read()).equals("2.0"));

		// Subscribe to the buffer. (in console: events delivered)
		SubscribeCall subcall = new EventQueryRestWebservice().new SubscribeCall();
		subcall.postAddress = "some-address";
		subcall.messageName = "some-message";
		subcall.processInstanceId = "12345";
		String subId = service.addSubscription(subcall, queryId);

		System.out.println("\nTest waiting 3 seconds...\n");
		Thread.sleep(3000); // allow to async deliver events

		// Unsubscribe.
		service.removeSubscription(subId);

		// Remove query.
		BufferManager.removeBuffer(queryId);
	}

	private String getTestValue(EventBean[] bean) {
		Map<Object, Serializable> map = new HashMap<Object, Serializable>();
		final Object eventObject = bean[0].getUnderlying();
		if (eventObject instanceof ElementImpl) {
			map = LiveQueryListener.convertEventToMap((ElementImpl) eventObject);
		} else if (eventObject instanceof HashMap) {
			map = (Map<Object, Serializable>) eventObject;
		}

		return map.get("TestValue").toString();
	}

	private void helperSendEvent() {
		try {
			String url = "REST/Event";
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(eventString)));

			List<EapEvent> events = XMLParser.generateEventsFromDoc(doc);
			EapEvent newEvent = events.get(0);
			Broker.getEventImporter().importEvent(newEvent);

		} catch (Exception e) {

		}

	}
}
