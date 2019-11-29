import java.io.IOException;
import java.net.Socket;
import java.security.Key;
import java.util.Vector;
import java.util.HashMap; // import the HashMap class

public class Node {
    private int ID;
    private int PORT;
    private String IP;
    private boolean InRing;
    private  Node succesor;
    private Node predecessor;
    Vector<Node> FingerTable = new Vector();

    HashMap<String, String> hashMap;



    Node(int port,String ip) throws Exception {
        this.PORT = port;
        this.IP = ip;
        this.ID = Function.getHash(ip+":"+port);
        InRing = false;
        hashMap = new HashMap<String, String>();
    }


    Node(int port,String ip,int id){
        this.PORT = port;
        this.IP = ip;
        this.ID = id;
        InRing = false;
        hashMap = new HashMap<String, String>();

    }

    public void setPredecessor(Node node){
        predecessor = node;
        if(predecessor!=null)
            predecessor.setRingStatus(true);
    }
    public Node getPredecessor(){
        return predecessor;
    }
    public void setSuccesor(Node node){
        succesor = node;
        succesor.setRingStatus(true);
    }
    public Node getSuccesor(){
        return succesor;
    }
    public boolean isInRing(){
        return InRing;
    }
    public void setRingStatus(boolean isInRing){
        this.InRing  = isInRing;
    }
    public int getPort(){
        return this.PORT;
    }
    public String getIP(){
        return this.IP;
    }
    public int getID(){
        return this.ID;
    }


    public void fillFingerTable() {
        for(int i = 0  ; i < Config.M ; i++){
            FingerTable.add(this);
        }
    }

    public String findSuccesor(int key) {
//        Function.print("myid "+ID+" finding "+key);
        if(key > this.getID() && key <=this.succesor.getID()){
            return Function.NodeToMessage(this.succesor);
        }
        else if(key == succesor.getID()){
            return Function.NodeToMessage(this.succesor);
        }
        else if(succesor.getID()==this.getID() || key==this.getID()){
            return Function.NodeToMessage(this);
        }
        //ID = 4 SUC = 2 KEY = 5 ,ID = 4 SUC = 2 KEY = 1
        else if((ID > succesor.getID() )&& (key>this.getID() && key > this.succesor.getID())||((ID > succesor.getID() ) && key<this.getID() && key < this.succesor.getID())){
            return Function.NodeToMessage(this.succesor);
        }
//        //id 2 suc 4 key 0
//        else if(key<this.getID() && key < this.succesor.getID()){
//            return Function.NodeToMessage(this);
//        }
        else{
            Node node = closestPreceedingNode(key);
//            Function.print("precesing : "+node.ID);
            if (node ==this)return Function.NodeToMessage(this);

            Node succesor = Function.findSuccesor(key,node.IP,node.PORT);
            return Function.NodeToMessage(succesor);
        }
    }

    private Node closestPreceedingNode(int key) {
        for(int i = Config.M-1 ; i >=0 ; i--){
            if(Function.belongs(FingerTable.get(i).getID(),getID(),key)){
                // finger[i] ∈ (n,key) where E is belongs
                return FingerTable.get(i);
            }
        }
        return this;
    }

    public void stablize() {
        Node s_p = Function.SuccesorsPredecessor(succesor.IP,succesor.PORT);
        if(s_p==null){
//            Function.print("s_p : null");

        }
        else {
//            Function.print("S_p" + s_p.getID());
//            Function.print("s_p not null" +" "+s_p.getID()+" "+ this.ID+" "+this.succesor.getID());
            if ( this.ID == this.succesor.getID()||Function.belongs(s_p.getID(), this.ID, this.succesor.getID())) {
//                Function.print("Yes");
                this.succesor = s_p;
//                Function.print("Successor changed : " + this.succesor.getID());
            }
        }
        notify(succesor,this);

    }
    //notify succesor about myself as predecessor
    private void notify(Node succesor, Node node) {
//        Function.print("Notifying to "+succesor.getID());
        Function.sendPredecessorAsNotifier(node,succesor);
    }


    public void fixfingers() {
        FingerTable.set(0,this.succesor);
        for(int i = 1 ; i < FingerTable.size() ; i++){
            int key = (this.ID + (int)Math.pow(2,i))%Config.MaxNodes;
            Node node = Function.MessageToNode(findSuccesor(key));
            FingerTable.set(i,node);
        }
    }
}