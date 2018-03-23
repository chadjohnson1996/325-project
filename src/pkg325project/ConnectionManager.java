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

    public static final int MaxTimeout = 12000;
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

            Port = port;
            RunTimeoutPoll();
            RunTimeoutCheck();
        } catch (Exception e) {

        }

    }

    public void Listen() {
                    
        new Thread(() -> {
            try {
                Server = new ServerSocket(Port);
                while (true) {
                    Socket server = Server.accept();
                    Connection conn = new Connection(this, server);
                    System.out.println("Accepted connection from " + conn.HostName);
                    Servers.add(conn);
                    ServerHandler(conn);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }finally{
                try{
                    Server.close();
                }catch(Exception e){
                    
                }
                
            }

        }).start();

    }

    public void ServerHandler(Connection conn) {
        final Connection localConn = conn;
        new Thread(() -> {
            while (true) {
                try {
                    String data = localConn.Read();
                    localConn.HeartbeatStatus = true;

                    String[] splitData = data.split(":");
                    String arg = splitData[0];
                    if (arg.equals(Heartbeat)) {
                        System.out.println("Received heartbeat " + data);
                        HeartbeatHandler(conn);
                    } else if (arg.equals(Query)) {
                        System.out.println("Received query " + data);
                        QueryHandler(conn, splitData[1]);
                    } else if (arg.equals(Transfer)) {
                        System.out.println("Received transfer request " + data);
                        TransferHandler(conn, splitData[1]);
                    }
                } catch (Exception e) {

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
        if (FilesToShare.stream().anyMatch(x -> x.FileName.equals(fileName))) {
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
            System.out.println("Finished transfering file " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RunTimeoutPoll() {
        new Thread(() -> {
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

        }).start();
    }

    public void RunTimeoutCheck() {
        new Thread(() -> {
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

        }).start();
    }
}
