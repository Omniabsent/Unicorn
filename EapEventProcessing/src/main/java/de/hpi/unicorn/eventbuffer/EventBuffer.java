package de.hpi.unicorn.eventbuffer;

import com.espertech.esper.client.EventBean;

import de.hpi.unicorn.query.QueryWrapper;

public class EventBuffer {
	private String bufferId;
	private QueryWrapper query;
	private EventBean[] latestEvents;

	public EventBuffer(String bId, QueryWrapper qw) {
		bufferId = bId;
		query = qw;
	}

	public void update(EventBean[] events) {
		System.out.println("(EventBuffer) storing new data for query " + query.getID());
		latestEvents = events;
	}

	public EventBean[] read() {
		return latestEvents;
	}

	/* Getters & Setters */
	public String getBufferId() {
		return bufferId;
	}

	public QueryWrapper getQuery() {
		return query;
	}

}
