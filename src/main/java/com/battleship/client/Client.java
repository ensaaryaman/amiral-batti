package com.battleship.client;

import com.battleship.core.Ship;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.JFrame; 
import javax.swing.JOptionPane; 

public class Client {

    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameBoardUI gameBoardUI; 
    private int clientId = -1; 
    private String username; 

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void connectToServer() {
        
        UsernameInputUI usernameUI = new UsernameInputUI(null); 
        usernameUI.setVisible(true); 

        username = usernameUI.getEnteredUsername(); 

        
        if (username != null && !username.isEmpty()) {
            try {
                socket = new Socket(serverAddress, serverPort);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Sunucuya bağlandı: " + serverAddress + ":" + serverPort + " (Kullanıcı: " + username + ")");

                
                SwingUtilities.invokeLater(() -> {
                    
                    gameBoardUI = new GameBoardUI(this, username); 
                    gameBoardUI.setVisible(true);
                    gameBoardUI.setStatusLabel("Sunucuya bağlanıldı. Oyun bilgisi bekleniyor...");

                    
                    new Thread(this::listenToServer).start();
                });

            } catch (IOException e) {
                System.err.println("Sunucuya bağlanılamadı: " + e.getMessage());
                
                if (gameBoardUI != null) {
                     SwingUtilities.invokeLater(() -> gameBoardUI.setStatusLabel("Sunucuya bağlanılamadı: " + e.getMessage()));
                } else {
                    
                    JOptionPane.showMessageDialog(null, "Sunucuya bağlanılamadı: " + e.getMessage(), "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
                }
                 System.exit(1); 
            }
        } else {
            
            System.out.println("Kullanıcı adı girilmedi. İstemci kapatılıyor.");
            
            System.exit(0);
        }
    }

    private void listenToServer() {
        try {
            Object messageFromServer;
            
            while (socket != null && socket.isConnected() && !socket.isClosed() && (messageFromServer = in.readObject()) != null) {
                System.out.println("Sunucudan gelen mesaj: " + messageFromServer + " (Mevcut ID: "+this.clientId+")");

                
                if (messageFromServer instanceof String) {
                    String msgStr = (String) messageFromServer;
                    if (msgStr.startsWith("CLIENT_ID ")) {
                        try {
                            String idStr = msgStr.substring("CLIENT_ID ".length());
                            setClientId(Integer.parseInt(idStr.trim()));
                            
                            continue;
                        } catch (NumberFormatException e) {
                            System.err.println("HATA: CLIENT_ID mesajı ayrıştırılamadı: " + msgStr + " - " + e.getMessage());
                        } catch (IndexOutOfBoundsException e) {
                            System.err.println("HATA: CLIENT_ID mesajı formatı hatalı: " + msgStr + " - " + e.getMessage());
                        }
                    }
                }

                
                final Object finalMessage = messageFromServer;
                SwingUtilities.invokeLater(() -> {
                    if (gameBoardUI != null) {
                        gameBoardUI.processServerMessage(finalMessage);
                    } else {
                           System.err.println("listenToServer: gameBoardUI null, mesaj işlenemiyor: " + finalMessage);
                    }
                });
            }
        } catch (java.io.EOFException e) {
            System.out.println("Sunucu bağlantısı (istemci tarafı) beklenmedik şekilde sonlandı (EOF).");
             if (gameBoardUI != null) {
                 SwingUtilities.invokeLater(() -> gameBoardUI.setStatusLabel("Sunucu bağlantısı koptu (EOF)."));
             }
        } catch (java.net.SocketException e) {
             if (e.getMessage() != null && (e.getMessage().equalsIgnoreCase("Socket closed") || e.getMessage().equalsIgnoreCase("Connection reset"))) {
                 System.out.println("Soket kapatıldı veya sıfırlandı (istemci tarafı): " + e.getMessage());
             } else {
                 System.err.println("Soket hatası (istemci tarafı): " + e.getMessage());
             }
             if (gameBoardUI != null) {
                 SwingUtilities.invokeLater(() -> gameBoardUI.setStatusLabel("Sunucuyla bağlantı kesildi: " + e.getMessage()));
             }
        }
        catch (IOException | ClassNotFoundException e) {
            System.err.println("Sunucuyla iletişimde genel hata (istemci tarafı): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (gameBoardUI != null) {
                SwingUtilities.invokeLater(() -> gameBoardUI.setStatusLabel("Sunucuyla iletişim hatası: " + e.getMessage()));
            }
        } finally {
            try {
                 
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
                System.out.println("İstemci kaynakları kapatıldı.");
                
                if (gameBoardUI != null) {
                     SwingUtilities.invokeLater(() -> {
                         
                     });
                 } else {
                     
                 }
                
            } catch (IOException e) {
                System.err.println("Kaynaklar kapatılırken hata: " + e.getMessage());
            }
        }
    }

    public void sendMessageToServer(Object message) {
        try {
            if (out != null && socket != null && socket.isConnected() && !socket.isClosed()) {
                out.writeObject(message);
                out.flush();
                System.out.println("Sunucuya gönderilen mesaj: " + message);
            } else {
                System.err.println("Sunucuya mesaj gönderilemedi: Çıkış akışı (out) null veya soket kapalı/bağlı değil.");
                 if (gameBoardUI != null) {
                     SwingUtilities.invokeLater(() -> gameBoardUI.setStatusLabel("Hata: Sunucuya mesaj gönderilemiyor (bağlantı sorunu)."));
                 }
            }
        } catch (IOException e) {
            System.err.println("Sunucuya mesaj gönderilemedi (IOException): " + e.getMessage());
            if (gameBoardUI != null) {
                SwingUtilities.invokeLater(() -> gameBoardUI.setStatusLabel("Hata: Sunucuya mesaj gönderilemedi - " + e.getMessage()));
            }
        }
    }

    public void sendShipsToServer(List<Ship> ships) {
        sendMessageToServer(ships);
    }

    public void sendAttackToServer(int x, int y) {
        sendMessageToServer("ATTACK " + x + " " + y);
    }

    public void setClientId(int id) {
        this.clientId = id;
        System.out.println("Benim İstemci ID'm ayarlandı: " + this.clientId);
        if (gameBoardUI != null) {
            final int finalId = this.clientId;
            final String finalUsername = this.username; 
            SwingUtilities.invokeLater(() -> {
                gameBoardUI.setTitle("Amiral Battı - " + finalUsername + " (Oyuncu " + (finalId + 1) + ")");
                gameBoardUI.setStatusLabel("Oyuncu " + (finalId + 1) + " (" + finalUsername + ") olarak bağlandınız. Sunucu bilgisi bekleniyor...");
            });
        }
    }

    public int getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }

    public static void main(String[] args) {
        String address = "ec2-13-53-32-16.eu-north-1.compute.amazonaws.com"; 
        int port = 5000;        

        if (args.length == 1) {
            address = args[0];
        } else if (args.length >= 2) {
            address = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Port numarası (" + args[1] + ") geçersiz, varsayılan port (" + port + ") kullanılıyor.");
            }
        }

        Client client = new Client(address, port);
        SwingUtilities.invokeLater(() -> client.connectToServer()); // Bağlantı ve UI işlemleri Swing thread'inde başlamalı

    }
}