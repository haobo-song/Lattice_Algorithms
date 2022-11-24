package cs451.links;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;



public class Logger {
    String directory;
    File file;
    OutputStream outputStream;
    private final ConcurrentLinkedQueue<String> logs = new ConcurrentLinkedQueue<>();

    public Logger(String dir) {
        this.directory = dir;
        file = new File(directory);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();//set up this file
        }
        try {
            outputStream = new FileOutputStream(file); //open the file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void log(String msg) {
        logs.add(msg);

    }

    //write broadcast or delivery record to the file
    public void write() {
        logs.forEach(s -> {
            try {
                outputStream.write(s.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //close the output stream
    public void close() {

        try {
            this.write();
            outputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
