package edu.deploylab;
public class ForbiddenOperationException extends DeployLabException {
    public ForbiddenOperationException(){super(403,"No tienes permiso para esta operación");}
    public ForbiddenOperationException(String message){super(403,message);}
}
