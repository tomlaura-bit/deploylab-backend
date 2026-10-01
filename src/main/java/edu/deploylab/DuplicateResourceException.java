package edu.deploylab;
public class DuplicateResourceException extends DeployLabException {
    public DuplicateResourceException(String message){super(409,message);}
}
