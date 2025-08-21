package it.polimi.ds.model.exception;
/**
 * Exception thrown when trying to create a TimeVector of size less than 2
 */
public class InvalidDimensionException extends Exception {
    public InvalidDimensionException(){
        super("Time Vector must have size at least equal to 2");
    }
}
