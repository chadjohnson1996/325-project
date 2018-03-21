/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pkg325project;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author cjohnson
 */
public class Main {

    public static ExecutorService Pool = Executors.newCachedThreadPool();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    
        
    }
    
    public static void logic(){
        try{
            boolean shouldContinue = true;
            Scanner sc = new Scanner(System.in);
            while(shouldContinue){
                System.out.print("fileCheck>");
                String input = sc.nextLine();
                shouldContinue = handle(input);
            }
        }catch(Exception e){
            logic();
        }
    }
    
    public static boolean handle(String input){
        String[] data = input.split(" ");
        
        if(data.length == 0){
            return true;
        }
        
        String command = data[0].toLowerCase();
        if(command.equals("get")){
            getHandler(data[1]);
            return true;
        }else if(command.equals("leave")){
            leaveHandler();
            return true;
        }else if(command.equals("connect")){
            connectHandler();
            return true;
        }else if(command.equals("exit")){
            exitHandler();
            return false;
        }else{
            System.out.println("Invalid command");
            return true;
        }
    }
    
    public static void getHandler(String filename){
        
    }
    
    public static void leaveHandler(){
        
    }
    
    public static void connectHandler(){
        
    }
    
    public static void exitHandler(){
        
    }
}
