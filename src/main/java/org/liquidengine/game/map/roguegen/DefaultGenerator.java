package org.liquidengine.game.map.roguegen;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.util.FastMath;

import java.util.*;

/**
 * Created by ShchAlexander on 30.04.2017.
 */
public class DefaultGenerator implements Generator {

    public static final int    MIN_SPACE_BETWEEN_ROOMS        = 2;
    public static final int    ADDITIONAL_SPACE_BETWEEN_ROOMS = 3;
    private final       long   seed                           =
//            1493592428116l;
            System.currentTimeMillis();
    private             Random random                         = new Random(seed);

    {
        System.out.println(seed);
    }

    @Override
    public int[][] generateMap(Map<String, Object> parameters) {
        int roomNumber    = (int) parameters.get("roomNumber");
        int minRoomHeight = (int) parameters.get("minRoomHeight");
        int maxRoomHeight = (int) parameters.get("maxRoomHeight");
        int minRoomWidth  = (int) parameters.get("minRoomWidth");
        int maxRoomWidth  = (int) parameters.get("maxRoomWidth");
        return generate(
                roomNumber,
                minRoomHeight,
                maxRoomHeight,
                minRoomWidth,
                maxRoomWidth
        );
    }

    public int[][] generate(
            int roomNumber,
            int minRoomHeight,
            int maxRoomHeight,
            int minRoomWidth,
            int maxRoomWidth) {
        int        map[][]  = null;
        List<Room> rooms    = new ArrayList<>();
        List<Room> coridors = new ArrayList<>();
        generateRooms(roomNumber, minRoomHeight, maxRoomHeight, minRoomWidth, maxRoomWidth, random, rooms);

        int floors = (int) Math.sqrt(roomNumber);


        UnivariateIntegrator integrator          = new SimpsonIntegrator();
        UnivariateFunction   univariateFunction  = x -> (roomNumber * 1d / FastMath.PI * (FastMath.cos(x) + 1));
        int[]                sortedRoomsPerFloor = associateRoomCountToFloors(roomNumber, floors, integrator, univariateFunction);

        List<List<Room>> roomsOnFloors = new ArrayList<>();
        for (int i = 0; i < floors; i++) {
            ArrayList<Room> floorRooms = new ArrayList<>();
            for (int j = 0; j < sortedRoomsPerFloor[i]; j++) {
                int size  = rooms.size();
                int index = random.nextInt(size);
                floorRooms.add(rooms.remove(index));
            }
            roomsOnFloors.add(floorRooms);
        }
        Collections.reverse(roomsOnFloors);

        int w     = 0;
        int h     = 0;
        int prevY = 1;
        for (int i = 0; i < floors; i++) {
            int        maxY       = 0;
            int        prevX      = 1;
            List<Room> floorRooms = roomsOnFloors.get(i);
            for (int j = 0; j < floorRooms.size(); j++) {
                Room room = floorRooms.get(j);
                room.x = prevX + random.nextInt(ADDITIONAL_SPACE_BETWEEN_ROOMS);
                room.y = prevY + random.nextInt(ADDITIONAL_SPACE_BETWEEN_ROOMS);
                prevX = room.x + room.width + MIN_SPACE_BETWEEN_ROOMS;

                if (w < prevX) w = prevX;
                int y = room.y + room.height;
                if (maxY < y) maxY = y;
            }

            prevY = maxY + MIN_SPACE_BETWEEN_ROOMS;
            if (h < prevY) h = prevY + MIN_SPACE_BETWEEN_ROOMS;
        }
        // spread rooms
        spreadRooms(floors, roomsOnFloors, w, random);

        for (int i = 0; i < floors; i++) {
            List<Room> floorRooms = roomsOnFloors.get(i);
            int        prevX      = 1;
            int        maxY       = 0;
            for (int j = 0; j < floorRooms.size(); j++) {
                Room room = floorRooms.get(j);
                prevX = room.x + room.width + 1;
                if (w < prevX) w = prevX;
                int y = room.y + room.height;
                if (maxY < y) maxY = y;
            }
            prevY = maxY + MIN_SPACE_BETWEEN_ROOMS;
            if (h < prevY) h = prevY + MIN_SPACE_BETWEEN_ROOMS;
        }

        addHorizontalLinks(random, coridors, floors, roomsOnFloors);
        addVerticalLinks(random, coridors, floors, roomsOnFloors);
//        addVerticalLinksReverse(random, coridors, floors, roomsOnFloors);

        map = new int[w][h];
        for (int i = 0; i < floors; i++) {
            List<Room> floorRooms = roomsOnFloors.get(i);
            for (int j = 0; j < floorRooms.size(); j++) {
                Room room = floorRooms.get(j);
                if (room.linkToBot.size() >= 2 || room.linkToTop.size() >= 2) {
                    System.out.println("two");
                }
                for (int x = room.x; x < room.x + room.width; x++) {
                    for (int y = room.y; y < room.y + room.height; y++) {
                        map[x][y] = 1;
                    }
                }
            }
        }
        for (Room coridor : coridors) {
            for (int x = coridor.x; x < coridor.x + coridor.width; x++) {
                for (int y = coridor.y; y < coridor.y + coridor.height; y++) {
                    map[x][y] = 1;
                }
            }
        }

        return map;
    }

    private void addVerticalLinks(Random random, List<Room> links, int floors, List<List<Room>> roomsOnFloors) {
        for (int i = 0; i < floors - 1; i++) {
            List<Room> currentFloor = roomsOnFloors.get(i);
            for (int k = 0; k < currentFloor.size(); k++) {
                Room    room      = currentFloor.get(k);
                boolean addedLink = false;

                for (int j = i + 1; j < floors; j++) {
                    List<Room> bottomFloor = roomsOnFloors.get(j);

                    for (int l = 0; l < bottomFloor.size(); l++) {
                        Room botRoom = bottomFloor.get(l);
                        if (addedLink || !room.linkToBot.isEmpty()) continue;

                        // addCoridor
                        if (room.x < botRoom.x + botRoom.width && room.x + room.width > botRoom.x) {
                            Room coridor = new Room();
                            room.linkToBot.add(coridor);
                            botRoom.linkToTop.add(coridor);

                            int leftX  = FastMath.max(room.x, botRoom.x);
                            int rightX = FastMath.min(room.x + room.width, botRoom.x + botRoom.width);

                            int bound      = rightX - leftX;
                            int boundToUse = bound;
                            if (bound > 2) boundToUse = bound - 2;

                            int added = boundToUse != 0 ? random.nextInt(boundToUse) : 0;
                            coridor.x = leftX + added + (bound > 3 ? 1 : 0);
                            coridor.width = 1;
                            coridor.y = room.y + room.height;
                            coridor.height = botRoom.y - coridor.y;

                            links.add(coridor);
                            addedLink = true;
                        }
                    }
                }
            }
        }

    }

    private void addVerticalLinksReverse(Random random, List<Room> coridors, int floors, List<List<Room>> roomsOnFloors) {
        for (int i = floors - 1; i > 1; i--) {
            List<Room> currentFloor = roomsOnFloors.get(i);
            for (int k = 0; k < currentFloor.size(); k++) {
                Room    botRoom   = currentFloor.get(k);
                boolean addedLink = false;

                for (int j = i - 1; j > 0; j--) {
                    List<Room> bottomFloor = roomsOnFloors.get(j);

                    for (int l = 0; l < bottomFloor.size(); l++) {
                        Room room = bottomFloor.get(l);
                        if (addedLink || !room.linkToBot.isEmpty() || !botRoom.linkToTop.isEmpty()) continue;

                        // addCoridor
                        if (room.x < botRoom.x + botRoom.width && room.x + room.width > botRoom.x) {
                            Room coridor = new Room();
                            room.linkToBot.add(coridor);
                            botRoom.linkToTop.add(coridor);

                            int leftX  = FastMath.max(room.x, botRoom.x);
                            int rightX = FastMath.min(room.x + room.width, botRoom.x + botRoom.width);

                            int bound      = rightX - leftX;
                            int boundToUse = bound;
                            if (bound > 2) boundToUse = bound - 2;

                            int added = boundToUse != 0 ? random.nextInt(boundToUse) : 0;
                            coridor.x = leftX + added + (bound > 3 ? 1 : 0);
                            if (coridor.x == room.x + room.width || coridor.x == botRoom.x + botRoom.width - 1) {
                                System.out.println(coridor.x);
                            }
                            coridor.width = 1;
                            coridor.y = room.y + room.height;
                            coridor.height = botRoom.y - coridor.y;

                            coridors.add(coridor);
                            addedLink = true;
                        }
                    }
                }
            }
        }

    }

    private void addHorizontalLinks(Random random, List<Room> links, int floors, List<List<Room>> roomsOnFloors) {
        for (int i = 0; i < floors; i++) {
            List<Room> floorRooms = roomsOnFloors.get(i);
            for (int j = 0; j < floorRooms.size() - 1; j++) {
                Room room      = floorRooms.get(j);
                Room rightRoom = floorRooms.get(j + 1);
                if (room.x + room.width + 2 * (MIN_SPACE_BETWEEN_ROOMS + ADDITIONAL_SPACE_BETWEEN_ROOMS) >= rightRoom.x) {
                    int topy = FastMath.max(room.y, rightRoom.y);
                    int boty = FastMath.min(room.y + room.height, rightRoom.y + rightRoom.height);

                    Room coridor = new Room();
                    if (topy == boty) {
                        coridor.x = room.x + room.width;
                        coridor.width = rightRoom.x - coridor.x;
                        coridor.y = topy;
                        coridor.height = 1;
                        links.add(coridor);
                    } else {
                        coridor.x = room.x + room.width;
                        coridor.width = rightRoom.x - coridor.x;
                        coridor.y = topy + random.nextInt(boty - topy);
                        coridor.height = 1;
                        links.add(coridor);
                    }
                }
            }
        }
    }

    private void spreadRooms(int floors, List<List<Room>> roomsOnFloors, int w, Random random) {
        for (int i = 0; i < floors; i++) {
            List<Room> floorRooms   = roomsOnFloors.get(i);
            int        spacePerRoom = (w) / floorRooms.size();
            for (int j = 0; j < floorRooms.size(); j++) {
                Room room = floorRooms.get(j);
                if (spacePerRoom - room.width != 0)
                    room.x = 1 + j * spacePerRoom + random.nextInt(spacePerRoom - room.width);
                else
                    room.x = 1 + j * spacePerRoom;
            }
        }
    }

    private int[] associateRoomCountToFloors(int roomNumber, int floors, UnivariateIntegrator integrator, UnivariateFunction univariateFunction) {
        List<Integer> roomsPerFloor = new ArrayList<>();

        int sum = 0;

        double step = FastMath.PI / floors;

        double min;
        double max;
        for (int i = 0; i < floors; i++) {
            min = i * step;
            max = min + step;
            double integrate = integrator.integrate(64, univariateFunction, min, max);
            if (i != floors - 1) {
                int roomsOnFloor = (int) integrate;
                sum += roomsOnFloor;
                roomsPerFloor.add(roomsOnFloor);
            } else {
                roomsPerFloor.add(roomNumber - sum);
            }
        }
        Collections.sort(roomsPerFloor);
        Collections.reverse(roomsPerFloor);

        int sortedRoomsPerFloor[] = new int[floors];
        int mid                   = floors / 2;

        for (int i = 0; i < floors; i++) {
            int newIndex = mid + i;
            if (newIndex >= floors) newIndex = mid - newIndex + floors - 1;
            sortedRoomsPerFloor[newIndex] = roomsPerFloor.get(i);
        }
        return sortedRoomsPerFloor;
    }

    private void generateRooms(int roomNumber, int minRoomHeight, int maxRoomHeight, int minRoomWidth, int maxRoomWidth, Random random, List<Room> rooms) {
        for (int i = 0; i < roomNumber; i++) {
            Room room = new Room();
            rooms.add(room);
            if (minRoomWidth == maxRoomWidth)
                room.width = minRoomWidth;
            else
                room.width = random.nextInt(maxRoomWidth - minRoomWidth) + minRoomWidth;
            if (minRoomHeight == maxRoomHeight)
                room.height = minRoomHeight;
            else
                room.height = random.nextInt(maxRoomHeight - minRoomHeight) + minRoomHeight;
        }
    }


    private static class Room {
        private List<Room> linkToBot = new ArrayList<>();
        private List<Room> linkToTop = new ArrayList<>();
        private int width;
        private int height;
        private int x;
        private int y;
    }
}
