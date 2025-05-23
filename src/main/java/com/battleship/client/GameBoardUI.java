package com.battleship.client;

import com.battleship.core.Ship;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GameBoardUI extends JFrame {

    private static final int GRID_SIZE = 10;
    private JButton[][] playerGridButtons = new JButton[GRID_SIZE][GRID_SIZE];
    private JButton[][] opponentGridButtons = new JButton[GRID_SIZE][GRID_SIZE];
    private JLabel statusLabel;
    private Client client;
    private String username; 

    private boolean placingShipsMode = false;
    private int shipsToPlaceIndex = 0;
    private final int[] shipSizes = {5, 4, 3, 2, 2};
    private List<Ship> placedShips = new ArrayList<>();
    private boolean myTurn = false;
    private boolean isPlacingHorizontal = true; 

    
    public GameBoardUI(Client client, String username) {
        this.client = client;
        this.username = username; 

        
        setTitle("Amiral Battı - " + username);

        setSize(1000, 650); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); 
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 10)); 

        
        JPanel playerPanel = new JPanel(new BorderLayout(0, 5)); 
        playerPanel.setBorder(BorderFactory.createTitledBorder(username + "'in Tahtası (Yerleştirmek için tıkla, yön için sağ tık)"));
        JPanel playerGridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        initializeGrid(playerGridButtons, playerGridPanel, true);
        playerPanel.add(playerGridPanel, BorderLayout.CENTER);
        mainPanel.add(playerPanel);

        JPanel opponentPanel = new JPanel(new BorderLayout(0, 5));
        opponentPanel.setBorder(BorderFactory.createTitledBorder("Rakip Tahtası (Saldırmak için tıkla)"));
        JPanel opponentGridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        initializeGrid(opponentGridButtons, opponentGridPanel, false);
        opponentPanel.add(opponentGridPanel, BorderLayout.CENTER);
        mainPanel.add(opponentPanel);

        add(mainPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("Kullanıcı adı '" + username + "' ile bağlandı. Sunucu bilgisi bekleniyor...", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0)); 
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void initializeGrid(JButton[][] gridButtons, JPanel gridPanel, boolean isPlayerGrid) {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(40, 40)); 
                button.setBackground(Color.LIGHT_GRAY);
                button.setOpaque(true);
                button.setBorder(BorderFactory.createLineBorder(Color.GRAY)); 
                final int r = i; 
                final int c = j;

                if (isPlayerGrid) {
                    button.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (placingShipsMode) {
                                if (SwingUtilities.isLeftMouseButton(e)) {
                                    handleShipPlacement(r, c);
                                } else if (SwingUtilities.isRightMouseButton(e)) {
                                    isPlacingHorizontal = !isPlacingHorizontal;
                                    updatePlacementStatusLabel();
                                }
                            }
                        }
                    });
                } else { 
                    button.addActionListener(e -> {
                        if (myTurn) {
                            handleAttack(r, c);
                        } else {
                            if (placingShipsMode) {
                                setStatusLabel("Lütfen önce kendi tahtanıza gemilerinizi yerleştirin.");
                            } else {
                                setStatusLabel("Sıra rakipte. Lütfen bekleyin.");
                            }
                        }
                    });
                }
                gridButtons[i][j] = button;
                gridPanel.add(button);
            }
        }
    }

    private void updatePlacementStatusLabel() {
        if (placingShipsMode && shipsToPlaceIndex < shipSizes.length) {
            String direction = isPlacingHorizontal ? "Yatay" : "Dikey";
            setStatusLabel("Sıradaki gemi (" + shipSizes[shipsToPlaceIndex] + " birim). Yön: " + direction + ". Tahtanıza tıklayarak yerleştirin. (Sağ tık yön değiştirir)");
        }
    }


    public void setStatusLabel(String text) {
        statusLabel.setText(text);
    }

    private void startShipPlacement() {
        placingShipsMode = true;
        isPlacingHorizontal = true; 
        shipsToPlaceIndex = 0;
        placedShips.clear();
        
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                playerGridButtons[i][j].setBackground(Color.LIGHT_GRAY);
                playerGridButtons[i][j].setText("");
                playerGridButtons[i][j].setEnabled(true); 
            }
        }
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                opponentGridButtons[i][j].setEnabled(false); 
            }
        }
        updatePlacementStatusLabel(); 
    }

    private void handleShipPlacement(int r, int c) {
        if (!placingShipsMode || shipsToPlaceIndex >= shipSizes.length) {
            return; 
        }

        int currentShipSize = shipSizes[shipsToPlaceIndex];
        List<Point> shipCoordinates = new ArrayList<>();

        
        boolean possibleToPlace = true;
        if (isPlacingHorizontal) {
            if (c + currentShipSize > GRID_SIZE) {
                possibleToPlace = false;
            } else {
                for (int i = 0; i < currentShipSize; i++) {
                    shipCoordinates.add(new Point(r, c + i));
                }
            }
        } else { 
            if (r + currentShipSize > GRID_SIZE) {
                possibleToPlace = false;
            } else {
                for (int i = 0; i < currentShipSize; i++) {
                    shipCoordinates.add(new Point(r + i, c));
                }
            }
        }

        if (!possibleToPlace) {
            setStatusLabel("Gemi buraya sığmıyor! (" + (isPlacingHorizontal ? "Yatay" : "Dikey") + ", " + currentShipSize + " birim)");
            return;
        }

        
        for (Point newCoord : shipCoordinates) {
            for (Ship existingShip : placedShips) {
                
                if (existingShip.hasCoordinate(newCoord.x, newCoord.y)) {
                    setStatusLabel("Gemiler çakışıyor! Lütfen başka bir yer seçin.");
                    return;
                }
            }
        }

        
        Ship newShip = new Ship(currentShipSize);
        newShip.setCoordinates(shipCoordinates); 
        placedShips.add(newShip);

        
        for (Point coord : shipCoordinates) {
            playerGridButtons[coord.x][coord.y].setBackground(Color.DARK_GRAY); // Gemi rengi
            
        }

        shipsToPlaceIndex++;
        if (shipsToPlaceIndex < shipSizes.length) {
            updatePlacementStatusLabel(); 
        } else {
            
            placingShipsMode = false;
            setStatusLabel("Tüm gemiler yerleştirildi. Sunucuya gönderiliyor...");
            
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    playerGridButtons[i][j].setEnabled(false);
                 }
            }
            
            client.sendShipsToServer(new ArrayList<>(placedShips)); 
        }
    }

    
    private boolean isValidUiPlacement(int r, int c, int size, boolean isHorizontal) {
        if (isHorizontal) {
            return c + size <= GRID_SIZE;
        } else {
            return r + size <= GRID_SIZE;
        }
    }


    private void handleAttack(int r, int c) {
        if (!myTurn) {
            setStatusLabel("Sıra rakipte. Lütfen bekleyin.");
            return;
        }
        
        if (opponentGridButtons[r][c].getBackground() == Color.RED || opponentGridButtons[r][c].getBackground() == Color.BLUE) {
            setStatusLabel("Bu kareye daha önce ateş edildi. Başka bir kare seçin.");
            return;
        }
        setStatusLabel("(" + r + "," + c + ") koordinatına ateş ediliyor...");
        client.sendAttackToServer(r, c);
        
        if (opponentGridButtons[r][c].getBackground() != Color.RED && opponentGridButtons[r][c].getBackground() != Color.BLUE) {
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    opponentGridButtons[i][j].setEnabled(false);
                 }
             }
        }

    }

    public void processServerMessage(Object message) {
        if (message instanceof String) {
            String msg = (String) message;
            System.out.println("UI Mesajı İşleniyor: " + msg); 
            if (msg.equals("PLACE_SHIPS")) {
                startShipPlacement(); 
            } else if (msg.equals("YOUR_TURN")) {
                myTurn = true;
                setStatusLabel("Sıra sizde! Rakip tahtasına tıklayarak ateş edin.");
                
                if (!placingShipsMode) {
                    for (int i = 0; i < GRID_SIZE; i++) {
                        for (int j = 0; j < GRID_SIZE; j++) {
                             
                             if (opponentGridButtons[i][j].getBackground() != Color.RED && opponentGridButtons[i][j].getBackground() != Color.BLUE) {
                                  opponentGridButtons[i][j].setEnabled(true);
                             }
                             if (!placingShipsMode) {
                                 playerGridButtons[i][j].setEnabled(false);
                             }
                        }
                    }
                }
            } else if (msg.equals("OPPONENT_TURN")) {
                myTurn = false;
                setStatusLabel("Sıra rakipte. Bekleniyor...");
                 
                 if (!placingShipsMode) {
                     for (int i = 0; i < GRID_SIZE; i++) {
                         for (int j = 0; j < GRID_SIZE; j++) {
                             opponentGridButtons[i][j].setEnabled(false); 
                              if (!placingShipsMode) {
                                  playerGridButtons[i][j].setEnabled(false);
                              }
                         }
                     }
                 }
            } else if (msg.startsWith("ATTACK_RESULT ")) {
                String[] parts = msg.split(" ");
                
                if (parts.length < 5) {
                    System.err.println("HATA: ATTACK_RESULT mesaj formatı hatalı: " + msg);
                    setStatusLabel("Sunucu Hatası: Saldırı sonucu mesajı anlaşılamadı.");
                    return;
                }

                int serverAttackerId = Integer.parseInt(parts[1]);
                int r = Integer.parseInt(parts[2]);
                int c = Integer.parseInt(parts[3]);
                String resultType = parts[4];
                String detail = parts.length > 5 ? parts[5] : ""; 

                int myClientId = client.getClientId();
                if (myClientId == -1) {
                    System.err.println("GameBoardUI: Client ID alınamadı, ATTACK_RESULT işlenemiyor!");
                    setStatusLabel("HATA: Oyuncu kimliği alınamadı!");
                    return;
                }

                boolean iWasAttacker = (myClientId == serverAttackerId);

                if (iWasAttacker) { 
                    JButton targetButton = opponentGridButtons[r][c];
                     
                    if (targetButton.getBackground() != Color.RED && targetButton.getBackground() != Color.BLUE) {
                        if (resultType.equals("HIT")) {
                            targetButton.setBackground(Color.RED);
                            targetButton.setText("X");
                            setStatusLabel("Vurdunuz! Rakip: (" + r + "," + c + ")");
                        } else if (resultType.equals("MISS")) { 
                            targetButton.setBackground(Color.BLUE);
                            targetButton.setText("O");
                            setStatusLabel("Iska! Rakip: (" + r + "," + c + ")");
                        }
                    }

                    if (resultType.startsWith("SUNK")) {
                         
                         setStatusLabel("Rakibin bir gemisini batırdınız! (" + r + "," + c + ") Boyut: " + detail);
                    } else if (resultType.equals("ALREADY_SHOT")) {
                        setStatusLabel("Bu kareye daha önce ateş etmiştiniz: (" + r + "," + c + "). Sıra hala sizde.");
                         
                         if (!placingShipsMode && myTurn) { 
                             for (int i = 0; i < GRID_SIZE; i++) {
                                 for (int j = 0; j < GRID_SIZE; j++) {
                                      if (opponentGridButtons[i][j].getBackground() != Color.RED && opponentGridButtons[i][j].getBackground() != Color.BLUE) {
                                           opponentGridButtons[i][j].setEnabled(true);
                                      }
                                 }
                             }
                         }
                    }
                } else { 
                    JButton targetButton = playerGridButtons[r][c];
                    if (resultType.equals("HIT")) {
                        targetButton.setBackground(Color.ORANGE); 
                        targetButton.setText("X");
                        setStatusLabel("Geminiz vuruldu! (" + r + "," + c + ")");
                    } else if (resultType.startsWith("SUNK")) {
                        
                        setStatusLabel("Bir geminiz battı! (" + r + "," + c + ") Boyut: " + detail);
                    } else if (resultType.equals("MISS")) {
                        targetButton.setBackground(Color.CYAN); // Kendi tahtamda ıska (isteğe bağlı)
                        targetButton.setText("O");
                        setStatusLabel("Rakip ıska geçti! (" + r + "," + c + ")");
                    }
                    
                }

                
                if (resultType.startsWith("GAME_OVER")) {
                    myTurn = false;
                    placingShipsMode = false;
                    String winnerIdStr = detail; 
                    int winnerId = -1;
                    String gameResultMsg = "Oyun Bitti!";
                    try {
                        winnerId = Integer.parseInt(winnerIdStr);
                        if (myClientId == winnerId) {
                            gameResultMsg = "Tebrikler, Kazandınız!";
                            setStatusLabel("Oyun bitti! Kazandınız!");
                        } else {
                            gameResultMsg = "Kaybettiniz. Kazanan: Oyuncu " + (winnerId + 1);
                            setStatusLabel("Oyun bitti! Kaybettiniz. Kazanan: Oyuncu " + (winnerId + 1));
                        }
                    } catch (NumberFormatException e) {
                        setStatusLabel("Oyun bitti! Kazanan bilgisi alınamadı.");
                        gameResultMsg = "Oyun Bitti! Kazanan belirlenemedi.";
                    } finally {
                         
                        JOptionPane.showMessageDialog(this, gameResultMsg, "Oyun Sonu", JOptionPane.INFORMATION_MESSAGE);
                         disableAllButtons();
                    }
                }

            } else if (msg.startsWith("INFO ")) {
                setStatusLabel(msg.substring("INFO ".length()));
            } else if (msg.startsWith("ERROR ")) {
                setStatusLabel("Sunucu Hatası: " + msg.substring("ERROR ".length()));
                 if (msg.contains("Geçersiz gemi yerleşimi")) { 
                     
                     startShipPlacement();
                     setStatusLabel("Geçersiz gemi yerleşimi. Lütfen gemilerinizi tekrar yerleştirin.");
                 }
            }
        }
    }

    private void disableAllButtons() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                playerGridButtons[i][j].setEnabled(false);
                opponentGridButtons[i][j].setEnabled(false);
            }
        }
    }

    
    public static void main(String[] args) {
        
    }
}