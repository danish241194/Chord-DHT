import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.HashMap;

public class Function {
    public static int MAX_MSG_SIZE=Config.MaxNodes+100;

    public static void print(String s){
        System.out.println(s);
    }
    public static int getHash(String input)throws  Exception{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        return no.mod(BigInteger.valueOf(Config.MaxNodes)).intValue();
    }

    public  static  void showAllPortsWithChordKeys() throws Exception{
            String arr[]  =new String[8];
            for(int i = 0 ; i < arr.length ; i++){
                arr[i]="key "+i;
            }
            int ar[] = new int[8];
            int count = 0 ;
            int port=2000;
            while(count < 16){
                int val = getHash("localhost:"+port);

                if(ar[val]<2){
                    arr[val] += " port "+ar[val]+" = "+port;
                    count++;
                    ar[val]+=1;
                }
                port++;
            }

            for(String a : arr){
                print(a);
            }
    }
    public static void sendMsg(String message, OutputStream os) throws  Exception{
//        Function.print("Msg to Send : "+message);

        message = message+";;";
        int i = message.length();
        for(;i<MAX_MSG_SIZE;i++){
            message+="x";
        }
//        Function.print("Msg to Send E : "+message);

        byte[] Arr = message.getBytes();
        os.write(Arr, 0, Arr.length);
        os.flush();
    }

    public static String recvMsg(InputStream is) throws Exception {
        byte[] arr = new byte[MAX_MSG_SIZE+1];
//        int bytesRead = is.read(arr, 0, MAX_MSG_SIZE);

        int bytesRead = -1;
        bytesRead = is.read(arr,0,MAX_MSG_SIZE);
        int current = bytesRead;

        do {
            bytesRead =
                    is.read(arr, current, (MAX_MSG_SIZE-current));
            if(bytesRead >= 0) current += bytesRead;
        }while(bytesRead > 0);


        String s = new String(arr);
//        Function.print("Msg Received E: "+s);
//        Function.print("Msg Received : "+s.split(" ")[0]);

        return s.split(";;")[0];
    }
    public static void Store_Key_Value(Node receiver_node,String key,String Value,int hash_of_key){
        Socket sock = null;
        try {
//            Function.print("Send key value to "+NodeToMessage(receiver_node));
            sock = new Socket(receiver_node.getIP(),receiver_node.getPort());
            Function.sendMsg(Config.STORE_KEY_VALUE_CODE+":"+key+":"+Value+":"+hash_of_key,sock.getOutputStream());
            Function.recvMsg(sock.getInputStream());//ack
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static  void DisplayNode(Node MySelf){
        print("Myself");
        print("ID - "+MySelf.getID()+" Address:<"+MySelf.getIP()+ " : "+ MySelf.getPort()+">");
        print("predecessor");

        if(MySelf.getPredecessor()!=null){
            print("ID - "+MySelf.getPredecessor().getID()+" Address: <"+MySelf.getPredecessor().getIP()+ " : "+ MySelf.getPredecessor().getPort()+">");

        }
        print("Succesor");

        if(MySelf.getSuccesor()!=null){
            print("ID - "+MySelf.getSuccesor().getID()+" Address:<"+MySelf.getSuccesor().getIP()+ " : "+ MySelf.getSuccesor().getPort()+">");

        }
        print("Finger Table");

        if(!MySelf.isInRing())
        {
            return;
        }
        for(int i = 0 ; i <MySelf.FingerTable.size() ; i++){
            int key = (MySelf.getID() + (int)Math.pow(2,i))%Config.MaxNodes;

            Function.print("key "+key+" "+MySelf.FingerTable.get(i).getIP() + " "+MySelf.FingerTable.get(i).getPort()+" "+MySelf.FingerTable.get(i).getID());
        }
        print("\nKey Value MAP\n");
        for (String i : MySelf.hashMap.keySet()) {
            try {
                System.out.println("key: " + i + " value: " + MySelf.hashMap.get(i)+" hash: "+getHash(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Node findSuccesor(int id,String ip , int port) {
        Node successor=null;
        try {
            Socket sock = new Socket(ip, port);
            Function.sendMsg(Config.FIND_SUCCESOR_CODE+":"+id,sock.getOutputStream());
            String result = Function.recvMsg(sock.getInputStream());
//            Function.print("REceived : "+result);
            String ip_successor = result.split(":")[0];
            int port_successor = Integer.parseInt(result.split(":")[1]);
            int id_successor = Integer.parseInt(result.split(":")[2]);
            successor = new Node(port_successor,ip_successor,id_successor);
            sock.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return successor;
    }

    public static String NodeToMessage(Node node) {
        String s = node.getIP()+":"+node.getPort()+":"+node.getID();
        return s;
    }

    public static boolean belongs(int mid, int src, int dst) {
        if(src<dst &&mid > src && mid <dst){
            return true;
        }
        else if(src > dst && mid < src && mid < dst){
            return true;
        }
        else if(src > dst && mid > src && mid > dst){
            return true;
        }
        return false;
    }

    public static void sendPredecessor(Node mySelf, Node succesor) {
        try {
            Socket sock = new Socket(succesor.getIP(), succesor.getPort());
            Function.sendMsg(Config.TAKE_PREDECESSOR_CODE+":"+mySelf.getID()+":"+mySelf.getIP()+":"+mySelf.getPort(),sock.getOutputStream());
            String result = Function.recvMsg(sock.getInputStream());
            return;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    public static Node SuccesorsPredecessor(String IP, int PORT) {
        Node successors_Predecessor=null;
        try {
            Socket sock = new Socket(IP, PORT);
            Function.sendMsg(Config.GIVE_PREDECESSOR_CODE,sock.getOutputStream());
            String result = Function.recvMsg(sock.getInputStream());
            if(result.equals("null")){
                return null;
            }
            String ip_successor = result.split(":")[0];
            int port_successor = Integer.parseInt(result.split(":")[1]);
            int id_successor = Integer.parseInt(result.split(":")[2]);
            successors_Predecessor = new Node(port_successor,ip_successor,id_successor);
            sock.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return successors_Predecessor;
    }



    public static void sendPredecessorAsNotifier(Node node, Node succesor) {
        try {
            Socket sock = new Socket(succesor.getIP(), succesor.getPort());
            Function.sendMsg(Config.NOTIFY_CODE+":"+node.getID()+":"+node.getIP()+":"+node.getPort(),sock.getOutputStream());
            String result = Function.recvMsg(sock.getInputStream());
            sock.close();

            return;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    public static Node MessageToNode(String succesor) {
        String ip  = succesor.split(":")[0];
        int port = Integer.parseInt(succesor.split(":")[1]);
        int id = Integer.parseInt(succesor.split(":")[2]);
        return new Node(port,ip,id);
    }
}

