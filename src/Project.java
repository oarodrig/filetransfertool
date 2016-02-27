import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by Omar on 10/3/2015.
 * This is the entry point to our program. Ask the user to run as a server or as a client, and execute choice.
 * Allows for command-line arguments, but no input verification (ain't nobody got time fo that)
 */
public class Project {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        String host = "127.0.0.1";
        //TODO: Not sure of what range of ports to allow
        int port = 49152;

        String hostIP = "";
        try{
            hostIP = InetAddress.getLocalHost().toString();
        } catch(UnknownHostException e){
            System.out.println("You are not connected to a network.");
        }

        if(args.length < 1){
            boolean loop = false;
            do{
                System.out.println("Would you like to run a sender or a receiver session?");
                String option  = sc.nextLine().trim().substring(0,1).toLowerCase();

                if(option.equals("s")){
                    System.out.print("Please enter a hostname or IP address: ");
                    host = sc.nextLine(); // TODO: need input verification here
                    System.out.print("Please enter a port number: ");
                    port = sc.nextInt(); // TODO: need input verification here
                    System.out.println("Connecting to " + host + " on port " + port + "...");
                    Client c = new Client(host,port);
                    c.runClient();
                    loop = false;

                } else if (option.equals("r")){
                    System.out.print("Please enter a port number: ");
                    port = sc.nextInt(); // TODO: need input verification here

                    System.out.println("Listening as receiver at "+ hostIP + " on port " + port);
                    Server s = new Server(port);
                    s.runServer();
                    loop = false;

                } else {
                    System.out.println("Please enter \"sender\" or \"receiver\".");
                    loop = true;
                }
            } while (loop);
        } else if (args.length == 1){
            port = Integer.parseInt(args[0]); // TODO: need input verification here
            System.out.println("Listening as receiver at "+ hostIP + " on port " + args[0]);
            Server server = new Server(port);
            server.runServer();
        } else {
            host = args[0]; // TODO: need input verification here
            port = Integer.parseInt(args[1]); // TODO: need input verification here
            System.out.println("Connecting to " + host + " on port " + port + "...");
            Client client = new Client(host,port);
            client.runClient();
        }
    }
}
