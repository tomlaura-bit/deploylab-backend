package edu.deploylab;
public class ExpiredAssignmentException extends DeployLabException {
    public ExpiredAssignmentException(){super(409,"La fecha límite ha vencido");}
}
