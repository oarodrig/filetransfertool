import java.io.*;

/**
 * Created by Omar on 11/25/2015.
 */
public class FileHandler {
    private FileInputStream fileInputStream;

    public FileInputStream getFileInputStream() {
        return fileInputStream;
    }

    private FileOutputStream fileOutputStream;
    private byte[] buffer;

    private FileHandler(String filename, int bufferSize, boolean isInputStream){
        if(isInputStream){
            try{
                fileInputStream = new FileInputStream(new File(filename));
            }catch(FileNotFoundException fnf){
                System.out.println("File not found!");
                fileInputStream = null;
            }
        } else {
            try{
                fileOutputStream = new FileOutputStream(new File(filename));
            }catch(FileNotFoundException fnf){
                System.out.println("File not found!");
                fileOutputStream = null;
            }
        }
        buffer = new byte[bufferSize];
    }

    public byte[] getNextChunk(){
        try{
            fileInputStream.read(buffer);
        }catch(IOException io){
            System.out.println("Unable to read from file.");
        }
        return buffer;
    }
    public void writeNextChunk(byte[] chunk) throws IOException {
        try{
            fileOutputStream.write(chunk);
        }catch(IOException io){
            System.out.println("Unable to write to file.");
        }
    }

    public int getBytesRemaining(){
        try{
            return fileInputStream.available();
        }catch(IOException io){
            System.out.println("Error.");
            return -1;
        }
    }

    public static FileHandler createInputStreamFileHandler(String filename, int bufferSize){
        return new FileHandler(filename,bufferSize, true);
    }
    public static FileHandler createOutputStreamFileHandler(String filename, int bufferSize){
        return new FileHandler(filename,bufferSize, false);
    }
    public int getBufferSize(){
        return buffer.length;
    }

}
