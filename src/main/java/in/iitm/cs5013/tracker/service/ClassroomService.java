package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.importer.ImportData;
import org.apache.commons.csv.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;

/** Only project IDs are registered remotely. Names/titles stay in SQLite. */
public final class ClassroomService {
    public static final String DEFAULT_URL="https://cs5013-student-updates.sedateoats2.chatgpt.site";
    private final SettingsService settings;
    private final ProjectService projects;
    public ClassroomService(SettingsService settings,ProjectService projects){this.settings=settings;this.projects=projects;}
    private static String protect(String input,boolean decrypt) throws Exception {
        if(!System.getProperty("os.name").startsWith("Windows"))throw new IllegalArgumentException("Classroom connection requires Windows credential protection in this build.");
        String operation=decrypt?"$bytes=[Convert]::FromBase64String($text); [Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))"
            :"$bytes=[Text.Encoding]::UTF8.GetBytes($text); [Convert]::ToBase64String([Security.Cryptography.ProtectedData]::Protect($bytes,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))";
        var process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-Command",
            "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Security; $text=[Console]::In.ReadToEnd(); "+operation).start();
        try(var writer=process.outputWriter(StandardCharsets.UTF_8)){writer.write(input);}
        if(!process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalArgumentException("Windows credential protection timed out.");}
        String result=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
        if(process.exitValue()!=0)throw new IllegalArgumentException("Could not use Windows credential protection.");
        return result;
    }
    private String key(Properties preferences) throws Exception {
        String encrypted=preferences.getProperty("classroomKeyProtected","");
        if(encrypted.isEmpty()) {
            byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
            String key=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            preferences.setProperty("classroomKeyProtected",protect(key,false));settings.save(preferences);return key;
        }
        return protect(encrypted,true);
    }
    public static String classroomId(String key) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))).substring(0,32);
    }
    private static URI endpoint(Properties preferences,String path) {
        String base=preferences.getProperty("formServer",DEFAULT_URL).replaceAll("/+$","");
        URI uri=URI.create(base+path);
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException("Use the HTTPS form-server address.");
        return uri;
    }
    private static String send(HttpRequest request) throws Exception {
        var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build().send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new IllegalArgumentException("The form server returned "+response.statusCode()+". Check the connection and try again.");
        if(response.body().length()>5_000_000)throw new IllegalArgumentException("Too many responses; contact the administrator.");
        return response.body();
    }
    public String connect() throws Exception {
        var preferences=settings.load();String key=key(preferences);
        String ids=String.join("\n",projects.list(false).stream().map(p->p.id()).toList());
        if(ids.isBlank())throw new IllegalArgumentException("Add at least one active project first.");
        send(HttpRequest.newBuilder(endpoint(preferences,"/api/classroom")).timeout(Duration.ofSeconds(30))
            .header("Authorization","Bearer "+key).header("Content-Type","text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(ids)).build());
        String url=endpoint(preferences,"/s/"+classroomId(key)).toString();
        preferences=settings.load();preferences.setProperty("submissionUrl",url);settings.save(preferences);return url;
    }
    public Path download(Path directory) throws Exception {
        var preferences=settings.load();
        if(preferences.getProperty("classroomKeyProtected","").isEmpty())throw new IllegalArgumentException("Connect the classroom first.");
        String csv=send(HttpRequest.newBuilder(endpoint(preferences,"/api/responses")).timeout(Duration.ofSeconds(30))
            .header("Authorization","Bearer "+key(preferences)).GET().build());
        Files.createDirectories(directory);Path file=Files.createTempFile(directory,"online-responses-",".csv");
        try(var parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(new java.io.StringReader(csv));
            var writer=Files.newBufferedWriter(file);var output=new CSVPrinter(writer,CSVFormat.DEFAULT)) {
            output.printRecord(ImportData.COLUMNS);
            int count=0;
            for(var row:parser) {
                in.iitm.cs5013.tracker.model.Project project=null;
                try{project=projects.get(row.get("project_id"));}catch(IllegalArgumentException unknown){/* Show unknown IDs for review, never create them automatically. */}
                output.printRecord(row.get("project_id"),project==null?"":project.studentName(),project==null?"":project.title(),project==null?"":project.startedOn(),row.get("update_id"),row.get("update_date"),row.get("summary"),row.get("next_steps"));count++;
            }
            if(count==0)throw new IllegalArgumentException("No student responses have arrived yet.");
        }catch(Exception exception){Files.deleteIfExists(file);throw exception;}
        return file;
    }
    public void acknowledge(Path imported) throws Exception {
        var ids=new ArrayList<String>();
        try(var reader=Files.newBufferedReader(imported);var csv=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)){
            for(var row:csv)ids.add(row.get("update_id"));
        }
        var preferences=settings.load();
        send(HttpRequest.newBuilder(endpoint(preferences,"/api/responses")).timeout(Duration.ofSeconds(30))
            .header("Authorization","Bearer "+key(preferences)).method("DELETE",HttpRequest.BodyPublishers.ofString(String.join("\n",ids))).build());
    }
}
