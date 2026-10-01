package edu.deploylab;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="app.mail-enabled=true") @ActiveProfiles("test")
class ExternalIntegrationsTest {
    @MockitoBean S3Storage storage;
    @MockitoBean JavaMailSender mail;
    @Autowired MaterialController materials;
    @Autowired CatalogService catalog;
    @Autowired AuthService auth;
    @Autowired JobService jobs;
    @Autowired JobWorker worker;
    @Autowired Db db;
    AuthService.Actor teacher() {
        return auth.login(new AuthService.Login("teacher@deploylab.test","TestingOnly123!")).user();
    }
    @Test void uploadedMaterialAppearsOnlyAfterVerification() {
        var t=teacher();UUID w=catalog.create(t,new CatalogService.CreateWorkshop("Material","Description","Topic","BEGINNER",List.of("API_URL")));
        when(storage.uploadUrl(anyString())).thenReturn("https://storage.example/upload");
        when(storage.downloadUrl(anyString())).thenReturn("https://storage.example/download");
        @SuppressWarnings("unchecked") var result=(Map<String,Object>)materials.upload(w,t,new MaterialController.Upload("guia.pdf"));
        UUID id=(UUID)result.get("id");
        assertThat((List<?>)materials.list(w)).isEmpty();
        assertThatThrownBy(()->materials.download(w,id)).isInstanceOf(ResourceNotFoundException.class);
        materials.confirm(w,id,t);
        verify(storage).verifyUpload(contains("/"+id+"/guia.pdf"));
        assertThat((List<?>)materials.list(w)).hasSize(1);
        assertThat(materials.download(w,id).toString()).contains("https://storage.example/download");
    }
    @Test void rejectedUploadRemainsPending() {
        var t=teacher();UUID w=catalog.create(t,new CatalogService.CreateWorkshop("Invalid file","Description","Topic","BEGINNER",List.of("API_URL")));
        when(storage.uploadUrl(anyString())).thenReturn("https://storage.example/upload");
        @SuppressWarnings("unchecked") var result=(Map<String,Object>)materials.upload(w,t,new MaterialController.Upload("guia.pdf"));
        doThrow(new ApiException(400,"Archivo demasiado grande")).when(storage).verifyUpload(anyString());
        assertThatThrownBy(()->materials.confirm(w,(UUID)result.get("id"),t)).isInstanceOf(ApiException.class);
        assertThat((List<?>)materials.list(w)).isEmpty();
    }
    @Test void successfulMailMarksJobSucceeded() throws Exception {
        var t=teacher();UUID job=jobs.enqueue(t.id(),"EMAIL","Nueva asignación");
        var message=new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        worker.tick();
        assertThat(db.one("SELECT state FROM background_job WHERE id=?",job).get("state")).isEqualTo("SUCCEEDED");
        verify(mail,atLeastOnce()).send(message);
        assertThat(message.getContentType()).containsIgnoringCase("text/html");
    }
    @Test void anotherInstructorCannotUploadToAnExistingWorkshop() {
        var t=teacher();UUID w=catalog.create(t,new CatalogService.CreateWorkshop("Private owner","Description","Topic","BEGINNER",List.of("API_URL")));
        var other=new AuthService.Actor(UUID.randomUUID(),"Other","other@example.test","INSTRUCTOR");
        assertThatThrownBy(()->materials.upload(w,other,new MaterialController.Upload("guia.pdf")))
            .isInstanceOf(ForbiddenOperationException.class).hasMessageContaining("permiso");
        verifyNoInteractions(storage);
    }
}
