package nicelee.dns.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import nicelee.dns.model.QueryInfo;
import nicelee.dns.model.protocol.DnsHeader;
import nicelee.dns.model.protocol.DnsPacket;
import nicelee.dns.model.protocol.Question;
import nicelee.dns.model.protocol.ResourcePointer;
import nicelee.dns.util.CommonMethods;

public class PureDNSProxy {
	final int MAX_LENGTH = 512;
	final int PORT_NUM = 53;
	byte[] receMsgs = new byte[MAX_LENGTH];

	DatagramSocket udpSocket;
	DatagramPacket packet;

	InetAddress dnsHost;

	// 记录向服务器的查询记录, 因仅针对一台计算机用户,故采用ID Short标识, 用来做本地服务器
	ConcurrentHashMap<Short, QueryInfo> queryMap = new ConcurrentHashMap<>();

	// 记录本地host解析表
	HashMap<String, String> domainIpMap = new HashMap<>();

	public static void main(String[] args) throws UnknownHostException {
		PureDNSProxy dnsProxy = new PureDNSProxy();
		dnsProxy.init();
		dnsProxy.start();
	}

	public void init() throws UnknownHostException {
		System.out.println("PureDNSProxy 初始化中...");
		dnsHost = InetAddress.getByName("114.114.114.114");
		domainIpMap.put("www.baidu.com", "127.0.0.1");
		//domainIpMap.put("nicelee.top", "127.0.0.1");
	}

	public void start() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				for(Entry<Short, QueryInfo> entry: queryMap.entrySet()) {
					if( System.nanoTime() - entry.getValue().lastnanos >= 5000) {
						queryMap.remove(entry.getKey());
						System.out.println("-------删除任务--------" + entry.getKey());
					}
				}
			}
		},0, 5000);
		try {
			udpSocket = new DatagramSocket(PORT_NUM);
			packet = new DatagramPacket(receMsgs, receMsgs.length);
			while (true) {
				udpSocket.receive(packet);

				DnsHeader header = new DnsHeader(receMsgs, 0);
				Short dnsId = header.getID();
				// 如果是来自客户端的, 直接转发给DNS服务器
				if (!dnsHost.equals(packet.getAddress())) {
//					System.out.printf("收到来自客户端的消息, 长度为: %d, 会话标识为: %d\r\n", packet.getLength(),
//					 dnsId);
					// 如果已有设定, 直接构造回复
					ByteBuffer buffer = ByteBuffer.wrap(receMsgs);
					DnsPacket dnsPacket = DnsPacket.FromBytes(buffer);
					Question q = dnsPacket.Questions[0];
					
					System.out.printf("查询的地址是: %s \r\n", q.Domain);
					String ipAddr = null;
					if ((ipAddr = domainIpMap.get(q.Domain)) != null) {
						createDNSResponseToAQuery(dnsPacket, packet, ipAddr);
						DatagramPacket sendPacket = new DatagramPacket(receMsgs, dnsPacket.Size,
								packet.getSocketAddress());
						udpSocket.send(sendPacket);
					} else {
						QueryInfo info = new QueryInfo();
						info.socketAddr = packet.getSocketAddress();
						info.lastnanos = System.nanoTime();
						queryMap.put(dnsId, info);
						DatagramPacket sendPacket = new DatagramPacket(receMsgs, packet.getLength(), dnsHost, 53);
						udpSocket.send(sendPacket);
					}

				} else {
					// 收到来自服务器的包,直接转发给客户端
					// System.out.println("收到来自服务器的消息, 会话标识为: " + dnsId);
					if (queryMap.containsKey(dnsId)) {
						// System.out.println("存在该会话标识, 返回结果");
						DatagramPacket sendPacket = new DatagramPacket(receMsgs, packet.getLength(),
								queryMap.get(dnsId).socketAddr);
						udpSocket.send(sendPacket);
						queryMap.remove(dnsId);
					}
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭socket
			if (udpSocket != null) {
				udpSocket.close();
			}
		}
	}

	public void createDNSResponseToAQuery(DnsPacket dnsPacket, DatagramPacket packet, String ipAddr) {
		Question question = dnsPacket.Questions[0];

		dnsPacket.Header.setResourceCount((short) 1);
		dnsPacket.Header.setAResourceCount((short) 0);
		dnsPacket.Header.setEResourceCount((short) 0);

		ResourcePointer rPointer = new ResourcePointer(packet.getData(), question.Offset() + question.Length());
		rPointer.setDomain((short) 0xC00C);
		rPointer.setType(question.Type);
		rPointer.setClass(question.Class);
		rPointer.setTTL(300);
		rPointer.setDataLength((short) 4);
		rPointer.setIP(CommonMethods.ipStringToInt(ipAddr));

		dnsPacket.Size = 12 + question.Length() + 16;
		
	}
}
