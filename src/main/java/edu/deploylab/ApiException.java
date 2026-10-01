package edu.deploylab;

/** Compatibility exception for older service paths; new code uses specific subclasses. */
public class ApiException extends DeployLabException {
    public ApiException(int status,String message){super(status,message);}
    public static DeployLabException missing(){return new ResourceNotFoundException();}
    public static DeployLabException forbidden(){return new ForbiddenOperationException();}
}
