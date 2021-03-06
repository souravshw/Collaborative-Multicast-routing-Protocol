/**
 * 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import routing.util.RoutingInfo;
import util.Tuple;
import core.Connection;
import core.DTNHost;
import core.HostGroup;
import core.Message;
import core.Settings;
import core.SimClock;

/**
 * @author Sourav Sanu Shaw
 * Department of Computer Application
 * Kalyani Govt. Engineering College
 */

/**
 * Implementation of PRoPHET router as described in 
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class ColaborativeProphetRouter extends ActiveRouter {

	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHET_NS = "ColaborativeProphetRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of 
	 * delivery predictions. Should be tweaked for the scenario.
	 */
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
	
	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	
	/** last delivery predictability update (sim)time  */
	private double lastAgeUpdate;
	
	private List<DTNHost> destinations;
	
	Queue<FinalPriorityQueue> queue = new PriorityQueue<FinalPriorityQueue>();
	
	private Map<DTNHost,Double> timeStamp;
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ColaborativeProphetRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}
		this.timeStamp = new HashMap<DTNHost, Double>();
		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ColaborativeProphetRouter(ColaborativeProphetRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		initPreds();
	}
	
	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}
	
	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}
	
	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}
	
	public double getMaxPred(HostGroup hg){
		double max=0.0;
		ageDeliveryPreds(); // make sure preds are updated before getting
		for(DTNHost host: hg.getAllHosts())
		{
			if(preds.containsKey(host) && preds.get(host) > max)
			 max = preds.get(host);
		}
		
		return max;
	}
	
	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ColaborativeProphetRouter : "PRoPHET only works " + 
			" with other routers of same type";
		
		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 	((ColaborativeProphetRouter)otherRouter).getDeliveryPreds();
		
		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}
			
			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / 
			secondsInTimeUnit;
		
		if (timeDiff == 0) {
			return;
		}
		
		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}
		
		this.lastAgeUpdate = SimClock.getTime();
	}
	
	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to this final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		tryOtherMessages();		
	}
int i=1, j=1, k=1, l=1;
	private Tuple<Message, Connection> tryOtherMessages() {
		
	List<Tuple<Message, Connection>> messages = 	new ArrayList<Tuple<Message, Connection>>();
		
	Collection<Message> myMsgCollection = getMessageCollection();
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ColaborativeProphetRouter othRouter = (ColaborativeProphetRouter)other.getRouter();
			Collection<Message> othMsgCollection = othRouter.getMessageCollection();
				if (othRouter.isTransferring()) {
					continue; // skip hosts that are transferring
				}
			message:
				for (Message m1 : myMsgCollection) {					
					for(Message m2:othMsgCollection){
						
							if(m1.getTo().equals(m2.getTo())){								
									if(m1.getLocation().getDifference(m2.getLocation())){
										
											double priority1 = m1.getPriority()*0.3+((SimClock.getTime()-m1.getCreationTime())/60)*0.005+	0.2+getMaxPred(m1.getTo());
											
										for(FinalPriorityQueue temp: queue)
												if(temp.m.equals(m1)){
													continue message;}
											queue.add( new FinalPriorityQueue (m1,priority1));
											l++;
						}
									k++;
					}
					else
						{
											double priority2 = m1.getPriority()*0.3+((SimClock.getTime()-m1.getCreationTime())/60)*0.005+	0.2+getMaxPred(m1.getTo());
											for(FinalPriorityQueue temp: queue)
												if(temp.m.equals(m1)){
													continue message;}
											queue.add(new FinalPriorityQueue (m1,priority2));
											//System.out.println(this.getHost()+" "+other.toString()+" "+i+" "+j+" "+k+" "+l);
						}
							j++;
				}
				
				//System.out.println("Size:"+queue.size()+" Time:"+SimClock.getTime()+" "+this.getHost().toString()+" -> "+other.toString()+" "+myMsgCollection.toString());
				i++;
			}
			while(!queue.isEmpty()){
				//System.out.println("Size:"+queue.size()+" Time:"+SimClock.getTime()+" "+queue.toString());
				FinalPriorityQueue peek=queue.poll();
				//System.out.println(peek.toString()+" "+con);
				messages.add(new Tuple<Message, Connection>(peek.m,con));
			}
		}
		if (messages.size() == 0) {
			return null;
		}		
		return tryMessagesForConnected(messages);	// try to send messages
}
	
	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the 
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator <Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ColaborativeProphetRouter)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getMaxPred(tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ColaborativeProphetRouter)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getMaxPred(tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}
	
	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + 
				" delivery prediction(s)");
		
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();
			
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					host, value)));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		ColaborativeProphetRouter r = new ColaborativeProphetRouter(this);
		return r;
	}
}

class FinalPriorityQueue implements Comparable<FinalPriorityQueue>
{
	Message m;
	double priority;
	 FinalPriorityQueue(Message m, double priority){
		this.m=m;
		this.priority=priority;
	}
	@Override
	public	 int compareTo(FinalPriorityQueue temp) {
		if(this.equals(temp))
			return 0;
		else if(this.priority >temp.priority)
			return -1;
		else
			return 1;
	}
	
	public String toString(){
		return this.m.getId()+" "+this.priority;
	}
	
	public boolean equals(Message temp){
		if(m.getId().equals(this.m.getId()))
			return true;
		
		return false;
		
	}
}