package nicelee.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import nicelee.nat.NATSessionManager;
import nicelee.util.CommonMethods;
import nicelee.util.ResourcesUtil;

public class TCPServer extends Handler implements Runnable {
	// Socket协议服务端
	public int port;
	public String localIP = "127.0.0.1";
	ServerSocketChannel serverSocketChannel;
	Selector selector = null;

	public static void main(String[] args) throws IOException {
		TCPServer tServer = new TCPServer();
		tServer.service();
	}

	public TCPServer() throws IOException {
		selector = Selector.open();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().setReuseAddress(true);
//		serverSocketChannel.socket().bind(null);
		serverSocketChannel.socket().bind(new InetSocketAddress(12320));
		port = serverSocketChannel.socket().getLocalPort();
		System.out.println("TCP服务器启动, 端口为: " + port);
	}

	/* 服务器服务方法 */
	public void service() throws IOException {

//		NATSessionManager.createSession("tcp", 9867, CommonMethods.ipStringToInt("127.0.0.1"), (short) 5000);

		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		/** 外循环，已经发生了SelectionKey数目 */
		while (selector.select() > 0) {
			/* 得到已经被捕获了的SelectionKey的集合 */
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			while (iterator.hasNext()) {
//				System.out.println("有事件来了啊");
				SelectionKey key = null;
				SocketChannel sc = null;
				try {
					key = (SelectionKey) iterator.next();
					iterator.remove();
					if (key.isAcceptable()) {
						ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
						sc = ssc.accept();
						System.out.println("客户端机子的地址是 " + sc.socket().getRemoteSocketAddress() + "  本地机子的端口号是 "
								+ sc.socket().getLocalPort());
						InetSocketAddress address = (InetSocketAddress) sc.getRemoteAddress();
						Integer portKey = address.getPort();
						// 这一步不应该在这里实现，仅作为测试用例，将收到的连接转给127.0.0.1:7778
						NATSessionManager.createSession("tcp", portKey, CommonMethods.ipStringToInt("127.0.0.1"),
								(short) 7778);

						sc.configureBlocking(false);

						TwinsChannel twins = new TwinsChannel(sc, selector);
						twins.connectRemoteSc();
						sc.register(selector, SelectionKey.OP_READ, twins);// buffer通过附件方式，传递
					} else {
						handle(key);
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (IOException e) {
//					e.printStackTrace();
					ResourcesUtil.closeQuietly(sc);
					if (key != null) {
						key.cancel();
						ResourcesUtil.closeQuietly(key.channel());
					}
				}
			}
		}
		System.out.println("-----程序结束-----");
	}

	@Override
	public void run() {
		try {
			service();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
