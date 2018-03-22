/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg325project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import static pkg325project.Main.Manager;

/**
 *
 * @author cjohnson
 */
public class ConnectionManager {
    
    public static final int MaxTimeout = 30000;
    public static final int TimeoutTryInterval = 3000;
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
            Server = new ServerSocket(port);
            Port = port;
            RunTimeoutPoll();
            RunTimeoutCheck();
        } catch (Exception e) {
            
        }
        
    }
    
    public void Listen() {
        try {
            while (true) {
                Socket server = Server.accept();
                Connection conn = new Connection(this, server);
                Servers.add(conn);
            }
        } catch (Exception e) {
            return;
        }
        
    }
    
    public void ServerHandler(Connection conn) {
        final Connection localConn = conn;
        Main.Pool.execute(() -> {
            while (true) {
                try {
                    String data = localConn.Read();
                    localConn.HeartbeatStatus = true;
                    
                    String[] splitData = data.split(":");
                    String arg = splitData[0];
                    if (arg.equals(Heartbeat)) {
                        HeartbeatHandler(conn);
                    } else if (arg.equals(Query)) {
                        QueryHandler(conn, splitData[1]);
                    } else if (arg.equals(Transfer)) {
                        TransferHandler(conn, splitData[1]);
                    }
                } catch (Exception e) {
                    
                }
            }
        }
        );
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
        if (FilesToShare.stream().anyMatch(x -> x.FileName.equals(fileName))) {
            conn.Write(HasFile);
            return;
        }
        
        for (Connection peer : Clients) {
            try {
                if (!peer.Socket.getInetAddress().equals(conn.Socket.getInetAddress())) {
                    peer.Write(BuildQuery(id, fileName));
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
    
    public String BuildFileTransfer(String fileName){
        return Transfer + ":" + fileName;
    }
    
    public void TransferHandler(Connection conn, String fileName) {
        try {
            File file = new File("./shared/" + fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                conn.Write(line);
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void RunTimeoutPoll() {
        Main.Pool.execute(() -> {
            try {
                while (true) {
                    Thread.sleep(TimeoutTryInterval);
                    for (Connection conn : Clients) {
                        conn.Write(Heartbeat);
                    }
                    
                    for (Connection conn : Servers) {
                        conn.Write(Heartbeat);
                    }
                }
            } catch (Exception e) {
                RunTimeoutPoll();
            }
            
        });
    }
    
    public void RunTimeoutCheck() {
        Main.Pool.execute(() -> {
            try {
                while (true) {
                    Thread.sleep(MaxTimeout);
                    for (Connection conn : Clients) {
                        if (!conn.HeartbeatStatus) {
                            Clients.remove(conn);
                            conn.Close();
                        }
                        conn.HeartbeatStatus = false;
                    }
                    
                    for (Connection conn : Servers) {
                        if (!conn.HeartbeatStatus) {
                            Clients.remove(conn);
                            conn.Close();
                        }
                        conn.HeartbeatStatus = false;
                    }
                }
            } catch (Exception e) {
                RunTimeoutCheck();
            }
            
        });
    }
}
