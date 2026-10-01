package edu.deploylab;

public abstract class DeployLabException extends RuntimeException {
    private final int status;
    protected DeployLabException(int status,String message){super(message);this.status=status;}
    public int status(){return status;}
}
