package nicelee.dns.test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import nicelee.dns.model.protocol.DnsHeader;
import nicelee.dns.model.protocol.DnsPacket;

public class UDPServer {
	final int MAX_LENGTH = 1024;
	final int PORT_NUM = 5333;
	byte[] receMsgs = new byte[MAX_LENGTH];

	DatagramSocket udpSocket;
	DatagramPacket packet;
	
	InetAddress dnsHost ;
	HashMap<Short, SocketAddress> queryMap = new HashMap<Short, SocketAddress>();
	
	public static void main(String[] args) throws UnknownHostException {
		UDPServer dnsProxy = new UDPServer();
		dnsProxy.init();
		dnsProxy.start();
	}
	public void init() throws UnknownHostException {
		dnsHost = InetAddress.getByName("222.246.129.81");
	}
	
	public void start() {
		try {
			udpSocket = new DatagramSocket(PORT_NUM);
			packet = new DatagramPacket(receMsgs, receMsgs.length);
			udpSocket.receive(packet);
			
			System.out.println(packet.getSocketAddress().toString());
			DnsHeader header = new DnsHeader(receMsgs, 0);
			Short dnsId = header.getID();
			//如果是来自客户端的, 直接转发给DNS服务器
			if(packet.getAddress() != null && !dnsHost.equals(packet.getAddress())) {
				System.out.printf("收到来自客户端的消息, 长度为: %d, 会话标识为: %d\r\n",packet.getLength(), dnsId);
				queryMap.put(dnsId, packet.getSocketAddress());
				//DatagramPacket sendPacket = new DatagramPacket(receMsgs, packet.getLength(), dnsHost, 5333);
				//DatagramSocket udpQuerySocket = new DatagramSocket();
				//udpQuerySocket.send(sendPacket);
			}else {
				//收到来自服务器的包,直接转发给客户端
				System.out.println("收到来自服务器的消息, 会话标识为: " + dnsId);
				DatagramPacket sendPacket = new DatagramPacket(receMsgs, packet.getLength(), packet.getSocketAddress());
				udpSocket.send(sendPacket);
				queryMap.remove(dnsId);
			}
			

//			ByteBuffer buffer = ByteBuffer.wrap(receMsgs);
//			DnsPacket dnsPacket = DnsPacket.FromBytes(buffer);
//			System.out.println(dnsPacket.ReadDomain(buffer, 0));
//	            /***** 返回ACK消息数据报*/
//	            // 组装数据报
//	            byte[] buf = "I receive the message".getBytes();
//	            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
//	            // 发送消息
//	            udpSocket.send(sendPacket);
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
}
