package nicelee.tcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UDPServer implements Runnable {
	public String localIP = "127.0.0.1";
	public int port;
	
	final int MAX_LENGTH = 512;
	byte[] receMsgs = new byte[MAX_LENGTH];

	DatagramSocket udpSocket;
	DatagramPacket packet;
	
	Pattern patternURL = Pattern.compile("^/([^:]+):(.*)$");
	public static void main(String[] args) throws UnknownHostException, SocketException {
		UDPServer uServer = new UDPServer();
		uServer.init();
		uServer.start();
	}
	
	public UDPServer() {
		try {
			init();
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void init() throws UnknownHostException, SocketException {
		udpSocket = new DatagramSocket(53);
		port = udpSocket.getLocalPort();
		packet = new DatagramPacket(receMsgs, receMsgs.length);
		System.out.println("UDP服务器启动, 端口为: " + port);
	}

	public void start() {
		try {
			
			while (true) {
				udpSocket.receive(packet);
				
				Matcher matcher = patternURL.matcher(packet.getSocketAddress().toString());
				matcher.find();
				
				// 如果消息来自本地, 转发出去
				if (localIP.equals(matcher.group(1))) {
//					NATSessionManager.createSession(Integer.parseInt(matcher.group(2)), CommonMethods.ipStringToInt("114.114.114.114"), (short) 53);
					NATSessionManager.createSession(packet.getPort(), CommonMethods.ipStringToInt("114.114.114.114"), (short) 53);
					
					//System.out.println("收到本地消息" + packet.getSocketAddress().toString());
					NATSession session = NATSessionManager.getSession(packet.getPort());
					if( session == null) {
						continue;
					}
					DatagramPacket sendPacket = new DatagramPacket(receMsgs, packet.getLength(), CommonMethods.ipIntToInet4Address(session.RemoteIP), (int)session.RemotePort);
					udpSocket.send(sendPacket);
				}else {
					//System.out.println("收到外部消息"+ packet.getSocketAddress().toString());
					//如果消息来自外部, 转进来
					NATSession session = new NATSession();
					session.RemoteIP = CommonMethods.ipStringToInt(matcher.group(1));
					session.RemotePort = (short) packet.getPort();
					Integer port = NATSessionManager.getPort(session);
					if( port == null) {
						continue;
					}
					DatagramPacket sendPacket = new DatagramPacket(receMsgs, packet.getLength(), new InetSocketAddress(localIP, port));
					udpSocket.send(sendPacket);
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

	@Override
	public void run() {
		start();
	}
}
