package nicelee.dns.model;

import java.net.SocketAddress;

public class QueryInfo {
	public SocketAddress socketAddr;
//	public short id;
	public long lastnanos;
	
//	@Override
//	public int hashCode() {
//		int hash = id;
//		String socket = socketAddr.toString();
//		for(int i =0; i < socket.length(); i++) {
//			hash += socket.charAt(i) * 13^(i+1);
//		}
//		return hash;
//	}
//	
//	@Override
//	public boolean equals(Object obj) {
//		if(obj !=null && obj instanceof QueryInfo) {
//			short dstId = ((QueryInfo)obj).id;
//			SocketAddress dstSocketAddr = ((QueryInfo)obj).socketAddr;
//			
//			return (id==(dstId) && socketAddr.equals(dstSocketAddr));
//		}
//		return false;
//	}
}
