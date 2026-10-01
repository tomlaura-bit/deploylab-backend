package edu.deploylab;
public class InvalidOperationException extends DeployLabException {
    public InvalidOperationException(String message){super(409,message);}
}
