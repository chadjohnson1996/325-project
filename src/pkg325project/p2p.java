/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Integer.parseInt;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author cjohnson
 */
public class p2p {


    public static ConnectionManager Manager;
    
    public static int MainPort = 50290;

    /**
     * @param args the command line arguments
     */
    //"cwj14","50280-50299"
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println(new File(".").getAbsolutePath());

        Scanner sc = new Scanner(System.in);
        init(sc);
        logic(sc);

    }

    public static void logic(Scanner sc) {
        System.out.println("Starting");
        try {
            boolean shouldContinue = true;

            while (shouldContinue) {
                System.out.print("fileCheck>");
                String input = sc.nextLine();
                shouldContinue = handle(input);
            }
        } catch (Exception e) {
            logic(sc);
        }
    }

    public static boolean handle(String input) {
        String[] data = input.split(" ");

        if (data.length == 0) {
            return true;
        }

        String command = data[0].toLowerCase();
        if (command.equals("get")) {
            getHandler(data[1]);
            return true;
        } else if (command.equals("leave")) {
            leaveHandler();
            return true;
        } else if (command.equals("connect")) {
            connectHandler();
            return true;
        } else if (command.equals("exit")) {
            exitHandler();
            return false;
        } else {
            System.out.println("Invalid command");
            return true;
        }
    }

    public static void init(Scanner sc) {
        System.out.println("Note differences in transfered file size are due to text encoding not due to failure to clear buffers");
        Manager = new ConnectionManager(MainPort);

        loadPeers();
        getFilesToShare();
        Manager.Listen();
    }

    public static void loadPeers() {
        try {
            File file = new File("./config_neighbors.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] data = line.split(";");
                String host = data[0];
                int port = parseInt(data[1]);
                Manager.Clients.add(new Connection(Manager, host, port));
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getFilesToShare() {
        try {
            File file = new File("./config_sharing.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] data = line.split(";");
                String fileName = data[0];
                String bookName = data[1];
                Manager.FilesToShare.add(new TextFile(fileName, bookName));
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getHandler(String filename) {
        try {
            String id = UUID.randomUUID().toString();
            String query = Manager.BuildQuery(id, filename);
            for (Connection conn : Manager.Clients) {
                System.out.println("Sending query " + query + " to host " + conn.HostName);
                conn.Write(query);
                String response = conn.Read();
                if (response.trim().isEmpty()) {
                    continue;
                }

                Connection fileConn = null;
                if (response.startsWith(ConnectionManager.HasFile)) {
                    fileConn = new Connection(conn.Manager, conn.HostName, conn.Port);
                }

                if (response.startsWith(ConnectionManager.Response)) {
                    String[] data = response.split(":")[1].split(";");
                    fileConn = new Connection(Manager, data[1], parseInt(data[2]));

                }

                fileConn.Open();
                fileConn.Write(Manager.BuildFileTransfer(filename));
                try {
                    File file = new File("./obtained/" + filename);
                    FileWriter fileWriter = new FileWriter(file);
                    BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get("./obtained/" + filename), 
                                                StandardCharsets.UTF_8);
                    String line;
                    System.out.println("Beginning to receive file " + filename);
                    while ((line = fileConn.Read()) != null) {
                        bufferedWriter.write(line + "\n");
                        bufferedWriter.flush();
                    }
                    bufferedWriter.close();
                    fileWriter.close();
                    System.out.println("Finished Receiving file " + filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return;
            }

            System.out.println("File not found");
        } catch (Exception e) {

        }

    }

    public static void leaveHandler() {
        for (Connection conn : Manager.Clients) {
            conn.Close();
        }
    }

    public static void connectHandler() {
        for (Connection conn : Manager.Clients) {
            System.out.println("Starting to connect to " + conn.HostName + ": " + conn.Port);
            conn.Open();
            if (conn.Opened) {
                System.out.println("Connection successful");
            } else {
                System.out.println("Connection failed");
            }

        }
    }

    public static void exitHandler() {
        leaveHandler();
    }
}

class TextFile {
    public String FileName;
    public String BookName;
    
    public TextFile(String fileName, String bookName){
        FileName = fileName;
        BookName = bookName;
    }
}

class ConnectionManager {

    public static final int MaxTimeout = 120000;
    public static final int TimeoutTryInterval = 30000;
    public static final String Heartbeat = "H";
    public static final String Query = "Q";
    public static final String Response = "R";
    public static final String Transfer = "T";
    public static final String HasFile = "HF";

    public ServerSocket Server;
    public ArrayList<Connection> Clients = new ArrayList<Connection>();
    public ArrayList<Connection> Servers = new ArrayList<Connection>();
    public HashSet<String> DispatchedQueries = new HashSet<String>();
    public ArrayList<TextFile> FilesToShare = new ArrayList<TextFile>();
    public int Port;

    public ConnectionManager(int port) {
        try {

            Port = port;
            RunTimeoutPoll();
            RunTimeoutCheck();
        } catch (Exception e) {

        }

    }

    public void Listen() {
        final ConnectionManager _this = this;
        new Thread(new Runnable() {

            public void run() {
                try {
                    Server = new ServerSocket(Port);
                    while (true) {
                        Socket server = Server.accept();
                        Connection conn = new Connection(_this, server);
                        System.out.println("Accepted connection from " + conn.HostName);
                        Servers.add(conn);
                        ServerHandler(conn);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return;
                } finally {
                    try {
                        Server.close();
                    } catch (Exception e) {

                    }

                }
            }
        }).start();

    }

    public void ServerHandler(Connection conn) {
        final Connection localConn = conn;
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        String data = localConn.Read();
                        localConn.HeartbeatStatus = true;

                        String[] splitData = data.split(":");
                        String arg = splitData[0];
                        if (arg.equals(Heartbeat)) {
                            System.out.println("Received heartbeat " + data);
                            HeartbeatHandler(localConn);
                        } else if (arg.equals(Query)) {
                            System.out.println("Received query " + data);
                            QueryHandler(localConn, splitData[1]);
                        } else if (arg.equals(Transfer)) {
                            System.out.println("Received transfer request " + data);
                            TransferHandler(localConn, splitData[1]);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
        ).start();
    }

    public void HeartbeatHandler(Connection conn) {
        //do nothing because heartbeat status is already set, may change later
    }

    public void QueryHandler(Connection conn, String body) {
        String[] data = body.split(";");
        String id = data[0];
        String fileName = data[1];

        //if it has already received the query, don't do it again
        if (DispatchedQueries.contains(id)) {
            conn.Write("");
            return;
        }

        DispatchedQueries.add(id);
        
        boolean hasFile = false;
        for(TextFile file : FilesToShare){
            if(file.FileName.equals(fileName)){
                hasFile = true;
                break;
            }
        }
        if (hasFile) {
            System.out.println("Peer has requested file");
            conn.Write(HasFile);
            return;
        }

        System.out.println("Peer does not have requested file, checking its own peers");

        for (Connection peer : Clients) {
            try {
                if (!peer.Socket.getInetAddress().equals(conn.Socket.getInetAddress())) {

                    String query = BuildQuery(id, fileName);
                    System.out.println("Sending query " + query + " to host " + conn.HostName);

                    peer.Write(query);

                    String response = peer.Read();
                    if (response.trim().isEmpty()) {
                        continue;
                    }

                    if (response.startsWith(HasFile)) {
                        conn.Write(BuildResponse(id, peer.HostName, new Integer(peer.Port).toString()));
                    }

                    if (response.startsWith(Response)) {
                        conn.Write(response);
                        return;
                    }
                }
            } catch (Exception e) {

            }

        }

        conn.Write("");
    }

    public String BuildQuery(String id, String fileName) {
        return Query + ":" + id + ";" + fileName;
    }

    public String BuildResponse(String id, String hostName, String port) {
        return Response + ":" + id + ";" + hostName + ";" + port;
    }

    public String BuildFileTransfer(String fileName) {
        return Transfer + ":" + fileName;
    }

    public void TransferHandler(Connection conn, String fileName) {
        try {
            System.out.println("Received request to transfer " + fileName);
            File file = new File("./shared/" + fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                conn.Write(line);
            }
            fileReader.close();
            conn.In.close();
            conn.Close();
            System.out.println("Finished transfering file " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RunTimeoutPoll() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(TimeoutTryInterval);
                        for (Connection conn : Clients) {
                            if (conn.Opened) {
                                System.out.println("Sending heartbeat to connection " + conn.HostName);
                                conn.Write(Heartbeat);
                            }
                        }
                    }
                } catch (Exception e) {
                    RunTimeoutPoll();
                }

            }

        }).start();
    }

    public void RunTimeoutCheck() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(MaxTimeout);

                        for (int i = 0; i < Servers.size(); i++) {
                            Connection conn = Servers.get(i);
                            if (conn.Opened && !conn.HeartbeatStatus) {
                                System.out.println("Timeout elapsed, closing connection " + conn.HostName);
                                conn.Close();
                            }
                            conn.HeartbeatStatus = false;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error occured with timeout check");
                    RunTimeoutCheck();
                }
            }

        }).start();
    }
}

class Connection {

    public ConnectionManager Manager;

    public String HostName;

    public int Port;

    public Socket Socket;

    public BufferedReader In;

    public PrintWriter Out;

    public Lock Lock;

    public Boolean HeartbeatStatus;
    
    public Boolean Opened = false;

    public Connection(ConnectionManager manager, Socket socket) {
        try {
            Manager = manager;
            HostName = socket.getInetAddress().getCanonicalHostName();
            Port = socket.getPort();
            In = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Out = new PrintWriter(socket.getOutputStream(), true);
            Socket = socket;
            Lock = new ReentrantLock();
        }catch(Exception e){
            System.out.println("Error making connection: " + e.getMessage());
        }

    }

    public Connection(ConnectionManager manager, String hostName, int port) {

        Manager = manager;
        HostName = hostName;
        Port = port;
        Lock = new ReentrantLock();

    }

    public void Open() {
        try {
            Socket = new Socket(HostName, Port);
            In = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
            Out = new PrintWriter(Socket.getOutputStream(), true);
            Opened = true;
        } catch (Exception e) {
            System.out.println("Socket failed to open\n" + e.getMessage());
        }

    }

    public void Close() {
        try {
            Socket.close();
        } catch (Exception e) {

        }
        Opened = false;
    }

    public void Write(String toWrite) {
        Lock.lock();
        try {
            Out.write(toWrite + "\n");
            Out.flush();
        } finally {
            Lock.unlock();
        }

    }

    public String Read() throws Exception {
        Lock.lock();
        try {
            String data = In.readLine();
            return data;
        } finally {
            Lock.unlock();
        }

    }

    public String[] ReadArray() throws Exception {
        Lock.lock();
        try {
            String data = In.readLine();
            return data.split(";");
        } finally {
            Lock.unlock();
        }
    }

    @Override
    public int hashCode() {
        return HostName.hashCode() ^ Port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Connection other = (Connection) obj;
        if (this.Port != other.Port) {
            return false;
        }
        if (!Objects.equals(this.HostName, other.HostName)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return HostName + ";" + Port;
    }
}
