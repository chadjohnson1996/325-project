/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg325project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author cjohnson
 */
public class Main {

    public static ExecutorService Pool = Executors.newCachedThreadPool();

    public static ConnectionManager Manager;

    public static int Timeout;

    /**
     * @param args the command line arguments
     */
    //"cwj14","50280-50299"
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println(new File(".").getAbsolutePath());

        logic();

    }

    public static void logic() {
        System.out.println("Starting");
        try {
            boolean shouldContinue = true;
            Scanner sc = new Scanner(System.in);
            init(sc);
            while (shouldContinue) {
                System.out.print("fileCheck>");
                String input = sc.nextLine();
                shouldContinue = handle(input);
            }
        } catch (Exception e) {
            logic();
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
        System.out.print("Input port to listen on>");
        int port = parseInt(sc.nextLine());
        Manager = new ConnectionManager(port);

        System.out.print("Input timeout in milliseconds>");
        Timeout = parseInt(sc.nextLine());
        loadPeers();
        getFilesToShare();
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
                    fileConn.Write(Manager.BuildFileTransfer(filename));
                    try {
                        File file = new File("./obtained/" + filename);
                        FileWriter fileWriter = new FileWriter(file);
                        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                        String line;
                        while ((line = fileConn.Read()) != null) {
                            bufferedWriter.write(line + "\n");
                            bufferedWriter.flush();
                        }
                        bufferedWriter.close();
                        fileWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
            System.out.println("Starting to connect to " + conn.HostName);
            conn.Open();
            if(conn.Opened){
                System.out.println("Connection successful");
            }else{
                System.out.println("Connection failed");
            }

        }
    }

    public static void exitHandler() {
        leaveHandler();
    }
}
