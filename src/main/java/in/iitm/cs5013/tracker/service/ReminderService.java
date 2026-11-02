package in.iitm.cs5013.tracker.service;

import java.net.*;
import java.nio.charset.StandardCharsets;

/** Creates a draft URI only; never sends email. */
public final class ReminderService {
    public URI draft(String email,String student,String projectId,String message,String formUrl) {
        if(email==null||!email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) throw new IllegalArgumentException("Enter a valid student email address.");
        String body="Dear "+student+",\n\n"+message+"\n\nProject: "+projectId
            +(formUrl.isBlank()?"":"\nSubmit your weekly update: "+formUrl)+"\n\nThank you.";
        return URI.create("mailto:"+email+"?subject="+encode("Project update reminder - "+projectId)+"&body="+encode(body));
    }
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
}
