import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by Omar on 10/3/2015.
 * Code for running as a server.
 */
public class Server {
	private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
	
	public Server(int port){
        boolean socketCreated = false;
        while(!socketCreated){
            try {
                serverSocket = new ServerSocket(port);
                socketCreated = true;
            } catch(Exception IOException) {
                Scanner sc = new Scanner(System.in);
                System.out.println("Cannot create socket. Would you like to try another port number? (y/n)");
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

    /**
     * runServer
     * <p>
     * @return true if server ran successfully
     * <p>
     * This method will cause our server to start listening on its designated port,
     * and once connected will start services.
     */
	public boolean runServer(){

        // this do-while block begins by listening until a client connects.
        // it will then create an input and output stream to the client,
        // and finally, it will kick off the authentication method/class.
        // if the client fails authentication 3 times, the server will go
        // back to listening.
        do{
            try {
                System.out.println("Listening for a connection...");
                clientSocket = serverSocket.accept();
                System.out.println(clientSocket.getInetAddress() + " has connected.");
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch(IOException ioe){
                Scanner sc = new Scanner(System.in);
                System.out.println("Cannot connect to sender.");
                System.out.println("Continue listening? (y/n)");
                if(sc.nextLine().toLowerCase().startsWith("y")){
                    continue;
                }
            }
        } while (!authenticateClient());

        // at this point our client is authenticated, so we begin file transfer
        System.out.println("Beginning file transfer.");
        try {
            Transfer.receiveFile(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File transfer complete.");
        out.println("Bye.");
        return true;
	}

    /**
     * authenticateClient
     * <p>
     * @return true if client was successfully authenticated
     * <p>
     * This method kicks off the authenticate method in the Authentication class.
     * It will also test if the client has tried to authenticate too many times
     * in the connection (3 failures).
     */
    private boolean authenticateClient(){
        int tries = 0;
        System.out.println("Number of tries: " + tries);
        boolean authenticated = false;
        do{
            int authCode = Authentication.authenticate(this);

            if(authCode == 1){ //Refer to Authorization class to see authCode meanings.
                return true;
            }
            tries++;
        } while (!authenticated && tries <4);
        System.out.println("Exceeded maximum amount of tries. Closing connection.");
        return false;
    }

    /**
     * readClientInput
     * <p>
     * @return String containing response from client
     * <p>
     * This is a helper method to collect the client response from the buffer.
     */
    public String readClientInput(){
        String response = "";
        try{
            response = in.readLine().trim();
        } catch(IOException ioe){
            System.out.println("Unable to connect");
            return null;
        }
        return response;
    }

    /**
     * getOut
     * <p>
     * @return the PrintWriter associated with the client connected to this server
     * <p>
     * Use this to print to the client
     */
    public PrintWriter getOut() {
        return out;
    }
    /**
     * getIn
     * <p>
     * @return The BufferedReader associated with the client connected to this server
     * <p>
     * Use this to read from the client.
     */
    public BufferedReader getIn() {
        return in;
    }

    public InputStream getInputStream(){
        try{
            return clientSocket.getInputStream();
        }catch(IOException io){
            System.out.println("Unable to connect");
            return null;
        }
    }
}
