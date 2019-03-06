import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Server {
    static Map<String, SelectableChannel> userSocket = new HashMap<>();

    @SuppressWarnings("unused")
    public static void main(String[] args) throws IOException, InterruptedException {

        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 8090);

        serverSocketChannel.bind(inetSocketAddress);

        serverSocketChannel.configureBlocking(false);

        int ops = serverSocketChannel.validOps();
        SelectionKey selectKy = serverSocketChannel.register(selector, ops, null);

        while (true) {

            log("new connection...");
            System.out.println(userSocket);
            selector.select();
            Set<SelectionKey> crunchifyKeys = selector.selectedKeys(); //
            Iterator<SelectionKey> crunchifyIterator = crunchifyKeys.iterator();//

            while (crunchifyIterator.hasNext()) {
                SelectionKey myKey = crunchifyIterator.next();

                if (myKey.isAcceptable()) {
                    SocketChannel Client = serverSocketChannel.accept();
                    Client.configureBlocking(false);
                    Client.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
                    log("Connection Accepted: " + Client.getLocalAddress() + "\n");
                } else if (myKey.isReadable()) {

                    SocketChannel sender = (SocketChannel) myKey.channel();
                    messageResponder(sender);
                }
                crunchifyIterator.remove();
                Thread.sleep(1000);
            }
        }
    }

    private static void messageResponder(SocketChannel sender) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        sender.read(byteBuffer);
        String result = new String(byteBuffer.array()).trim();
        if (result.matches("^\\s*close\\s*$")) {
            userSocket.remove(findUserBySocket(sender));
            log("disconnecting user...");
            sender.close();
        } else if (!userSocket.values().contains(sender)) {
            userSocket.put(result, sender);
            log("User registered: " + result);
        } else {

            try {
                int delimiter = result.indexOf('#'); //щоб написати повідомлення користувачу в чаті потрібно в форматі  username# message
                String[] userMessage = {result.substring(0, delimiter), result.substring(delimiter + 1)};
                SocketChannel receiver = (SocketChannel) userSocket.get(userMessage[0]);
                log("to user: " + userMessage[1] + " to " + userMessage[0]);
                send("\033[34mFrom: " + findUserBySocket(sender) + "\033[36m" + userMessage[1] + "\033[30m", receiver);
            } catch (RuntimeException e) {
                sender.write(ByteBuffer.wrap("Usage: <Receiver name>#Message".getBytes()));
            }
        }
    }

    private static String findUserBySocket(SocketChannel s) {
        return userSocket.entrySet().stream().filter(e -> e.getValue().equals(s)).findFirst().get().getKey();
    }

    private static void send(String message, SocketChannel receiver) {
        try {
            receiver.write(ByteBuffer.wrap(message.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void log(String str) {
        System.out.println(str);
    }
}
