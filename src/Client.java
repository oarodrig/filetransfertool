import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by Omar on 10/3/2015.
 * Code for running as a client.
 */
public class Client {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Client(String host, int port){
        boolean connected = false;
        while(!connected){
            try{
                socket = new Socket(host, port);
                System.out.println("Connection successful!");
                connected = true;
            } catch (UnknownHostException uhe){
                Scanner sc = new Scanner(System.in);
                System.out.println("Cannot Find host. Would you like to enter another hostname? (y/n)");
                if(sc.nextLine().toLowerCase().startsWith("y")){
                    System.out.println("Enter hostname: ");
                    host = sc.nextLine();
                    continue;
                }else{
                    System.out.println("Quitting.");
                    System.exit(1);
                }
            } catch (IOException ioe) {
                Scanner sc = new Scanner(System.in);
                System.out.println("Cannot connect to port. Would you like to enter another port number? (y/n)");
                if(sc.nextLine().toLowerCase().startsWith("y")){
                    System.out.println("Enter port number: ");
                    port = Integer.parseInt(sc.nextLine());
                    continue;
                }else{
                    System.out.println("Quitting.");
                    System.exit(1);
                }
            }
        }

    }

    public boolean runClient(){
        Scanner sc = new Scanner (System.in);

        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ioe){
            System.out.println("Cannot connect to receiver.");
            return false;
        }

        String fromServer;
        String fromUser;

        System.out.println("Select an option:" +
                "\n 1. Normal Run" +
                "\n 2. DEMO: Some failures, ultimate success (corrupted hash)" +
                "\n 3. DEMO: Some failures, ultimate success (corrupted data)" +
                "\n 4. DEMO: Max failures, transfer failed (corrupted hash)" +
                "\n 5. DEMO: Max failures, transfer failed (corrupted data)");

        int runType;
        try{
            runType = Integer.parseInt(sc.nextLine().substring(0,1));
        }catch(NumberFormatException e){
            System.out.println("Not a number. Assuming normal run.");
            runType = 1;
        }catch(IndexOutOfBoundsException e){
            System.out.println("No input. Assuming normal run.");
            runType = 1;
        }
        if(runType == 2){
            Transfer.setFailure(false,2);
        }else if(runType == 3){
            Transfer.setFailure(true,2);
        }else if(runType == 4){
            Transfer.setFailure(false,4);
        }else if(runType == 5){
            Transfer.setFailure(true,4);
        }

        while ((fromServer = readServerInput()) != null) {
            System.out.println("Receiver: " + fromServer);
            if (fromServer.equals("Bye.")){
                break;
            }

            if(fromServer.equals("Ready to receive file.")){
                try {
                    Transfer.transferFile(this);
                } catch (IOException e) {
                    System.out.println("There was an error in transferring the file. Terminating connection.");
                    return false;
                }
            }

            if(fromServer.endsWith("?") || fromServer.endsWith(":")){
                fromUser = sc.nextLine();
                if (fromUser != null) {
                    out.println(fromUser);
                }
            }
        }

        System.out.println("Receiver has terminated communication.");
        return true;
    }

    public String readServerInput(){
        String response = "";
        try{
            response = in.readLine().trim();
        } catch(IOException ioe){
            System.out.println("Unable to connect");
            return null;
        }
        return response;
    }

    public PrintWriter getOut() {
        return out;
    }

    public OutputStream getOutputStream(){
        try{
            return socket.getOutputStream();
        }catch(IOException io){
            System.out.println("Can't connect.");
            return null;
        }
    }
}
