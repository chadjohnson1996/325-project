/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg325project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

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
                    if(arg.equals(Heartbeat)){
                        HeartbeatHandler(conn);
                    }else if(arg.equals(Query)){
                        QueryHandler(conn, splitData[1]);
                    }else if(arg.equals(Transfer)){
                        TransferHandler(conn, splitData[1]);
                    }
                }catch(Exception e){
                    
                }
            }
        }
        );
    }
    
    public void HeartbeatHandler(Connection conn){
        //do nothing because heartbeat status is already set, may change later
    }
    
    public void QueryHandler(Connection conn, String body){
        
    }
    
    public void TransferHandler(Connection conn, String body){
        
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
