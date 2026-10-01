package edu.deploylab;
public class InvalidCredentialsException extends DeployLabException {
    public InvalidCredentialsException(String message){super(401,message);}
}
