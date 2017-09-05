package de.hpi.unicorn.eventbuffer;

public class BufferPolicies {
	public static final String CONST_CONSUMPTION_CONSUME = "consume";
	public static final String CONST_CONSUMPTION_REUSE = "reuse";
	public static final String CONST_ORDER_FIFO = "FIFO";
	public static final String CONST_ORDER_LIFO = "LIFO";

	// vars with default values
	public String consumption = CONST_CONSUMPTION_REUSE, order = CONST_ORDER_LIFO;
	public int size = 1, lifespan = 0;

}
