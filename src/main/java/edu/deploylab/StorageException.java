package edu.deploylab;
public class StorageException extends DeployLabException {
    public StorageException(String message){super(503,message);}
}
