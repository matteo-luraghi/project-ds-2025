package it.polimi.ds.model.exception;

public class InvalidInitValuesException extends Exception {

    public InvalidInitValuesException(){
        super("The initialization values are icompatible with the Time Vector dimension");
    }

}
