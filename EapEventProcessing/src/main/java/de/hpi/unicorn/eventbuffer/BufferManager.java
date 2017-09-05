package de.hpi.unicorn.eventbuffer;

import java.util.LinkedList;

import com.espertech.esper.client.EventBean;

import de.hpi.unicorn.notification.NotificationRule;
import de.hpi.unicorn.query.QueryWrapper;

public class BufferManager {
	private static LinkedList<EventBuffer> bufferList = new LinkedList<EventBuffer>();

	public static EventBuffer getEventBuffer(QueryWrapper qwrapper) {
		for (EventBuffer eb : bufferList) {
			if (eb.getQuery().getID() == qwrapper.getID()) {
				return eb;
			}
		}
		return null;
	}

	public static EventBuffer getEventBuffer(String bufferId) {
		for (EventBuffer eb : bufferList) {
			if (eb.getBufferId().equals(bufferId)) {
				return eb;
			}
		}
		return null;
	}

	public static void addBuffer(EventBuffer eb) {
		System.out.println(
				"(BufferManager) adding a new buffer: " + eb.getQuery().getEsperQuery() + " | " + eb.getBufferId());
		bufferList.add(eb);
	}

	public static void update(EventBean[] events, QueryWrapper qw) {
		getEventBuffer(qw).update(events);
	}

	public static void removeBuffer(String bufferId) {
		System.out.println("(BufferManager) Removing buffer with id " + bufferId);
		EventBuffer buff = getEventBuffer(bufferId);
		bufferList.remove(buff);
		for (NotificationRule r : buff.getQuery().getNotificationRulesForQuery()) {
			r.remove();
		}
		buff.getQuery().remove();
	}

}
