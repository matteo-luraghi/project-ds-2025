package it.polimi.ds.model.exception;

/**
 * Exception thrown when triyng to initialize a TimeVector with an array of different size
 */
public class InvalidInitValuesException extends Exception {

    public InvalidInitValuesException(){
        super("The initialization values are icompatible with the Time Vector dimension");
    }

}
