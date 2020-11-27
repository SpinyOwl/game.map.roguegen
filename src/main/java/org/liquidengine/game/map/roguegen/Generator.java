package org.liquidengine.game.map.roguegen;

import java.util.Map;

/**
 * Created by ShchAlexander on 30.04.2017.
 */
public interface Generator {

    int[][] generateMap(Map<String, Object> parameters);
}
