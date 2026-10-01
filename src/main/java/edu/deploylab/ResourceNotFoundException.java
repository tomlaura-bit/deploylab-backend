package edu.deploylab;
public class ResourceNotFoundException extends DeployLabException {
    public ResourceNotFoundException(){super(404,"Recurso no encontrado");}
    public ResourceNotFoundException(String message){super(404,message);}
}
