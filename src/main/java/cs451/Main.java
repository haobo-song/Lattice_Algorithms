package cs451;

import cs451.broadcast.LatticeBroadcast;
import cs451.links.Logger;
import cs451.links.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

public class Main {

    static LatticeBroadcast latticeBroadcast;
    static Thread threadlatticeBroadcast;
    static Logger logger;
    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        latticeBroadcast.close();
        //write/flush output file if necessary
        System.out.println("Writing output.");
        logger.close();
        System.out.println("Writing output done.");
    }

    private static void initSignalHandlers() {
        //Runtime.getRuntime().addShutdownHook(new Thread() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");
        try{
            File configFile = new File(parser.config());
            Scanner reader = new Scanner(configFile);
            int messageNumber = Integer.parseInt(reader.next());
            int vs = Integer.parseInt(reader.next());
            int ds = Integer.parseInt(reader.next());
            //int destinationProcess = Integer.parseInt(reader.next());
            Host host = parser.hosts().get(parser.myId()-1);
            logger = new Logger(parser.output());
            //perfectLink = new PerfectLinks(host.getPort(), parser.hosts());
            latticeBroadcast = new LatticeBroadcast(host, parser.hosts(),logger, messageNumber);
            Thread.sleep(10);
            threadlatticeBroadcast = new Thread(latticeBroadcast);
            threadlatticeBroadcast.start();
            System.out.println("Broadcasting and delivering messages...\n");

            Scanner reader_line = new Scanner(configFile);
            reader_line.nextLine();
            for (Integer j = 1; j <= messageNumber; j++){
                String m = reader_line.nextLine();
                String[] words = m.split("\\s+");
                int[] proposal = new int[words.length];
                for (int i = 0; i < words.length; i++) {
                    proposal[i] = Integer.parseInt(words[i]);
                }
                Message message = new Message(j, 0, proposal, -1, host.getId(), false);

                //perfectLink.send(m, perfectLink.getIpFromHosts(parser.hosts(), destinationProcess), perfectLink.getPortFromHosts(parser.hosts(), destinationProcess));
                while(latticeBroadcast.proposePosition < j | latticeBroadcast.broadflag==false){
                    Thread.sleep(5);
                }
                System.out.println("read: [" +m+"] "+latticeBroadcast.proposePosition);
                latticeBroadcast.broadcast(message);
            }

        }catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
