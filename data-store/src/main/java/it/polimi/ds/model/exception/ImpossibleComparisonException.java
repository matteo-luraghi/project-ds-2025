package it.polimi.ds.model.exception;
/**
 * Exception thrown when trying comparing TimeVector with different size
 */
public class ImpossibleComparisonException  extends Exception{

    public ImpossibleComparisonException(){
        super("the given TimeVectors have different size, impossible to check Happen Before Relation");
    }

}
