package nicelee.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Handler {

	public void handle(SelectionKey key) throws IOException {
//		System.out.println("事件");
//		System.out.println("key.isReadable():" + key.isReadable());
//		System.out.println("key.isWritable():" + key.isWritable());
		if (key == null)
			return;
		SocketChannel sc = (SocketChannel) key.channel();
		TwinsChannel twins = (TwinsChannel) key.attachment();
		if (key.isReadable()) {
//			System.out.println("可读事件");
			// 只有在与远端建立时，才有用
			if (twins.remoteSc.isConnected()) {
				if (sc == twins.localSc) {
//					System.out.println("来自local的可读事件");
					pip(twins.localSc, twins.remoteSc);
				} else {
//					System.out.println("来自remote的可读事件");
					pip(twins.remoteSc, twins.localSc);
				}
			} else {
				twins.remoteSc.finishConnect();
			}
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
