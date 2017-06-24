package de.hpi.unicorn.query;

import com.espertech.esper.client.EventBean;

import de.hpi.unicorn.eventbuffer.BufferManager;

public class BufferedLiveQueryListener extends LiveQueryListener {

	public BufferedLiveQueryListener(QueryWrapper liveQuery) {
		super(liveQuery);
	}

	@Override
	public void update(final EventBean[] newData, final EventBean[] oldData) {
		BufferManager.update(newData, this.query);

		super.update(newData, oldData);
	}

}
