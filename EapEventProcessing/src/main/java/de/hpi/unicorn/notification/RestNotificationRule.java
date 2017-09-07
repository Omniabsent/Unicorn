package de.hpi.unicorn.notification;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Query;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import de.hpi.unicorn.persistence.Persistable;
import de.hpi.unicorn.persistence.Persistor;
import de.hpi.unicorn.query.QueryWrapper;
import de.hpi.unicorn.user.EapUser;
import net.sf.json.JSONObject;

/**
 * The RestNotificationRule class is responsible for handling automatic
 * subscriptions to Unicorn. These always have to include a callback path, where
 * the notification has to be send to as a POST request.
 */
@Entity
@DiscriminatorValue("R")
public class RestNotificationRule extends NotificationRuleForQuery {
	public static final String CONST_SPLIT_CHARACTER = "$";

	// Needed for REST Notifications
	// for camunda notifications: <address>||<prInstId>||<messageName>
	protected String notificationPath;

	/**
	 * @param query
	 *            Query which triggers this notification rule
	 * @param notificationPath
	 *            path to send request to.
	 */
	public RestNotificationRule(final QueryWrapper query, String notificationPath) {
		this.priority = NotificationMethod.REST;
		this.timestamp = new Date();
		this.uuid = UUID.randomUUID().toString();
		this.query = query;
		this.notificationPath = notificationPath;
		this.user = null;
	}

	/**
	 * Default constructor for JPA
	 */
	public RestNotificationRule() {
		this.priority = NotificationMethod.REST;
		this.timestamp = new Date();
		this.uuid = UUID.randomUUID().toString();
		this.query = new QueryWrapper();
		this.notificationPath = "";
		this.user = null;
	}

	public static RestNotificationRule findByUUID(final String uuid) {
		final Query q = Persistor.getEntityManager().createNativeQuery(
				"SELECT * FROM NotificationRule WHERE UUID = '" + uuid + "'", RestNotificationRule.class);

		if (q.getResultList().isEmpty()) {
			return null;
		} else {
			return (RestNotificationRule) q.getResultList().get(0);
		}
	}

	@Override
	public EapUser getUser() {
		EapUser user = new EapUser();
		user.setID(-1);
		user.setMail("");
		user.setName("");
		return user;
	}

	@Override
	public Persistable getTriggeringEntity() {
		return this.query;
	}

	@Override
	/*
	 * No longer persisting RestNotificationForQuery
	 */
	public boolean trigger(final Map<Object, Serializable> eventObject) {
		try {
			final JSONObject event = NotificationRuleUtils.toJSON(eventObject);
			// final RestNotificationForQuery notification = new
			// RestNotificationForQuery(event.toString(), this);
			// no longer storing the notifications, as they were causing errors
			// with JPA
			// probably because the entity was configured incorrectly. However,
			// as no one
			// ever looks them up again, we should stop persisting these anyway.
			// notification.save();

			String payloadJson = "";
			String notificationAddress = "";
			if (this.notificationPath.contains(CONST_SPLIT_CHARACTER)) {
				String[] pathComponents = this.notificationPath.split("[" + CONST_SPLIT_CHARACTER + "]");
				// this is a notification for camunda
				EventPostPayload epp = new EventPostPayload();
				epp.processVariables = eventObject;
				epp.processInstanceId = pathComponents[1];
				epp.messageName = pathComponents[2];

				notificationAddress = pathComponents[0];
				payloadJson = epp.toJson().toString();
			} else {
				// original notification content
				payloadJson = event.toString();
				notificationAddress = this.notificationPath;
			}

			System.out.println("(RestNotificationRule) Notifying " + notificationAddress + " with data " + payloadJson);

			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(notificationAddress);

			Response response = target.request().post(javax.ws.rs.client.Entity.json(payloadJson));
			return response.getStatus() == 200;
		} catch (UnsupportedJsonTransformation e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("(RestNotificationRule) failed: " + e.getMessage());
			// e.printStackTrace();
		}
		return false;
	}

	@Override
	public String toString() {
		String representation = "Notification for " + this.query;
		representation += " for endpoint " + this.notificationPath;
		return representation;
	}

	private class EventPostPayload {
		public Map<Object, Serializable> processVariables;
		public String messageName, processInstanceId;

		public JSONObject toJson() {
			JSONObject json = new JSONObject();
			json.put("messageName", messageName);
			json.put("processInstanceId", processInstanceId);

			JSONObject jsonPVars = new JSONObject();
			for (Object mapKey : processVariables.keySet()) {
				JSONObject varEntry = new JSONObject();
				String val = processVariables.get(mapKey).toString();
				try {
					Double dVal = Double.parseDouble(val);
					varEntry.put("value", dVal);
					varEntry.put("type", "Double");
				} catch (Exception e) {
					varEntry.put("value", val);
					varEntry.put("type", "String");
				}
				jsonPVars.put(mapKey.toString(), varEntry);
			}
			json.put("processVariables", jsonPVars);

			return json;
		}
	}
}
