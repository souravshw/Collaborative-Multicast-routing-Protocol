/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

/**
 * A message related external event
 */
public abstract class MessageEvent extends ExternalEvent {
	/** address of the node the message is from */
	protected int fromAddr;
	/** address of the node the message is to */
	protected int toAddr;
	/** identifier of the message */
	protected String id;
	/** Priority of the message */
	protected int priority;
	
	/**
	 * Creates a message  event
	 * @param from Where the message comes from
	 * @param to Who the message goes to 
	 * @param importance 
	 * @param id ID of the message
	 * @param time Time when the message event occurs
	 */
	public MessageEvent(int from, int to, int importance, String id, double time) {
		super(time);
		this.fromAddr = from;
		this.toAddr= to;
		this.id = id;
		this.priority= importance;
	}
	
	@Override
	public String toString() {
		return "MSG @" + this.time + " " + id;
	}
}
