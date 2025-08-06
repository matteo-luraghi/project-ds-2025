package it.polimi.ds.model.exception;

public class ImpossibleComparisonException  extends Exception{

    public ImpossibleComparisonException(){
        super("the given TimeVectors have different size, impossible to check Happen Before Relation");
    }

}
