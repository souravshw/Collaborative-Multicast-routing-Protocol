/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Coord;
import core.DTNHost;
import core.HostGroup;
import core.Message;
import core.SimClock;
import core.World;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	private List<collaborate> collaboration = new ArrayList<collaborate>();
	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param importance 
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, int importance, String id, int size,	int responseSize, double time) {
		super(from,to, importance,id, time);
		this.size = size;
		this.responseSize = responseSize;
	}


	/**
	 * Creates the message this event represents. 
	 */
	@Override
	public void processEvent(World world) {
	//System.out.println(world.getGroups().size()+" "+this.toAddr);
    	HostGroup to  =  world.getGroups().get(this.toAddr);
		DTNHost from = world.getNodeByAddress(this.fromAddr);
		Coord loc =from.getLocation();
		Message m = new Message(from, to,priority, this.id, this.size, loc);
		boolean result=false;
		for(collaborate temp: collaboration){
			if((SimClock.getTime() - temp.getTime())==60)
				if(loc.getDifference(temp.getLoc())){
					if(temp.getMessage().getTo().equals(to))
					result =true;
				}
		}
		if(result == false){
			m.setResponseSize(this.responseSize);
			from.createNewMessage(m);
			collaborate ob = new collaborate(m,SimClock.getTime(),loc);
			this.collaboration.add(ob);
		}
	}
	
	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}
}

class collaborate{
	Message m;
	Double time;
	Coord loc;
	collaborate(Message m, Double time, Coord loc){
		this.m = m;
		this.time = time;
		this.loc = loc;
	}
	
	public Message getMessage(){
		return m;
	}
	
	public Double getTime(){
		return time;
	}
	
	public Coord getLoc(){
		return loc;
	}
}
