package it.polimi.ds.model.exception;

public class InvalidDimensionException extends Exception {
    public InvalidDimensionException(){
        super("Time Vector must have size at least equal to 2");
    }
}
