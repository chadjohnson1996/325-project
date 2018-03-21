/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg325project;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author cjohnson
 */
public class Connection {

    public ConnectionManager Manager;

    public String HostName;

    public int Port;

    public Socket Socket;

    public BufferedReader In;

    public PrintWriter Out;

    public Lock Lock;

    public Connection(ConnectionManager manager, String hostName, int port) {
        try {
            Manager = manager;
            HostName = hostName;
            Port = port;
            Socket = new Socket(hostName, port);
            In = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
            Out = new PrintWriter(Socket.getOutputStream(), true);
            Lock = new ReentrantLock();
        }catch(Exception e){
            
        }

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
