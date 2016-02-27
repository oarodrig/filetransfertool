import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

/**
 * Created by Omar on 10/28/2015.
 */
public class Authentication {

    // TODO: Did this with simple int matching. Might be able to do enums instead.
    private static final int WRONG_PASSWORD = 0;
    private static final int AUTHORIZED = 1;
    private static final int UN_NOT_FOUND = 2;
    private static final int AUTH_FAILED = 3;

    /**
     * authenticate
     * <p>
     * @param server The server object connected to the client to be authenticated
     * @return An integer symbolizing the state of authentication (see constant variables)
     * <p>
     * This method reads in a username (not case sensitive) and a password (case-sensitive)
     * and verifies it against a list stored as auth.txt
     */
    public static int authenticate(Server server){
        // TODO: How do we store usernames/passwords to authenticate against?
        // TODO: Adding a new user
        // SQL database? Keep it simple and just a plaintext file?
        // Are username and password predetermined?
        String userName = "";
        String password = "";
        System.out.println("Sender attempting authentication.");
        server.getOut().println("Please enter your username:");
        try{
            userName = server.getIn().readLine().trim();
            System.out.println("User "+ userName + "is requesting authentication.");
            server.getOut().println("Please enter your password:");
            password = server.getIn().readLine().trim();
        } catch (Exception e){
            server.getOut().println("Something went wrong with the connection.");
        }

        try{
            BufferedReader authReader = new BufferedReader(new FileReader("auth.txt"));
            while(authReader.ready()){
                StringTokenizer st = new StringTokenizer(authReader.readLine());
                if(st.nextToken().equals(userName)){
                    hasher h = new hasher();
                    byte[] b = h.hash(password.getBytes());
                    
                    if(st.nextToken().equals(Base64.encodeAsString(b))){
                        System.out.println("Password accepted. " + userName + " authenticated.");
                        server.getOut().println("Authenticated.");
                        return AUTHORIZED;
                    }
                    System.out.println("Wrong password. " + userName + " not authenticated.");
                    server.getOut().println("Wrong Password.");
                    return WRONG_PASSWORD;
                }
            }
            System.out.println("Username: "+ userName + "not found");
            server.getOut().println("Username not found.");
            return UN_NOT_FOUND;
        } catch(Exception e){
            server.getOut().println("Authentication failed: Reciever issue.");
            System.out.println("Authentication file not found.");
        }
        server.getOut().println("Authentication Failure.");
        System.out.println("Authentication failure.");
        return AUTH_FAILED;
    }
}
