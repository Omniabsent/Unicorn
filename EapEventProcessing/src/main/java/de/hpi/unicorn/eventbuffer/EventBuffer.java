package de.hpi.unicorn.eventbuffer;

import java.util.LinkedList;

import com.espertech.esper.client.EventBean;

import de.hpi.unicorn.query.QueryWrapper;

public class EventBuffer {
	private String bufferId;
	private QueryWrapper query;
	private LinkedList<EventBean[]> latestEvents;
	private BufferPolicies bufferPolicies;

	public EventBuffer(String bId, QueryWrapper qw, BufferPolicies bufferPolicies) {
		bufferId = bId;
		query = qw;
		this.bufferPolicies = bufferPolicies;
		latestEvents = new LinkedList<EventBean[]>();
	}

	public void update(EventBean[] events) {
		System.out.println("(EventBuffer) storing new data for query " + query.getID());
		latestEvents.add(events);
	}

	public EventBean[] read() {
		return latestEvents.get(0);
	}

	/* Getters & Setters */
	public String getBufferId() {
		return bufferId;
	}

	public QueryWrapper getQuery() {
		return query;
	}

	public int size() {
		return latestEvents.size();
	}

}
