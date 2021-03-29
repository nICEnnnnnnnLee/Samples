package nicelee.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.nat.NATSession;
import nicelee.nat.NATSessionManager;

public class TwinsChannel {
	
	final static Pattern patternURL = Pattern.compile("^/([^:]+):(.*)$");
	Selector selector;

	SocketChannel localSc;
	SocketChannel remoteSc;
	boolean isPureConnection = false;
	
	public TwinsChannel(SocketChannel localSc, Selector selector) {
		this.localSc = localSc;
		this.selector = selector;
	}
	
	SocketChannel connectRemoteSc() throws IOException {
		if (remoteSc != null) {
			return remoteSc;
		}

		remoteSc = SocketChannel.open();
		remoteSc.configureBlocking(false);

		// 客户端连接服务器,其实方法执行并没有实现连接，需要在listen（）方法中调
		// 用channel.finishConnect();才能完成连接
		
		//获取连接TCPServer的本地端口号
		//System.out.println(localSc.getLocalAddress().toString());
		//System.out.println(localSc.getRemoteAddress().toString());
		Matcher matcher = patternURL.matcher(localSc.getRemoteAddress().toString());
		matcher.find();
		Integer localPort = Integer.parseInt(matcher.group(2));
		NATSession session = NATSessionManager.getSession("tcp", localPort);
		//建立远程连接
		System.out.println(session.RemoteHost);
		System.out.println(session.RemotePort);
		remoteSc.connect(new InetSocketAddress(session.RemoteHost, (int)session.RemotePort));
		remoteSc.register(selector, SelectionKey.OP_CONNECT, this);
		remoteSc.register(selector, SelectionKey.OP_READ, this);
		return remoteSc;
	}
}
