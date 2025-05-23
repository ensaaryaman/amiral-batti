package com.battleship.server;

import com.battleship.core.GameLogic;
import com.battleship.core.Ship; 
import java.io.IOException;
import java.io.ObjectInputStream;   
import java.io.ObjectOutputStream;
import java.net.ServerSocket;   
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private ServerSocket serverSocket;  
    private List<ClientHandler> clients = new ArrayList<>();   
    private GameLogic gameLogic;
    private int currentPlayerIndex = 0; 

    public Server(int port) throws IOException {   
        serverSocket = new ServerSocket(port);
        gameLogic = new GameLogic(); 
    }

    public void startServer() {
        try {
            System.out.println("Başlatılan PORT ===> " + serverSocket.getLocalPort());
            while (clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni bir istemci bağlandı: " + clientSocket.getInetAddress().getHostAddress()); 
                ClientHandler clientHandler = new ClientHandler(clientSocket, this, clients.size());    
                clients.add(clientHandler);
                new Thread(clientHandler).start();  
                System.out.println("İstemci sayısı: " + clients.size());

                
                try {
                    if (clientHandler.getClientId() == 0) {
                        clientHandler.sendMessage("INFO Oyuncu 1 olarak bağlandınız. Rakip bekleniyor...");
                        clientHandler.sendMessage("CLIENT_ID " + clientHandler.getClientId());
                    } else if (clientHandler.getClientId() == 1) {
                        clientHandler.sendMessage("INFO Oyuncu 2 olarak bağlandınız. Diğer oyuncuyla eşleşildi.");
                        clientHandler.sendMessage("CLIENT_ID " + clientHandler.getClientId());

                        
                        System.out.println("İki istemci bağlandı. Gemi yerleştirme başlıyor...");
                        clients.get(0).sendMessage("INFO Gemilerinizi yerleştirin (Oyuncu 1).");
                        clients.get(0).sendMessage("PLACE_SHIPS");
                        clients.get(1).sendMessage("INFO Rakibin (Oyuncu 1) gemilerini yerleştirmesi bekleniyor...");
                    }
                } catch (IOException e) {
                    System.err.println("İstemciye ilk mesaj gönderilemedi ("+ clientHandler.getClientId() +"): " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("Sunucu başlatma hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void broadcastMessage(Object message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("Mesaj gönderilemedi (broadcast): " + client.getClientId() + " - " + e.getMessage());
                }
            }
        }
    }

    public synchronized void sendMessageToClient(Object message, int clientIndex) {
        if (clientIndex >= 0 && clientIndex < clients.size()) {
            try {
                clients.get(clientIndex).sendMessage(message);
            } catch (IOException e) {
                System.err.println("Mesaj gönderilemedi (specific): " + clients.get(clientIndex).getClientId() + " - " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked") 
    public synchronized void handleClientMessage(Object message, ClientHandler clientHandler) {
        System.out.println("Sunucuya gelen mesaj (" + clientHandler.getClientId() + "): " + message);

        if (message instanceof String) {
            String command = (String) message;
            if (command.startsWith("SHIPS_PLACED")) {
                int clientId = clientHandler.getClientId();
                System.out.println("İstemci " + clientId + " gemilerini yerleştirdi.");
                clientHandler.sendMessageSafe("INFO Gemileriniz sunucu tarafından alındı.");


                if (clientId == 0) { 
                    sendMessageToClient("INFO Rakibiniz (Oyuncu 2) gemilerini yerleştiriyor...", 0);
                    sendMessageToClient("INFO Sıra sizde, gemilerinizi yerleştirin (Oyuncu 2).", 1);
                    sendMessageToClient("PLACE_SHIPS", 1);
                } else if (clientId == 1) { 
                    System.out.println("Tüm gemiler yerleştirildi. Oyun başlıyor.");
                    sendMessageToClient("INFO Tüm gemiler yerleştirildi. Oyun başlıyor!", 0);
                    sendMessageToClient("INFO Tüm gemiler yerleştirildi. Oyun başlıyor!", 1);

                    currentPlayerIndex = 0; 
                    sendMessageToClient("YOUR_TURN", currentPlayerIndex);
                    sendMessageToClient("OPPONENT_TURN", (currentPlayerIndex == 0) ? 1 : 0);
                }
            } else if (command.startsWith("ATTACK")) {
                String[] parts = command.split(" ");
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int attackerId = clientHandler.getClientId();
                int targetId = (attackerId == 0) ? 1 : 0;

                System.out.println("İstemci " + attackerId + ", (" + x + "," + y + ") koordinatına saldırdı (hedef: " + targetId + ").");

                String result = gameLogic.processAttack(attackerId, targetId, x, y);
                String resultMessage = "ATTACK_RESULT " + attackerId + " " + x + " " + y + " " + result;

                
                sendMessageToClient(resultMessage, 0);
                sendMessageToClient(resultMessage, 1);

                if (result.startsWith("GAME_OVER")) {
                    System.out.println("Oyun bitti! Kazanan: " + result.split(" ")[1]);
                } else if (!result.equals("ALREADY_SHOT")) { 
                    currentPlayerIndex = targetId;
                    sendMessageToClient("YOUR_TURN", currentPlayerIndex);
                    sendMessageToClient("OPPONENT_TURN", attackerId);
                } else {
                    sendMessageToClient("INFO Aynı kareye tekrar ateş ettiniz. Sıra hala sizde.", attackerId);
                    sendMessageToClient("YOUR_TURN", attackerId); 
                    sendMessageToClient("OPPONENT_TURN", targetId);
                }
            }
        } else if (message instanceof List) {
            if (!((List<?>) message).isEmpty() && ((List<?>) message).get(0) instanceof com.battleship.core.Ship) {
                List<com.battleship.core.Ship> ships = (List<com.battleship.core.Ship>) message;
                int clientId = clientHandler.getClientId();
                boolean placementOk = gameLogic.placeShips(clientId, ships); 
                if (placementOk) {
                    System.out.println("İstemci " + clientId + " için gemiler sunucuya kaydedildi.");
                    
                    handleClientMessage("SHIPS_PLACED", clientHandler);
                } else {
                    
                    clientHandler.sendMessageSafe("ERROR Geçersiz gemi yerleşimi. Lütfen tekrar deneyin.");
                    clientHandler.sendMessageSafe("PLACE_SHIPS"); 
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(5000);
            server.startServer();
        } catch (IOException e) {
            System.err.println("Sunucu ana hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int clientId;

    public ClientHandler(Socket socket, Server server, int clientId) {
        this.clientSocket = socket;
        this.server = server;
        this.clientId = clientId;
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush(); 
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("ClientHandler ("+clientId+") stream oluşturulamadı: " + e.getMessage());
        }
    }

    public int getClientId() {
        return clientId;
    }

    public void sendMessage(Object message) throws IOException {
        if (out != null) {
            out.writeObject(message);
            out.flush();
        } else {
            System.err.println("ClientHandler ("+clientId+"): ObjectOutputStream null, mesaj gönderilemiyor: " + message);
        }
    }
    
    public void sendMessageSafe(String message) {
        try {
            sendMessage(message);
        } catch (IOException e) {
            System.err.println("ClientHandler ("+clientId+") güvenli mesaj gönderirken hata: " + e.getMessage());
        }
    }


    @Override
    public void run() {
        try {
            Object messageFromClient;
            while (clientSocket.isConnected() && !clientSocket.isClosed() && (messageFromClient = in.readObject()) != null) {
                server.handleClientMessage(messageFromClient, this);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (e.getMessage() != null && e.getMessage().contains("Socket closed")) {
                System.out.println("İstemci bağlantısı (" + clientId + ") normal bir şekilde kapatıldı.");
            } else if (e.getMessage() != null && e.getMessage().equals("Connection reset")) {
                 System.out.println("İstemci (" + clientId + ") bağlantıyı aniden kesti (reset).");
            } else if (e instanceof java.io.EOFException) {
                System.out.println("İstemci (" + clientId + ") bağlantısı beklenmedik şekilde sonlandı (EOF).");
            }
            else {
                System.err.println("İstemci (" + clientId + ") dinleme hatası: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                System.out.println("İstemci (" + clientId + ") kaynakları kapatıldı.");
                
            } catch (IOException e) {
                System.err.println("İstemci (" + clientId + ") kaynakları kapatılırken hata: " + e.getMessage());
            }
        }
    }
}
