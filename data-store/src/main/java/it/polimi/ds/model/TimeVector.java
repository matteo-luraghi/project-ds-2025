package it.polimi.ds.model;

import it.polimi.ds.model.exception.ImpossibleComparisonException;
import it.polimi.ds.model.exception.InvalidDimensionException;
import it.polimi.ds.model.exception.InvalidInitValuesException;
import java.io.Serializable;

/**
 * Implementation of a Vector Clock that represent the temporal advancement
 * of each server. DIMENSION represent the number of servers 
 * 
 */
public class TimeVector implements Serializable {
    private final int dimension;
    private int[] vector;

    /**
     * Dimension getter
     */
    public int getDimension(){
        return this.dimension;
    }

    /**
     * Dector getter, makes a copy of the vector and returns it
     */
    public int[] getVector(){
        int[] retVector= new int[this.dimension];
        for (int i = 0; i < retVector.length; i++) {
            retVector[i]=this.vector[i];
        }
        return retVector;
    }
    /**
     * Declaration of an array of size dimension and initializes every timestamps to 0
     * @throws IvalidDimensionException
     */
    public TimeVector(int dimension) throws InvalidDimensionException{
        if(dimension<2)
            throw new InvalidDimensionException();

        this.dimension=dimension;

        this.vector=new int[dimension];
    }
    /**
     * Declaration of an array of size dimension and initializes every timestamps with the initValues
     * @throws IvalidDimensionException
     * @throws InvalidInitValuesException
     */
    public TimeVector(int dimension, int[] initValues) throws InvalidDimensionException, InvalidInitValuesException{
        if(dimension<2)
            throw new InvalidDimensionException();
        if(initValues.length != dimension)
            throw new InvalidInitValuesException();

        this.dimension=dimension;

        this.vector=new int[dimension];

        for(int i=0;i<this.dimension;i++){
            this.vector[i] = initValues[i];
        }
    }

    /**
     * add one time step to the serverID's timestamp
     * @throws ArrayIndexOutOfBoundsException
     */
    public void increment(int serverID) throws ArrayIndexOutOfBoundsException{
        if(serverID >= this.dimension || serverID < 0)
            throw new ArrayIndexOutOfBoundsException();

        this.vector[serverID]++;
    }

    /**
     * returns true iif THIS is smaller than or equal to OTHER in all timestamps, 
     * except for the timestamp of the senderID for which THIS can be bigger than OTHER 
     * by at most one
     * @throws ImpossibleComparisonException
     */
   public boolean happensBefore(TimeVector other,int senderID) throws ImpossibleComparisonException{
        if(other.dimension != this.dimension)
            throw new ImpossibleComparisonException();

        for(int i=0; i<this.dimension;i++){
            int myVector = this.vector[i] - (i==senderID ? 1:0);
            if(myVector > other.vector[i])
                return false;
        }

        return true;

    }

    /** 
     *  returns the temporal difference THIS - OTHER
     *  maybe useless
     * @throws ImpossibleComparisonException
     */
    public int[] timeDifference(TimeVector other) throws ImpossibleComparisonException{
        if(other.dimension != this.dimension)
            throw new ImpossibleComparisonException();
        int[] timeDifference= new int[this.dimension];
        for (int i = 0; i < timeDifference.length; i++) {
            timeDifference[i]= this.vector[i] - other.vector[i];
        }
        return timeDifference;
        
    }

    /**
     *  Allign the timestamps of THIS vector with OTHER vector.
     *  for each timestamps set to the max between THIS and OTHER,
     *  except for the owner serverID
     * @throws ImpossibleComparisonException
     */
    public void merge(TimeVector other, int serverID) throws ImpossibleComparisonException{
        for (int i = 0; i < vector.length; i++) {
            if(serverID==i)
                continue;
            vector[i]= Math.max(vector[i],other.vector[i]);
        }
    }

    /**
    * Copy a time vector into a new time vector object
    *
    * @throws InvalidDimensionException
    * @throws InvalidInitValuesException
    */
    public static TimeVector copyTimeVector(TimeVector vector) 
      throws InvalidDimensionException, InvalidInitValuesException {
      return new TimeVector(vector.getDimension(), vector.getVector());
    }

}
