import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

public class Main extends Thread {


    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[1280];

    public Main() throws SocketException {
        socket = new DatagramSocket(5757);
    }

    class Peer {
        public Peer(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        InetAddress address;
        int port;
    }

    public void run() {
        running = true;
        System.out.println("Started");
        HashMap<String,Peer> peerHashMap = new HashMap<>();

        while (running) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            String received
                    = new String(packet.getData(), 0, packet.getLength());
            System.out.println(packet.getLength() + ":" + received);

            String[] command = received.split(":");
            command[0] = "ECHO";
            command[1] = "Hi";
            String returnMsg = "";
            if(command[0].equals("ECHO")) {
                returnMsg = received;
                packet = new DatagramPacket(returnMsg.getBytes(), returnMsg.getBytes().length, address, 6000);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(command[0].equals("JOIN")) {
                peerHashMap.put(command[1], new Peer(address, port));

                returnMsg = "USER_LIST";
                for(String user : peerHashMap.keySet()) {
                    returnMsg += ":" + user;
                }

                packet = new DatagramPacket(returnMsg.getBytes(), returnMsg.getBytes().length, address, 6000);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(command[0].equals("USER")) {
                returnMsg = "USER_LIST";
                for(String user : peerHashMap.keySet()) {
                    returnMsg += ":" + user;
                }

                packet = new DatagramPacket(returnMsg.getBytes(), returnMsg.getBytes().length, address, 6000);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(command[0].equals("CHAT")) {
                String user = "";
                for(String k : peerHashMap.keySet()) {
                    if(peerHashMap.get(k).address.getHostAddress().equals(address.getHostAddress())) {
                            user = k;
                        break;
                    }
                }
                returnMsg = "CHAT:" + user + ":" + command[1];
                for (Peer peer : peerHashMap.values()) {
                    packet = new DatagramPacket(returnMsg.getBytes(), returnMsg.getBytes().length, peer.address, 6000);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if(command[0].equals("MSG")) {

                //find the person who sent this message.
                String sentUser = "";
                for(String k : peerHashMap.keySet()) {
                    if(peerHashMap.get(k).address.getHostAddress().equals(address.getHostAddress())) {
                        sentUser = k;
                        break;
                    }
                }

                //populate a private chat message.
                returnMsg = "PRIVATE CHAT:" + sentUser + ":" + command[2];
                //find the target person
                for (String peerUser : peerHashMap.keySet()) {
                    if(peerUser.equals(command[1])) {
                        Peer peer = peerHashMap.get(peerUser);
                        packet = new DatagramPacket(returnMsg.getBytes(), returnMsg.getBytes().length, peer.address, 6000);
                        try {
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (received.equals("end")) {
                running = false;
                continue;
            }

        }
        socket.close();
    }

    public static void main(String[] args) throws SocketException {
        Main mainThread = new Main();
        mainThread.start();
    }
}
