import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Created by Omar on 10/19/2015.
 * HEAVILY modified by Mi and Francis :P
 */
public class Transfer {

    // Constants
    public final static int DEFAULT_BUFFER_SIZE = 1000000;
    private final static int CHUNK_SIZE = 1024 * 1024;
    private final static int LENGTH_SIZE = 4;
    private final static String DEFAULT_KEY_FILE = "key.txt";
    
    // Sender
    private static boolean runNormal = true;
    private static boolean failData = true;
    private static int numberOfFails = 2;
    private static int failCountSender = 0;
    private static BufferedInputStream keyFisSender = null;
    private static byte[] previousKeySender = null;
    private static int originalKeySizeSender;
    private static int keySizeSender;
    
    // Receiver
    private static int failCountReceiver = 0;
    private static BufferedInputStream keyFisReceiver = null;
    private static byte[] previousKeyReceiver = null;
    private static int originalKeySizeReceiver;
    private static int keySizeReceiver;
    

    public static int transferFile(Client client)throws IOException{
        Scanner sc = new Scanner(System.in);

        System.out.println("Would you like to ASCII armor this transfer? (y/n)");
        boolean ASCIIArmor = sc.nextLine().toLowerCase().startsWith("y");
        client.getOut().println("ASCIIARMOR:" + ASCIIArmor);

//        System.out.println("Please enter the path for encryption key or leave empty for default:");
//        String keyFile = sc.nextLine();
//        if (keyFile.isEmpty()) keyFile = DEFAULT_KEY_FILE;
        String keyFile = DEFAULT_KEY_FILE;
        FileHandler keyHandler = FileHandler.createInputStreamFileHandler(keyFile,DEFAULT_BUFFER_SIZE);
        originalKeySizeSender = keyHandler.getBytesRemaining();

        System.out.println("Please enter the path to the file to be transferred:");
        String path = sc.nextLine();
        StringTokenizer st = new StringTokenizer(path,"\\/");
        while(st.countTokens() > 1){
            st.nextToken();
        }
        String fileName = st.nextToken();
        client.getOut().println("FILENAME:" + fileName);
        
        FileHandler fileHandler = FileHandler.createInputStreamFileHandler(path,DEFAULT_BUFFER_SIZE);
        int fileSize = fileHandler.getBytesRemaining();
        client.getOut().println("FILESIZE:" + fileSize);

        client.getOut().println("READY");

        try
        {
            FileInputStream fis = fileHandler.getFileInputStream();
            FileInputStream keyfis = keyHandler.getFileInputStream();
            keyFisSender = new BufferedInputStream(keyfis);
            keyFisSender.mark(originalKeySizeSender);
            keySizeSender = originalKeySizeSender;
            
            int read = 0, readLength = CHUNK_SIZE;
            byte[] chunk;

            if (fileSize <= CHUNK_SIZE)
                try {
                    chunk = new byte[fileSize];
                    fis.read(chunk, 0, fileSize);
                    
                    if (!successSend(client, chunk, ASCIIArmor)) return -1;
                } catch(IOException e) {
                    //TODO:
                    System.out.println(e.getMessage());
                }
            else
            {
                while (fileSize > 0)
                {
                    try {
                        if (fileSize <= CHUNK_SIZE) readLength = fileSize;
                        chunk = new byte[readLength];
                        read = fis.read(chunk, 0, readLength);
                        fileSize -= read;
                        assert(read == chunk.length);

                        if (!successSend(client, chunk, ASCIIArmor)) return -1;
                    } catch(IOException e) {
                        //TODO:
                        System.out.println(e.getMessage());
                    }
                }
                fis.close();
                if (keyFisSender != null) keyFisSender.close();
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        return -1;
    }
    
    private static boolean successSend(Client client, byte[] chunk, boolean ASCIIArmor) throws IOException
    {
        do
        {
            byte[] packageChunk = packageToSend(chunk, ASCIIArmor);
            sendToServer(client, packageChunk);
            String response = client.readServerInput();
            if (response == null)
                return false;
            else
            {
                if (!response.equals("Continue.")) System.out.println(response);
                if (response.equals("Failed too many times.")) return false;
                if (response.equals("Continue.")) break;
            }
            
            // To run fail only by a certain number of times
            if (numberOfFails <= ++failCountSender)
                runNormal = true;
        } while (true);
        
        // Discard key
        previousKeySender = null;
        
        return true;
    }

    public static int receiveFile(Server server) throws IOException {
        Scanner sc = new Scanner(System.in);
        boolean ASCIIArmored = false;
        FileHandler fileHandler = null;
        int fileSize = 0;
        
        server.getOut().println("Ready to receive file.");

        String fromClient;
        while ((fromClient = server.readClientInput()) != null) {
            if(fromClient.length() >= 11){
                if(fromClient.substring(0,11).equals("ASCIIARMOR:")) {
                    fromClient = fromClient.substring(11,fromClient.length());
                    ASCIIArmored = fromClient.equals("true");
                }
            }
            if(fromClient.length() >= 9){
                if(fromClient.substring(0,9).equals("FILENAME:")){
                    fromClient = fromClient.substring(9,fromClient.length());
                    fileHandler = FileHandler.createOutputStreamFileHandler(fromClient,DEFAULT_BUFFER_SIZE);
                }
                else if(fromClient.substring(0,9).equals("FILESIZE:")){
                    fromClient = fromClient.substring(9,fromClient.length());
                    fileSize = Integer.parseInt(fromClient);
                }
            }
            if(fromClient.length() == 5){
                if (fromClient.substring(0,5).equals("READY")){
                    break;
                }
            }
        }
        
//        System.out.println("Please enter the path for encryption key or leave empty for default:");
//        String keyFile = sc.nextLine();
//        if (keyFile.isEmpty()) keyFile = DEFAULT_KEY_FILE;
        String keyFile = DEFAULT_KEY_FILE;
        FileHandler keyHandler = FileHandler.createInputStreamFileHandler(keyFile,DEFAULT_BUFFER_SIZE);
        originalKeySizeReceiver = keyHandler.getBytesRemaining();
        
        byte[] previousBytes = null;
        FileInputStream keyfis = keyHandler.getFileInputStream();
        keyFisReceiver = new BufferedInputStream(keyfis);
        keyFisReceiver.mark(originalKeySizeReceiver);
        keySizeReceiver = originalKeySizeReceiver;
        
        while(fileSize > 0)
        {
            byte[] bytes;
            
            // Read from inputstream
            if (previousBytes == null)
                bytes = readServerInput(server);
            else
            {
                byte[] currentBytes = readServerInput(server);
                bytes = (currentBytes.length > 0)   ? concate(previousBytes, currentBytes)
                                                    : Arrays.copyOf(previousBytes, previousBytes.length);
            }
            
            // Get the file size of a package
            int readLength = getSize(bytes) + 4;
            
            if (readLength > bytes.length) // read less than needed
            {
                previousBytes = Arrays.copyOf(bytes, bytes.length);
                continue;
            }
            else if (readLength < bytes.length) // read more than needed
            {
                previousBytes = Arrays.copyOfRange(bytes, readLength, bytes.length);
                bytes = Arrays.copyOf(bytes, readLength);
            }
            else // read enough
                previousBytes = null;
            
            if (bytes.length != 0)
            {
                try {
                    // Get data
                    bytes = unpackage(bytes, ASCIIArmored, server);
                    
                    // Inconsistency handling
                    if (bytes == null)
                    {
                        if (failCountReceiver < 3)
                        {
                            failCountReceiver++;
                            previousBytes = null;
                            continue;
                        }
                        else break;
                    }
                    
                    fileHandler.writeNextChunk(bytes);
                    fileSize -= bytes.length;
                    System.out.printf("Remaining file size: %d\n", fileSize);
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000);                 //1000 milliseconds is one second.
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        if (keyFisReceiver != null) keyFisReceiver.close();
        
        server.getOut().println("Bye.");
        return -1;
    }

    private static byte[] packageToSend(byte[] chunk, boolean isBase64Encode) throws IOException
    {
        // Hash
        hasher x = new hasher(chunk);
        byte[] hash;
        
        if (runNormal == true) hash = x.getHash();
        else hash = x.forceToFail(chunk, failData);

        // Get key
        byte[] key;
        if (previousKeySender != null)
            key = Arrays.copyOf(previousKeySender, previousKeySender.length);
        else
        {
            if (hash.length < keySizeSender) key = new byte[hash.length];
            else key = new byte[keySizeSender];
            int read = keyFisSender.read(key, 0, key.length);
            keySizeSender -= read;
            assert(read == key.length);
            if (keySizeSender <= 0) 
            {
                keySizeSender = originalKeySizeSender;
                keyFisSender.reset();
            }
        }
        
        // Store key
        previousKeySender = Arrays.copyOf(key, key.length);
                  
        // Encrypt
        Encryptor encryptor = new Encryptor(hash, key);
        byte[] encrypt = encryptor.encryptAsByte();
        
        // Encode
        byte[] result;
        if (isBase64Encode)
        {
            Base64 encoder = new Base64();
            byte[] encode = encoder.encodeAsByte(encrypt);
            byte[] size = ByteBuffer.allocate(LENGTH_SIZE).putInt(encode.length).array();
            result = concate(size, encode);
        }
        else
        {
            byte[] size = ByteBuffer.allocate(LENGTH_SIZE).putInt(encrypt.length).array();
            result = concate(size, encrypt);
        }
        
        return result;
    }
    
    public static byte[] concate(byte[] a, byte[] b)
    {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
    
    public static int getSize(byte[] chunk)
    {
        // Strip length
        int sizeIndex = 4;
        byte[] size = Arrays.copyOf(chunk, sizeIndex);
        int result = java.nio.ByteBuffer.wrap(size).getInt();
        return result;
    }

    public static byte[] unpackage(byte[] chunk, boolean isBase64Encode, 
                                    Server server) throws IOException
    {
        // Decode
        byte[] decode = Arrays.copyOfRange(chunk, LENGTH_SIZE, chunk.length);
        if (isBase64Encode)
        {
            Base64 decoder = new Base64();
            decode = decoder.decodeAsByte(decode);
        }
        
        // Get key
        byte[] key;
        if (previousKeyReceiver != null)
            key = Arrays.copyOf(previousKeyReceiver, previousKeyReceiver.length);
        else
        {
            if (decode.length < keySizeReceiver) key = new byte[decode.length];
            else key = new byte[keySizeReceiver];
            int read = keyFisReceiver.read(key, 0, key.length);
            keySizeReceiver -= read;
            assert(read == key.length);
            if (keySizeReceiver <= 0) 
            {
                keySizeReceiver = originalKeySizeReceiver;
                keyFisReceiver.reset();
            }
        }
        
        // Decrypt
        Encryptor decryptor = new Encryptor(decode, key);
        byte[] decrypt = decryptor.encryptAsByte();

        // Checksum
        hasher x = new hasher(decrypt);
        byte[] reHashed = x.reHash();
        
        if(Arrays.equals(decrypt, reHashed))
        {
            server.getOut().println("Continue.");
            failCountReceiver = 0;
            return x.getData();
        }
        else
        {
            if (failCountReceiver < 3)
            {
                previousKeyReceiver = Arrays.copyOf(key, key.length);
                server.getOut().println("Inconsistent. Resend.");
            }
            else
                server.getOut().println("Failed too many times.");
            return null;
        }
    }

    public static byte[] readServerInput (Server server){
        try{
            byte[] response = new byte[server.getInputStream().available()];
            server.getInputStream().read(response);
            return response;
        } catch(IOException ioe){
            System.out.println("Server unnable to connect to inputstream");
        }
       return null;
    }

    public static void sendToServer(Client client, byte[] b){
        try{
            client.getOutputStream().write(b);
        } catch(IOException ioe){
            System.out.println("Client unable to connect to outputstream");
        }
    }

    public static void setFailure (boolean failType, int fails)
    {
        runNormal = false;
        failData = failType;
        numberOfFails = fails;
    }
}
