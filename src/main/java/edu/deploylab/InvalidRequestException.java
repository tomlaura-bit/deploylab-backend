package edu.deploylab;
public class InvalidRequestException extends DeployLabException {
    public InvalidRequestException(String message){super(400,message);}
}
