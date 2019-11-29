import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static Node MySelf;
    public static boolean close_all=false;
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter Listening Port Number");
        int LISTEN_PORT = scan.nextInt();
        System.out.println("Enter Listening ID");
        int id = scan.nextInt();
        String IP = "localhost";
        MySelf = new Node(LISTEN_PORT,IP,id);


        while(true){
            Function.print("Enter Command");
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();

            if(command.equals("create")){
                if(!MySelf.isInRing()){
                    MySelf.setPredecessor(null);
                    MySelf.setSuccesor(MySelf);
                    MySelf.setRingStatus(true);

                    MySelf.fillFingerTable();

                    ListeningThread listeningThread = new ListeningThread();
                    listeningThread.start();
                    Function.print("Starting Background Service");
                    Background background = new Background();
                    background.start();

                }
                else{
                    Function.print("Already in CHORD");
                }
            }
            else if(command.equals("put")) {

                try {
                    Function.print("Enter key");
                    Scanner scanner1 = new Scanner(System.in);
                    String key_ = scanner.nextLine();
                    Function.print("Enter value");
                    String val_ = scanner.nextLine();
                    int key_hash = Function.getHash(key_);
                    String res = MySelf.findSuccesor(key_hash);
                    Node node = Function.MessageToNode(res);
                    Function.Store_Key_Value(node,key_,val_,key_hash);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            else if(command.equals("join")){
                if(!MySelf.isInRing()){
                    MySelf.setPredecessor(null);
                    String Ip = "localhost";
                    Scanner scanner1 =  new Scanner(System.in);
                    Function.print("Enter Any nodes Port Number");
                    int port = scanner1.nextInt();
                    Node succesor = Function.findSuccesor(MySelf.getID(),Ip,port);
                    MySelf.setSuccesor(succesor);
                    MySelf.setRingStatus(true);
                    MySelf.fillFingerTable();
//                    MySelf.fillFingerTable();
                    ListeningThread listeningThread = new ListeningThread();
                    listeningThread.start();
                    Function.sendPredecessor(MySelf,succesor);

                    Background background = new Background();
                    background.start();

                }

            }
            else if(command.equals("get")){

                try {
                    Function.print("Enter key");
                    Scanner scanner1 = new Scanner(System.in);
                    String key_ = scanner.nextLine();
                    int key_hash = Function.getHash(key_);
                    String res = MySelf.findSuccesor(key_hash);
                    Node node = Function.MessageToNode(res);
                    Socket socket = new Socket(node.getIP(),node.getPort());
                    Function.sendMsg(Config.GET_KEY_VALUE_CODE+":"+key_,socket.getOutputStream());
                    String ans = Function.recvMsg(socket.getInputStream());
                    Function.print("RESULT : "+ans);
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if(command.equals("stablize")){
                MySelf.stablize();
            }
            else if(command.equals("fix_fingers")){
                MySelf.fixfingers();
            }
            else if(command.equals("show")){
                Function.DisplayNode(MySelf);
            }
        }
    }

    public static class Background extends Thread{
        @Override
        public void run() {
            super.run();
            while(true) {
//                Function.print("Stabilizing");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MySelf.stablize();
                MySelf.fixfingers();
            }
        }
    }
    public static class ListeningThread extends Thread {
        ServerSocket servsock;

        @Override
        public void run() {
            super.run();
            {
                try {
                    servsock = new ServerSocket(MySelf.getPort());
                    while (true){
                        if(close_all)break;
                        Socket sock = null;
                        sock = servsock.accept();
                        String message = Function.recvMsg(sock.getInputStream());
                        String code = message.split(":")[0];
                        if(code.equals(Config.FIND_SUCCESOR_CODE)){
                            String Message = MySelf.findSuccesor(Integer.parseInt(message.split(":")[1]));
                            Function.sendMsg( Message,sock.getOutputStream());
                        }
                        else if(code.equals(Config.TAKE_PREDECESSOR_CODE)){
                            Node node = new Node(Integer.parseInt(message.split(":")[3]),message.split(":")[2],Integer.parseInt(message.split(":")[1]));
                            MySelf.setPredecessor(node);
                            Function.sendMsg("ack",sock.getOutputStream());
                            int len = 0;
                            String s[] = new String[Config.MaxNodes];
                            for (String i : MySelf.hashMap.keySet()) {
                                try {
                                    int key_hash = Function.getHash(i);
                                    String key_ = i;
                                    String val_ = MySelf.hashMap.get(key_);
                                    if (key_hash==MySelf.getID() || Function.belongs(key_hash,node.getID(),MySelf.getID())){
                                    }else{
                                        Function.Store_Key_Value(node,key_,val_,key_hash);
                                        s[len]=key_;
                                        len++;
                                    }
//                                    System.out.println("key: " + i + " value: " + MySelf.hashMap.get(i)+" hash: "+getHash(i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            for (int i = 0 ; i< len ; i++){
                                MySelf.hashMap.remove(s[i]);

                            }
                        }
                        else if(code.equals(Config.GIVE_PREDECESSOR_CODE)){
                            String Message;
                            if(MySelf.getPredecessor()==null){
                                Message="null";

                            }else{
                                Message = Function.NodeToMessage(MySelf.getPredecessor());
                            }
                            Function.sendMsg(Message,sock.getOutputStream());

                        }
                        else if(code.equals(Config.NOTIFY_CODE)){
                            Node predecessor;
                            int id = Integer.parseInt(message.split(":")[1]);
                            String ip = message.split(":")[2];
                            int port = Integer.parseInt(message.split(":")[3]);
                            predecessor = new Node(port,ip,id);
                            if(MySelf.getPredecessor()==null || Function.belongs(id,MySelf.getPredecessor().getID(),MySelf.getID())){
                                MySelf.setPredecessor(predecessor);
                            }
                            Function.sendMsg("ack",sock.getOutputStream());

                        }
                        else if(code.equals(Config.STORE_KEY_VALUE_CODE)){

                            String key_ = message.split(":")[1];
                            String value_ = message.split(":")[2];
                            MySelf.hashMap.put(key_,value_);
                            Function.sendMsg("ack",sock.getOutputStream());
                        }
                        else if(code.equals(Config.GET_KEY_VALUE_CODE)) {
                            String key_ = message.split(":")[1];
                            if (MySelf.hashMap.containsKey(key_)){
                                String value = MySelf.hashMap.get(key_);
                                Function.sendMsg(value,sock.getOutputStream());
                            }
                            else{
                                Function.sendMsg("KEY_DOEST_NOT_EXIST",sock.getOutputStream());

                            }
                        }
                            sock.close();
                    }
                    servsock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
