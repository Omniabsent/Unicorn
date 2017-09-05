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

		// set bufferPolicies to default if values are missing
		if (this.bufferPolicies == null) {
			this.bufferPolicies = new BufferPolicies();
		} else {
			this.bufferPolicies.fillNullsWithDefaults();
		}
	}

	public void update(EventBean[] events) {
		System.out.println("(EventBuffer) storing new data for query " + query.getID());
		latestEvents.add(events);
		if (latestEvents.size() > bufferPolicies.size) {
			switch (bufferPolicies.order) {
			case BufferPolicies.CONST_ORDER_FIFO:
				latestEvents.removeLast();
				break;
			default:
				latestEvents.removeFirst();
			}
		}
	}

	public EventBean[] read() {
		if (latestEvents.size() < 1) {
			return null;
		}
		if (bufferPolicies.consumption.equals(BufferPolicies.CONST_CONSUMPTION_CONSUME)) {
			switch (bufferPolicies.order) {
			case BufferPolicies.CONST_ORDER_FIFO:
				return latestEvents.removeFirst();
			default:
				return latestEvents.removeLast();
			}
		} else {
			switch (bufferPolicies.order) {
			case BufferPolicies.CONST_ORDER_FIFO:
				return latestEvents.getFirst();
			default:
				return latestEvents.getLast();
			}
		}
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
