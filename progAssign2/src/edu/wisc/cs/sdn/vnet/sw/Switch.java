package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import java.util.*;


/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	private long TIMEOUT = 15000;

	private Map<MACAddress, ForwardingTable> table = new HashMap<MACAddress, ForwardingTable>();
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
                etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                    */
		// 移除超时的mac地址
		for(Map.Entry<MACAddress, ForwardingTable> entry : table.entrySet()){
			if (System.currentTimeMillis() - entry.getValue().timeout > TIMEOUT) {
				table.remove(entry.getKey());
			}
		}

		// 发送数据包
		MACAddress destinationMacAddress = etherPacket.getDestinationMAC();
		ForwardingTable destination = table.get(destinationMacAddress);
		if(destination == null){	/// 转发表中不存在，直接广播
			for(Iface i : interfaces.values()){
				if(inIface.equals(i))continue;
				sendPacket(etherPacket,i);
			}
		}
		else{	/// 按转发表转发
			sendPacket(etherPacket,destination.getIface());
		}

		// 更新转发表
		MACAddress sourceMacAddress = etherPacket.getSourceMAC(); 
		ForwardingTable source = table.get(sourceMacAddress);
		if(source == null){	/// 不在转发表中，将mac地址添加入转发表
			table.put(sourceMacAddress,new ForwardingTable(inIface));
		}
		else{	/// 在转发表中，只更新时间
			source.setTimeout(System.currentTimeMillis());
		}
		/********************************************************************/
	}
	private static class ForwardingTable{
		private Iface iface;
		private long timeout;

		public ForwardingTable(Iface iface){
			this.iface = iface;

			this.timeout = System.currentTimeMillis();
		}

		public Iface getIface(){
			return iface;
		}

		public long getTimeout(){
			return timeout;
		}

		public void setTimeout(long timeout){
			this.timeout = timeout;
		}
	}
}
