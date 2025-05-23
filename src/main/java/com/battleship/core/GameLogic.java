package com.battleship.core;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameLogic {
    private static final int GRID_SIZE = 10;
    
    private Map<Integer, List<Ship>> playerShips;
    
    private Map<Integer, List<Point>> playerAttemptedShots; 


    public GameLogic() {
        playerShips = new HashMap<>();
        playerShips.put(0, new ArrayList<>());
        playerShips.put(1, new ArrayList<>());

        playerAttemptedShots = new HashMap<>();
        playerAttemptedShots.put(0, new ArrayList<>());
        playerAttemptedShots.put(1, new ArrayList<>());
    }

    
    public synchronized boolean placeShips(int playerId, List<Ship> ships) {
        if (!playerShips.containsKey(playerId)) {
            System.err.println("Oyun Mantığı: Geçersiz oyuncu ID'si: " + playerId);
            return false; 
        }

        
        if (!isValidPlacement(playerId, ships)) {
            System.err.println("Oyun Mantığı: Oyuncu " + playerId + " için geçersiz gemi yerleşimi.");
            
            return false; 
        }

        playerShips.get(playerId).clear(); 
        
        for (Ship originalShip : ships) {
            Ship copiedShip = new Ship(originalShip.getSize());
            List<Point> coordsCopy = new ArrayList<>();
            if (originalShip.getCoordinates() != null) { 
                for(Point p : originalShip.getCoordinates()){
                    if (p != null) { 
                         coordsCopy.add(new Point(p.x, p.y));
                    } else {
                        System.err.println("Oyun Mantığı: Gemi yerleştirirken null bir koordinatla karşılaşıldı. Gemi: " + originalShip.getSize());
                        return false; 
                    }
                }
            } else {
                 System.err.println("Oyun Mantığı: Geminin koordinatları null. Gemi Boyutu: " + originalShip.getSize());
                 return false; 
            }
            copiedShip.setCoordinates(coordsCopy); 
            playerShips.get(playerId).add(copiedShip);
        }

        System.out.println("Oyun Mantığı: Oyuncu " + playerId + " için gemiler yerleştirildi. Gemi sayısı: " + playerShips.get(playerId).size());
        for(Ship ship : playerShips.get(playerId)){
            System.out.println("  -> " + ship);
        }
        return true; 
    }


    // Bir saldırıyı işler
    // attackerId: Saldıran oyuncu
    // targetPlayerId: Saldırılan oyuncu
    // x, y: Saldırı koordinatları
    // Dönen değer: "HIT", "MISS", "SUNK <size>", "GAME_OVER <winnerId>", "ALREADY_SHOT"
    public synchronized String processAttack(int attackerId, int targetPlayerId, int x, int y) {
        System.out.println("GameLogic: Oyuncu " + attackerId + " -> Oyuncu " + targetPlayerId + " tahtasına (" + x + "," + y + ") saldırıyor.");

        Point shotPoint = new Point(x, y);

        // Bu kareye daha önce bu oyuncu tarafından ateş edilmiş mi kontrol et
        if (playerAttemptedShots.get(attackerId) != null && playerAttemptedShots.get(attackerId).contains(shotPoint)) {
            System.out.println("GameLogic: (" + x + "," + y + ") koordinatına daha önce ateş edilmiş. Sonuç: ALREADY_SHOT");
            return "ALREADY_SHOT";
        }
        // Ateş edilen noktayı kaydet
        if (playerAttemptedShots.get(attackerId) != null) {
            playerAttemptedShots.get(attackerId).add(shotPoint);
        }


        List<Ship> targetShips = playerShips.get(targetPlayerId);
        if (targetShips == null) {
            System.err.println("Oyun Mantığı: Hedef oyuncu " + targetPlayerId + " için gemi listesi null!");
            return "ERROR_TARGET_PLAYER_NOT_INITIALIZED"; // Hata durumu
        }
         if (targetShips.isEmpty()) {
            System.out.println("Oyun Mantığı: Hedef oyuncu " + targetPlayerId + " için gemi bulunamadı (liste boş). Sonuç: MISS");
            return "MISS";
        }


        for (Ship ship : targetShips) {
            if (ship.hasCoordinate(x, y)) {
                boolean hitRegistered = ship.registerHit(x, y);

                if (hitRegistered) {
                    if (ship.isSunk()) {
                        System.out.println("GameLogic: Gemi battı! Boyut: " + ship.getSize());
                        if (checkGameOver(targetPlayerId)) {
                            System.out.println("GameLogic: Oyun bitti! Kazanan: " + attackerId);
                            return "GAME_OVER " + attackerId; // Kazananın ID'sini de gönder
                        }
                        return "SUNK " + ship.getSize(); // Batan geminin boyutu
                    }
                    System.out.println("GameLogic: Vuruş! (" + x + "," + y + ")");
                    return "HIT";
                } else {
                    System.out.println("GameLogic: (" + x + "," + y + ") koordinatı gemiye ait ama zaten vurulmuş (registerHit false). Sonuç: ALREADY_SHOT");
                    return "ALREADY_SHOT";
                }
            }
        }

        System.out.println("GameLogic: Iska! (" + x + "," + y + ")");
        return "MISS";
    }

    // Bir oyuncunun tüm gemilerinin batıp batmadığını kontrol eder
    private boolean checkGameOver(int playerId) {
        List<Ship> ships = playerShips.get(playerId);
        if (ships == null || ships.isEmpty()) {
            System.out.println("checkGameOver: Oyuncu " + playerId + " için gemi listesi null veya boş.");
            return false;
        }

        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false; // Hala batmamış bir gemi var
            }
        }
        System.out.println("checkGameOver: Oyuncu " + playerId + " için tüm gemiler battı.");
        return true; // Tüm gemiler battı
    }

    // Gemi yerleştirme kurallarının (çakışma, tahta dışı vb.) kontrolü
    public boolean isValidPlacement(int playerId, List<Ship> newShips) {
        final int[] expectedSizesArray = {5, 4, 3, 2, 2};
        if (newShips == null || newShips.size() != expectedSizesArray.length) {
            System.err.println("Geçersiz yerleşim: Gemi sayısı yanlış. Beklenen: " + expectedSizesArray.length + ", Gelen: " + (newShips == null ? "null" : newShips.size()));
            return false;
        }

        List<Integer> currentShipSizes = new ArrayList<>();
        for (Ship s : newShips) {
            if (s == null || s.getSize() <= 0) {
                 System.err.println("Geçersiz yerleşim: Gemilerden biri null veya boyutu geçersiz.");
                return false;
            }
            // Geminin koordinatlarının da null veya boş olmaması gerekir.
            if (s.getCoordinates() == null || s.getCoordinates().isEmpty()){
                System.err.println("Geçersiz yerleşim: Geminin ("+ s.getSize()+" boyutlu) koordinatları boş/null.");
                return false;
            }
            currentShipSizes.add(s.getSize());
        }
        Collections.sort(currentShipSizes, Collections.reverseOrder());
        List<Integer> expectedSizesList = new ArrayList<>();
        for (int size : expectedSizesArray) {
            expectedSizesList.add(size);
        }
        Collections.sort(expectedSizesList, Collections.reverseOrder());

        if (!currentShipSizes.equals(expectedSizesList)) {
            System.err.println("Geçersiz yerleşim: Gemi boyutları eşleşmiyor. Beklenen: " + expectedSizesList + ", Gelen: " + currentShipSizes);
            return false;
        }

        List<Point> allOccupiedPoints = new ArrayList<>();
        for (Ship ship : newShips) {
            // Gemi koordinatları null olamaz (yukarıda kontrol edildi)
            for (Point p : ship.getCoordinates()) {
                if (p == null) { // Ekstra null kontrolü
                    System.err.println("Geçersiz yerleşim: Gemi koordinatlarından biri null. Gemi: " + ship.getSize());
                    return false;
                }
                if (p.x < 0 || p.x >= GRID_SIZE || p.y < 0 || p.y >= GRID_SIZE) {
                    System.err.println("Geçersiz yerleşim: Gemi tahta dışında. Gemi: " + ship.getSize() + ", Koordinat: " + p);
                    return false;
                }
                if (allOccupiedPoints.contains(p)) {
                     System.err.println("Geçersiz yerleşim: Gemiler çakışıyor. Koordinat: " + p);
                    return false;
                }
                allOccupiedPoints.add(p);
            }
            // Geminin kendi içindeki koordinatlarının geçerliliği (örn: bitişiklik, doğru uzunluk)
            // Ship sınıfı oluşturulurken veya istemci tarafında kontrol edilmeli.
            // Burada basit bir kontrol: Koordinat sayısı gemi boyutuna eşit mi?
            if (ship.getCoordinates().size() != ship.getSize()){
                System.err.println("Geçersiz yerleşim: Geminin ("+ship.getSize()+" boyutlu) koordinat sayısı ("+ship.getCoordinates().size()+") boyutuna eşit değil.");
                return false;
            }
        }
        return true;
    }
}
