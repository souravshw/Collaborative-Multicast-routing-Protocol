package core;

import java.util.ArrayList;
import java.util.List;

import routing.MessageRouter;

public class HostGroup implements Comparable<HostGroup> {

	static int gang_num=0;
	private List<DTNHost> gang;
	private String gang_id;
	private int gangIndex;
	
	public HostGroup(List<DTNHost> nodes, String gang_id) {
		gang= new ArrayList<DTNHost>();
		for(DTNHost host:nodes)
		gang.add(host);
		this.gang_id = gang_id;
		this.gangIndex=++gang_num;
		allowAllHostsToJoin(nodes);
	}
/*	
	public HostGroup(int []adr, String gang_id, World world){
		gang = new ArrayList<DTNHost>();
		for(int i=0;i<adr.length;i++)
			gang.add(world.getNodeByAddress(adr[i]));
		
		this.gang_id= gang_id;
		this.gang_num=++gang_num;
		
	}
*/
	public HostGroup getGroup(DTNHost host)
	{
		if(!gang.contains(host))
			return null;
		return this;
	}
	
	public int getGroupIndex()
	{
		return gangIndex;
	}	
	public int getGroupAddress()
	{
		return gangIndex;
	}
	public int get_total_gangs()
	{
		return gang_num;
	}
	public DTNHost containsAddress(int address){
		DTNHost res=null;
		for(DTNHost host:gang)
			if(host.getAddress()==address)
				res=host;
		return res;
		
}
	public String get_gang_id()
	{
		return gang_id;
	}
	@Override
	public int compareTo(HostGroup o) {
		return this.gang_id.compareTo((o.gang_id));
	}
	
	public void allowAllHostsToJoin(List<DTNHost> nodes){
		for(DTNHost node: nodes)
			node.joinGang(this);
	}
	
	public boolean hasDTNHost(DTNHost host)
	{
		return gang.contains(host);
	}
	public MessageRouter getRouter(DTNHost host){
		for(DTNHost node: gang)
		{
			if(host.equals(node))
				return host.getRouter();
		}		
		return null;
	}
	
	public boolean hostAddressinGroup( int address){
		for(DTNHost host: gang)
			if(host.getAddress() == address)
				return true;
		
		return false;
	}
	
	public List<Connection> getConnections(DTNHost host)
	{
		for(DTNHost node: gang)
		{
			if(host.equals(node))
				return host.getConnections();
		}		
		return null;
		
	}
	
	public List<DTNHost> getAllHosts(){
		return gang;
	}
	
	public String toString(){
		return this.gang_id;
	}
}
