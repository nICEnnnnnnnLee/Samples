package nicelee.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPServer implements Runnable{
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
		serverSocketChannel.socket().bind(null);
//		serverSocketChannel.socket().bind(new InetSocketAddress(12320));
		port = serverSocketChannel.socket().getLocalPort();
		System.out.println("TCP服务器启动, 端口为: " + port);
	}

	/* 服务器服务方法 */
	public void service() throws IOException {
		//
		// NATSessionManager.createSession(9867,
		// CommonMethods.ipStringToInt("192.168.1.103"), (short) 7777);
		//
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		/** 外循环，已经发生了SelectionKey数目 */
		while (selector.select() > 0) {
			/* 得到已经被捕获了的SelectionKey的集合 */
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			while (iterator.hasNext()) {
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
						sc.configureBlocking(false);

						TwinsChannel twins = new TwinsChannel(sc, selector);
						twins.connectRemoteSc();
						sc.register(selector, SelectionKey.OP_READ, twins);// buffer通过附件方式，传递
					}
					if (key.isReadable()) {
						reveice(key);
					}
				} catch (NullPointerException e) {
					// 没有找到对应的
					try {
						if (sc != null) {
							sc.close();
						}
					} catch (Exception cex) {
						cex.printStackTrace();
					}
				} catch (IOException e) {
					e.printStackTrace();
					try {
						if (sc != null) {
							sc.close();
						}
						if (key != null) {
							key.cancel();
							key.channel().close();
						}
					} catch (ClosedChannelException cex) {
						cex.printStackTrace();
					}
				} finally {

				}
			}
		}
		System.out.println("-----程序结束-----");
	}

	Pattern patternURL = Pattern.compile("^/([^:]+):(.*)$");

	public void reveice(SelectionKey key) throws IOException {
		// System.out.println("----收到Read事件----");
		if (key == null)
			return;

		SocketChannel sc = (SocketChannel) key.channel();
		// System.out.println("消息来自: " + sc.getRemoteAddress().toString());
		Matcher matcher = patternURL.matcher(sc.getRemoteAddress().toString());
		matcher.find();

		TwinsChannel twins = (TwinsChannel) key.attachment();
		// 如果消息来自本地, 转发出去
		if (localIP.equals(matcher.group(1))) {

			if (!twins.remoteSc.isConnected()) {
				// 如果正在连接，则完成连接
				twins.remoteSc.finishConnect();
				twins.remoteSc.configureBlocking(false);
			} else {
				// System.out.println("已经连接完成..");
				// System.out.println("消息来自本地: " + sc.getRemoteAddress().toString());
				ByteBuffer buf = ByteBuffer.allocate(2014);
				int bytesRead = sc.read(buf);
				//String content = "";
				while (bytesRead > 0) {
					//content += new String(buf.array(), 0, buf.position());
					buf.flip();
					twins.remoteSc.write(buf);
					buf.clear();
					bytesRead = sc.read(buf);
				}
				// System.out.println("来自内部的消息是: " + content.trim());
			}
		} else {
			// 如果消息来自外部, 转给内部
			// System.out.println("消息来自外部: " + sc.getRemoteAddress().toString());
			ByteBuffer buf = ByteBuffer.allocate(2014);
			int bytesRead = sc.read(buf);
			//String content = "";
			while (bytesRead > 0) {
				//content += new String(buf.array(), 0, buf.position());
				buf.flip();
				twins.localSc.write(buf);
				buf.clear();
				bytesRead = sc.read(buf);
			}
			// System.out.println("来自外部的消息是: " + content.trim());
		}
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
