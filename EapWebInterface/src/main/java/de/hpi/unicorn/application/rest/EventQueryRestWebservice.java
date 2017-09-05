package de.hpi.unicorn.application.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.espertech.esper.client.EPException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.hpi.unicorn.EventProcessingPlatformWebservice;
import de.hpi.unicorn.eventbuffer.BufferPolicies;
import de.hpi.unicorn.notification.NotificationRule;
import de.hpi.unicorn.notification.NotificationRuleForQuery;
import de.hpi.unicorn.notification.RestNotificationRule;
import de.hpi.unicorn.query.QueryWrapper;

/**
 * Rest interface create EapEventTypes
 */
@Path("REST")
public class EventQueryRestWebservice {

	/**
	 * Method handling HTTP GET requests for EventQueries. The query is
	 * specified by the uuid of its notification rule. The QueryString will be
	 * sent to the user as "text/plain" media type.
	 *
	 * @param eventQueryUuid
	 *            uuid of the NotificationRule belonging to the requested
	 *            EventQuery
	 * @return String containing the requested EventQuery
	 */
	@GET
	@Path("/EventQuery/{eventQueryUuid}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getEventQuery(@PathParam("eventQueryUuid") String eventQueryUuid) {
		NotificationRule notificationRule = NotificationRuleForQuery.findByUUID(eventQueryUuid);
		if (notificationRule == null) {
			notificationRule = RestNotificationRule.findByUUID(eventQueryUuid);
			if (notificationRule == null) {
				return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
						.entity("No query found for given id.").build();
			}
			QueryWrapper query = ((RestNotificationRule) notificationRule).getQuery();
			return Response.ok(query.getQueryString(), MediaType.TEXT_PLAIN).build();
		}
		QueryWrapper query = ((NotificationRuleForQuery) notificationRule).getQuery();
		return Response.ok(query.getQueryString(), MediaType.TEXT_PLAIN).build();
	}

	/**
	 * Method handling HTTP DELETE requests for EventQueries. The query is
	 * specified by the uuid of its notification rule. The query and its
	 * notification rule will be removed from the database. Also, the
	 * corresponding Message Queue will be destroyed.
	 *
	 * @param eventQueryUuid
	 *            uuid of the NotificationRule belonging to the requested
	 *            EventQuery
	 */
	@DELETE
	@Path("/EventQuery/Queue/{eventQueryUuid}")
	public Response deleteEventQueryWithQueue(@PathParam("eventQueryUuid") String eventQueryUuid) {
		return deleteEventQuery(eventQueryUuid);
	}

	/**
	 * Method handling HTTP DELETE requests for EventQueries. The query is
	 * specified by the uuid of its notification rule. The query and its
	 * notification rule will be removed from the database.
	 *
	 * @param eventQueryUuid
	 *            uuid of the NotificationRule belonging to the requested
	 *            EventQuery
	 */
	@DELETE
	@Path("/EventQuery/REST/{eventQueryUuid}")
	public Response deleteEventQueryWithRest(@PathParam("eventQueryUuid") String eventQueryUuid) {
		return deleteEventQuery(eventQueryUuid);
	}

	private Response deleteEventQuery(String eventQueryUuid) {
		EventProcessingPlatformWebservice service = new EventProcessingPlatformWebservice();
		boolean success = service.unregisterQuery(eventQueryUuid);
		if (success) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Query could not be found, or an error occurred on deletion.").type(MediaType.TEXT_PLAIN)
					.build();
		}
	}

	/**
	 * Method handling HTTP POST requests for EventQueries. The EventQuery in
	 * the POST body will be added to the database, and a NotificationRule will
	 * be created.
	 *
	 * @param queryJson
	 *            JSON string containing title, queryString and an email for the
	 *            NotificationRule
	 * @return the uuid of the NotificationRule
	 */
	@POST
	@Path("/EventQuery/Queue")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response createEventQueryWithQueue(String queryJson) {
		try {
			Gson gson = new Gson();
			EventQueryJsonForQueue ele = gson.fromJson(queryJson, EventQueryJsonForQueue.class);
			EventProcessingPlatformWebservice service = new EventProcessingPlatformWebservice();
			String uuid = service.registerQueryForQueue(ele.getTitle(), ele.getQueryString(), ele.getEmail());
			return Response.ok(uuid).build();
		} catch (EPException | JsonSyntaxException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Event Query could not be registered: " + e.getMessage()).type("text/plain").build();
		}
	}

	/**
	 * Method for handling HTTP POST registration of EventQueries. The
	 * EventQuery in the post body will be added to the database, and a
	 * notificationRule will be created.
	 * <p>
	 * This is a one time Notification: after the notificationRule triggers, the
	 * query and the rule will be deleted.
	 *
	 * @param queryJson
	 * @return
	 */
	@POST
	@Path("/EventQuery/REST")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response createEventQueryWithRest(String queryJson) {
		try {
			Gson gson = new Gson();
			EventQueryJsonForRest ele = gson.fromJson(queryJson, EventQueryJsonForRest.class);
			EventProcessingPlatformWebservice service = new EventProcessingPlatformWebservice();
			String uuid = service.registerQueryForRest(ele.getQueryString(), ele.getNotificationPath());

			return Response.ok(uuid).build();
		} catch (EPException | JsonSyntaxException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Event Query could not be registered: " + e.getMessage()).type("text/plain").build();
		}
	}

	/**
	 * Register a buffered query in the system. (without notification
	 * recipients). Instantiates a buffer that will hold the latest query output
	 * according to the bufferPolicies.
	 * 
	 * @param payloadJson:
	 *            JSON (eventQuery[, bufferPolicies]) with
	 *            bufferPolicies:(lifetime, consumption, size, order)
	 * @return Response containing uuid of the query
	 */
	@POST
	@Path("/BufferedEventQuery")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response registerQuery(String payloadJson) {
		// register query in esper without notificationPath
		try {
			Gson gson = new Gson();
			RegisterQueryCall callData = gson.fromJson(payloadJson, RegisterQueryCall.class);
			EventProcessingPlatformWebservice service = new EventProcessingPlatformWebservice();

			System.out.println("The following query will be buffered: " + callData.eventQuery);
			String uuid = service.registerBufferedQuery(callData);

			return Response.ok(uuid).build();
		} catch (EPException | JsonSyntaxException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Event Query could not be registered: " + e.getMessage()).type("text/plain").build();
		}
	}

	/**
	 * Add a subscription to a query. Deliver the latest events from the buffer.
	 * 
	 * @param queryId
	 * @param payloadJson:
	 *            JSON (notificationPath) with
	 *            notificationPath:(notificationAddress, processInstanceId,
	 *            message-Name)
	 * @return Response containing uuid of the subscription
	 */
	@POST
	@Path("/BufferedEventQuery/{queryId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response subscribe(@PathParam("queryId") String queryId, String payloadJson) {
		try {
			Gson gson = new Gson();
			SubscribeCall callData = gson.fromJson(payloadJson, SubscribeCall.class);

			System.out.println("(RestAPI) delivering events from buffer " + queryId + " to " + callData.postAddress);

			EventProcessingPlatformWebservice service = new EventProcessingPlatformWebservice();
			String uuid = service.addSubscription(callData, queryId);

			return Response.ok(uuid).build();
		} catch (EPException | JsonSyntaxException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Request Events failed: " + e.getMessage())
					.type("text/plain").build();
		}
	}

	@DELETE
	@Path("/BufferedEventQuery/{queryId}/{subscriptionId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response unsubscribe(@PathParam("queryId") String queryId,
			@PathParam("subscriptionId") String subscriptionId) {
		return null;
	}

	@DELETE
	@Path("/BufferedEventQuery/{queryId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public Response removeQuery(@PathParam("queryId") String queryId) {
		return null;
	}

	/**
	 * Class for parsing Event Query JSON documents with a message queue
	 * notification that are received via the REST POST request
	 */
	private class EventQueryJsonForQueue {
		private String title;
		private String queryString;
		private String email;

		public String getEmail() {
			return email;
		}

		public String getQueryString() {
			return queryString;
		}

		public String getTitle() {
			return title;
		}
	}

	/**
	 * Class for parsing Event Query JSON documents with a REST notification
	 * that are received via the REST POST request
	 */
	private class EventQueryJsonForRest {
		private String notificationPath;
		private String queryString;

		public String getQueryString() {
			return queryString;
		}

		public String getNotificationPath() {
			return notificationPath;
		}
	}

	public class RegisterQueryCall {
		public String eventQuery;
		public BufferPolicies bufferPolicies;
	}

	public class SubscribeCall {
		public String postAddress, processInstanceId, messageName;
	}

}
