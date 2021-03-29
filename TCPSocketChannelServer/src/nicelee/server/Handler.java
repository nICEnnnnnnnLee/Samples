package nicelee.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

public class Handler {

	Pattern patternURL = Pattern.compile("^/([^:]+):(.*)$");

	
	public void handle(SelectionKey key) throws IOException{
		System.out.println("事件");
		if(key == null)
			return;
		SocketChannel sc = (SocketChannel) key.channel();
		TwinsChannel twins = (TwinsChannel) key.attachment();
		if(key.readyOps() == SelectionKey.OP_READ) {
			System.out.println("可读事件");
			// 只有在与远端建立时，才有用
			if(twins.isPureConnection) {
				if(sc == twins.localSc) {
					pip(twins.localSc, twins.remoteSc);
				}else {
					pip(twins.remoteSc, twins.localSc);
				}
			}
		}else if(key.readyOps() == SelectionKey.OP_CONNECT) {
			System.out.println("连接建立事件");
			sc.finishConnect();
			sc.configureBlocking(false);
		}
	}


	/**
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	private void pip(SocketChannel source, SocketChannel destination) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1024);
		int bytesRead = source.read(buf);
		while (bytesRead > 0) {
			buf.flip();
			destination.write(buf);
			buf.clear();
			bytesRead = source.read(buf);
		}
	}
	
}
