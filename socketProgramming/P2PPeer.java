import java.io.*;
import java.net.*;

public class P2PPeer {
    private static final int PORT = 5001;  // Port for listening and connecting
    private static final String SHARED_DIRECTORY = "C:\\Users\\Asus\\Desktop\\socketProgramming\\shareFiles"; // Relative path for shared files
    private static final String DOWNLOADS_DIRECTORY = "C:\\Users\\Asus\\Desktop\\socketProgramming\\downloads"; // Relative path for downloads
    private final String peerName;  // Unique name or ID for each peer

    public P2PPeer(String name) {
        this.peerName = name;
    }

    // Start a server to listen for incoming file requests
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println(peerName + " is listening on port " + PORT);

                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println(peerName + " connected to: " + socket.getInetAddress());
                    handleFileRequest(socket);
                }
            } catch (IOException e) {
                System.err.println("Error starting server: " + e.getMessage());
            }
        }).start();
    }

    // Handle file requests from other peers
    private void handleFileRequest(Socket socket) {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String fileName = in.readLine();
                System.out.println(peerName + " received request for file: " + fileName);

                // Locate the requested file in the shared directory
                File file = new File(SHARED_DIRECTORY, fileName);

                if (file.exists() && file.isFile()) {
                    sendFile(file, socket);  // Send the file if it exists
                } else {
                    System.out.println("Requested file not found: " + fileName);
                }
            } catch (IOException e) {
                System.err.println("Error handling file request: " + e.getMessage());
            }
        }).start();
    }

    // Send file to the connected peer
    private void sendFile(File file, Socket socket) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             OutputStream os = socket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytesSent = 0;

            while ((bytesRead = bis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;  // Track total bytes sent
                System.out.println(peerName + " sent " + bytesRead + " bytes, total sent so far: " + totalBytesSent);
            }

            os.flush();  // Ensure all data is sent out
            System.out.println(peerName + " finished sending file. Total bytes sent: " + totalBytesSent);

        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
    }


    // Make a request to another peer for a file
    public void requestFile(String serverIP, String fileName) {
        try (Socket socket = new Socket(serverIP, PORT)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(fileName);  // Send the requested file name to the peer

            receiveFile(fileName, socket);
        } catch (IOException e) {
            System.err.println("Error requesting file: " + e.getMessage());
        }
    }

    // Receive a file from another peer
    private void receiveFile(String fileName, Socket socket) {
        File downloadDir = new File(DOWNLOADS_DIRECTORY);

        // Create the downloads directory if it doesn't exist
        if (!downloadDir.exists()) {
            downloadDir.mkdir();
        }

        // Save the file in the downloads directory with the original file name
        File outputFile = new File(downloadDir, "received_" + fileName);
        int totalBytesReceived = 0;

        try (BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesReceived += bytesRead;  // Track total bytes received
                System.out.println(peerName + " received " + bytesRead + " bytes, total received so far: " + totalBytesReceived);
            }

            fos.flush();  // Ensure all data is written to the file
            System.out.println(peerName + " finished receiving file. Total bytes received: " + totalBytesReceived);

        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        // Start peer with a unique name (for example, "Peer1" or "Peer2")
        P2PPeer peer = new P2PPeer("Peer1");  // Replace with unique name
        peer.startServer();
        

        // To request a file from another peer, uncomment the line below and provide the correct IP and file name
        // peer.requestFile("192.168.1.6", "sample.txt");  // Replace with friend's IP and desired file
    }
}
