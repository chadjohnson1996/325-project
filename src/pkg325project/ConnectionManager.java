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
    
    public ServerSocket Server;
    public ArrayList<Connection> Clients = new ArrayList<Connection>();
    public ArrayList<TextFile> FilesToShare = new ArrayList<TextFile>();
    
    public ConnectionManager(int port){
        try{
           Server = new ServerSocket(port); 
        }catch(Exception e){
            
        }
        
    }
    
}
