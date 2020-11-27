package org.liquidengine.game.map.roguegen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ShchAlexander on 30.04.2017.
 */
public class Main {
    public static void main(String[] args) {
        Generator           generator  = new DefaultGenerator();
        Map<String, Object> parameters = new HashMap<>();

        int roomNumber    = 17;
        int minRoomHeight = 7;
        int maxRoomHeight = 13;
        int minRoomWidth  = 19;
        int maxRoomWidth  = 23;

        parameters.put("roomNumber", roomNumber);
        parameters.put("minRoomHeight", minRoomHeight);
        parameters.put("maxRoomHeight", maxRoomHeight);
        parameters.put("minRoomWidth", minRoomWidth);
        parameters.put("maxRoomWidth", maxRoomWidth);
        int[][] map = generator.generateMap(parameters);

        for (int y = 0; y < map[0].length; y++) {
            for (int x = 0; x < map.length; x++) {
                if (map[x][y] == 0)
                    System.out.print("#");
                else
                    System.out.print(" ");
            }
            System.out.println();
        }
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
