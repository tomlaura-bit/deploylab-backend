package edu.deploylab;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class S3Storage {
    @Value("${app.s3-bucket}") String bucket;
    @Value("${app.s3-region}") String region;
    public String uploadUrl(String key) {
        enabled();
        try(var signer=S3Presigner.builder().region(Region.of(region)).build()) {
            var req=PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf").build();
            return signer.presignPutObject(r->r.signatureDuration(Duration.ofMinutes(5)).putObjectRequest(req)).url().toString();
        } catch(SdkException e) {throw new ApiException(503,"No se pudo generar el acceso al almacenamiento");}
    }
    public void verifyUpload(String key) {
        enabled();
        try(var client=S3Client.builder().region(Region.of(region)).build()) {
            var head=client.headObject(r->r.bucket(bucket).key(key));
            if(head.contentLength()==null || head.contentLength()<1 || head.contentLength()>10*1024*1024 || !"application/pdf".equals(head.contentType()))
                throw new ApiException(400,"El material debe ser PDF y tener como máximo 10 MiB");
        } catch(NoSuchKeyException e) {throw new ApiException(409,"El archivo aún no se ha subido");}
        catch(S3Exception e) {
            if(e.statusCode()==404) throw new ApiException(409,"El archivo aún no se ha subido");
            throw new ApiException(503,"No se pudo verificar el archivo");
        } catch(SdkException e) {throw new ApiException(503,"No se pudo acceder al almacenamiento");}
    }
    public String downloadUrl(String key) {
        enabled();
        try(var signer=S3Presigner.builder().region(Region.of(region)).build()) {
            var req=GetObjectRequest.builder().bucket(bucket).key(key).responseContentDisposition("attachment").build();
            return signer.presignGetObject(r->r.signatureDuration(Duration.ofMinutes(5)).getObjectRequest(req)).url().toString();
        } catch(SdkException e) {throw new ApiException(503,"No se pudo generar el acceso al archivo");}
    }
    private void enabled() {if(bucket.isBlank()) throw new ApiException(503,"Almacenamiento S3 no configurado");}
}
