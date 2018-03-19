/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg325project;

/**
 *
 * @author cjohnson
 */
public class Connection {
    
    public ConnectionManager Manager;
    
    public String HostName;
    
    public int Port;
    public Connection(ConnectionManager manager, String hostName, int port){
        Manager = manager;
        HostName = hostName;
        Port = port;
    }
}
