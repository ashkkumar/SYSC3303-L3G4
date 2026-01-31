package FireFightingDroneSwarm.FireIncidentSubsystem;

import java.util.Arrays;

/**
 * This class represents a Zone including its ID, and start and end coordinates
 */
public class Zone {
    private int ID;
    private int[] startCoordinates;
    private int[] endCoordinates;

    /**
     * Constructor for Zone object, instantiates Zone with ID, and proper coordinates
     * @param ID int representing Zone ID
     * @param startCoordinates integer array of size 2 with [x,y] start coordinates
     * @param endCoordinates integer array of size 2 with [x,y] end coordinates
     */
    public Zone(int ID, int[] startCoordinates, int[] endCoordinates) {
        this.ID = ID;
        this.startCoordinates = startCoordinates;
        this.endCoordinates = endCoordinates;
    }

    /**
     * Getter for the zone's ID
     * @return int representing zone number/id
     */
    public int getID() {
        return ID;
    }

    /**
     * Getter for start coordinates of the zone
     * @return integer array of size 2 with coordinates as [x,y]
     */
    public int[] getStartCoordinates() {
        return startCoordinates;
    }

    /**
     * Getter for end coordinates of the zone
     * @return integer array of size 2 with coordinates as [x,y]
     */
    public int[] getEndCoordinates() {
        return endCoordinates;
    }

    /**
     * toString() override to test system zone reading
     * @return string representation of a zone
     */
    public String toString() {
        return "Zone ID " + this.ID + " " + Arrays.toString(startCoordinates) + " " + Arrays.toString(endCoordinates);
    }
}

